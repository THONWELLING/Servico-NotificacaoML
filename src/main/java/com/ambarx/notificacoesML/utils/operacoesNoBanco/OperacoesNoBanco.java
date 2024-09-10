package com.ambarx.notificacoesML.utils.operacoesNoBanco;

import com.ambarx.notificacoesML.dto.infodobanco.DadosEcomMetodosDTO;
import com.ambarx.notificacoesML.dto.infodobanco.DadosMlSkuFullDTO;
import com.ambarx.notificacoesML.dto.item.InfoItemsMLDTO;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class OperacoesNoBanco {
  private static final Logger logger = Logger.getLogger(OperacoesNoBanco.class.getName());

  //region Função Para Buscar o Valor Parâmetro IGNORAR_GETSKU.
  public boolean ignorarGetSku(Connection pConexao) throws SQLException {
    logger.info("Buscando Parâmetro IGNORAR_GETSKU.");
    try (Statement statement = pConexao.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT VALOR FROM PARAMETROS_SYS WHERE PARAMETRO = 'IGNORAR_GETSKU' AND EMPRESA = 0")) {
      if (resultSet.next()) {
        String valorParametro = resultSet.getString("VALOR").trim();
        if (valorParametro.trim().isEmpty()) {
          logger.info("Parâmetro IGNORARGETSKU Está Vazio Ou Null. Considerar N.");
          return false;
        }
        logger.info("IGNORARGETSKU Encontrado: Valor Do Parâmetro." + valorParametro);
        return "S".equalsIgnoreCase(valorParametro);
      }
    } catch (SQLException excecao){
      logger.log(Level.SEVERE, "FALHA: Erro Ao Buscar Parâmetro IGNORARGETSKU.", excecao);
      throw excecao;
    }
    return false;
  }
  //endregion

  //region Função Para Buscar o Valor Do Parâmetro PEDIDO Na ECOM_ORIGEM ("S" Significa Origem Ativa).
  public boolean buscaParamPedido(Connection pConexao, String pUserId) throws SQLException {
    logger.info("Buscando Parâmetro PEDIDO No Banco Do Seller.");
    String Qry_BuscaPedido = "SELECT PEDIDO FROM ECOM_ORIGEM WHERE ORIGEM_ID = (SELECT ORIGEM FROM ECOM_METODOS WHERE CANAL = ?)";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_BuscaPedido)){
      statement.setString(1, pUserId);
      try(ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String vParametroPedido = resultSet.getString("PEDIDO").trim();
          if (vParametroPedido.trim().isEmpty()) {
            logger.info("Configuração Do Pedido Vazio ou NulL. Considerar N.");
            return false;
          }
          logger.info("Parâmetro PEDIDO Encontrado. Valor Do Parâmetro." + vParametroPedido);
          return "S".equalsIgnoreCase(vParametroPedido);
        }
      }
    } catch(SQLException excecao){
      logger.log(Level.SEVERE, "FALHA: Erro Ao Buscar Parâmetro PEDIDO.", excecao);
      throw excecao;
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
      logger.log(Level.SEVERE, "FALHA: Erro Ao Buscar O Token Do Seller.", excecao);
      throw excecao;
    }
    return null;
  }
  //endregion

  //region Função Para Buscar Produto SIMPLES Na Tabela ECOM_SKU.
  public InfoItemsMLDTO buscaProdutoNaECOM_SKU(Connection pConexao, String pSkuML, int pOrigem) throws SQLException {
    logger.info("Buscando SKU No Banco Do Seller");
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKU = ? AND ORIGEM_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, pSkuML);
      statement.setInt(   2, pOrigem);
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
      logger.log(Level.SEVERE, "FALHA: Erro Ao Verificar Se o SKU Existe Na Tabela ECOM_SKU.");
      throw excecao;
    }
	}
  //endregion

  // region Função Para Buscar Produto Variação Na Tabela ECOM_SKU.
  public InfoItemsMLDTO buscaProdutovARIACAONaECOM_SKU(Connection pConexao, String pIDVariacao, int pOrigem) throws SQLException {
    logger.info("Buscando SKU No Banco Do Seller");
    String Qry_InfoDBSeller = "SELECT MATERIAL_ID, SKU, ATIVO, FULFILLMENT FROM ECOM_SKU WHERE SKUVARIACAO_MASTER = ? AND ORIGEM_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBSeller)) {
      statement.setString(1, pIDVariacao);
      statement.setInt(   2, pOrigem);
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
      logger.log(Level.SEVERE, "FALHA: Erro Ao Verificar Se a Variação Existe Na Tabela ECOM_SKU.");
      throw excecao;
    }
	}
  //endregion

  //region Função Para Atualizar Dados De Um Produto Na Tabela ECOM_SKU.
  public void atualizaProdutoNaTabelaECOM_SKU(Connection pConexao, String pSkuVariacao, String pSkuNotificacao) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ECOM_SKU.");

    String Qry_AtualizaECOM_Sku = "UPDATE ECOM_SKU SET SKUVARIACAO_MASTER = ? WHERE SKU = ?";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOM_Sku)) {
      statement.setString(1, pSkuVariacao);
      statement.setString(2, pSkuNotificacao);

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas > 0) {
        logger.info("SUCESSO: Dados Atualizados Na Tabela ECOM_SKU.");
      } else {
        logger.warning("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU. Verificar Os Valores Fornecidos.");
      }

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Atualizar Dados Na Tabela ECOM_SKU.");
      throw excecao;
    }
  }
  //endregion

  //region Função Para Atualizar Dados e ESTOQUE De Um Produto Na Tabela ECOM_SKU.
  public void atualizaDadosEEstoqNaECOMSKU(Connection pConexao, int pEstoque, String pAtivo, String pFulfillment, double pFrete, double pCustoAdicional, double pComissao, double pPrecoDe, double pPrecoPor, String pSkuNotificacao) throws SQLException {

    logger.info("Atualizando Dados e ESTOQUE do SKU Na Tabela ECOM_SKU.");

    String Qry_AtualizaECOMSku = "UPDATE ECOM_SKU SET ESTOQUE = ?, ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, VLR_SITE1 = ?, VLR_SITE2 = ?, DT_GET = ?" +
                                  " WHERE SKU = ?";

    Date vDataHr = new Date(System.currentTimeMillis());
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOMSku)) {

			//region Preenchendo Parâmetros Da Query.
			statement.setInt(    1, pEstoque);
      statement.setString( 2, pAtivo);
      statement.setString( 3, pFulfillment);
      statement.setDouble( 4, pFrete);
      statement.setDouble( 5, pCustoAdicional);
      statement.setDouble( 6, pComissao);
      statement.setDouble( 7, pPrecoDe);
      statement.setDouble( 8, pPrecoPor);
      statement.setDate(   9, vDataHr);
      statement.setString(10, pSkuNotificacao);
			//endregion

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas > 0) {
        logger.info("SUCESSO: Dados e ESTOQUE Atualizados Na Tabela ECOM_SKU.");
      } else {
        logger.warning("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU. Verificar Os Valores Fornecidos.");
      }

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Atualizar Dados e ESTOQUE Na Tabela ECOM_SKU.");
      throw excecao;
    }
  }
  //endregion

  //region Função Para Atualizar Dados e ESTOQUE De Um Produto Na Tabela ECOM_SKU.
  public void atualizaDadosNaECOMSKU(Connection pConexao, String pAtivo, String pFulfillment, double pFrete, double pCustoAdicional, double pComissao, double pPrecoDe, double pPrecoPor, String pSkuNotificacao) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ECOM_SKU.");

    String Qry_AtualizaECOMSku = "UPDATE ECOM_SKU SET ATIVO = ?, FULFILLMENT = ?, CUSTO_FRETE = ?, CUSTO_ADICIONAL = ?, COMISSAO_SKU = ?, VLR_SITE1 = ?, VLR_SITE2 = ?, DT_GET = ?" +
                                  " WHERE SKU = ?";

    Date vDataHr = new Date(System.currentTimeMillis());
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOMSku)) {

			//region Preenchendo Parâmetros Da Query.
      statement.setString( 1, pAtivo);
      statement.setString( 2, pFulfillment);
      statement.setDouble( 3, pFrete);
      statement.setDouble( 4, pCustoAdicional);
      statement.setDouble( 5, pComissao);
      statement.setDouble( 6, pPrecoDe);
      statement.setDouble( 7, pPrecoPor);
      statement.setDate(   8, vDataHr);
      statement.setString( 9, pSkuNotificacao);
			//endregion

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas > 0) {
        logger.info("SUCESSO: Dados Atualizados Na Tabela ECOM_SKU.");
      } else {
        logger.warning("Nenhum Dado Foi Atualizado Na Tabela ECOM_SKU. Verificar Os Valores Fornecidos.");
      }

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Atualizar Dados Na Tabela ECOM_SKU.");
      throw excecao;
    }
  }
  //endregion

  //region Função Para Buscar Produto SIMPLES Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO existeNaTabelaMlSkuFull(Connection pConexao, String pSkuML) throws SQLException {
    logger.info("Buscando SKU Na Tabela ML_SKU-FULL");
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
      logger.log(Level.SEVERE, "FALHA: Erro Ao Verificar Se o SKU Existe Na Tabela ML_SKU_FULL.");
      throw excecao;
    }
	}
  //endregion

  //region Função Para Buscar Produto VARIAÇÃO Na Tabela ML_SKU_FULL e Setar vExiste.
  public DadosMlSkuFullDTO existeNaTabelaMlSkuFull(Connection pConexao, String pSkuML, String pVariacaoId) throws SQLException {
    logger.info("Buscando Variação Na Tabela ML_SKU_FULL");
    String Qry_InfoDBMlSkuFull = "SELECT COUNT(*) FROM ML_SKU_FULL WHERE SKU = ? AND VARIACAO_ID = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_InfoDBMlSkuFull)) {
      statement.setString(1, pSkuML);
      statement.setString(2, pVariacaoId);
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
      logger.log(Level.SEVERE, "FALHA: Erro Ao Verificar Se a Variação Existe Na Tabela ML_SKU_FULL.");
      throw excecao;
    }
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
        logger.log(Level.SEVERE, "FALHA: Erro Ao Inserir IDs Na Tabela ESTOQUE_MKTP.");
        throw excecao;
      }
    }
    //endregion

  //region Função Para Inserir Produto Na Tabela ML_SKU_FULL.
  public void inserirProdutoNaTabelaMlSkuFull(Connection pConexao, int pOrigem, int pCodID, String pSkuML, String pVariationId, String pVariacao, String pInventoryId, String pTitulo, String pStatus,
                                              double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado) throws SQLException {

    logger.info("Inserindo Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_InsertMlSkuFull = "INSERT INTO ML_SKU_FULL (DATAHR, ORIGEM_ID, CODID, SKU, VARIACAO_ID, VARIACAO, INVENTORY_ID, TITULO, ATIVO, VALOR, URL, CATALOGO, RELACIONADO) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    Date vDataHr = new Date(System.currentTimeMillis());
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
      logger.info("SUCESSO: Dados Inseridos Na Tabela ML_SKU_FULL.");

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Inserir Dados Na Tabela ML_SKU_FULL.", excecao);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Atualizar Dados De Um Produto Na Tabela ML_SKU_FULL.
  public void atualizaProdutoNaTabelaMlSkuFull(Connection pConexao, String pInventoryId, String pStatus,
                                               double pPreco, String pImagemUrl, String pCatalogo, String pRelacionado, String pSkuNotificacao) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ML_SKU_FULL");

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
      if (linhasAfetadas > 0) {
        logger.info("SUCESSO: Dados Atualizados Na Tabela ML_SKU_FULL.");
      } else {
        logger.warning("Nenhum Dado Foi Atualizado Na Tabela ML_SKU_FULL. Verificar Os Valores Fornecidos.");
      }

    } catch (SQLException excecao) {

      logger.log(Level.SEVERE, "FALHA: Erro Ao Atualizar Dados Na Tabela ML_SKU_FULL", excecao);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Inserir Produto SIMPLES Na Tabela ECOM_SKU_SEMVINCULO.
  public void inserirProdutoNaTabelaEcomSkuSemVinculo(Connection pConexao, int pOrigem, String pSkuML, String pSellerSku, String pTitulo, double pPreco, String pAnuncioUrl,
                                                      String pImagemUrl, int pCodID, int pVariacoes) throws SQLException {

    logger.info("Inserindo Dados do SKU Na Tabela ML_SKU_FULL");

    String Qry_InsertEcomSkuSemVinculo = "INSERT INTO ECOM_SKU_SEMVINCULO (DATAHR, ORIGEM_ID, SKU, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID, VARIACOES) " +
                                                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    Date vDataHr = new Date(System.currentTimeMillis());
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
      logger.info("SUCESSO: Dados Inseridos Na Tabela ECOM_SKU_SEMVINCULO.");

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Inserir Dados Na Tabela ECOM_SKU_SEMVINCULO.", excecao);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Inserir Produto VARIAÇÃO Na Tabela ECOM_SKU_SEMVINCULO.
  public void inserirVariacNaTabelaEcomSkuSemVinculo(Connection pConexao, int pOrigem, String pSkuML, String pIdVariacao, String pSellerSkuVariac, String pTitulo, double pPreco, String pAnuncioUrl,
                                                       String pImagemUrl, int pCodID) throws SQLException {

    logger.info("Inserindo Variações do SKU Na Tabela ML_SKU_FULL.");

    String Qry_InsertVariacEcomSkuSemVinculo = "INSERT INTO ECOM_SKU_SEMVINCULO (DATAHR, ORIGEM_ID, SKU, SKU_VAR, SELLER_SKU, TITULO, VALOR, LINK_URL, LINK_IMAGE, CODID) " +
                                                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    Date vDataHr = new Date(System.currentTimeMillis());
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
      logger.info("SUCESSO: Dados Inseridos Na Tabela ECOM_SKU_SEMVINCULO.");

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Inserir Variação Na Tabela ECOM_SKU_SEMVINCULO.", excecao);
      throw excecao;
    }
  }
  //endregion

  //region Função Para Buscar o CODID Na Tabela MATERIAIS.
  public int buscaCodidNaTabelaMateriais(Connection pConexao, String pSellerSku) throws SQLException {
    logger.info("Buscando SKU No Banco Do Seller");
    String Qry_CODIDMateriais = "SELECT CODID FROM MATERIAIS WHERE COD_INTERNO = ?";
    try (PreparedStatement statement = pConexao.prepareStatement(Qry_CODIDMateriais)) {
      statement.setString(1, pSellerSku);
      try(ResultSet resultSet = statement.executeQuery()) {

        //region Verifica Se Foi Retornado Dados Na Busca.
        if (resultSet.next()) {
          int vCodID = resultSet.getInt("CODID");
          logger.info("CODID Encontrado Na Tabela Materiais.");

          return vCodID;
        }
        return 0;
        //endregion

      }
    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao Verificar Se o CODID Existe Na Tabela MATERIAIS.");
      throw excecao;
    }
  }
  //endregion

  //region Função Para DELETAR Dados De Um Produto Na Tabela ECOM_SKU_SEMVINCULO.
  public void deletaProdutoNaTabelaECOM_SKU_SEMVINCULO(Connection pConexao, String pSkuNotificacao) throws SQLException {

    logger.info("Atualizando Dados do SKU Na Tabela ECOM_SKU_SEMVINCULO.");

    String Qry_AtualizaECOM_Sku = "DELETE FROM ECOM_SKU_SEMVINCULO WHERE SKU = ?";

    try (PreparedStatement statement = pConexao.prepareStatement(Qry_AtualizaECOM_Sku)) {
      statement.setString(1, pSkuNotificacao);

      int linhasAfetadas = statement.executeUpdate();
      if (linhasAfetadas > 0) {
        logger.info("SUCESSO: Dados DELETADOS Na Tabela ECOM_SKU_SEMVINCULO.");
      } else {
        logger.warning("Nenhum Dado Foi DELETADO Na Tabela ECOM_SKU_SEMVINCULO. Verificar Os Valores Fornecidos.");
      }

    } catch (SQLException excecao) {
      logger.log(Level.SEVERE, "FALHA: Erro Ao DELETAR Dados Na Tabela ECOM_SKU_SEMVINCULO.");
      throw excecao;
    }
  }
  //endregion



}