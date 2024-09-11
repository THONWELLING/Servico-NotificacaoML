package com.ambarx.notificacoesML.services;

import com.ambarx.notificacoesML.config.db.Conexao;
import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
import com.ambarx.notificacoesML.dto.item.AtributoDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.dto.item.VariacaoDTO;
import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import com.ambarx.notificacoesML.httpclients.MercadoLivreHttpClient;
import com.ambarx.notificacoesML.mapper.ModelMapperMapping;
import com.ambarx.notificacoesML.models.NotificacaoMLItens;
import com.ambarx.notificacoesML.models.NotificacaoUserProductFamiliesMLDTO;
import com.ambarx.notificacoesML.models.SellerMercadoLivre;
import com.ambarx.notificacoesML.repositories.AcessoApiCadClientesRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLItensRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLUserProductsFamiliesRepository;
import com.ambarx.notificacoesML.repositories.SellerMercadoLivreRepository;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import com.ambarx.notificacoesML.utils.operacoesNoBanco.OperacoesNoBanco;
import com.ambarx.notificacoesML.utils.requisicoesml.RequisicoesMercadoLivre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class NotificacaoItensMLItensServiceImpl implements NotificacaoMLItensService {
  private final Logger logger = Logger.getLogger(NotificacaoItensMLItensServiceImpl.class.getName());

  //region Injeção De Dependências Necessárias.
  @Autowired
  private SellerMercadoLivreRepository sellerRepository;
  @Autowired
  private AcessoApiCadClientesRepository acessoApiCadClientesRepository;
  @Autowired
  private NotificacaoMLItensRepository notificacaoMLItensRepository;
  @Autowired
  private NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository;
  private final FuncoesUtils utils = new FuncoesUtils();

  @Autowired
  private final RequisicoesMercadoLivre requisicoesMercadoLivre;
  @Autowired
  OperacoesNoBanco operacoesNoBanco;

  @Autowired
  public NotificacaoItensMLItensServiceImpl(RestTemplate restTemplate, RequisicoesMercadoLivre requisicoesMercadoLivre) {
    MercadoLivreHttpClient mercadoLivreHttpClient = new MercadoLivreHttpClient(restTemplate);
    this.requisicoesMercadoLivre = requisicoesMercadoLivre;
  }

  //endregion

  @Scheduled(fixedRate = 60000)
  public void executaThreadDeItens() throws Exception {
		try {
			logger.info("Executando Busca de Notificações de Forma Automática...");
			buscarTodasNotificacoes(notificacaoMLItensRepository);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Erro Na Execução Da Tarefa " + e.getMessage());
		}
	}

  @Scheduled(fixedRate = 60000)
  public void executaThreadDeUserProductsFamilies() throws Exception {
		try {
			logger.info("Executando Busca de Notificações de Forma Automática...");
			processaNotificacoesUserProductsFamilies(notificacaoMLUserProductsFamiliesRepository);
		} catch (EmptyResultDataAccessException excecao) {
			logger.log(Level.WARNING, "Nenhuma Notificação Encontrada: " + excecao.getMessage());
		} catch (SQLException excecaoSql) {
      logger.log(Level.SEVERE, "Erro De Banco de Dados: " + excecaoSql.getMessage(), excecaoSql);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Erro Inesperado: " + e.getMessage(), e);
    }
	}

  @Override
  public void buscarTodasNotificacoes(NotificacaoMLItensRepository notificacaoMLItensRepository) throws Exception {

    //region Buscando Todas As Notificações No Mysql.
    logger.info("Buscando Todas As Notificações De Itens Do Mercado Livre!!!");
    List<NotificacaoMLItens> vTodasNotificacoes = notificacaoMLItensRepository.findAll();
    //endregion

    //region Transforma As Notificações Em DTO.
    List<NotificacaoMLDTO> vNotificacoesML = ModelMapperMapping.parseListObjects(vTodasNotificacoes, NotificacaoMLDTO.class);
    //endregion

    //region Se a Lista De Notificações Não está vazia.
    if (vNotificacoesML != null) {

      logger.info("Lista de Notificações Existe!!. Mapeando Lista De Notificações Por User_Id!!");
      logger.info("Agrupando Notificações e Filtrando Notificações Repetidas Com Menos De 2Minutos De Intervalo.");
      Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesML);

      //region Processa Cada Lista de Notificações(de Cada SELLERID).
      for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
        String userId = entry.getKey();
        List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual = entry.getValue();

        logger.info("Buscando Seller Mercado Livre!!");
        logger.info("Id Do Usuário!!  " + userId);

        Optional<SellerMercadoLivre> sellerMercadoLivre = sellerRepository.findIdentificadorClienteBySellerId(userId);

        //region Se o Seller Foi Encontrado Na Tabela de Sellers Do Mercado Livre.
        if (sellerMercadoLivre.isPresent()) {
          SellerMercadoLivre vSeller   = sellerMercadoLivre.get();
          String vIdentificadorCliente = vSeller.getIdentificadorCliente();

          //region Busca Dados de Acesso Do Seller(user_id) No Banco.
          logger.info("Seller Encontrado.");
          Optional<ConexaoDTO> acessoCliente = Optional.ofNullable(acessoApiCadClientesRepository.findByIdentificadorCliente(vIdentificadorCliente));
          logger.info("Buscando Dados de Conexão Do Seller.");
          //endregion

          //region Se o Credenciais De Acesso Do Seller Foram Encontradas No Banco.
          if (acessoCliente.isPresent()) {
            ConexaoDTO vAcessoCliente = acessoCliente.get();
            String banco = vAcessoCliente.getBanco();
            logger.info("Dados De Conexão Encontrados. Conectando No Banco Do Seller.");

            //region Conecta No Banco Do Seller.
            try (Connection conexaoSQLServer = Conexao.conectar(vAcessoCliente)) {

              //region Conectado Com Sucesso.
              if (conexaoSQLServer != null && conexaoSQLServer.isValid(2500)) {
                logger.info("SUCESSO: Conectado Ao Banco");

                //region Se o Parâmetro `IGNORAR_GETSKU` For Sim No Banco Do Seller.
                if (operacoesNoBanco.ignorarGetSku(conexaoSQLServer)) {
                  logger.info("IgnorarGetSKU Definido Como (S). Apagar Notificações.");
                  operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
                  logger.info("SUCESSO: Notificações Apagadas!!");
                } //endregion

                //region Se Origem Não Estiver Ativa No Banco Do Seller.
                else if (! operacoesNoBanco.buscaParamPedido(conexaoSQLServer, userId)) {
                  logger.info("Parâmetro Pedido é  (N)  Apagar Notificações.");
                  operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
                  logger.info("Notificações Apagadas Com Sucesso!!");

                } //endregion

                //region Não Ignorar GETSKU e Origem Está Ativa.
                else {

                  //region Obtem Dados Da ECOM_METODOS.
                  DadosEcomMetodosDTO vDadosEcomMetodosDTO = operacoesNoBanco.buscarTokenTemp(conexaoSQLServer, userId);
                  String vTokenTempSeller = vDadosEcomMetodosDTO.getTokenTemp();
                  int vOrigem = vDadosEcomMetodosDTO.getOrigem();
                  //endregion

                  //region Processa Cada Notificação.
                  for (NotificacaoMLDTO objNotificacao : arrNotificacoesUsuarioAtual) {
                    String vResourceNotificacao = objNotificacao.getResource();
                    String vSkuNotificacao = utils.extrairSkuMLDasNotificacoes(vResourceNotificacao);
                    if (vSkuNotificacao != null) {
                      logger.info("SKU Atual: " + vSkuNotificacao);

                      //region Fazendo a Requisição de Itens no Mercado Livre.
                      logger.info("Fezendo GET De Items Na API Do ML.");
                      ItemDTO objRespostaAPI = RequisicoesMercadoLivre.fazerRequisicaoGetItem(vSkuNotificacao, vTokenTempSeller);
                      //endregion

                      //region Verifica Se Obteve Resposta.
                      if (objRespostaAPI != null) {
                        logger.info("SUCESSO: JSON GET Item Do Mercado Livre Capturado!!");
                        logger.info("Resposta Da API Do Mercado Livre: " + objRespostaAPI.toString());

												//region Se Não For Full No GET (APAGA NOTIFICAÇÃO SEM FAZER NADA).
												if (!objRespostaAPI.getShipping().getLogisticType().equalsIgnoreCase("fulfillment")) {
                          logger.info("Não É Full No GET De Items Mercado Livre.");
                          notificacaoMLItensRepository.deleteById(objNotificacao.getId());
                          logger.info("Notificação Apagada Com Sucesso.");
                          continue;
                        }
												//endregion

                        //region Pegando Dados Do JSON Response
                        String vSellerSkuGET     = "";
                        String vTituloGET        = objRespostaAPI.getTitle();
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
                            logger.info(" O Produto É Full  No Banco De Dados, Mas, Não é Full No GET Da API.");
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
                            vSellerSkuGET = atributo.getValueName();
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
                              logger.severe("Erro: " + excecao.getMessage());
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
                          /*double vValorComissao = RequisicoesMercadoLivre.fazerRequisicaoGetComissaoML(vTipoDeAnuncioGET, vPrecoNoGET, vCategoriaGET, vTokenTempSeller);
                          logger.info("Valor da Comissão No ML " + vValorComissao + "%");
                          double  vCustoAdicional = utils.calculaCustoAdicional(vValorComissao, vPrecoNoGET, vSupermercado);*/
                          //endregion

                          //region Frete Fazendo o GET do Valor Do Frete No Mercado Livre(SÓ VAI USAR SE FOR ATUALIZAR ECOM_SKU)
                          /*double vValorFrete = RequisicoesMercadoLivre.fazerRequisicaoGetFrete(userId, vSkuNotificacao, vTokenTempSeller);
                          logger.info("Valor do Frete No ML R$ " + vValorFrete);*/
                          //endregion

                          //region Sem Variação.
                          if (arrVariacoes.isEmpty()) {
                            try {
                              //Verifica Se o SKU Existe Na ML_SKU_FULL
                              DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao);

                              if (vExiste.getVExiste() <= 0) {
                                logger.warning("O SKU " + vSkuNotificacao + " Não Existe Na Tabela ML_SKU_FULL. Deve Fazer Insert Na Tabela.");
																try {
																	operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSkuNotificacao, "0", "0", vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
																} catch (SQLException excecao) {
																	logger.severe("ERRO: " + excecao.getMessage());
																}
                                operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
                              } else {
                                logger.info("Atualizando o Sku atual na tabela ml_sku_full ");
																try {
																	operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuNotificacao);
																} catch (SQLException excecao) {
																	logger.severe("ERRO: " + excecao.getMessage());
																}
                                operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
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
                              vCodID = Math.max(objItensVariacaoEcomSku.getCodid(), 0); // => objItensVariacaoEcomSku.getCodid() > 0 ? objItensVariacaoEcomSku.getCodid() : 0;
                              //endregion

                              //region Percorre o Array de Atributos da Variação e Concatena Os Atributos.
                              StringBuilder vVarBuilder = new StringBuilder();
                              for (AtributoDTO atributoVariacao : vVariacao.getAttributes()) {
                                if ("attribute_combinations".equalsIgnoreCase(atributoVariacao.getId())) {
                                  vVarBuilder.append(atributoVariacao.getValueName()).append(", ");
                                  break;
                                }
                                if ("SELLER_SKU".equalsIgnoreCase(atributoVariacao.getId())) {
                                  vSellerSKUVariac = atributoVariacao.getValueName();
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
                                  logger.warning("O SKU " + vSkuNotificacao + " Não Existe Na Tabela ML_SKU_FULL. Inserindo Na Tabela.");
                                  try {
                                    operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSellerSKUVariac, vIdVariacao, vVar, vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                                  } catch (SQLException excecao) {
                                    logger.severe("ERRO: " + excecao.getMessage());
                                  }
                                  operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
                                } else {
                                  logger.info("Atualizando o Sku atual na tabela ml_sku_full ");
                                  try {
                                    operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET, vSkuNotificacao);
                                  } catch (SQLException excecao) {
                                    logger.severe("ERRO: " + excecao.getMessage());
                                  }
                                  operacoesNoBanco.deletaNotificacaoPorID(objNotificacao.getId());
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
                        logger.warning("Erro Ao Consultar API Requisição Não Retornou Resultado.");
                      }
                      //endregion

                    }

                  } //endregion

                } //endregion

              } //endregion
            } catch (SQLException excecao) {
              logger.log(Level.SEVERE,"ERRO: Falha Ao Conectar No Banco Do User ID: " + userId, excecao);
            }
            //endregion

          } //endregion

          //region Dados de Acesso Não Encontrados.
          else {
            logger.warning("Dados de Acesso Do Seller Não Encontrado Na Tabela. Apagando Notificações");
            operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
          }
          //endregion

        } //endregion

        //region Seller Não Encontrado.
        else {
          logger.warning("Seller Não Encontrado no Mercado Livre");
          operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
				} //endregion

      } //endregion

    }
    //endregion

  }

  @Override
  public void processaNotificacoesUserProductsFamilies(NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository) throws Exception {

    //region Buscando Todas As Notificações No Mysql.
    logger.info("Buscando Todas As Notificações De User Products Families Do Mercado Livre!!!");
    List<NotificacaoUserProductFamiliesMLDTO> vTodasNotificacoesUserProductsFamilies = notificacaoMLUserProductsFamiliesRepository.findAll();
    //endregion

    //region Transforma As Notificações Em DTO.
    List<NotificacaoMLDTO> vNotificacoesMLUserFiltradas = ModelMapperMapping.parseListObjects(vTodasNotificacoesUserProductsFamilies, NotificacaoMLDTO.class);
    //endregion

    if (!vTodasNotificacoesUserProductsFamilies.isEmpty()) {
      logger.info("Lista de Notificações Existe!!. Mapeando Lista De Notificações Por User_Id!!");
      Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesMLUserFiltradas);

      for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
        List<NotificacaoMLDTO> arrNotificacoesUsuarioAtual = entry.getValue();

        logger.info("Apagar Notificações.");
        operacoesNoBanco.deletaNotificacoesDoSeller(arrNotificacoesUsuarioAtual);
        logger.info(arrNotificacoesUsuarioAtual.size() + " Apagadas Com sucesso.");
      }
    }
  }
}
