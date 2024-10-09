package com.ambarx.notificacoesML.services;

//region Importações

import com.ambarx.notificacoesML.config.db.Conexao;
import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.customizedExceptions.LimiteRequisicaoMLException;
import com.ambarx.notificacoesML.customizedExceptions.NotificacaoMLException;
import com.ambarx.notificacoesML.dto.comissao.ComissaoDTO;
import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import com.ambarx.notificacoesML.dto.frete.FreteDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.item.AtributoDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.dto.item.VariacaoDTO;
import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import com.ambarx.notificacoesML.mapper.ModelMapperMapping;
import com.ambarx.notificacoesML.models.NotificacaoMLItensEntity;
import com.ambarx.notificacoesML.models.NotificacaoUserProductFamiliesEntity;
import com.ambarx.notificacoesML.models.SellerMercadoLivre;
import com.ambarx.notificacoesML.repositories.AcessoApiCadClientesRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLItensRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLUserProductsFamiliesRepository;
import com.ambarx.notificacoesML.repositories.SellerMercadoLivreRepository;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import com.ambarx.notificacoesML.utils.ProcessarProdutos;
import com.ambarx.notificacoesML.utils.RespostaAPI;
import com.ambarx.notificacoesML.utils.operacoesNoBanco.OperacoesNoBanco;
import com.ambarx.notificacoesML.utils.requisicoesml.RequisicoesMercadoLivre;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
//endregion

@AllArgsConstructor
@Service
public class NotificacaoItensMLItensServiceImpl implements NotificacaoMLItensService {
  private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();
  private static final Logger logger      = Logger.getLogger(NotificacaoItensMLItensServiceImpl.class.getName());

  //region Injeção De Dependências Necessárias.
  @Autowired
  private SellerMercadoLivreRepository sellerRepository;
  @Autowired
  private AcessoApiCadClientesRepository acessoApiCadClientesRepository;
  @Autowired
  private NotificacaoMLItensRepository notificacaoMLItensRepository;
  @Autowired
  private NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository;
	@Autowired
	private FuncoesUtils utils;
  private final Conexao vConexaoSqlServer;
	@Autowired
	private final ProcessarProdutos processarProdutos;

  private final RequisicoesMercadoLivre requisicoesMercadoLivre;
  @Autowired
  private OperacoesNoBanco operacoesNoBanco;

  //endregion

  @Scheduled(fixedRate = 20000)
  public void executaThreadDeItens() throws Exception {
		try {
			processaNotificacoesMLItems(notificacaoMLItensRepository);
			loggerRobot.info("Finaliza Tarefa Items.");
		} catch (NotificacaoMLException e) {
      loggerRobot.severe("Erro Na Execucao Da Tarefa ML Itens. -> " + e + " \nUserId: " + e.getUserId() + " \nIdentificador Do Cliente: " + e.getVIdentificadorCliente());
		}
	}

  @Scheduled(fixedDelay = 240000)
  public void executaThreadDeUserProductsFamilies() throws Exception {
		try {
			processaNotificacoesUserProductsFamilies(notificacaoMLUserProductsFamiliesRepository);
			loggerRobot.info("Finaliza Tarefa User Products Families");
		} catch (EmptyResultDataAccessException excecao) {
			logger.log(Level.WARNING, "Nenhuma Notificacao User Products Families Encontrada: " + excecao.getMessage());
		} catch (SQLException excecaoSql) {
      loggerRobot.severe("Erro De Banco de Dados: -> " + excecaoSql.getMessage());
    } catch (Exception e) {
      loggerRobot.severe("Erro Inesperado: -> " + e.getMessage());
    }
	}

	//region Função Principal De Processamento Notificação de Itens.
	@Override
	public void processaNotificacoesMLItems(NotificacaoMLItensRepository notificacaoMLItensRepository) throws Exception {
		List<NotificacaoMLItensEntity> arrNotificacoesLimitadas = notificacaoMLItensRepository.findNotificacoesPorUserIda();

		/*List<NotificacaoMLItensEntity> arrNotificacoesLimitadas = notificacaoMLItensRepository.findTopNotificacoesPorUserId(1000);*/
		List<NotificacaoMLDTO>          arrNotificacoesMLDTOS   = ModelMapperMapping.parseListObjects(arrNotificacoesLimitadas, NotificacaoMLDTO.class);

		if (arrNotificacoesMLDTOS == null || arrNotificacoesMLDTOS.isEmpty()) {
			logger.info("Nenhuma Notificacao Encontrada");
			return;
		}
		operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesMLDTOS);
		Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(arrNotificacoesMLDTOS);

		for (Map.Entry<String, List<NotificacaoMLDTO>> entrada : vNotificacoesFiltradas.entrySet()) {
			buscarDadosAcessoSeller(entrada.getKey(), entrada.getValue());
		}
		loggerRobot.info("Finaliza Tarefa Items Mercado Livre.");
	}
	//endregion

	//region Função Principal Para Processamento De User Products Families.
	@Override
	public void processaNotificacoesUserProductsFamilies(NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository) throws Exception {

		//region Buscando Todas As Notificações No Mysql.
		List<NotificacaoUserProductFamiliesEntity> vTodasNotificacoesUserProductsFamilies = notificacaoMLUserProductsFamiliesRepository.findAll();
		//endregion

		//region Transforma As Notificações Em DTO.
		List<NotificacaoMLDTO> vNotificacoesMLUserFiltradas = ModelMapperMapping.parseListObjects(vTodasNotificacoesUserProductsFamilies, NotificacaoMLDTO.class);
		//endregion

		if (!vTodasNotificacoesUserProductsFamilies.isEmpty()) {
			Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesMLUserFiltradas);

			for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
				List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual = entry.getValue();

				// Deletando todos os registros
				try {
					notificacaoMLUserProductsFamiliesRepository.deleteAll(vTodasNotificacoesUserProductsFamilies);
				} catch (Exception e) {
					loggerRobot.severe("Erro Ao Apagar Notificações User Products Families.");
					loggerRobot.info("Finaliza Tarefa.User Products Families");
				}

//        operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
//        logger.info(arrNotificacoesUsuarioAtual.size() + " Notificações User Product Families Apagadas Com sucesso.");
			}
		}
	}
	//endregion

	//region Função Para Buscar Os Dados De Acesso Do Seller.
	private void buscarDadosAcessoSeller(String userId, List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual) {
		Optional<SellerMercadoLivre> objSellerMercadoLivre = sellerRepository.findIdentificadorClienteBySellerId(userId);
		if (objSellerMercadoLivre.isEmpty()) {
			loggerRobot.warning("Seller Nao Encontrado Na Tabela Sellers Mercado Livre. Apagar Notificacoes Do Seller ");
			return;
		}
		SellerMercadoLivre objSeller = objSellerMercadoLivre.get();
		String vIdentificadorCliente = objSeller.getIdentificadorCliente();

		try {
			List<ConexaoDTO> arrAcessoCliente = acessoApiCadClientesRepository.findAllByIdentificadorCliente(vIdentificadorCliente);

			if (arrAcessoCliente == null || arrAcessoCliente.isEmpty()) {
				loggerRobot.warning("Dados de Acesso Do Seller " + vIdentificadorCliente + " Nao Encontrados. Apagar Notificacoes.");
				return;
			}
			ConexaoDTO vAcessoCliente = arrAcessoCliente.get(0); //Pega Primeiro Resultado
			conectarNoBancoDoSeller(vIdentificadorCliente, vAcessoCliente, arrNotificacoesUsuarioAtual);

		} catch (Exception e) {
			loggerRobot.severe("Erro Ao Buscar Dados de Acesso Do Seller " + vIdentificadorCliente + " ID: -> " + objSeller.getSellerId() + ". Apagar Notificacoes.\n");
		}
	}
	//endregion

	//region Função Para Estabelecer A Conexão Com O Banco Do Seller.
	private void conectarNoBancoDoSeller(String pIdentificadorCliente, ConexaoDTO pAcessoCliente, List<NotificacaoMLDTO> pArrNotificacoesUsuarioAtual) {
		try (Connection conexaoSQLServer = vConexaoSqlServer.conectar(pAcessoCliente)) {
			if (conexaoSQLServer != null) {

				if (conexaoSQLServer.isValid(2500)){
					String  vUserID        = pArrNotificacoesUsuarioAtual.get(0).getUserId();
					boolean vIgnorarGetSku = operacoesNoBanco.ignorarGetSku(conexaoSQLServer);
					boolean vOrigemAtiva   = operacoesNoBanco.buscaParamPedido(conexaoSQLServer, vUserID);

					if (vIgnorarGetSku) { return;
					}	else if (!vOrigemAtiva) {	return;
					}

					processarNotificacoes(conexaoSQLServer, pIdentificadorCliente, pArrNotificacoesUsuarioAtual);

				} else {
					loggerRobot.severe("FALHA: Conexao Com o Banco Do Seller " + pIdentificadorCliente + ", Deu TimeOut.");	}

			} else { loggerRobot.severe("FALHA: Objeto de Conexao Com o Banco Do Seller " + pIdentificadorCliente + ", Esta Nulo."); }

		} catch (UnknownHostException e) {
			loggerRobot.warning("ERRO: Um Erro Desconhecido Ao Conectar Ao Banco Do Seller " + pIdentificadorCliente + " -> " +  e.getMessage());
		} catch (SQLException | IOException e) {
			loggerRobot.severe("ERRO: Falha Ao Conectar No Banco Do Usuario " + pIdentificadorCliente + " -> " +  e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	//endregion

	//region Função Para Processar A Lista De Notificações De Cada Seller.
	private void processarNotificacoes(Connection pConexaoSQLServer, String pIdentificadorCliente, List<NotificacaoMLDTO> pArrNotificacoesUsuarioAtual) throws Exception {
		if (pArrNotificacoesUsuarioAtual == null || pArrNotificacoesUsuarioAtual.isEmpty()) {
			return;
		}
		String userId = pArrNotificacoesUsuarioAtual.get(0).getUserId();

		//region Obtem Dados Da ECOM_METODOS.
		DadosEcomMetodosDTO objDadosEcomMetodosDTO = operacoesNoBanco.buscarTokenTemp(pConexaoSQLServer, userId);
		String        			vTokenTempSeller			 = objDadosEcomMetodosDTO.getTokenTemp();
		int           			vOrigemDaContaML			 = objDadosEcomMetodosDTO.getOrigem();
		LocalDateTime 			vDataTokenExpira			 = objDadosEcomMetodosDTO.getTokenExpira().minusMinutes(15); //Pega o tokenExpira - 15min.
		LocalDateTime 			vDataEHoraAtual				 = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
		//endregion

		if (vTokenTempSeller.isEmpty()) {
			loggerRobot.warning("ATENCAO: Token Do Seller: -> " + pIdentificadorCliente + " Nao Encontrado.");
			return;
		}

		if (vDataEHoraAtual.isAfter(vDataTokenExpira) || vDataEHoraAtual.isEqual(vDataTokenExpira)) {
			loggerRobot.warning("ATENCAO: Token do Seller -> " + pIdentificadorCliente + " Expirado.");
			return;
		}

		for (NotificacaoMLDTO objNotificacao : pArrNotificacoesUsuarioAtual) {
			String vResourceNotificacao = objNotificacao.getResource();
			String vSkuDaNotificacao	  = utils.extrairSkuMLDasNotificacoes(vResourceNotificacao);

			if (vSkuDaNotificacao == null) {
				logger.info("SKU Nao Encontrado Na Notificacao: -> " + objNotificacao.getId());
				continue;
			}

			try {
				//region Fazendo a Requisição de Itens no Mercado Livre.
				RespostaAPI<ItemDTO> objRespostaItems = RequisicoesMercadoLivre.fazerRequisicaoGetItem(vSkuDaNotificacao, vTokenTempSeller, pIdentificadorCliente, "Items");
				ItemDTO objRespostaAPIItems = objRespostaItems.getCorpoResposta();

				//region Se Não Obteve Resposta Da API.
				if (objRespostaAPIItems == null) {
					loggerRobot.severe("API Nao Retornou Resultado. Proxima Notificacao.");
					continue;
				}
				//endregion

				//endregion

				//region Pegando Dados Do JSON Response
				int		 vStatusCode 		 	 = objRespostaItems.getStatusCode().value();
				String vSellerSkuGET 	 	 = "";
				String vTituloGET 	 	 	 = processarProdutos.limitarQuantCatacteres(objRespostaAPIItems.getTitle(), 150); //Captura o Título e limita à 150 Caracteres
				String vCategoriaGET 	 	 = objRespostaAPIItems.getCategoryId();
				int 	 vEstoque 				 = objRespostaAPIItems.getAvailableQuantity();
				String vInventoryIdGET 	 = objRespostaAPIItems.getInventoryId() == null || objRespostaAPIItems.getInventoryId().isBlank() ? "0" : objRespostaAPIItems.getInventoryId();
				String vLinkAnuncioGET 	 = objRespostaAPIItems.getPermalink();
				String vTipoDeAnuncioGET = objRespostaAPIItems.getListingTypeId();
				double vPrecoNoGET 			 = objRespostaAPIItems.getBasePrice();
				String vStatusNoGet 		 = objRespostaAPIItems.getStatus();

				//region Verfica Se Existe Substatus E Concatena Com Status.
				List<String> arrSubstatus = objRespostaAPIItems.getSubStatus();
				if (arrSubstatus != null && ! arrSubstatus.isEmpty()) {
					StringBuilder statusCompleto = new StringBuilder(vStatusNoGet).append(" - ");
					for (String substatus : arrSubstatus) {
						statusCompleto.append(substatus).append(" - ");
					}
					vStatusNoGet = statusCompleto.substring(0, statusCompleto.length() - 3);
				}
				//endregion

				String vUrlDaImagemGET = objRespostaAPIItems.getThumbnail();
				String vEstaAtivoNoGET = objRespostaAPIItems.getStatus().equalsIgnoreCase("active") ? "S" : "N";
				String vEFullNoGET 		 = objRespostaAPIItems.getShipping().getLogisticType().equalsIgnoreCase("fulfillment") ? "S" : "N";
				String vSupermercado 	 = objRespostaAPIItems.getTags().contains("supermarket_eligible") ? "S" : "N";
				String vCatalogoGET 	 = objRespostaAPIItems.isCatalogListing() ? "S" : "N";
				String vRelacionadoGET = objRespostaAPIItems.getItemRelations() != null && ! objRespostaAPIItems.getItemRelations().isEmpty() ? "S" : "N";
				//endregion

				//region GET da Comissão No Mercado Livre e Custo Adicional.
				RespostaAPI<ComissaoDTO> objRespostaComissao = RequisicoesMercadoLivre.fazerRequisicaoGetComissaoML(vTipoDeAnuncioGET, vPrecoNoGET, vCategoriaGET, vTokenTempSeller, pIdentificadorCliente, "Comissao");
				double vValorComissao = objRespostaComissao.getStatusCode() == HttpStatus.OK && objRespostaComissao.getCorpoResposta() != null
						? objRespostaComissao.getCorpoResposta().getSaleFeeDetails().getPercentageFee()
						: 0.00;
				double vCustoAdicional = utils.calculaCustoAdicional(vValorComissao, vPrecoNoGET, vSupermercado);
				//endregion

				//region Pegando Informações Na Tabela ECOM_SKU do Seller
				InfoItemsMLDTO 				 objProdutoNaEcomSku = null;
				boolean 			 				 vProdVinculado  	   = false;
				String  			 				 vEFulNoDB 		 	 		 = "";
				int		  			 				 vCodID 				 		 = 0;
				ArrayList<VariacaoDTO> arrVariacoes   		 = objRespostaAPIItems.getVariations();
				List<AtributoDTO> 		 arrAtributos 			 = objRespostaAPIItems.getAttributes();
				String            		 vSellerSKUVariac	   = processarProdutos.capturaSellerSku(arrAtributos);

				try {
					objProdutoNaEcomSku = operacoesNoBanco.buscaProdutoNaECOM_SKU(pConexaoSQLServer, vSkuDaNotificacao, vOrigemDaContaML);
					if (objProdutoNaEcomSku != null) {
						//Se Tem Informações Na ECOM_SKU O Produto Existe Na Tabela, Logo, vExiste é True e o Produto Está Vinculado.
						vProdVinculado = true;
						vEFulNoDB			 = objProdutoNaEcomSku.getEFulfillment();
						vCodID 				 = Math.max(objProdutoNaEcomSku.getCodid(), 0);
					}
				} catch (SQLException e) {
					loggerRobot.severe("Erro Ao Buscar Produto Na ECOM_SKU: -> " + e.getMessage());
					return;
				}

				//endregion

				//region Se é Full No DB e Não É Full No GET De Items, Insere o CODID Na Tabela(ESTOQUE_MKTP) .
				if (vEFulNoDB.equalsIgnoreCase("S") && vEFullNoGET.equalsIgnoreCase("N")) {
					if (vCodID > 0) {
						operacoesNoBanco.inserirSkuIDNaESTOQUE_MKTP(pConexaoSQLServer, vCodID);
					}
				}
				//endregion

				//region GET do Valor Do Frete No Mercado Livre (Comentado Muitos Erros 429).
				/*RespostaAPI<FreteDTO> objRespostaFrete = RequisicoesMercadoLivre.fazerRequisicaoGetFrete(userId, vSkuDaNotificacao, vTokenTempSeller, pIdentificadorCliente, "Custo_Frete");
				double vValorFrete = objRespostaFrete.getStatusCode() == HttpStatus.OK && objRespostaFrete.getCorpoResposta() != null
						? objRespostaFrete.getCorpoResposta().getCoverage().getAllCountry().getListCost()
						: 0.00;*/
				//endregion

				//region Verificando Vínculo
				if (vProdVinculado) {

					//region Produto Vinculado.

				/*	//Deleta da tabela ECOM_SKU_SEMVINCULO
					operacoesNoBanco.deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(pConexaoSQLServer, vSkuDaNotificacao);*/

					if (!vEFullNoGET.equalsIgnoreCase("S")) {
						//region Se Não For Full No GET.
						if (arrVariacoes.isEmpty()) {
							//region Produto Sem Variação.

							//region Atualiza Dados e EstoqueNa ECOM_SKU.
							if (! vCategoriaGET.isEmpty()) {
								try {
									processarProdutos.atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, /*vValorFrete,*/ vCustoAdicional, vValorComissao,
																													vSkuDaNotificacao, false, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId
									);
								} catch (SQLException excecao) {
									excecao.getCause();
								}
							}
							//endregion
							processarProdutos.inventarioML(vInventoryIdGET);

							// endregion
						} else {
							//region Produto Com Variação.
							processarProdutos.processarVariacoesProdutoNaoFull(arrVariacoes, pConexaoSQLServer, vCategoriaGET, vEFullNoGET, /*vValorFrete,*/ vCustoAdicional, vValorComissao,
																																 vInventoryIdGET, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId, vOrigemDaContaML
							);
							//endregion
						}
						//endregion
					} else {
						//region  Se É Full No GET.
						if (arrVariacoes.isEmpty()) {
							//region Sem Variação.
							processarProdutos.processarProdutoSimplesFull(pConexaoSQLServer, vOrigemDaContaML, vSkuDaNotificacao, vUrlDaImagemGET, vEstoque, vCodID,
																														vCategoriaGET, vEFullNoGET, vPrecoNoGET, /*vValorFrete,*/ vCustoAdicional, vValorComissao, vInventoryIdGET,
																														vTituloGET, vEstaAtivoNoGET, vCatalogoGET, vRelacionadoGET, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId
							);

							// endregion
						} else {
							//region Com Variação.
							processarProdutos.processaVariacoesProdutoFull(arrVariacoes, pConexaoSQLServer, vOrigemDaContaML, vSkuDaNotificacao, vCategoriaGET,
																														 vEFullNoGET, /*vValorFrete,*/ vCustoAdicional, vValorComissao, vInventoryIdGET, vTituloGET,
																														 vEstaAtivoNoGET, vCatalogoGET, vRelacionadoGET, vStatusNoGet, vStatusCode, pIdentificadorCliente, userId
																														);
							//endregion
						}
						//endregion
					}
					//endregion

				} else {
					//Deleta da tabela ECOM_SKU_SEMVINCULO
					operacoesNoBanco.deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(pConexaoSQLServer, vSkuDaNotificacao);
					//region Produto Não Vinculado.
					if (StringUtils.hasText(vSellerSkuGET)) {
						try {
							vCodID = operacoesNoBanco.buscaCodidNaTabelaMateriais(pConexaoSQLServer, vSellerSkuGET);
						} catch (SQLException e) {
							loggerRobot.severe("Erro Ao Buscar CODID Na Tabela MATERIAIS: -> " + e.getMessage());
						}
					}

					//Produto Simples
					if (arrVariacoes.isEmpty()) {
						operacoesNoBanco.inserirProdutoNaTabelaEcomSkuSemVinculo(
								pConexaoSQLServer, vOrigemDaContaML, vSkuDaNotificacao, vSellerSkuGET, vTituloGET, vPrecoNoGET, vLinkAnuncioGET, vUrlDaImagemGET, vCodID,
								objRespostaAPIItems.getVariations().size()
						);
					}
					//Variações
					else {
						for (VariacaoDTO vVariacao : arrVariacoes) {
							String vIdVariacao    = vVariacao.getId();
							double vVariacaoPreco = vVariacao.getPrice();
							String vUrlImagem     = "https://http2.mlstatic.com/D_" + vVariacao.getPictureIds().get(0) + "-N.jpg";

							if (!vSellerSKUVariac.isEmpty()) {
								try {
									vCodID = operacoesNoBanco.buscaCodidNaTabelaMateriais(pConexaoSQLServer, vSellerSKUVariac);
								} catch (SQLException excecao) {
									loggerRobot.severe("Erro Ao Buscar CODID Da Variacao : -> " + excecao.getMessage() + " Na Tabela MATERIAIS.");
								}
							}

							operacoesNoBanco.inserirVariacNaTabelaEcomSkuSemVinculo(
									pConexaoSQLServer, vOrigemDaContaML, vSkuDaNotificacao, vIdVariacao, vSellerSKUVariac,
									vTituloGET, vVariacaoPreco, vLinkAnuncioGET, vUrlImagem, vCodID
							);
						}

					}
					//endregion

				}
				//endregion

			} catch (LimiteRequisicaoMLException limiteRequisicaoMLException) {
				loggerRobot.severe("Recebido Status Code 429 Da API De " + limiteRequisicaoMLException.getvApi() + " Para o Seller: " + limiteRequisicaoMLException.getvIdentificadorCliente());
				return;
			}
		}
  }
	//endregion

}