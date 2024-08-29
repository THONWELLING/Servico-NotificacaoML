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
import com.ambarx.notificacoesML.models.NotificacaoML;
import com.ambarx.notificacoesML.models.SellerMercadoLivre;
import com.ambarx.notificacoesML.repositories.AcessoApiCadClientesRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMercadoLivreRepository;
import com.ambarx.notificacoesML.repositories.SellerMercadoLivreRepository;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import com.ambarx.notificacoesML.utils.operacoesNoBanco.OperacoesNoBanco;
import com.ambarx.notificacoesML.utils.requisicoesml.RequisicoesMercadoLivre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class NotificacaoServiceImpl implements NotificacaoService {
  private final Logger logger = Logger.getLogger(NotificacaoServiceImpl.class.getName());

  //region Injeção De Dependências Necessárias.
  @Autowired
  private SellerMercadoLivreRepository sellerRepository;
  @Autowired
  private AcessoApiCadClientesRepository acessoApiCadClientesRepository;

  @Autowired
  private NotificacaoMercadoLivreRepository notificacaoMercadoLivreRepository;
  private final FuncoesUtils utils = new FuncoesUtils();

  @Autowired
  private final RequisicoesMercadoLivre requisicoesMercadoLivre;
  @Autowired
  OperacoesNoBanco operacoesNoBanco;

  @Autowired
  public NotificacaoServiceImpl(RestTemplate restTemplate, RequisicoesMercadoLivre requisicoesMercadoLivre) {
    MercadoLivreHttpClient mercadoLivreHttpClient = new MercadoLivreHttpClient(restTemplate);
    this.requisicoesMercadoLivre = requisicoesMercadoLivre;
  }

  //endregion

  @Scheduled(fixedRate = 60000)
  public void executaBuscaPeriodica() throws Exception {
    logger.info("Executando Busca de Notificações de Forma Automática...");
    buscarTodasNotificacoes(notificacaoMercadoLivreRepository);
  }

  @Override
  public void buscarTodasNotificacoes(NotificacaoMercadoLivreRepository notificacaoMercadoLivreRepository) throws Exception {

		//region Buscando Todas As Notificações No Mysql.
		logger.info("Buscando Todas As Notificações Do Mercado Livre!!!");
    List<NotificacaoML> vTodasNotificacoes = notificacaoMercadoLivreRepository.findAll();
		//endregion

		//region Transforma As Notificações Em DTO.
		List<NotificacaoMLDTO> vNotificacoesML = ModelMapperMapping.parseListObjects(vTodasNotificacoes, NotificacaoMLDTO.class);
		//endregion

		//region Se a Lista De Notificações Não está vazia.
		if (vNotificacoesML != null) {

      logger.info("Lista de Notificações Existe!!. Mapeando Lista De Notificações Por User_Id!!");
      logger.info("Agrupando Notificações e Ordenando Por Maior Quantidade!!");
      Map<String, List<NotificacaoMLDTO>> vNotificacoesFiltradas = utils.agruparEFiltrarNotificacoes(vNotificacoesML);

			//region Processa Cada Notificação Da Lista.
			for (Map.Entry<String, List<NotificacaoMLDTO>> entry : vNotificacoesFiltradas.entrySet()) {
        String                                 userId = entry.getKey();
        List<NotificacaoMLDTO> vNotificacoesDoUsuario = entry.getValue();

        logger.info("Buscando Seller Mercado Livre!!");
        logger.info("Id Do Usuário!!  " + userId);

        Optional<SellerMercadoLivre> sellerMercadoLivre = sellerRepository.findIdentificadorClienteBySellerId(userId);

				//region Se o Seller Foi Encontrado Na Tabela de Sellers Do Mercado Livre.
				if (sellerMercadoLivre.isPresent()) {
          SellerMercadoLivre   vSeller               = sellerMercadoLivre.get();
          String               vIdentificadorCliente = vSeller.getIdentificadorCliente();

					//region Busca Dados de Acesso Do Seller(user_id) No Banco.
					logger.info("Seller Encontrado.");
          Optional<ConexaoDTO> acessoCliente = Optional.ofNullable(acessoApiCadClientesRepository.findByIdentificadorCliente(vIdentificadorCliente));
          logger.info("Buscando Dados de Conexão Do Seller.");
					//endregion

					//region Se o Acesso Do Seller Fi encontrado No Banco.
					if (acessoCliente.isPresent()) {
            ConexaoDTO vAcessoCliente = acessoCliente.get();
            String     banco          = vAcessoCliente.getBanco();
            logger.info("Dados Encontrados!!! Conectando No Banco");

						//region Conecta No Banco Do Seller.
						try (Connection conexaoSQLServer  = Conexao.conectar(vAcessoCliente)) {
              if (conexaoSQLServer != null && conexaoSQLServer.isValid(2500)) {
                logger.info("SUCESSO: Conectado Ao Banco" );

                //region Se o Parâmetro `IGNORAR_GETSKU` For Sim No Banco Do Seller.
                if (operacoesNoBanco.ignorarGetSku(conexaoSQLServer)) {
                  logger.info("IgnorarGetSKU Definido Como (S). Apagar Notificações.");
                  notificacaoMercadoLivreRepository.deleteAllById(utils.apagarNotificacoes(vNotificacoesDoUsuario));
                  logger.info("SUCESSO: Notificações Apagadas!!");

                } //endregion

                //region Se Origem Não Estiver Ativa No Banco Do Seller.
                else if (!operacoesNoBanco.buscaParamPedido(conexaoSQLServer, userId)) {
                  logger.info("Parâmetro Pedido é  (N)  Apagar Notificações.");
                  notificacaoMercadoLivreRepository.deleteAllById(utils.apagarNotificacoes(vNotificacoesDoUsuario));
                  logger.info("Notificações Apagadas Com Sucesso!!");

                } //endregion


								//region Não Ignorar GETSKU e Origem Está Ativa.
								else {

                  //region Obtem Dados Da ECOM_METODOS.
                  DadosEcomMetodosDTO vDadosEcomMetodosDTO = operacoesNoBanco.buscarTokenTemp(conexaoSQLServer, userId);
                  String              vTokenTempSeller     = vDadosEcomMetodosDTO.getTokenTemp();
                  int                 vOrigem              = vDadosEcomMetodosDTO.getOrigem();
                  //endregion

                  for (NotificacaoMLDTO objNotificacao : vNotificacoesDoUsuario) {
                    String vResourceNotificacao = objNotificacao.getResource();
                    String vSkuNotificacao      = utils.extrairSkuMLDasNotificacoes(vResourceNotificacao);
                    if (vSkuNotificacao != null) {

                      //region Fazendo a Requisição de Itens no Mercado Livre.
                      ItemDTO objRespostaAPI = requisicoesMercadoLivre.fazerRequisicaoGetItem(vSkuNotificacao, vTokenTempSeller);
                      //endregion

                      //region Verifica Se Obteve Resposta.
                      if (objRespostaAPI != null) {
                        logger.info("SUCESSO: JSON GET Item Do Mercado Livre Capturado!!");
                        logger.info(" Resposta Da API Do Mercado Livre: " + objRespostaAPI);

                        //region Pegando Dados Do JSON Response
                        String vTituloGET         = objRespostaAPI.getTitle();
                        String vCategoriaGET      = objRespostaAPI.getCategoryId();
                        int    vQtdeOriginalGET   = objRespostaAPI.getInitialQuantity();
                        int    vQtdeDisponivelGET = objRespostaAPI.getAvailableQuantity();
                        int    vQtdeVendidaGET    = objRespostaAPI.getSoldQuantity();
                        String vInventoryIdGET    = objRespostaAPI.getInventoryId() == null || objRespostaAPI.getInventoryId().isBlank() ? "0" : objRespostaAPI.getInventoryId();
                        String vLinkAnuncioGET    = objRespostaAPI.getPermalink();
                        String vTipoDeAnuncioGET  = objRespostaAPI.getListingTypeId();
                        double vPrecoNoGET        = objRespostaAPI.getPrice();
                        String vSellerSkuGET      = "";
                        String vUrlDaImagemGET    = objRespostaAPI.getThumbnail();
                        String vEstaAtivoNoGET    = objRespostaAPI.getStatus().equalsIgnoreCase("active") ? "S" : "N";
                        String vEFullNoGET        = objRespostaAPI.getShipping().getLogisticType().equalsIgnoreCase("fulfillment") ? "S" : "N";
                        String vCatalogoGET       = objRespostaAPI.isCatalogListing() ? "S" : "N";
                        String vRelacionadoGET    = objRespostaAPI.getItemRelations() != null && !objRespostaAPI.getItemRelations().isEmpty() ? "S" : "N";
                        String vSupermercado      = objRespostaAPI.getTags().contains("supermarket_eligible") ? "S" : "N";

                        //region Pegando o Seller_sku do Array de Attributos
                        for (AtributoDTO atributo : objRespostaAPI.getAttributes()) {
                          if ("SELLER_SKU".equalsIgnoreCase(atributo.getId())) {
                            vSellerSkuGET = atributo.getValueName();
                            break;
                          }
                        }
                        //endregion

                        //endregion

                        //region Fazendo o GET da Comissão No Mercado Livre
                        double vValorComissao = requisicoesMercadoLivre.fazerRequisicaoGetComissaoML(vTipoDeAnuncioGET, vPrecoNoGET, vCategoriaGET, vTokenTempSeller);
                        logger.info("Valor da Comissão No ML " + vValorComissao + "%");
                        //endregion

                        //region Frete Fazendo o GET do Valor Do Frete No Mercado Livre
                        double vValorFrete = requisicoesMercadoLivre.fazerRequisicaoGetFrete(userId, vSkuNotificacao, vTokenTempSeller);
                        logger.info("Valor do Frete No ML R$ " + vValorFrete);
                        //endregion

                        //region Buscando Informações Na Tabela ECOM_SKU do Seller
                        InfoItemsMLDTO objItensEcomSku = operacoesNoBanco.buscaProdutoNaECOM_SKU(conexaoSQLServer, vSkuNotificacao);

                        if (objItensEcomSku == null) {
                          /*
                            Depois de Buscar o SKU ATUAL que está na notifcação lá no Banco do cliente, Vefificamos:
                            Se, não existir este SKU no banco, Devemos fazer um INSERT do atual SKU no Banco.
                            Se, o SKU ATUAL Existir no banco, Atualizamos suas informações
                            @Author Thonwelling
                          */
                          logger.info("O SKU  " + vSkuNotificacao + "  Não Existe No Tabela ECOM_SKU Do Seller!!");
                          continue;
                        }

                        int    vCodID          = objItensEcomSku.getCodid() > 0 ? objItensEcomSku.getCodid() : 0;
                        String vEstaAtivoNoDB  = objItensEcomSku.getEstaAtivo();
                        String vEFulNoDB       = objItensEcomSku.getEFulfillment();

                        logger.info("Está Ativo No Banco De Dados ? " + vEstaAtivoNoDB);
                        logger.info("É    Full  No Banco De Dados ? " + vEFulNoDB);
                        //endregion

                        //region Produto é FULL NO GET E NO BANCO.
                        if (vEFullNoGET.equalsIgnoreCase("S") && vEFulNoDB.equalsIgnoreCase("S") ) {

                          //Verificando se existe o campo variations e pegando a quantidade de registros!
                          ArrayList<VariacaoDTO> arrVariacoes = objRespostaAPI.getVariations();

                          if (arrVariacoes.isEmpty()) {

                            //region Sem Variação.
                            String vVariationId = "";
                            try {
                              //Verifica Se o SKU Existe Na ML_SKU_FULL
                              DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao);

                              if (vExiste.getVExiste() <= 0) {
                                logger.warning("O SKU " + vSkuNotificacao + " Não Existe Na Tabela ML_SKU_FULL. Deve Fazer Insert Na Tabela.");
                                operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSkuNotificacao, "0", "0",vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                              } else {
                                logger.info("Atualizando o Sku atual na tabela ml_sku_full ");
                                operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                              }
                            } catch (Exception excecao) {
                              excecao.printStackTrace();
                            }
                            if (vInventoryIdGET.length() > 2) {
                              //ML_Inventario_Full(pOrig, vInventoryIdGET);
                              logger.warning("Implementar Método de Inventário");

                            }
                            // endregion

                          } else{

                            //region Com Variação.
                            for (VariacaoDTO vVariacao : arrVariacoes) {
                              Long   vIdVariacao      = vVariacao.getId();
                              double vVariacaoPreco   = vVariacao.getPrice();
                              int    vEstoqVariacao   = vVariacao.getAvailableQuantity();
                              String vSellerSKUVariac = "";

                              //region Pegando o Seller_sku do Array de Attributos
                              for (AtributoDTO atributoVariacao : vVariacao.getAttributes()) {
                                if ("SELLER_SKU".equalsIgnoreCase(atributoVariacao.getId())) {
                                  vSellerSKUVariac = atributoVariacao.getValueName();
                                  break;
                                }
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
                              //endregion

                              //region Retira o Ultimo Espaço e , Da String Final.
                              if (! vVarBuilder.isEmpty()) {
                                vVarBuilder.setLength(vVarBuilder.length() - 2);
                              }
                              String vVar = vVarBuilder.toString();
                              //endregion

                              try {
                                //Verifica Se o SKU Existe Na ML_SKU_FULL
                                DadosMlSkuFullDTO vExiste = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao, vIdVariacao);

                                if (vExiste.getVExiste() == 0) {
                                  logger.warning("O SKU " + vSkuNotificacao + " Não Existe Na Tabela ML_SKU_FULL. Deve Fazer Insert Na Tabela.");
                                  operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSellerSKUVariac, String.valueOf(vIdVariacao), vVar, vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                                } else {
                                  logger.info("Atualizando o Sku atual na tabela ml_sku_full ");
                                  operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vVariacaoPreco, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                                }
                              } catch (Exception excecao) {
                                excecao.printStackTrace();
                              }
                              if (vInventoryIdGET.length() > 2) {
                                //ML_Inventario_Full(pOrig, vInventoryIdGET);
                                System.out.println("Implementar Método de Inventário");
                              }
                            }



                            //endregion

                          }

                        }
                        //endregion

                        //region Produto é Full No GET e Não é Full No BANCO.
                        else if (vEFullNoGET.equalsIgnoreCase("S") && vEFulNoDB.equalsIgnoreCase("N")) {

                          logger.info("O SKU É Fullfilment No Mercado Livre, Mas, Não é Fullfilment No Banco. Atualizar o Fulfillment No Banco Para S!!");

                          DadosMlSkuFullDTO vExisteNaMlSkuFull = operacoesNoBanco.existeNaTabelaMlSkuFull(conexaoSQLServer, vSkuNotificacao);
                          if (vExisteNaMlSkuFull.getVExiste() == 0) {

                            logger.warning("O SKU " + vSkuNotificacao + " Não Existe Na Tabela ML_SKU_FULL. Deve Fazer Insert Na Tabela.");
                            operacoesNoBanco.inserirProdutoNaTabelaMlSkuFull(conexaoSQLServer, vOrigem, vCodID, vSkuNotificacao, "0", "0", vInventoryIdGET, vTituloGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);

                          } else {
                            logger.info("Atualizando o Sku atual na tabela ml_sku_full ");
                            operacoesNoBanco.atualizaProdutoNaTabelaMlSkuFull(conexaoSQLServer, vInventoryIdGET, vEstaAtivoNoGET, vPrecoNoGET, vUrlDaImagemGET, vCatalogoGET, vRelacionadoGET);
                          }

                        }
                        //endregion

                        //region Produto Não é Full No GET Mas é Full No BANCO.
                        else if (!vEFullNoGET.equalsIgnoreCase("S") && vEFulNoDB.equalsIgnoreCase("S")) {

                          logger.info("Não é Fullfilment No Mercado Livre, Mas, é Fullfilment No DB. Atualizar o Fulffilment No Banco Para N e Inserir em Outra Tabela!!");
                          //Inserindo o Id Do Material na NaESTOQUE_MKTP
                          operacoesNoBanco.inserirSkuIDNaESTOQUE_MKTP(conexaoSQLServer, vCodID);
                        }
                        //endregion


                      }
                      //endregion

                      else {
                        logger.info("Erro ao Chamar API Do Mercado Livre.");
                      }
                    } else {
                      logger.warning("MLB Não Pôde Ser Extraido Para a Notificação " + objNotificacao.getId());
                    }

                  }


                }
								//endregion
              }
            }
						//endregion
          }
					//endregion
        }
				//endregion
      }
			//endregion
    }
		//endregion

  }




}
