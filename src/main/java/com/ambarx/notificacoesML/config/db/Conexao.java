package com.ambarx.notificacoesML.config.db;

import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Conexao {
  private static final Logger logger = Logger.getLogger(Conexao.class.getName());

  public static Connection conectar(ConexaoDTO cliente) throws SQLException {
    String port   = "1433";
    String server = System.getenv(cliente.getServidor());

    if(server  == null) {
      server = cliente.getServidor();
      if (server != null && !server.isEmpty()) {
        String[] partes = server.split(",");
        if (partes.length == 2) {
          server = partes[0];
          port   = partes[1];
        }
      }
    }

    String database   = cliente.getBanco();
    String user       = cliente.getUsuarioSql();
    String usuarioSql = user.equalsIgnoreCase("padrao") ? System.getenv("USUARIO_SQLSERVER") : user;
    String password   = cliente.getSenhaSql();
    String senhaSql   = user.equalsIgnoreCase("padrao") ? System.getenv("SENHA_SQLSERVER")   : user;
    String tokenMd5   = cliente.getTokenMd5();
    String tokenSha   = cliente.getTokenSha();

    if(cliente.getStatusCliente().equalsIgnoreCase("ativo") ||
       cliente.getStatusCliente().equalsIgnoreCase("prospect") &&
       cliente.getTipoAcesso().equalsIgnoreCase("remoto")) {

      //System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");
      logger.info("Conectando Ao Banco" + database);
      String urlConexao = "";
      if (database.equalsIgnoreCase("Ambar70") || database.equalsIgnoreCase("BancoAmbarTRP")) {
        urlConexao = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true;sslProtocol=TLSv1.2;";
      } else {
        urlConexao = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true";
      }
      return DriverManager.getConnection(urlConexao, usuarioSql, senhaSql);
    }
    return null;
  }

  public static void fecharConexao(Connection conexao) throws SQLException {
    if (conexao != null) {
      try {
        conexao.close();
      } catch (SQLException e) {
        logger.log(Level.SEVERE,"Erro Ao Fechar Conexão", e);
        throw e;
      }
    }
  }
}