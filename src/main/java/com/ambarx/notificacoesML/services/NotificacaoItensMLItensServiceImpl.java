package com.ambarx.notificacoesML.services;

//region Importações
import com.ambarx.notificacoesML.config.db.Conexao;
import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.customizedExceptions.NotificacaoMLException;
import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
//import com.ambarx.notificacoesML.dto.infodobanco.SellerMercadoLivreDTO;
import com.ambarx.notificacoesML.dto.item.AtributoDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.dto.item.VariacaoDTO;
import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
//import com.ambarx.notificacoesML.httpclients.MercadoLivreHttpClient;
import com.ambarx.notificacoesML.mapper.ModelMapperMapping;
import com.ambarx.notificacoesML.models.NotificacaoMLItensEntity;
import com.ambarx.notificacoesML.models.NotificacaoUserProductFamiliesEntity;
import com.ambarx.notificacoesML.models.SellerMercadoLivre;
import com.ambarx.notificacoesML.repositories.AcessoApiCadClientesRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLItensRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLUserProductsFamiliesRepository;
import com.ambarx.notificacoesML.repositories.SellerMercadoLivreRepository;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import com.ambarx.notificacoesML.utils.operacoesNoBanco.OperacoesNoBanco;
import com.ambarx.notificacoesML.utils.requisicoesml.RequisicoesMercadoLivre;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
//import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
  private final FuncoesUtils utils  = new FuncoesUtils();
  private final Conexao vConexaoSqlServer;

  private final RequisicoesMercadoLivre requisicoesMercadoLivre;
  @Autowired
  private OperacoesNoBanco operacoesNoBanco;

  //endregion

  @Scheduled(fixedRate = 10000)
  public void executaThreadDeItens() throws Exception {
		try {
      loggerRobot.info("Iniciando Processamento Notificações ML Itens.");
			processaNotificacoesMLItems(notificacaoMLItensRepository);
		} catch (NotificacaoMLException e) {
      loggerRobot.severe("Erro Na Execução Da Tarefa ML Itens. -> " + e + " \nUserId: " + e.getUserId() + " \nIdentificador Do Cliente: " + e.getVIdentificadorCliente());
      logger.log(Level.SEVERE, "Stack Trace Detalhado. -> " , e);
		}
	}

  @Scheduled(fixedRate = 60000)
  public void executaThreadDeUserProductsFamilies() throws Exception {
		try {
      loggerRobot.info("Iniciou Processamento De Notificações De User Products Families.");
			processaNotificacoesUserProductsFamilies(notificacaoMLUserProductsFamiliesRepository);
			loggerRobot.info("Finaliza Tarefa.User Products Families");
		} catch (EmptyResultDataAccessException excecao) {
			logger.log(Level.WARNING, "Nenhuma Notificação Encontrada: " + excecao.getMessage());
		} catch (SQLException excecaoSql) {
      loggerRobot.severe("Erro De Banco de Dados: -> " + excecaoSql.getMessage());
    } catch (Exception e) {
      loggerRobot.severe("Erro Inesperado: -> " + e.getMessage());
    }
	}

	//region Função Principal De Processamento Notificação de Itens.
	@Override
  public void processaNotificacoesMLItems(NotificacaoMLItensRepository notificacaoMLItensRepository) throws Exception {
		List<NotificacaoMLItensEntity> vNotificacoesLimitadas = notificacaoMLItensRepository.findNotificacoesPorUserIda();

		/*List<NotificacaoMLItensEntity> vNotificacoesLimitadas = notificacaoMLItensRepository.findTopNotificacoesPorUserId(1000);*/
		List<NotificacaoMLDTO>          arrNotificacoesMLDTOS = ModelMapperMapping.parseListObjects(vNotificacoesLimitadas, NotificacaoMLDTO.class);

		if (arrNotificacoesMLDTOS == null || arrNotificacoesMLDTOS.isEmpty()) {
			logger.info("Nenhuma Notificação Encontrada");
			return;
		}

		Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(arrNotificacoesMLDTOS);

		for (Map.Entry<String, List<NotificacaoMLDTO>> entrada : vNotificacoesFiltradas.entrySet()) {
			buscarDadosAcessoSeller(entrada.getKey(), entrada.getValue());
		}
		loggerRobot.info("Finaliza Tarefa Items Mercado Livre.");
	}
	//endregion

	//region Função Principal Paa Processamento DeUser Products Families.
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
			loggerRobot.warning("Seller Não Encontrado Na Tabela Sellers Mercado Livre. Só Apagar Notificações Do Seller ");
			return;
		}
		SellerMercadoLivre objSeller = objSellerMercadoLivre.get();
		String vIdentificadorCliente = objSeller.getIdentificadorCliente();

		try {
			List<ConexaoDTO> arrAcessoCliente = acessoApiCadClientesRepository.findAllByIdentificadorCliente(vIdentificadorCliente);

			if (arrAcessoCliente.isEmpty()) {
				loggerRobot.warning("Dados de Acesso Do Seller " + vIdentificadorCliente + " Não Encontrados Na Tabela. Só Apagar Notificações.");
				return;
			}
			ConexaoDTO vAcessoCliente = arrAcessoCliente.get(0); //Pega Primeiro Resultado
			conectarNoBancoDoSeller(vIdentificadorCliente, vAcessoCliente, arrNotificacoesUsuarioAtual);

		} catch (Exception e) {
			loggerRobot.severe("Erro Ao Buscar Dados de Acesso Do Seller " + vIdentificadorCliente + ". Só Apagar Notificações.\n");
			loggerRobot.severe(e.toString());
		}
	}
	//endregion

	//region Função Para Estabelecer A Conexão Com O Banco Do Seller.
	private void conectarNoBancoDoSeller(String pIdentificadorCliente, ConexaoDTO pAcessoCliente, List<NotificacaoMLDTO> pArrNotificacoesUsuarioAtual) {
		try (Connection conexaoSQLServer = vConexaoSqlServer.conectar(pAcessoCliente)) {
			if (conexaoSQLServer != null && conexaoSQLServer.isValid(2500)) {

				String  vUserID        = pArrNotificacoesUsuarioAtual.get(0).getUserId();
				boolean vIgnorarGetSku = operacoesNoBanco.ignorarGetSku(conexaoSQLServer);
				boolean vOrigemAtiva   = operacoesNoBanco.buscaParamPedido(conexaoSQLServer, vUserID);

				if (vIgnorarGetSku) {
					logger.info("IgnorarGetSKU Definido Como (S). Só Apagar Notificações.");
					return;
				}	else if (!vOrigemAtiva) {
					logger.info("Parâmetro Pedido é N. Só Apagar Notificações.");
					return;
				}

				processarNotificacoes(conexaoSQLServer, pIdentificadorCliente, pArrNotificacoesUsuarioAtual);

			} else {
				loggerRobot.severe("FALHA: Falha Ao Validar Conexão Com o Banco Do Usuário " + pIdentificadorCliente); }
			}
		catch (UnknownHostException e) {
			loggerRobot.severe("ERRO: Um Erro Desconhecido Ocorreu Ao Conectar Ao Banco Do Seller " + pIdentificadorCliente + " -> " +  e.getMessage());
		} catch (SQLException | IOException e) {
			loggerRobot.severe("ERRO: Falha Ao Conectar No Banco Do Usuário " + pIdentificadorCliente + " -> " +  e.getMessage());
		}

	}
	//endregion

	//region Função Para Processar A Lista De Notificações De Cada Seller.
	private void processarNotificacoes(Connection pConexaoSQLServer, String pIdentificadorCliente, List<NotificacaoMLDTO> pArrNotificacoesUsuarioAtual) throws SQLException, IOException {
		if (pArrNotificacoesUsuarioAtual == null || pArrNotificacoesUsuarioAtual.isEmpty()) {
			logger.info("Nenhuma Notificação A Ser Processada Para o Seller " + pIdentificadorCliente);
			return;
		}
		String userId = pArrNotificacoesUsuarioAtual.get(0).getUserId();

		//region Obtem Dados Da ECOM_METODOS.
		DadosEcomMetodosDTO objDadosEcomMetodosDTO = operacoesNoBanco.buscarTokenTemp(pConexaoSQLServer, userId);
		String        vTokenTempSeller = objDadosEcomMetodosDTO.getTokenTemp();
		int           vOrigemDaContaML = objDadosEcomMetodosDTO.getOrigem();
		LocalDateTime vDataTokenExpira = objDadosEcomMetodosDTO.getTokenExpira().minusMinutes(15); //Pega o tokenExpira - 15min.
		LocalDateTime vDataEHoraAtual	 = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
		//endregion

		if (vTokenTempSeller.isEmpty()) {
			loggerRobot.severe("O Token Temp Do Seller: -> " + pIdentificadorCliente + " Não Foi Encontrado.");
			return;
		}

		if (vDataEHoraAtual.isAfter(vDataTokenExpira) || vDataEHoraAtual.isEqual(vDataTokenExpira)) {
			loggerRobot.warning("Token do Seller -> " + pIdentificadorCliente + " Expirado.");
			return;
		}

		for (NotificacaoMLDTO objNotificacao : pArrNotificacoesUsuarioAtual) {
			String vResourceNotificacao = objNotificacao.getResource();
			String vSkuPayloadNotificac = utils.extrairSkuMLDasNotificacoes(vResourceNotificacao);

			if (vSkuPayloadNotificac == null) { logger.info("SKU Não Encontrado Na Notificação: -> " + objNotificacao.getId()); }

			//region Fazendo a Requisição de Itens no Mercado Livre.
			ItemDTO objRespostaAPI = RequisicoesMercadoLivre.fazerRequisicaoGetItem(vSkuPayloadNotificac, vTokenTempSeller, pIdentificadorCliente);

			//region Se Não Obteve Resposta Da API.
			if (objRespostaAPI == null) {
				loggerRobot.severe("Erro Ao Consultar API Requisição Não Retornou Resultado. Só Apagar Notificação Atual.");
				return;
			}
			//endregion

			//endregion

			//region Pegando Dados Do JSON Response
			String vSellerSkuGET = "";
			String vTituloGET				 = utils.limitarQuantCatacteres(objRespostaAPI.getTitle(), 150); //Captura o Título e limita à 150 Caracteres
			String vCategoriaGET		 = objRespostaAPI.getCategoryId();
			int		 vEstoque					 = objRespostaAPI.getAvailableQuantity();
			String vInventoryIdGET	 = objRespostaAPI.getInventoryId() == null || objRespostaAPI.getInventoryId().isBlank() ? "0" : objRespostaAPI.getInventoryId();
			String vLinkAnuncioGET 	 = objRespostaAPI.getPermalink();
			String vTipoDeAnuncioGET = objRespostaAPI.getListingTypeId();
			double vPrecoNoGET			 = objRespostaAPI.getPrice();
			String vUrlDaImagemGET 	 = objRespostaAPI.getThumbnail();
			String vEstaAtivoNoGET 	 = objRespostaAPI.getStatus().equalsIgnoreCase("active") ? "S" : "N";
			String vEFullNoGET 			 = objRespostaAPI.getShipping().getLogisticType().equalsIgnoreCase("fulfillment") ? "S" : "N";
			String vSupermercado 		 = objRespostaAPI.getTags().contains("supermarket_eligible") ? "S" : "N";
			//endregion

			//region Buscando Informações Na Tabela ECOM_SKU do Seller
			InfoItemsMLDTO objProdutoNaEcomSku = null;
			try {
				objProdutoNaEcomSku = operacoesNoBanco.buscaProdutoNaECOM_SKU(pConexaoSQLServer, vSkuPayloadNotificac, vOrigemDaContaML);
			} catch (SQLException e) {
				loggerRobot.severe("Erro Ao Buscar Produto Na ECOM_SKU: -> " + e.getMessage());
				return;
			}
			//endregion

			//region Se Não Tem Informações da ECOM_SKU O Produto Não Existe Na Tabela, Logo vExiste é False e o Produto Não Está Vinculado.
			if (objProdutoNaEcomSku == null) {
				loggerRobot.warning("Informações Não Encontradas Na Tabela ECOM_SKU.");
				return;
			}
			//endregion

			String vEstaAtivoNoDB  				 = objProdutoNaEcomSku.getEstaAtivo();
			String vEFulNoDB 			 				 = objProdutoNaEcomSku.getEFulfillment();
			int vCodID 						 				 = objProdutoNaEcomSku.getCodid();
			boolean vProdVinculado 				 = true;
			List<AtributoDTO> arrAtributos = objRespostaAPI.getAttributes();
			String vSellerSKUVariac        = utils.capturaSellerSku(arrAtributos);

			//region Se é Full No DB e Não É Full No GET De Items, Insere o CODID Na Tabela(ESTOQUE_MKTP) .
			if (vEFulNoDB.equalsIgnoreCase("S") && vEFullNoGET.equalsIgnoreCase("N")) {
				vCodID = Math.max(objProdutoNaEcomSku.getCodid(), 0); // => objProdutoNaEcomSku.getCodid() > 0 ? objProdutoNaEcomSku.getCodid() : 0;
				/*if (vCodID > 0) {
					operacoesNoBanco.inserirSkuIDNaESTOQUE_MKTP(conexaoSQLServer, vCodID);
				}*/
			}
			//endregion

			//Verificando Se Existe o Campo Variations e Pegando a Quantidade de Registros!
			ArrayList<VariacaoDTO> arrVariacoes = objRespostaAPI.getVariations();

			//region Se For Variação Única Atualiza ECOM_SKU.
			if (arrVariacoes.isEmpty() || arrVariacoes.size() == 1) {
				vEstoque = objRespostaAPI.getAvailableQuantity();
				if (arrVariacoes.size() == 1) { // Se For Variação Única o Atualiza o ID da Variação;
					try {
						operacoesNoBanco.atualizaProdutoNaTabelaECOM_SKU(pConexaoSQLServer, arrVariacoes.get(0).getId(), vSkuPayloadNotificac);
					} catch (SQLException excecao) {
						loggerRobot.severe("FALHA: Erro Ao Atualizar Produto Na ECOM_SKU -> " + excecao.getMessage());
					}
				}
			}
			//endregion

			//region Se o Array de Variações Estiver Vazio Seta Estoque Para -1.
			if (arrVariacoes.isEmpty() && vSkuPayloadNotificac != null) {
				vEstoque = - 1; //Significa Que Não é Para Atualizar ECOM_SKU Com o Estoque.
			}
			//endregion

			String vCatalogoGET    = objRespostaAPI.isCatalogListing() ? "S" : "N";
			String vRelacionadoGET = objRespostaAPI.getItemRelations() != null && !objRespostaAPI.getItemRelations().isEmpty() ? "S" : "N";

			//region Fazendo o GET da Comissão No Mercado Livre e Custo Adicional(SÓ VAI USAR SE FOR ATUALIZAR ECOM_SKU).
			double vValorComissao  = RequisicoesMercadoLivre.fazerRequisicaoGetComissaoML(vTipoDeAnuncioGET, vPrecoNoGET, vCategoriaGET, vTokenTempSeller, pIdentificadorCliente);
			double vCustoAdicional = utils.calculaCustoAdicional(vValorComissao, vPrecoNoGET, vSupermercado);
			//endregion

			//region Frete Fazendo o GET do Valor Do Frete No Mercado Livre(SÓ VAI USAR SE FOR ATUALIZAR ECOM_SKU)
			double vValorFrete = RequisicoesMercadoLivre.fazerRequisicaoGetFrete(userId, vSkuPayloadNotificac, vTokenTempSeller, pIdentificadorCliente);
			//endregion

			//region Se Não For Full No GET.
			if (!vEFullNoGET.equalsIgnoreCase("S")) {

				//region Sem Variação.
				if (arrVariacoes.isEmpty()) {
					try {

						//region Atualiza Dados e EstoqueNa ECOM_SKU.
						if (!vCategoriaGET.isEmpty()) {
							utils.atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vPrecoNoGET, vPrecoNoGET, vSkuPayloadNotificac);
						}
						//endregion


					} catch (SQLException excecao) {
						excecao.getCause();
					}

					if (vInventoryIdGET.length() > 2) {
						//ML_Inventario_Full(pOrig, vInventoryIdGET);
						logger.warning("VERIFICAR: SE Implementar Método de Inventário Depois.");
					}

				}// endregion

				//region Com Variação.
				else {
					for (VariacaoDTO vVariacao : arrVariacoes) {
						String vIdVariacao		= vVariacao.getId();
						double vVariacaoPreco = vVariacao.getPrice();
						int 	 vEstoqVariacao	= vVariacao.getAvailableQuantity();
						String vVariacaoAtiva = vVariacao.getAvailableQuantity() > 0 ? "S" : "N";
						arrAtributos          = vVariacao.getAttributes();
						vSellerSKUVariac      = utils.capturaSellerSku(arrAtributos);

						//region Buscando Informações Na Tabela ECOM_SKU do Seller
						InfoItemsMLDTO objItensVariacaoEcomSku = operacoesNoBanco.buscaProdutovARIACAONaECOM_SKU(pConexaoSQLServer, vIdVariacao, vOrigemDaContaML);
						if (objItensVariacaoEcomSku != null) {
							vCodID = Math.max(objItensVariacaoEcomSku.getCodid(), 0);
						}
						//endregion

						try {

							//region Atualiza Dados e Estoque Na ECOM_SKU(COMENTADO POR ENQUANTO).
							if (!vCategoriaGET.isEmpty()) {
									utils.atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoqVariacao, vVariacaoAtiva, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vVariacaoPreco, vVariacaoPreco, vSellerSKUVariac);
							}
							//endregion

						} catch (SQLException excecao) {
							excecao.getCause();
						}
						if (vInventoryIdGET.length() > 2) {
							//ML_Inventario_Full(pOrig, vInventoryIdGET);
							logger.warning("VERIFICAR: Se Vai Implementar Método de Inventário Para Variações Depois.");
						}
					}

				} //endregion

			}
			//endregion

			//region  Se É Full No GET PROCESSA.
			if (vEFullNoGET.equalsIgnoreCase("S")) {

				//region Sem Variação.
				if (arrVariacoes.isEmpty()) {
					try {
						//SKU Existe Na ML_SKU_FULL ?
						DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(pConexaoSQLServer, vSkuPayloadNotificac);

						//region Atualiza Dados e EstoqueNa ECOM_SKU.
						if (!vCategoriaGET.isEmpty()) {
								utils.atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vPrecoNoGET, vPrecoNoGET, vSkuPayloadNotificac);
						}
						//endregion

						if (vExiste.getVExiste() <= 0) {
							try {
								operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vOrigemDaContaML, vCodID, vSkuPayloadNotificac, "0", "0", vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
							} catch (SQLException excecao) {
								loggerRobot.severe("ERRO: Seller -> " + pIdentificadorCliente + "\n Mensagem Erro: " + excecao);
							}
						} else {
							try {
								operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuPayloadNotificac);
							} catch (SQLException excecao) {
								loggerRobot.severe("ERRO: " + excecao.getMessage());
							}
						}

					} catch (SQLException excecao) {
						excecao.getCause();
					}

					if (vInventoryIdGET.length() > 2) {
						//ML_Inventario_Full(pOrig, vInventoryIdGET);
						logger.warning("VERIFICAR: SE Implementar Método de Inventário Depois.");
					}

				}// endregion

				//region Com Variação.
				else {
					for (VariacaoDTO vVariacao : arrVariacoes) {
						String vIdVariacao		  = vVariacao.getId();
						double vVariacaoPreco   = vVariacao.getPrice();
						int 	 vEstoqVariacao	  = vVariacao.getAvailableQuantity();
						String vVariacaoAtiva   = vVariacao.getAvailableQuantity() > 0 ? "S" : "N";
						String vUrlImagemVariac = "https://http2.mlstatic.com/D_" + vVariacao.getPictureIds().get(0) + "-N.jpg";
						arrAtributos            = vVariacao.getAttributes();
						vSellerSKUVariac        = utils.capturaSellerSku(arrAtributos);

						//region Buscando Informações Na Tabela ECOM_SKU do Seller
						InfoItemsMLDTO objItensVariacaoEcomSku = operacoesNoBanco.buscaProdutovARIACAONaECOM_SKU(pConexaoSQLServer, vIdVariacao, vOrigemDaContaML);
						if (objItensVariacaoEcomSku != null) {
							vCodID = Math.max(objItensVariacaoEcomSku.getCodid(), 0);
						}
						//endregion

						//region Percorre o Array de Atributos da Variação e Concatena Os Atributos.
						StringBuilder vVarBuilder = new StringBuilder();

						for (AtributoDTO atributoVariacao : vVariacao.getAttributes()) {
							if ("attribute_combinations".equalsIgnoreCase(atributoVariacao.getId())) {
								vVarBuilder.append(atributoVariacao.getValueName()).append(", ");
								break;
							}
						}

						//region Retira o Ultimo Espaço e (,) Da String Final.
						if (!vVarBuilder.isEmpty()) {
							vVarBuilder.setLength(vVarBuilder.length() - 2);
						}
						String vValorVariacao = vVarBuilder.toString();
						//endregion

						//endregion

						try {
							//O SKU Existe Na ML_SKU_FULL ?
							DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(pConexaoSQLServer, vSkuPayloadNotificac, vIdVariacao);

							//region Atualiza Dados e Estoque Na ECOM_SKU.
							if (!vCategoriaGET.isEmpty()) {
									utils.atualizaDadosECOM_SKU(pConexaoSQLServer, vEstoqVariacao, vVariacaoAtiva, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vVariacaoPreco, vVariacaoPreco, vSellerSKUVariac);
							}
							//endregion

							if (vExiste.getVExiste() == 0) {
								try {
									operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vOrigemDaContaML, vCodID, vSellerSKUVariac, vIdVariacao, vValorVariacao, vInventoryIdGET, vTituloGET, vVariacaoAtiva, vVariacaoPreco, vUrlImagemVariac, vCatalogoGET, vRelacionadoGET);
								} catch (SQLException excecao) {
									loggerRobot.severe("ERRO: " + excecao.getMessage());
								}
							} else {
								try {
									operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(pConexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuPayloadNotificac);
								} catch (SQLException excecao) {
									loggerRobot.severe("ERRO: " + excecao.getMessage());
								}
							}
						} catch (SQLException excecao) {
							excecao.getCause();
						}
						if (vInventoryIdGET.length() > 2) {
							//ML_Inventario_Full(pOrig, vInventoryIdGET);
							logger.warning("VERIFICAR: Se Vai Implementar Método de Inventário Para Variações Depois.");
						}
					}

				} //endregion

			}
			//endregion

			//region Verificando Vínculo
			if (!vProdVinculado) {
				//Deleta da tabela ECOM_SKU_SEMVINCULO
				operacoesNoBanco.deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(pConexaoSQLServer, vSkuPayloadNotificac);

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
							pConexaoSQLServer, vOrigemDaContaML, vSkuPayloadNotificac, vSellerSkuGET, vTituloGET, vPrecoNoGET, vLinkAnuncioGET, vUrlDaImagemGET, vCodID,
							objRespostaAPI.getVariations().size()
					);
				}
				//Variações
				else {
					for (VariacaoDTO vVariacao : arrVariacoes) {
						String vIdVariacao		= vVariacao.getId();
						double vVariacaoPreco	= vVariacao.getPrice();
						String vUrlImagem		 	= "https://http2.mlstatic.com/D_" + vVariacao.getPictureIds().get(0) + "-N.jpg";

						if (!vSellerSKUVariac.isEmpty()) {
							try {
								vCodID = operacoesNoBanco.buscaCodidNaTabelaMateriais(pConexaoSQLServer, vSellerSKUVariac);
							} catch (SQLException excecao) {
								loggerRobot.severe("Erro Ao Buscar CODID Na Tabela MATERIAIS Par aA Variação : -> " + excecao.getMessage());
							}
						}

						operacoesNoBanco.inserirVariacNaTabelaEcomSkuSemVinculo(
								pConexaoSQLServer, vOrigemDaContaML, vSkuPayloadNotificac, vIdVariacao, vSellerSKUVariac,
								vTituloGET, vVariacaoPreco, vLinkAnuncioGET, vUrlImagem, vCodID
						);
					}

				}

			}//endregion

		}
  }
	//endregion


}