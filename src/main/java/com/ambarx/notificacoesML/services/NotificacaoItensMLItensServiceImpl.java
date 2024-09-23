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
//import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
		} catch (EmptyResultDataAccessException excecao) {
			logger.log(Level.WARNING, "Nenhuma Notificação Encontrada: " + excecao.getMessage());
		} catch (SQLException excecaoSql) {
      loggerRobot.severe("Erro De Banco de Dados: -> " + excecaoSql.getMessage());
    } catch (Exception e) {
      loggerRobot.severe("Erro Inesperado: -> " + e.getMessage());
    }
	}

  @Override
  public void processaNotificacoesMLItems(NotificacaoMLItensRepository notificacaoMLItensRepository) throws Exception {

    //region Buscando Todas As Notificações No Mysql.
    logger.info("Buscando Notificações De Itens Do Mercado Livre!!!");
    List<NotificacaoMLItensEntity> vNotificacoesLimitadas = notificacaoMLItensRepository.findTopNotificacoesPorUserId(1000);
    //endregion

    //region Transforma As Notificações Em DTO.
    List<NotificacaoMLDTO> vNotificacoesML = ModelMapperMapping.parseListObjects(vNotificacoesLimitadas, NotificacaoMLDTO.class);
    //endregion

    //region Se a Lista De Notificações Não está vazia.
    if (vNotificacoesML != null) {

      logger.info("Deletando Notificações Da Tabela.");
      operacoesNoBanco.deletaNotificacoesDoSeller(vNotificacoesML);

      Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesML);

      //region Processa Cada Lista de Notificações(de Cada SELLERID).
      for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
        String userId = entry.getKey();
        List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual = entry.getValue();

        Optional<SellerMercadoLivre> sellerMercadoLivre = sellerRepository.findIdentificadorClienteBySellerId(userId);

        //region Se o Seller Foi Encontrado Na Tabela de Sellers Do Mercado Livre.
        if (sellerMercadoLivre.isPresent()) {
          SellerMercadoLivre vSeller = sellerMercadoLivre.get();
          String vIdentificadorCliente  = vSeller.getIdentificadorCliente();

					try {
						//region Busca Dados de Acesso Do Seller(user_id) No Banco.
						Optional<ConexaoDTO> objAcessoCliente = Optional.ofNullable(acessoApiCadClientesRepository.findByIdentificadorCliente(vIdentificadorCliente));
						//endregion

						//region Se o Credenciais De Acesso Do Seller Foram Encontradas No Banco.
						if (objAcessoCliente.isPresent()) {
							ConexaoDTO vAcessoCliente = objAcessoCliente.get();

							//region Conecta No Banco Do Seller.
							try (Connection conexaoSQLServer = vConexaoSqlServer.conectar(vAcessoCliente)) {

								//region Conectado Com Sucesso.
								if (conexaoSQLServer != null && conexaoSQLServer.isValid(2500)) {
									logger.info("SUCESSO: Conectado Ao Banco");

									//region Se o Parâmetro `IGNORAR_GETSKU` For Sim No Banco Do Seller.
									if (operacoesNoBanco.ignorarGetSku(conexaoSQLServer)) {
										logger.info("IgnorarGetSKU Definido Como (S). Só Apagar Notificações.");
										/*operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);*/
									} //endregion

									//region Se Origem Não Estiver Ativa No Banco Do Seller.
									else if (!operacoesNoBanco.buscaParamPedido(conexaoSQLServer, userId)) {
										logger.info("Parâmetro Pedido é N. Só Apagar Notificações.");
										/*operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);*/
									} //endregion

									//region Não Ignorar GETSKU e Origem Está Ativa.
									else {
										logger.info("O Seller é: -> " + vIdentificadorCliente);
										logger.info("O Canal  é: -> " + userId);

                    //region Obtem Dados Da ECOM_METODOS.
										DadosEcomMetodosDTO objDadosEcomMetodosDTO = operacoesNoBanco.buscarTokenTemp(conexaoSQLServer, userId);
										String vTokenTempSeller    = objDadosEcomMetodosDTO.getTokenTemp();
										int vOrigem                = objDadosEcomMetodosDTO.getOrigem();
										LocalDateTime vTokenExpira = objDadosEcomMetodosDTO.getTokenExpira().minusMinutes(15); //Pega o tokenExpira - 15min.
										LocalDateTime vHoraAtual   = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
										//endregion

										//region Se Encontrou O Token.
										if(!vTokenTempSeller.isEmpty() || !vTokenTempSeller.isBlank()) {

											//region Se O Token Não está Vencido.
											if (vHoraAtual.isBefore(vTokenExpira) || vHoraAtual.isEqual(vTokenExpira)) {

												//region Processa Cada Notificação.
												for (NotificacaoMLDTO objNotificacao : arrNotificacoesUsuarioAtual) {
													String vResourceNotificacao = objNotificacao.getResource();
													String vSkuNotificacao      = utils.extrairSkuMLDasNotificacoes(vResourceNotificacao);
													if (vSkuNotificacao != null) {
														logger.info("SKU Atual: " + vSkuNotificacao);

														//region Fazendo a Requisição de Itens no Mercado Livre.
														ItemDTO objRespostaAPI = RequisicoesMercadoLivre.fazerRequisicaoGetItem(vSkuNotificacao, vTokenTempSeller, vIdentificadorCliente);
														//endregion

														//region Verifica Se Obteve Resposta.
														if (objRespostaAPI != null) {

															//region Se Não For Full No GET (APAGA NOTIFICAÇÃO SEM FAZER NADA).
															if (!objRespostaAPI.getShipping().getLogisticType().equalsIgnoreCase("fulfillment")) {
																logger.info("Não É Full No GET De Items. Só Deletar.");
																/*operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());*/
																continue;
															}
															//endregion

															//region Pegando Dados Do JSON Response
															String vSellerSkuGET     = "";
															String vTituloGET        = utils.limitarQuantCatacteres(objRespostaAPI.getTitle(), 150); //Captura o Título e limita à 150 Caracteres
															String vCategoriaGET     = objRespostaAPI.getCategoryId();
															int    vEstoque          = objRespostaAPI.getAvailableQuantity();
															String vInventoryIdGET   = objRespostaAPI.getInventoryId() == null || objRespostaAPI.getInventoryId().isBlank() ? "0" : objRespostaAPI.getInventoryId();
															String vLinkAnuncioGET   = objRespostaAPI.getPermalink();
															String vTipoDeAnuncioGET = objRespostaAPI.getListingTypeId();
															double vPrecoNoGET       = objRespostaAPI.getPrice();
															String vUrlDaImagemGET   = objRespostaAPI.getThumbnail();
															String vEstaAtivoNoGET   = objRespostaAPI.getStatus().equalsIgnoreCase("active") ? "S" : "N";
															String vEFullNoGET       = objRespostaAPI.getShipping().getLogisticType().equalsIgnoreCase("fulfillment") ? "S" : "N";
															String vSupermercado   = objRespostaAPI.getTags().contains("supermarket_eligible") ? "S" : "N";
															//endregion

															//region Buscando Informações Na Tabela ECOM_SKU do Seller
															InfoItemsMLDTO objItensEcomSku = operacoesNoBanco.buscaProdutoNaECOM_SKU(conexaoSQLServer, vSkuNotificacao, vOrigem);
															//endregion

															String  vEstaAtivoNoDB  = "";
															String  vEFulNoDB       = "";
															int     vCodID          = 0;
															boolean vProdVinculado  = false;

															//region Se Tem Informações da ECOM_SKU O Produto Existe Na Tabela, Logo vExiste é True e o Produto Está Vinculado.
															if (objItensEcomSku != null) {
																vEstaAtivoNoDB = objItensEcomSku.getEstaAtivo();
																vEFulNoDB      = objItensEcomSku.getEFulfillment();
																vCodID         = objItensEcomSku.getCodid();
																vProdVinculado = true;

																//region Se é Full No DB e Não É Full No GET De Items, Insere o CODID Na Tabela(ESTOQUE_MKTP) .
																if (vEFulNoDB.equalsIgnoreCase("S") && vEFullNoGET.equalsIgnoreCase("N")) {
																	logger.info(" O Produto É Full  No Banco, Mas, Não é Full No GET.");
																	vCodID = Math.max(objItensEcomSku.getCodid(), 0); // => objItensEcomSku.getCodid() > 0 ? objItensEcomSku.getCodid() : 0;
																	/*if (vCodID > 0) {
																		operacoesNoBanco.inserirSkuIDNaESTOQUE_MKTP(conexaoSQLServer, vCodID);
																	}*/
																}
																//endregion

															}
															//endregion

															//region Pegando o Seller_sku do Array de Attributos Produto Simples.
															for (AtributoDTO atributo : objRespostaAPI.getAttributes()) {
																if ("SELLER_SKU".equalsIgnoreCase(atributo.getId())) {
																	vSellerSkuGET = !atributo.getValueName().isEmpty() || atributo.getValueName().isBlank() ? utils.limitarQuantCatacteres(atributo.getValueName().trim(), 30) : atributo.getValueName();
																	break;
																}
															}
															//endregion

															//Verificando Se Existe o Campo Variations e Pegando a Quantidade de Registros!
															ArrayList<VariacaoDTO> arrVariacoes = objRespostaAPI.getVariations();

															//region Se For Variação Única Atualiza ECOM_SKU.
															if (arrVariacoes.isEmpty() || arrVariacoes.size() == 1) {
																vEstoque = objRespostaAPI.getAvailableQuantity();
																if (arrVariacoes.size() == 1) { // Se For Variação Única o Atualiza o ID da Variação;
																	try {
																		operacoesNoBanco.atualizaProdutoNaTabelaECOM_SKU(conexaoSQLServer, arrVariacoes.get(0).getId(), vSkuNotificacao);
																	} catch (SQLException excecao) {
																		loggerRobot.severe("FALHA: -> " + excecao.getMessage());
																	}
																}
															}
															//endregion

															//region Se o Array de Variações Estiver Vazio Seta Estoque Para -1.
															if (arrVariacoes.isEmpty() && vSkuNotificacao.isEmpty() || vSkuNotificacao.isBlank()) {
																vEstoque = - 1; //Significa Que é Para Não Atualizar ECOM_SKU Com o Estoque.
															}
															//endregion

															//region  Se É Full No GET PROCESSA.
															if (vEFullNoGET.equalsIgnoreCase("S")) {
																String vCatalogoGET    = objRespostaAPI.isCatalogListing() ? "S" : "N";
																String vRelacionadoGET = objRespostaAPI.getItemRelations() != null && ! objRespostaAPI.getItemRelations().isEmpty() ? "S" : "N";

																//region Fazendo o GET da Comissão No Mercado Livre e Custo Adicional(SÓ VAI USAR SE FOR ATUALIZAR ECOM_SKU).
                                  /*double vValorComissao = RequisicoesMercadoLivre.fazerRequisicaoGetComissaoML(vTipoDeAnuncioGET, vPrecoNoGET, vCategoriaGET, vTokenTempSeller, vIdentificadorCliente);
                                  logger.info("Valor da Comissão No ML " + vValorComissao + "%");
                                  double  vCustoAdicional = utils.calculaCustoAdicional(vValorComissao, vPrecoNoGET, vSupermercado);*/
																//endregion

																//region Frete Fazendo o GET do Valor Do Frete No Mercado Livre(SÓ VAI USAR SE FOR ATUALIZAR ECOM_SKU)
                                  /*double vValorFrete = RequisicoesMercadoLivre.fazerRequisicaoGetFrete(userId, vSkuNotificacao, vTokenTempSeller, vIdentificadorCliente);
                                  logger.info("Valor do Frete No ML R$ " + vValorFrete);*/
																//endregion

																//region Sem Variação.
																if (arrVariacoes.isEmpty()) {
																	try {
																		//Verifica Se o SKU Existe Na ML_SKU_FULL
																		DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao);

																		if (vExiste.getVExiste() <= 0) {
																			logger.warning("Inserir O SKU " + vSkuNotificacao + " Na Tabela ML_SKU_FULL.");
																			try {
																				operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSkuNotificacao, "0", "0", vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
																			} catch (SQLException excecao) {
																				loggerRobot.severe("ERRO: Seller -> " + vIdentificadorCliente + "\n Mensagem Erro: " + excecao);
																			}
																			/*operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());*/
																		} else {
																			logger.info("Atualiza o SKU Atual Na Tabela ML_SKU_FULL.");
																			try {
																				operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuNotificacao);
																			} catch (SQLException excecao) {
																				loggerRobot.severe("ERRO: " + excecao.getMessage());
																			}
																			/*operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());*/
																		}

																	} catch (SQLException excecao) { excecao.getCause(); }

																	if (vInventoryIdGET.length() > 2) {
																		//ML_Inventario_Full(pOrig, vInventoryIdGET);
																		logger.warning("Implementar Método de Inventário");

																	}

																}// endregion

																//region Com Variação.
																else {
																	for (VariacaoDTO vVariacao : arrVariacoes) {
																		String vIdVariacao      = vVariacao.getId();
																		double vVariacaoPreco   = vVariacao.getPrice();
																		int    vEstoqVariacao   = vVariacao.getAvailableQuantity();
																		String vSellerSKUVariac = "";

																		//region Buscando Informações Na Tabela ECOM_SKU do Seller
																		InfoItemsMLDTO objItensVariacaoEcomSku = operacoesNoBanco.buscaProdutovARIACAONaECOM_SKU(conexaoSQLServer, vIdVariacao, vOrigem);
																		if (objItensVariacaoEcomSku != null) {
																			vCodID = Math.max(objItensVariacaoEcomSku.getCodid(), 0); // => objItensVariacaoEcomSku.getCodid() > 0 ? objItensVariacaoEcomSku.getCodid() : 0;
																		}
																		//endregion

																		//region Percorre o Array de Atributos da Variação e Concatena Os Atributos.
																		StringBuilder vVarBuilder = new StringBuilder();
																		for (AtributoDTO atributoVariacao : vVariacao.getAttributes()) {
																			if ("attribute_combinations".equalsIgnoreCase(atributoVariacao.getId())) {
																				vVarBuilder.append(atributoVariacao.getValueName()).append(", ");
																				break;
																			}
																			if ("SELLER_SKU".equalsIgnoreCase(atributoVariacao.getId())) {
																				vSellerSKUVariac = !atributoVariacao.getValueName().isEmpty()|| !atributoVariacao.getValueName().isBlank()  ?
																														utils.limitarQuantCatacteres(atributoVariacao.getValueName().trim(), 50)   :
																														atributoVariacao.getValueName();
																				break;
																			}
																		}

																		//region Retira o Ultimo Espaço e , Da String Final.
																		if (! vVarBuilder.isEmpty()) {
																			vVarBuilder.setLength(vVarBuilder.length() - 2);
																		}
																		String vVar = vVarBuilder.toString();
																		//endregion

																		//endregion

																		try {
																			//Verifica Se o SKU Existe Na ML_SKU_FULL
																			DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao, vIdVariacao);

																			if (vExiste.getVExiste() == 0) {
																				logger.warning("Inserir O SKU " + vSkuNotificacao + " Na Tabela ML_SKU_FULL.");
																				try {
																					operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSellerSKUVariac, vIdVariacao, vVar, vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
																				} catch (SQLException excecao) {
																					loggerRobot.severe("ERRO: " + excecao.getMessage());
																				}
	//                                  operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
																			} else {
																				logger.info("Atualiza O SKU Atual Na Tabela ML_SKU_FULL");
																				try {
																					operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuNotificacao);
																				} catch (SQLException excecao) {
																					loggerRobot.severe("ERRO: " + excecao.getMessage());
																				}
	//                                  operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
																			}
																		} catch (SQLException excecao) {
																			excecao.getCause();
																		}
																		if (vInventoryIdGET.length() > 2) {
																			//ML_Inventario_Full(pOrig, vInventoryIdGET);
																			System.out.println("Implementar Método de Inventário");
																		}
																	}

																} //endregion

															}
															//endregion

															//region Verificando Vínculo
															if (!vProdVinculado) {
																//Deleta da tabela ECOM_SKU_SEMVINCULO
																operacoesNoBanco.deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(conexaoSQLServer, vSkuNotificacao);

																if (!vSellerSkuGET.isEmpty() && !vSellerSkuGET.isBlank()) {
																	vCodID = operacoesNoBanco.buscaCodidNaTabelaMateriais(conexaoSQLServer, vSellerSkuGET);
																}
																//Produto Simples
																if (arrVariacoes.isEmpty()) {
																	operacoesNoBanco.inserirProdutoNaTabelaEcomSkuSemVinculo(conexaoSQLServer, vOrigem, vSkuNotificacao, vSellerSkuGET, vTituloGET, vPrecoNoGET, vLinkAnuncioGET, vUrlDaImagemGET, vCodID, objRespostaAPI.getVariations().size());
																}
																//Variações
																if (!arrVariacoes.isEmpty()) {
																	for (VariacaoDTO vVariacao : arrVariacoes) {
																		String vIdVariacao      = vVariacao.getId();
																		double vVariacaoPreco   = vVariacao.getPrice();
																		String vUrlImagem       = "https://http2.mlstatic.com/D_" + vVariacao.getPictureIds().get(0) + "-N.jpg";
																		String vSellerSKUVariac = "";

																		//region Percorre o Array de Atributos da Variação Buscar Seller_Sku.
																		for (AtributoDTO atributoVariacao : vVariacao.getAttributes()) {
																			if ("SELLER_SKU".equalsIgnoreCase(atributoVariacao.getId())) {
																				vSellerSKUVariac = atributoVariacao.getValueName();
																				break;
																			}
																		}
																		//endregion

																		if (!vSellerSKUVariac.isEmpty()) {
																			try {
																				vCodID = operacoesNoBanco.buscaCodidNaTabelaMateriais(conexaoSQLServer, vSellerSkuGET);
																			} catch (SQLException excecao) {
																				excecao.getCause();
																			}
																		}
																		operacoesNoBanco.inserirVariacNaTabelaEcomSkuSemVinculo(conexaoSQLServer, vOrigem, vSkuNotificacao, vIdVariacao, vSellerSKUVariac, vTituloGET, vVariacaoPreco, vLinkAnuncioGET, vUrlImagem, vCodID);
																	}

																}

															}//endregion

															//region Atualiza Dados e EstoqueNa ECOM_SKU(COMENTADO POR ENQUANTO).
                              /*if (!vCategoriaGET.isEmpty()) {
                                if (vEstoque >= 0) {
                                  operacoesNoBanco.atualizaDadosEEstoqNaECOMSKU(conexaoSQLServer, vEstoque, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vPrecoNoGET, vPrecoNoGET, vSkuNotificacao);
                                } else {
                                  operacoesNoBanco.atualizaDadosNaECOMSKU(conexaoSQLServer, vEstaAtivoNoGET, vEFullNoGET, vValorFrete, vCustoAdicional, vValorComissao, vPrecoNoGET, vPrecoNoGET, vSkuNotificacao);
                                }
                              }*/
															//endregion

														} else {
															loggerRobot.severe("Erro Ao Consultar API Requisição Não Retornou Resultado. Só Apagar Notificação Atual.");
															/*operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());*/
														}
														//endregion

													}

												}//endregion

											}//endregion

											//region Token Vencido.
											else {
												loggerRobot.severe("Token do Seller -> " + vIdentificadorCliente + " Expirado.");
											}
											//endregion

										}//endregion

										//region Token Temp Seller Não Encontrado.
										else {
											loggerRobot.severe("O Token Temp Do Seller: -> " + vIdentificadorCliente + " Não Foi Encontrado.");
										}
										//endregion

									} //endregion

									vConexaoSqlServer.fecharConexao(conexaoSQLServer);

								} //endregion

							} catch (SQLException excecao) {
								loggerRobot.severe("ERRO: Falha Ao Conectar No Banco Do Usuário " + vIdentificadorCliente + " -> " +  excecao.getMessage());
							}
							//endregion

						} //endregion

						//region Dados de Acesso Não Encontrados.
						else {
							loggerRobot.warning("Dados de Acesso Do Seller " + vIdentificadorCliente + " Não Encontrados Na Tabela. Só Apagar Notificações.");
							/*operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);*/
						}
						//endregion

          } catch (Exception e) {
						throw new NotificacaoMLException("Erro No Processamento", e, userId, vIdentificadorCliente);
					}

				} //endregion

        //region Seller Não Encontrado.
        else {
          loggerRobot.warning("Seller Não Encontrado Na Tabela Sellers Mercado Livre. Só Apagar Notificações Do Seller ");
          /*operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);*/
				} //endregion

      } //endregion

      loggerRobot.warning("Finaliza Tarefa Items Mercado Livre.");
    }
    //endregion

  }

  @Override
  public void processaNotificacoesUserProductsFamilies(NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository) throws Exception {

    //region Buscando Todas As Notificações No Mysql.
    logger.info("Buscando Todas As Notificações De User Products Families Do Mercado Livre!!!");
    List<NotificacaoUserProductFamiliesEntity> vTodasNotificacoesUserProductsFamilies = notificacaoMLUserProductsFamiliesRepository.findAll();
    //endregion

    //region Transforma As Notificações Em DTO.
    List<NotificacaoMLDTO> vNotificacoesMLUserFiltradas = ModelMapperMapping.parseListObjects(vTodasNotificacoesUserProductsFamilies, NotificacaoMLDTO.class);
    //endregion

    if (!vTodasNotificacoesUserProductsFamilies.isEmpty()) {
      logger.info("Lista de Notificações User Product Families Existe!!. Mapeando Lista De Notificações Por User_Id!!");
      Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesMLUserFiltradas);

      for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
        List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual = entry.getValue();

        logger.info("Apagar Notificações User Product Families.");
        // Deletando todos os registros
        notificacaoMLUserProductsFamiliesRepository.deleteAll(vTodasNotificacoesUserProductsFamilies);
        loggerRobot.info("Finaliza Tarefa.User Products Families");
//        operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
//        logger.info(arrNotificacoesUsuarioAtual.size() + " Notificações User Product Families Apagadas Com sucesso.");
      }
    }
  }

}
