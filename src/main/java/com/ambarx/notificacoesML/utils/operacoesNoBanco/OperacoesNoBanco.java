package com.ambarx.notificacoesML.utils.operacoesNoBanco;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import com.ambarx.notificacoesML.repositories.NotificacaoMLItensRepository;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
@Component
public class OperacoesNoBanco {
  private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();
  private static final Logger logger      = Logger.getLogger(OperacoesNoBanco.class.getName());

  @Autowired
  NotificacaoMLItensRepository itensRepository;
  @Autowired
  private FuncoesUtils utils;


	//region Função Deleta Todas As Notificações Do Seller Atual.
	public void deletaNotificacoesDoSeller(List<NotificacaoMLDTO> pArrNotificacoesUsuarioAtual) {
    try {
      itensRepository.deleteAllByIdInBatch(utils.pegarIDsDasNotificacoesDoUsuario(pArrNotificacoesUsuarioAtual));
    } catch (EmptyResultDataAccessException excecao) {
      logger.log(Level.WARNING, "FALHA: Notificação Pode Já Ter Sido Deletada: -> " + excecao.getMessage());
    } catch (Exception e) {
      loggerRobot.severe("FALHA: Erro Inesperado Ao Deletar Notificações: -> " + e.getMessage());
    }
  }
	//endregion

  //region Função Para Buscar o Valor Parâmetro IGNORAR_GETSKU.
  public boolean ignorarGetSku(Connection pConexao) throws SQLException {
    try (Statement statement = pConexao.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT VALOR FROM PARAMETROS_SYS WHERE PARAMETRO = 'IGNORAR_GETSKU' AND EMPRESA = 0")) {
      if (resultSet.next()) {
        String vValorParametro = !resultSet.getString("VALOR").isEmpty() ? resultSet.getString("VALOR").trim() : resultSet.getString("VALOR");
        if (vValorParametro == null || vValorParametro.isEmpty() || vValorParametro.equalsIgnoreCase("N")) {
          vValorParametro = "N";
          return false;
        }
        return true;
      }
    } catch (SQLException excecao){
      loggerRobot.severe("FALHA: Erro Ao Buscar Parâmetro IGNORARGETSKU: -> " + excecao.getMessage());
      throw excecao;
    }
    return false;
  }
  //endregion

  //region Função Para Buscar o Valor Do Parâmetro PEDIDO Na ECOM_ORIGEM ("S" Significa Origem Ativa).
  public boolean buscaParamPedido(Connection pConexao, String pUserId) throws SQLException {
    String Qry_BuscaPedido = "SELECT PEDIDO FROM ECOM_ORIGEM WHERE ORIGEM_ID = (SELECT ORIGEM FROM ECOM_METODOS WHERE CANAL = ?)";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_BuscaPedido)){
      statement.setString(1, pUserId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String vParametroPedido = resultSet.getString("PEDIDO");
          if (vParametroPedido == null || vParametroPedido.isEmpty()) {
            vParametroPedido = "N";
            return false;
          }
          return true;
        }
      }
    } catch(SQLException excecao){
      loggerRobot.severe("FALHA: Erro Ao Buscar Parâmetro PEDIDO: -> " + excecao.getMessage());
      logger.severe("FALHA: Erro Ao Buscar Parâmetro PEDIDO: -> " + excecao.getMessage());
      throw excecao;
    }
    return  false;
  }
  //endregion

  //region Função Para Buscar O Token Do Seller No Banco.
  public DadosEcomMetodosDTO buscarTokenTemp (Connection pConexao, String pUserId) throws SQLException {
    String Qry_BuscaDadosEcomMetodos = "SELECT TOKEN_TEMP, TOKEN_EXPIRA, ORIGEM FROM ECOM_METODOS WHERE CANAL = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_BuscaDadosEcomMetodos)){
      statement.setString(1, pUserId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String vTokenTemp          = resultSet.getString("TOKEN_TEMP");
          int    vOrigem             = resultSet.getInt("ORIGEM");
          Timestamp vExpiraTimestamp = resultSet.getTimestamp("TOKEN_EXPIRA");

          //Convertendo para LocalDateTime
          LocalDateTime vTokenExpira = vExpiraTimestamp.toInstant().atZone(ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.of("America/Sao_Paulo")) .toLocalDateTime();

          return new DadosEcomMetodosDTO(vTokenTemp, vOrigem, vTokenExpira);
        }
      }
    } catch(SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Buscar O Token Do Seller: -> " + excecao.getMessage());
      throw excecao;
    }
    return null;
  }
  //endregion

  //region Função Para Buscar Produto SIMPLES Na Tabela ECOM_SKU.
  public InfoItemsMLDTO buscaProdutoNaECOM_SKU(Connection pConexao, String pSkuML, int pOrigem) throws SQLException {
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKU = ? AND ORIGEM_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, pSkuML);
      statement.setInt(   2, pOrigem);
      try(ResultSet resultSet = statement.executeQuery()) {

        //region Verifica Se Foi Retornado Dados Na Busca.
        if (resultSet.next()) {

          //region Pega As Informações Do Banco e Guarda Nas Variáveis
          int    vCodID      = resultSet.getInt("MATERIAL_ID");
          String vSkuNoBanco = StringUtils.hasText(resultSet.getString("SKU"))         ? resultSet.getString("SKU").trim()         : resultSet.getString("SKU");
          String vEstaAtivo  = StringUtils.hasText(resultSet.getString("ATIVO"))       ? resultSet.getString("ATIVO").trim()       : resultSet.getString("ATIVO");
          String vEFull      = StringUtils.hasText(resultSet.getString("FULFILLMENT")) ? resultSet.getString("FULFILLMENT").trim() : resultSet.getString("FULFILLMENT");
          //endregion

          //region Seta Os Campos Do Objeto DTO Com As Informações Obtidas No Banco.
          InfoItemsMLDTO infoItemsEcomSkuDTO = new InfoItemsMLDTO();
          infoItemsEcomSkuDTO.setCodid(vCodID);
          infoItemsEcomSkuDTO.setSkuNoBanco(vSkuNoBanco);
          infoItemsEcomSkuDTO.setEstaAtivo(vEstaAtivo);
          infoItemsEcomSkuDTO.setEFulfillment(vEFull);
          //endregion

          return infoItemsEcomSkuDTO;
        }
        return null;
        //endregion

      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Verificar Se o SKU " + pSkuML + "  Existe Na Tabela ECOM_SKU: -> " + excecao.getMessage());
      throw excecao;
    }
	}
  //endregion

  // region Função Para Buscar Produto Variação Na Tabela ECOM_SKU.
  public InfoItemsMLDTO buscaProdutovARIACAONaECOM_SKU(Connection pConexao, String pIDVariacao, int pOrigem) throws SQLException {
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKUVARIACAO_MASTER = ? AND ORIGEM_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, pIDVariacao);
      statement.setInt(   2, pOrigem);
      try(ResultSet resultSet = statement.executeQuery()) {

        //region Verifica Se Foi Retornado Dados Na Busca.
        if (resultSet.next()) {

          //region Pega As Informações Do Banco e Guarda Nas Variáveis
          int    vCodID      = resultSet.getInt("MATERIAL_ID");
          String vSkuNoBanco = StringUtils.hasText(resultSet.getString("SKU"))         ? resultSet.getString("SKU").trim()         : resultSet.getString("SKU");
          String vEstaAtivo  = StringUtils.hasText(resultSet.getString("ATIVO"))       ? resultSet.getString("ATIVO").trim()       : resultSet.getString("ATIVO");
          String vEFull      = StringUtils.hasText(resultSet.getString("FULFILLMENT")) ? resultSet.getString("FULFILLMENT").trim() : resultSet.getString("FULFILLMENT");
          //endregion

          //region Seta Os Campos Do Objeto DTO Com As Informações Obtidas No Banco.
          InfoItemsMLDTO infoItemsEcomSkuDTO = new InfoItemsMLDTO();
          infoItemsEcomSkuDTO.setCodid(vCodID);
          infoItemsEcomSkuDTO.setSkuNoBanco(vSkuNoBanco);
          infoItemsEcomSkuDTO.setEstaAtivo(vEstaAtivo);
          infoItemsEcomSkuDTO.setEFulfillment(vEFull);
          //endregion

          return infoItemsEcomSkuDTO;
        }
        return null;
        //endregion

      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Verificar Se a Variação " + pIDVariacao + "  Existe Na Tabela ECOM_SKU: -> " + excecao.getMessage());
      throw excecao;
    }
	}
  //endregion

  //region Função Para Atualizar Dados e ESTOQUE De Um Produto Na Tabela ECOM_SKU.
  public void atualizaDadosEEstoqNaECOMSKU(Connection pConexao, int pEstoque, String pAtivo, String pFulfillment, /*double pFrete,*/ double pCustoAdicional, double pComissao,
                                           String vIdVariacao, boolean vEhVariacao, String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws SQLException {

    Date vDataHr = new Date(System.currentTimeMillis());
    /*String Qry_AtualizaECOMSku = vEhVariacao
        ? "UPDATE ECOM_SKU SET ESTOQUE = ?, ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKUVARIACAO_MASTER = ?"
        : "UPDATE ECOM_SKU SET ESTOQUE = ?, ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKU = ?";
    */
    String Qry_AtualizaECOMSku = vEhVariacao
        ? "UPDATE ECOM_SKU SET ESTOQUE = ?, ATIVO = ?, FULFILLMENT = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKUVARIACAO_MASTER = ?"
        : "UPDATE ECOM_SKU SET ESTOQUE = ?, ATIVO = ?, FULFILLMENT = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKU = ?";

    String Qry_preenchida = vEhVariacao
      ? "UPDATE ECOM_SKU SET ESTOQUE ='"+pEstoque+"', ATIVO ='"+pAtivo+"', FULFILLMENT ='"+pFulfillment+"', CUSTO_ADICIONAL ='"+pCustoAdicional+"', COMISSAO_SKU ='"+pComissao+
        "', DT_GET ='"+vDataHr+"', STCODE ='"+vStatusCode+"', RETORNO ='"+vStatusNoGet+"' WHERE SKUVARIACAO_MASTER ='"+vIdVariacao+"'"
      : "UPDATE ECOM_SKU SET ESTOQUE ='"+pEstoque+"', ATIVO ='"+pAtivo+"', FULFILLMENT ='"+pFulfillment+"', CUSTO_ADICIONAL ='"+pCustoAdicional+"', COMISSAO_SKU ='"+pComissao+
        "', DT_GET ='"+vDataHr+"', STCODE ='"+vStatusCode+"', RETORNO ='"+vStatusNoGet+"' WHERE SKU ='"+vIdVariacao+"'";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOMSku)) {

			//region Preenchendo Parâmetros Da Query.
			statement.setInt(    1, pEstoque);
      statement.setString( 2, pAtivo);
      statement.setString( 3, pFulfillment);
      statement.setDouble( 4, pCustoAdicional);
      statement.setDouble( 5, pComissao);
      statement.setDate(   6, vDataHr);
      statement.setInt(    7, vStatusCode);
      statement.setString( 8, vStatusNoGet);
      statement.setString( 9, vIdVariacao);
      //statement.setDouble( 4, pFrete); //Frete na Consulta correta é o 4º
			//endregion

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas == 0) {
        loggerRobot.info("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU Identificador -> " + pIdentificadorCliente + " UserId: " + userId +" .QUERY: -> " + Qry_preenchida);
      }

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Atualizar Dados e ESTOQUE Na Tabela ECOM_SKU: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion

  //region Função Para Atualizar Dados Sem ESTOQUE De Um Produto Na Tabela ECOM_SKU.
  public void atualizaDadosNaECOMSKU(Connection pConexao, String pAtivo, String pFulfillment, /*double pFrete,*/ double pCustoAdicional, double pComissao, String pSku,
                                     boolean vEhVariacao, String vStatusNoGet, int vStatusCode, String pIdentificadorCliente, String userId) throws SQLException {

    Date vDataHr = new Date(System.currentTimeMillis());

/*
    String Qry_AtualizaECOMSku = vEhVariacao
        ? "UPDATE ECOM_SKU SET ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKUVARIACAO_MASTER = ?"
        : "UPDATE ECOM_SKU SET ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKU = ?";
*/

    String Qry_AtualizaECOMSku = vEhVariacao
        ? "UPDATE ECOM_SKU SET ATIVO = ?, FULFILLMENT = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKUVARIACAO_MASTER = ?"
        : "UPDATE ECOM_SKU SET ATIVO = ?, FULFILLMENT = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, DT_GET = ?, STCODE = ?, RETORNO = ? WHERE SKU = ?";

    String Qry_preenchida = vEhVariacao
        ? "UPDATE ECOM_SKU SET ATIVO ='"+pAtivo+"', FULFILLMENT ='"+pFulfillment+"', CUSTO_ADICIONAL ='"+pCustoAdicional+"', COMISSAO_SKU ='"+pComissao+
        "', DT_GET ='"+vDataHr+"', STCODE ='"+vStatusCode+"', RETORNO ='"+vStatusNoGet+"', WHERE SKUVARIACAO_MASTER ='"+pSku+"')"
        : "UPDATE ECOM_SKU SET ATIVO ='"+pAtivo+"', FULFILLMENT ='"+pFulfillment+"', CUSTO_ADICIONAL ='"+pCustoAdicional+"', COMISSAO_SKU ='"+pComissao+
        "', DT_GET ='"+vDataHr+"', STCODE ='"+vStatusCode+"', RETORNO ='"+vStatusNoGet+"', WHERE SKU ='"+pSku+"')";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOMSku)) {

			//region Preenchendo Parâmetros Da Query.
      statement.setString( 1, pAtivo);
      statement.setString( 2, pFulfillment);
      statement.setDouble( 3, pCustoAdicional);
      statement.setDouble( 4, pComissao);
      statement.setDate(   5, vDataHr);
      statement.setInt(    6, vStatusCode);
      statement.setString( 7, vStatusNoGet);
      statement.setString( 8, pSku);
      //statement.setDouble( 3, pFrete);// Vai na posição 3
			//endregion

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas == 0) {
        loggerRobot.info("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU Identificador -> " + pIdentificadorCliente + " UserId: " + userId +". Query: -> " + Qry_preenchida);
      }

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Atualizar Dados Na Tabela ECOM_SKU: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion

  //region Função Para Buscar Produto SIMPLES Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO existeNaTabelaMlSkuFull(Connection pConexao, String pSkuML) throws SQLException {
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, pSkuML);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int totalResult   = resultSet.getInt(1);
          DadosMlSkuFullDTO dadosMlSkuFullDTO = new DadosMlSkuFullDTO();
          dadosMlSkuFullDTO.setVExiste(totalResult);
          return dadosMlSkuFullDTO;
        }
        return null;
      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Verificar Se o SKU " + pSkuML + " Existe Na Tabela ML_SKU_FULL: -> " + excecao.getMessage());
      throw excecao;
    }
	}
  //endregion

  //region Função Para Buscar Produto VARIAÇÃO Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO variacaoExisteNaTabelaMlSkuFull(Connection pConexao, String pSkuML, String pVariacaoId) throws SQLException {
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ? AND VARIACAO_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, pSkuML);
      statement.setString(2, pVariacaoId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int totalResult   = resultSet.getInt(1);
          DadosMlSkuFullDTO dadosMlSkuFullDTO = new DadosMlSkuFullDTO();
          dadosMlSkuFullDTO.setVExiste(totalResult);
          return dadosMlSkuFullDTO;
        }
        return null;
      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Verificar Se A Variação " + pVariacaoId + "  Existe Na Tabela ML_SKU_FULL: -> " + excecao.getMessage());
      throw excecao;
    }
	}
  //endregion

  //region Função Para Inserir Informacões Na Tabela ESTOQUE_MKTP.
    public void inserirSkuIDNaESTOQUE_MKTP(Connection pConexao, int pCodID) throws SQLException {

      String Qry_InsertESTOQUE_MKTP = "INSERT INTO ESTOQUE_MKTP (CODID) VALUES (?)";

      try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertESTOQUE_MKTP)) {
        statement.setInt(1,  pCodID);
        statement.executeUpdate();

      } catch (SQLException excecao) {
        loggerRobot.severe("FALHA: Erro Ao Inserir ID Na Tabela ESTOQUE_MKTP: -> " + excecao.getMessage());
        throw excecao;
      }
    }
    //endregion

  //region Função Para Inserir Produto Na Tabela ML_SKU_FULL.
  public void inserirProdutoNaTabelaMlSkuFull(Connection pConexao, int pOrigem, int pCodID, String pSkuML, String pVariationId, String pVariacao, String pInventoryId,
                                              String pTitulo, String pStatus, double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado) throws SQLException {

    String Qry_InsertMlSkuFull = "INSERT INTO ML_SKU_FULL (DATAHR, ORIGEM_ID, CODID, SKU, VARIACAO_ID, VARIACAO, INVENTORY_ID, TITULO, ATIVO, VALOR, URL, CATALOGO, RELACIONADO) "
                               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    Date vDataHr = new Date(System.currentTimeMillis());

    String Qry_ComValores =
      "INSERT INTO ML_SKU_FULL (DATAHR, ORIGEM_ID, CODID, SKU, VARIACAO_ID, VARIACAO, INVENTORY_ID, TITULO, ATIVO, VALOR, URL, CATALOGO, RELACIONADO) VALUES ('" + vDataHr + "', " + pOrigem + ", "
          + pCodID + ", '" + pSkuML + "', '" + pVariationId + "', '" + pVariacao + "', '" + pInventoryId + "', '" + pTitulo + "', '" + pStatus + "', " + pPreco + ", '" + pImagemUrl + "', '"
          + pCatalogo + "', '" + pRelacionado + "')";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertMlSkuFull)) {

      //region Preenche Parâmetos da Query.
      statement.setDate(   1,  vDataHr);
      statement.setInt(    2,  pOrigem);
      statement.setInt(    3,  pCodID);
      statement.setString( 4,  pSkuML);
      statement.setString( 5,  pVariationId);
      statement.setString( 6,  pVariacao);
      statement.setString( 7,  pInventoryId);
      statement.setString( 8,  pTitulo);
      statement.setString( 9,  pStatus);
      statement.setDouble( 10, pPreco);
      statement.setString( 11, pImagemUrl);
      statement.setString( 12, pCatalogo);
      statement.setString( 13, pRelacionado);
      //endregion

      statement.executeUpdate();

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Inserir Dados Na Tabela ML_SKU_FULL: -> \n QUERY: -> " + Qry_ComValores + " \nMensagem: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion

  //region Função Para Atualizar Dados De Um Produto Na Tabela ML_SKU_FULL.
  public void atualizaProdutoNaTabelaMlSkuFull(Connection pConexao, String pInventoryId, String pStatus,
                                               double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado, String pSkuNotificacao, String pIdentificadorCliente, String userId) throws SQLException {

    String Qry_AtualizaMlSkuFull = "UPDATE ML_SKU_FULL SET INVENTORY_ID = ? , ATIVO = ?, VALOR = ?, URL = ?, CATALOGO = ?, RELACIONADO = ? WHERE SKU = ?";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaMlSkuFull)) {

      //region Preenchendo Parâmetros da Query.
      statement.setString( 1, pInventoryId);
      statement.setString( 2, pStatus);
      statement.setDouble( 3, pPreco);
      statement.setString( 4, pImagemUrl);
      statement.setString( 5, pCatalogo);
      statement.setString( 6, pRelacionado);
      statement.setString( 7, pSkuNotificacao);
      //endregion

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas == 0) { logger.info("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU Identificador -> " + pIdentificadorCliente + " UserId: " + userId +". Verificar Os Valores Fornecidos."); }

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Atualizar Dados Na Tabela ML_SKU_FULL: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion

  //region Função Para Inserir Produto SIMPLES Na Tabela ECOM_SKU_SEMVINCULO.
  public void inserirProdutoNaTabelaEcomSkuSemVinculo(Connection pConexao, int pOrigem, String pSkuML, String pSellerSku, String pTitulo, double pPreco, String pAnuncioUrl,
                                                      String pImagemUrl, int pCodID, int pVariacoes) throws SQLException {

    Date vDataHr = new Date(System.currentTimeMillis());
    String Qry_InsertEcomSkuSemVinculo = "INSERT INTO ECOM_SKU_SEMVINCULO (DATAHR, ORIGEM_ID, SKU, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID, VARIACOES) "
                                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    String Qry_preenchida = "INSERT INTO ECOM_SKU_SEMVINCULO(DATAHR, ORIGEM_ID, SKU, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID, VARIACOES) VALUES ("
        + "'" + vDataHr + "', " + pOrigem + ", '" + pSkuML + "', '" + pSellerSku + "', '" + pTitulo + "', '" + pPreco + "', " + pAnuncioUrl + ", '" + pImagemUrl + "', '" + pCodID + "', " + pVariacoes + ")";


    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertEcomSkuSemVinculo)) {

      //region Preenche Parâmetos da Query.
      statement.setDate(  1, vDataHr);
      statement.setInt(   2, pOrigem);
      statement.setString(3, pSkuML);
      statement.setString(4, pSellerSku);
      statement.setString(5, pTitulo);
      statement.setDouble(6, pPreco);
      statement.setString(7, pAnuncioUrl);
      statement.setString(8, pImagemUrl);
      statement.setInt(   9, pCodID);
      statement.setInt(  10, pVariacoes);
      //endregion

      statement.executeUpdate();

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Inserir Dados Na Tabela ECOM_SKU_SEMVINCULO: -> " + excecao.getMessage()+ "\nQry_preenchida: -> " + Qry_preenchida);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Inserir Produto VARIAÇÃO Na Tabela ECOM_SKU_SEMVINCULO.
  public void inserirVariacNaTabelaEcomSkuSemVinculo(Connection pConexao, int pOrigem, String pSkuML, String pIdVariacao, String pSellerSkuVariac, String pTitulo, double pPreco, String pAnuncioUrl,
                                                       String pImagemUrl, int pCodID) throws SQLException {

    Date vDataHr = new Date(System.currentTimeMillis());
    String Qry_InsertVariacEcomSkuSemVinculo = "INSERT INTO ECOM_SKU_SEMVINCULO (DATAHR, ORIGEM_ID, SKU, SKUVAR, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID) "
                                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    String Qry_preenchida = "INSERT INTO ECOM_SKU_SEMVINCULO(DATAHR, ORIGEM_ID, SKU, SKUVAR, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID) VALUES ("
        + "'" + vDataHr + "', " + pOrigem + ", '" + pSkuML + "', '" + pIdVariacao + "', '" + pSellerSkuVariac + "', '" + pTitulo + "', " + pPreco + ", '" + pAnuncioUrl + "', '" + pImagemUrl + "', " + pCodID + ")";


    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertVariacEcomSkuSemVinculo)) {

      //region Preenche Parâmetos da Query.
      statement.setDate(  1, vDataHr);
      statement.setInt(   2, pOrigem);
      statement.setString(3, pSkuML);
      statement.setString(4, pIdVariacao);
      statement.setString(5, pSellerSkuVariac);
      statement.setString(6, pTitulo);
      statement.setDouble(7, pPreco);
      statement.setString(8, pAnuncioUrl);
      statement.setString(9, pImagemUrl);
      statement.setInt(  10, pCodID);
      //endregion

      statement.executeUpdate();

    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Inserir Variação Na Tabela ECOM_SKU_SEMVINCULO: -> " + excecao.getMessage() + "\nQry_preenchida: -> " + Qry_preenchida);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Buscar o CODID Na Tabela MATERIAIS.
  public int buscaCodidNaTabelaMateriais(Connection pConexao, String pSellerSku) throws SQLException {
    String Qry_CODIDMateriais = "SELECT CODID FROM MATERIAIS WHERE COD_INTERNO = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_CODIDMateriais)) {
      statement.setString(1, pSellerSku);
      try(ResultSet resultSet = statement.executeQuery()) {

        //region Verifica Se Foi Retornado Dados Na Busca.
        if (resultSet.next()) {
          return resultSet.getInt("CODID");
        }
        return 0;
        //endregion

      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Buscar o CODID Do SKU Atual Na Tabela MATERIAIS: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion

  //region Função Para DELETAR Dados De Um Produto Na Tabela ECOM_SKU_SEMVINCULO.
  public void deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(Connection pConexao, String pSkuNotificacao) throws SQLException {

    String Qry_AtualizaECOM_Sku = "DELETE FROM ECOM_SKU_SEMVINCULO WHERE SKU = ?";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOM_Sku)) {
      statement.setString(1, pSkuNotificacao);

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas == 0) {
        logger.warning("Nenhum Dado Foi DELETADO Da Tabela ECOM_SKU_SEMVINCULO. Verificar Valores Fornecidos.");
      }
    } catch (SQLException excecao) {
      loggerRobot.severe("FALHA: Erro Ao DELETAR O SKU " + pSkuNotificacao + " Da Tabela ECOM_SKU_SEMVINCULO: -> " + excecao.getMessage());
      throw excecao;
    }
  }
  //endregion



}