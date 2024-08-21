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
  public boolean ignorarGetSku(Connection conexao) throws SQLException {
    logger.info("Buscando Parâmetro IGNORAR_GETSKU!!");
    try (Statement statement = conexao.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT VALOR FROM PARAMETROS_SYS WHERE PARAMETRO = 'IGNORAR_GETSKU' AND EMPRESA = 0")) {
      if (resultSet.next()) {
        String valorParametro = resultSet.getString("VALOR").trim();
        logger.info("Valor Do Parâmetro IGNORARGETSKU " + valorParametro);
        return "S".equalsIgnoreCase(valorParametro);
      }
    }
    return false;
  }

  public boolean verificaOrigemAtiva(Connection conexao, String userId) throws SQLException {
    logger.info("Buscando Parâmetro PEDIDO Do Seller No Banco");
    String Qry_BuscaPedido = "SELECT PEDIDO FROM ECOM_ORIGEM WHERE ORIGEM_ID = (SELECT ORIGEM FROM ECOM_METODOS WHERE CANAL = ?)";
    try (PreparedStatement statement = conexao.prepareStatement(Qry_BuscaPedido)){
      statement.setString(1, userId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String parametroPedido = resultSet.getString("PEDIDO").trim();
          logger.info("Configuração Do Pedido Encontrado  " + parametroPedido);
          return "S".equalsIgnoreCase(parametroPedido);
        }
      }
    } catch(SQLException excecao){
      excecao.printStackTrace();
    }
    return  false;
  }

  public DadosEcomMetodosDTO buscarTokenTemp (Connection conexao, String userId) throws SQLException {
    logger.info("Buscando Token Do Seller No Banco");
    String Qry_BuscaDadosEcomMetodos = "SELECT TOKEN_TEMP, ORIGEM FROM ECOM_METODOS WHERE CANAL = ?";
    try (PreparedStatement statement = conexao.prepareStatement(Qry_BuscaDadosEcomMetodos)){
      statement.setString(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String tokenTemp = resultSet.getString("TOKEN_TEMP");
          int    origem    = resultSet.getInt("ORIGEM");
          logger.info("SUCESSO: Token  Encontrado  " + tokenTemp);
          logger.info("SUCESSO: Origem Encontrada  " + origem);
          return new DadosEcomMetodosDTO(tokenTemp, origem);
        }
      }
    } catch(SQLException excecao) {
      excecao.printStackTrace();
    }
    return null;
  }

  public InfoItemsMLDTO buscarInformacoesNaECOM_SKU(Connection conexao, String skuML) throws SQLException {
    logger.info("Buscando SKU No Banco Do Seller");
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKU = ?";
    try (PreparedStatement statement = conexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, skuML);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          int    vCodID      = resultSet.getInt("MATERIAL_ID");
          String vSkuNoBanco = resultSet.getString("SKU").trim();
          String vEstaAtivo  = resultSet.getString("ATIVO").trim();
          String vEFull      = resultSet.getString("FULFILLMENT").trim();
          logger.info("SKU Encontrado No Banco ");

          InfoItemsMLDTO infoItemsEcomSkuDTO = new InfoItemsMLDTO();
          infoItemsEcomSkuDTO.setCodid(vCodID);
          infoItemsEcomSkuDTO.setSkuNoBanco(vSkuNoBanco);
          infoItemsEcomSkuDTO.setEstaAtivo(vEstaAtivo);
          infoItemsEcomSkuDTO.setEFulfillment(vEFull);

          return infoItemsEcomSkuDTO;
        }
        return null;
      }
    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
    return null;
  }

  public DadosMlSkuFullDTO buscarNaTabelaMlSkuFull(Connection conexao, String skuML) throws SQLException {
    logger.info("Buscando SKU Na Tabela ml_sku_full");
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ?";
    try (PreparedStatement statement = conexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, skuML);
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

  public void inserirInformacoesNaESTOQUE_MKTP(Connection conexao, int codID) throws SQLException {

    logger.info("Inserindo Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_InsertESTOQUE_MKTP = "INSERT INTO ESTOQUE_MKTP (CODID)" +
            "VALUES (:CODID)";

    try (PreparedStatement statement = conexao.prepareStatement(Qry_InsertESTOQUE_MKTP)) {
      statement.setInt(    3,  codID);
      statement.executeUpdate();

      logger.info("SUCESSO: Dados Inseridos Na Tabela ESTOQUE_MKTP!!");

    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
  }

  public void inserirInformacoesNaTabelaMlSkuFull(Connection conexao, int origem, int codID, String skuML, String variationId, String inventoryId, String titulo, String status,
                                                  double preco, String urlDaImagem, String catalogo, String relacionado) throws SQLException {

    logger.info("Inserindo Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_InsertMlSkuFull = "INSERT INTO ML_SKU_FULL (DATAHR, ORIGEM_ID, CODID, SKU, VARIACAO_ID, INVENTORY_ID, TITULO, ATIVO, VALOR, URL, CATALOGO, RELACIONADO )" +
                                 "VALUES (:DT, :ORIG, :CODID, :SKU, :VAR, :INV, :TIT, :ST, :VLR, :URL, :CATAG, :RELAT)";

    Date dataHr = new Date(System.currentTimeMillis());
    try (PreparedStatement statement = conexao.prepareStatement(Qry_InsertMlSkuFull)) {
      statement.setDate(   1,  dataHr);
      statement.setInt(    2,  origem);
      statement.setInt(    3,  codID);
      statement.setString( 4,  skuML);
      statement.setString( 5,  variationId);
      statement.setString( 6,  inventoryId);
      statement.setString( 7,  titulo);
      statement.setString( 8,  status);
      statement.setDouble( 9,  preco);
      statement.setString( 10, urlDaImagem);
      statement.setString( 11, catalogo);
      statement.setString( 12, relacionado);

      statement.executeUpdate();

      logger.info("SUCESSO: Dados Inseridos Na Tabela ML_SKU_FULL!!");

    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
  }

  public void atualizaInformacoesNaTabelaMlSkuFull(Connection conexao, String inventoryId, String status,
                                                   double preco, String urlDaImagem, String catalogo, String relacionado) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ML_SKU_FULL");

    //UPDATE INVENTORY_ID, STATUS, PRECO, CATALOGO, RELACIONADO

    String Qry_AtualizaMlSkuFull = "UPDATE ML_SKU_FULL (INVENTORY_ID, ATIVO, VALOR, URL, CATALOGO, RELACIONADO)" +
                                   "VALUES (:INV, :ST, :VLR, :URL, :CATAG, :RELAT)";

    Date dataHr = new Date(System.currentTimeMillis());
    try (PreparedStatement statement = conexao.prepareStatement(Qry_AtualizaMlSkuFull)) {
      statement.setString( 1, inventoryId);
      statement.setString( 2, status);
      statement.setDouble( 3, preco);
      statement.setString( 4, urlDaImagem);
      statement.setString( 5, catalogo);
      statement.setString( 6, relacionado);

      statement.executeUpdate();

      logger.info("SUCESSO: Dados Atualizados Na Tabela ML_SKU_FULL!!");

    } catch (SQLException excecao) {
      excecao.printStackTrace();
    }
  }








}
