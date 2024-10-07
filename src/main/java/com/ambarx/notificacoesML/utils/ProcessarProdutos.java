package com.ambarx.notificacoesML.utils;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
import com.ambarx.notificacoesML.dto.item.AtributoDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import com.ambarx.notificacoesML.dto.item.VariacaoDTO;
import com.ambarx.notificacoesML.utils.operacoesNoBanco.OperacoesNoBanco;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

@Component
public class ProcessarProdutos {
	private static final Logger logger      = Logger.getLogger(ProcessarProdutos.class.getName());
	private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();
	private final OperacoesNoBanco operacoesNoBanco;

	public ProcessarProdutos(OperacoesNoBanco operacoesNoBanco) {
		this.operacoesNoBanco = operacoesNoBanco;
	}


	//region Função Para Limitar Quantidade De Caracteres
	public String limitarQuantCatacteres(String pDadoParaLimitar, int pQuantMaxima) {
		if (pDadoParaLimitar == null) {
			return null;
		}
		return pDadoParaLimitar.length() > pQuantMaxima ? pDadoParaLimitar.substring(0, pQuantMaxima) : pDadoParaLimitar;
	}
	//endregion

	//region Função Para Extrair o Valor Do Atributo `SELLER_SKU` Do Array De Atributos.
	public String capturaSellerSku(List<AtributoDTO> arrAtributos) {
		for (AtributoDTO atributo : arrAtributos) {
			if ("SELLER_SKU".equalsIgnoreCase(atributo.getId())) {
				return !atributo.getValueName().isEmpty() || !atributo.getValueName().isBlank()
					     ? limitarQuantCatacteres(atributo.getValueName().trim(), 30)
							 : atributo.getValueName();
			}
		}
		return "";
	}
	//endregion

	//region Função Auxiliar Para Atualizar Dados Na ECOM_SKU.
	public void atualizaDadosECOM_SKU(Connection pConexaoSQLServer, int vEstoque, String vEstaAtivoNoGET, String vEFullNoGET,
																		double vValorFrete, double vCustoAdicional, double vValorComissao, String vSku, boolean vEhVariacao,
																		String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws SQLException {

		if (vEstoque >= 0) {
			operacoesNoBanco.atualizaDadosEEstoqNaECOMSKU(pConexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional,
																										vValorComissao, vSku, vEhVariacao, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId);
		} else {
			operacoesNoBanco.atualizaDadosNaECOMSKU(pConexaoSQLServer, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao,
																							vSku, vEhVariacao, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId);
		}
	}
	//endregion

	//region Função Para Processar Variaçoes Do Produto Não FULL.
	public void processarVariacoesProdutoNaoFull (List<VariacaoDTO> arrVariacoes, Connection pConexaoSQLServer, String vCategoriaGET,
																								String vEFullNoGET, double vValorFrete, double vCustoAdicional, double vValorComissao,
																								String vInventoryIdGET, String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws Exception {
		boolean vEhVariacao = true;
		for (VariacaoDTO vVariacao : arrVariacoes) {
			String vIdVariacao      = vVariacao.getId();
			int 	 vEstoqueVariacao = vVariacao.getAvailableQuantity();
			String vVariacaoAtiva   = vVariacao.getAvailableQuantity() > 0 ? "S" : "N";

			//region Atualiza Dados e Estoque Na ECOM_SKU.
			try {
				if (!vCategoriaGET.isEmpty()) {
					atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoqueVariacao, vVariacaoAtiva, vEFullNoGET, vValorFrete, vCustoAdicional,
																vValorComissao, vIdVariacao, vEhVariacao, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId);
				}

			} catch (SQLException excecao) {
				loggerRobot.severe("Erro Ao Processar Variações Do Produto Não FULL: " + excecao.getMessage());
			}
			//endregion

			inventarioML(vInventoryIdGET);
		}
	}
	//endregion

	// region Função Para Processar Produto Simples FULL.
	public void processarProdutoSimplesFull (Connection pConexaoSQLServer, int vOrigemDaContaML, String vSkuDaNotificacao, String vUrlDaImagemGET, int vEstoque,
																					 int vCodID,  String vCategoriaGET, String vEFullNoGET, double vPrecoNoGET, double vValorFrete, double vCustoAdicional,
																					 double vValorComissao, String vInventoryIdGET, String vTituloGET, String vEstaAtivoNoGET, String vCatalogoGET,
																					 String vRelacionadoGET, String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws Exception {

		boolean vEhVariacao = false;
		try {
			//SKU Existe Na ML_SKU_FULL ?
			DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(pConexaoSQLServer, vSkuDaNotificacao);

			//region Atualiza Dados e EstoqueNa ECOM_SKU.
			if (!vCategoriaGET.isEmpty()) {
				atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao,
															vSkuDaNotificacao, vEhVariacao, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId);
			}
			//endregion

			if (vExiste.getVExiste() <= 0) {
				operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vOrigemDaContaML, vCodID, vSkuDaNotificacao, "0", "0", vInventoryIdGET, vTituloGET, vEstaAtivoNoGET,
																												 vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
			} else {
				operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET,
																													vRelacionadoGET, vSkuDaNotificacao, pIdentificadorCliente, userId);
			}

		} catch (SQLException excecao) {
			excecao.getCause();
		}

		inventarioML(vInventoryIdGET);

	}
	//endregion

	// region Função Para Processar Variaçoes Do Produto FULL.
	public void processaVariacoesProdutoFull(List<VariacaoDTO> arrVariacoes, Connection pConexaoSQLServer, int vOrigemDaContaML, String vSkuDaNotificacao,
																					 String vCategoriaGET, String vEFullNoGET, double vValorFrete, double vCustoAdicional, double vValorComissao,
																					 String vInventoryIdGET, String vTituloGET, String vEstaAtivoNoGET, String vCatalogoGET,
																					 String vRelacionadoGET, String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws Exception {
		boolean vEhVariacao = true;
		for (VariacaoDTO vVariacao : arrVariacoes) {
			String vIdVariacao             = vVariacao.getId();
			double vPrecoVariacao          = vVariacao.getPrice();
			int vEstoqueVariacao           = vVariacao.getAvailableQuantity();
			String vVariacaoAtiva          = vVariacao.getAvailableQuantity() > 0 ? "S" : "N";
			String vUrlImagemVariacao      = "https://http2.mlstatic.com/D_" + vVariacao.getPictureIds().get(0) + "-N.jpg";
			List<AtributoDTO> arrAtributos = vVariacao.getAttributes();
			String vSellerSKUVariacao      = capturaSellerSku(arrAtributos);

			//region Buscando CODID Na Tabela ECOM_SKU do Seller
			InfoItemsMLDTO objItensVariacaoEcomSku = operacoesNoBanco.buscaProdutovARIACAONaECOM_SKU(pConexaoSQLServer, vIdVariacao, vOrigemDaContaML);
			int vCodID = Math.max(objItensVariacaoEcomSku.getCodid(), 0);
			//endregion

			//region Atualiza Dados e Estoque Na ECOM_SKU.
			try {

				//SKU Existe Na ML_SKU_FULL ?
				DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(pConexaoSQLServer, vSkuDaNotificacao);

				//region Atualiza Dados e EstoqueNa ECOM_SKU.
				if (!vCategoriaGET.isEmpty()) {
					atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoqueVariacao, vVariacaoAtiva, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao,
																vSellerSKUVariacao, vEhVariacao, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId);
				}
				//endregion

				if (vExiste.getVExiste() <= 0) {
					operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vOrigemDaContaML, vCodID, vSkuDaNotificacao, "0", "0", vInventoryIdGET,
																													 vTituloGET, vEstaAtivoNoGET, vPrecoVariacao, vUrlImagemVariacao, vCatalogoGET, vRelacionadoGET);
				} else {
					operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoVariacao, vUrlImagemVariacao, vCatalogoGET,
																														vRelacionadoGET, vSkuDaNotificacao, pIdentificadorCliente, userId);
				}

			} catch (SQLException excecao) {
				loggerRobot.severe("Erro Ao Processar Variações Do Produto FULL: " + excecao.getMessage());
			}
			//endregion

		}

	}
	//endregion

	//region Função De Inventário Do ML (VERIFICAR SE VAI IMPLEMENTAR DEPOIS)
	public void inventarioML (String vInventoryIdGET) {
		if (vInventoryIdGET.length() > 2) {
			//ML_Inventario_Full(pOrig, vInventoryIdGET);
			logger.warning("VERIFICAR: Se Vai Implementar Metodo de Inventario Para Variacoes Depois.");
		}
	}
	//endregion

}
