package com.ambarx.notificacoesML.utils.operacoesNoBanco;

import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.logging.Logger;

@Component
public class OperacoesNoBanco {
  private static final Logger logger = Logger.getLogger(OperacoesNoBanco.class.getName());

  //region Função Para Buscar o Valor Parâmetro IGNORAR_GETSKU.
  public boolean ignorarGetSku(Connection pConexao) throws SQLException {
    logger.info("Buscando Parâmetro IGNORAR_GETSKU!!");
    try (Statement statement = pConexao.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT VALOR FROM PARAMETROS_SYS WHERE PARAMETRO = 'IGNORAR_GETSKU' AND EMPRESA = 0")) {
      if (resultSet.next()) {
        String valorParametro = resultSet.getString("VALOR").trim();
        logger.info("Valor Do Parâmetro IGNORARGETSKU " + valorParametro);
        return "S".equalsIgnoreCase(valorParametro);
      }
    }
    return false;
  }
  //endregion

  //region Função Para Buscar o Valor Do Parâmetro PEDIDO Na ECOM_ORIGEM ("S" Significa Origem Ativa).
  public boolean buscaParamPedido(Connection pConexao, String pUserId) throws SQLException {
    logger.info("Buscando Parâmetro PEDIDO Do Seller No Banco");
    String Qry_BuscaPedido = "SELECT PEDIDO FROM ECOM_ORIGEM WHERE ORIGEM_ID = (SELECT ORIGEM FROM ECOM_METODOS WHERE CANAL = ?)";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_BuscaPedido)){
      statement.setString(1, pUserId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String vParametroPedido = resultSet.getString("PEDIDO").trim();
          logger.info("Configuração Do Pedido Encontrado  " + vParametroPedido);
          return "S".equalsIgnoreCase(vParametroPedido);
        }
      }
    } catch(SQLException excecao){
      excecao.printStackTrace();
    }
    return  false;
  }
  //endregion

  //region Função Para Buscar O Token Do Seller No Banco.
  public DadosEcomMetodosDTO buscarTokenTemp (Connection pConexao, String pUserId) throws SQLException {
    logger.info("Buscando Token Do Seller No Banco");
    String Qry_BuscaDadosEcomMetodos = "SELECT TOKEN_TEMP, ORIGEM FROM ECOM_METODOS WHERE CANAL = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_BuscaDadosEcomMetodos)){
      statement.setString(1, pUserId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String vTokenTemp = resultSet.getString("TOKEN_TEMP");
          int    vOrigem    = resultSet.getInt("ORIGEM");
          logger.info("SUCESSO: Token  Encontrado  " + vTokenTemp);
          logger.info("SUCESSO: Origem Encontrada  " + vOrigem);
          return new DadosEcomMetodosDTO(vTokenTemp, vOrigem);
        }
      }
    } catch(SQLException excecao) {
      excecao.printStackTrace();
    }
    return null;
  }
  //endregion

  //region Função Para Buscar O Produto Na Tabela ECOM_SKU.
  public InfoItemsMLDTO buscaProdutoNaECOM_SKU(Connection pConexao, String pSkuML) throws SQLException {
    logger.info("Buscando SKU No Banco Do Seller");
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKU = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, pSkuML);
      try(ResultSet resultSet = statement.executeQuery()) {

        //region Verifica Se Foi Retornado Dados Na Busca.
        if (resultSet.next()) {

          //region Pega As Informações Do Banco e Guarda Nas Variáveis
          int    vCodID      = resultSet.getInt("MATERIAL_ID");
          String vSkuNoBanco = resultSet.getString("SKU").trim();
          String vEstaAtivo  = resultSet.getString("ATIVO").trim();
          String vEFull      = resultSet.getString("FULFILLMENT").trim();
          logger.info("SKU Encontrado No Banco ");
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
      excecao.printStackTrace();
    }
    return null;
  }
  //endregion

  //region Função Para Buscar Produto SIMPLES Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO existeNaTabelaMlSkuFull(Connection pConexao, String pSkuML) throws SQLException {
    logger.info("Buscando SKU Na Tabela ml_sku_full");
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, pSkuML);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int totalResult   = resultSet.getInt(1);
          logger.info("SKU Encontrado Na Tabela ML_SKU_FULL");

          DadosMlSkuFullDTO dadosMlSkuFullDTO = new DadosMlSkuFullDTO();
          dadosMlSkuFullDTO.setVExiste(totalResult);
          return dadosMlSkuFullDTO;
        }
        return null;
      }
    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
    return null;
  }
  //endregion

  //region Função Para Buscar Produto VARIAÇÃO Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO existeNaTabelaMlSkuFull(Connection pConexao, String pSkuML, Long pVariacaoId) throws SQLException {
    logger.info("Buscando SKU Na Tabela ml_sku_full");
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ? AND VARIACAO_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, pSkuML);
      statement.setLong(2  , pVariacaoId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int totalResult   = resultSet.getInt(1);
          logger.info("SKU Encontrado Na Tabela ML_SKU_FULL");

          DadosMlSkuFullDTO dadosMlSkuFullDTO = new DadosMlSkuFullDTO();
          dadosMlSkuFullDTO.setVExiste(totalResult);
          return dadosMlSkuFullDTO;
        }
        return null;
      }
    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
    return null;
  }
  //endregion

  //region Função Para Inserir Informacões Na Tabela ESTOQUE_MKTP.
    public void inserirSkuIDNaESTOQUE_MKTP(Connection pConexao, int pCodID) throws SQLException {

      logger.info("Inserindo ID do SKU Na Tabela ESTOQUE_MKTP");

      String Qry_InsertESTOQUE_MKTP = "INSERT INTO ESTOQUE_MKTP (CODID)" +
              "VALUES (:CODID)";

      try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertESTOQUE_MKTP)) {
        statement.setInt(    3,  pCodID);
        statement.executeUpdate();

        logger.info("SUCESSO: IDs Inseridos Na Tabela ESTOQUE_MKTP!!");

      } catch (SQLException excecao) {
        excecao.printStackTrace();
      }
    }
    //endregion

  //region Função Para Inserir Produto Na Tabela ML_SKU_FULL.
  public void inserirProdutoNaTabelaMlSkuFull(Connection pConexao, int pOrigem, int pCodID, String pSkuML, String pVariationId, String pVariacao, String pInventoryId, String pTitulo, String pStatus,
                                              double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado) throws SQLException {

    logger.info("Inserindo Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_InsertMlSkuFull = "INSERT INTO ML_SKU_FULL (DATAHR, ORIGEM_ID, CODID, SKU, VARIACAO_ID, VARIACAO, INVENTORY_ID, TITULO, ATIVO, VALOR, URL, CATALOGO, RELACIONADO )" +
                                 "VALUES (:DT, :ORIG, :CODID, :SKU, :VARID, :VAR, :INV, :TIT, :ST, :VLR, :URL, :CATAG, :RELAT)";

    Date vDataHr = new Date(System.currentTimeMillis());
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InsertMlSkuFull)) {

      //region Preenche Parâmetos da Query.
      statement.setDate(   1,  vDataHr);
      statement.setInt(    2,  pOrigem);
      statement.setInt(    3,  pCodID);
      statement.setString( 4,  pSkuML);
      statement.setString( 5,  pVariationId);
      statement.setString( 6,  pInventoryId);
      statement.setString( 7,  pTitulo);
      statement.setString( 8,  pStatus);
      statement.setDouble( 9,  pPreco);
      statement.setString( 10, pImagemUrl);
      statement.setString( 11, pCatalogo);
      statement.setString( 12, pRelacionado);
      //endregion

      statement.executeUpdate();
      logger.info("SUCESSO: Dados Inseridos Na Tabela ML_SKU_FULL!!");

    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
  }
  //endregion

  //region Função Para Atualizar Dados De Um Produto Na Tabela   ML_SKU_FULL.
  public void atualizaProdutoNaTabelaMlSkuFull(Connection pConexao, String pInventoryId, String pStatus,
                                               double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_AtualizaMlSkuFull = "UPDATE ML_SKU_FULL (INVENTORY_ID, ATIVO, VALOR, URL, CATALOGO, RELACIONADO)" +
                                   "VALUES (:INV, :ST, :VLR, :URL, :CATAG, :RELAT)";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaMlSkuFull)) {

      //region Preenchendo Parâmetros da Query.
      statement.setString( 1, pInventoryId);
      statement.setString( 2, pStatus);
      statement.setDouble( 3, pPreco);
      statement.setString( 4, pImagemUrl);
      statement.setString( 5, pCatalogo);
      statement.setString( 6, pRelacionado);
      //endregion

      statement.executeUpdate();
      logger.info("SUCESSO: Dados Atualizados Na Tabela ML_SKU_FULL!!");

    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
  }
  //endregion







}
