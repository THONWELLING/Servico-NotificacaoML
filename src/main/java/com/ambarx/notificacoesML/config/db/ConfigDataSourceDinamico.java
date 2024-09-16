package com.ambarx.notificacoesML.config.db;

import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class DataSourceConfig {
  private static final Logger logger = Logger.getLogger(DataSourceConfig.class.getName());
  private static final List<String> PROTOCOLOS_TLS = Arrays.asList("TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1");

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
    String usuarioSql = cliente.getUsuarioSql().equalsIgnoreCase("padrao") ? System.getenv("USUARIO_SQLSERVER") : cliente.getUsuarioSql();
    String senhaSql   = cliente.getSenhaSql()  .equalsIgnoreCase("padrao") ? System.getenv("SENHA_SQLSERVER")   : cliente.getSenhaSql();

    if("ativo".equalsIgnoreCase(cliente.getStatusCliente()) || "prospect".equalsIgnoreCase(cliente.getStatusCliente()) && "remoto".equalsIgnoreCase(cliente.getTipoAcesso())) {

      logger.info("Conectando Ao Banco" + database);
      for (String vProtocoloTLS : PROTOCOLOS_TLS) {
				try {
          SSLContext contextoSSL = SSLContext.getInstance(vProtocoloTLS);
          contextoSSL.init(null, null, null);
          SSLContext.setDefault(contextoSSL);

          String vUrlConexao = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true;sslProtocol="  + vProtocoloTLS + ";";
          return DriverManager.getConnection(vUrlConexao, usuarioSql, senhaSql);

				} catch (Exception e) {
					logger.log(Level.INFO, "FALHA Ao Conectar Usando O Protocolo " + vProtocoloTLS);
				}
			}
      logger.log(Level.SEVERE, "Nenhum Protocolo TLS Foi Capaz de Estabelecer Conexão.");
    }
    return null;
  }

  public static void fecharConexao(Connection conexao) throws SQLException {
    if (conexao != null) {
      try {
        conexao.close();
      } catch (SQLException e) {
        logger.log(Level.SEVERE,"Erro Ao Fechar Conexão", e);
      }
    }
  }
}


/*
* if("ativo".equalsIgnoreCase(cliente.getStatusCliente()) || "prospect".equalsIgnoreCase(cliente.getStatusCliente()) && "remoto".equalsIgnoreCase(cliente.getTipoAcesso())) {

      //System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");
      logger.info("Conectando Ao Banco" + database);
      String urlConexao = "";
      if (database.equalsIgnoreCase("Ambar70") || database.equalsIgnoreCase("BancoAmbarTRP")) {
        urlConexao = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1;";
      } else {
        urlConexao = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true";
      }
      return DriverManager.getConnection(urlConexao, usuarioSql, senhaSql);
    }
    return null;
* */