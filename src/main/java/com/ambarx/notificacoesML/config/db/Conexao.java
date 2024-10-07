package com.ambarx.notificacoesML.config.db;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
@Component
public class Conexao {
  private static final Logger loggerRobot   = LoggerConfig.getLoggerRobot();
  private final List<String> PROTOCOLOS_TLS = Arrays.asList("TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1");
  private final ConfigDataSourceDinamico dataSourceDinamico;

  public Connection conectar(ConexaoDTO cliente) throws SQLException, UnknownHostException {
    String port   = "1433";
    String server = System.getenv(cliente.getServidor());

    InetAddress objEnderecoIP = InetAddress.getByName(server); //Pega o IP Baseado No DNS
    String        vEnderecoIP = objEnderecoIP.getHostAddress();

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

      Exception ultimaExcecao = null;

      for (String vProtocoloTLS : PROTOCOLOS_TLS) {
        try {
          SSLContext contextoSSL = SSLContext.getInstance(vProtocoloTLS);
          contextoSSL.init(null, null, null);
          SSLContext.setDefault(contextoSSL);

          String vUrlConexao = "eletropartes".equalsIgnoreCase(server) ?
            "jdbc:sqlserver://" + vEnderecoIP + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true;sslProtocol=" + vProtocoloTLS + ";"
          :
            "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true;sslProtocol=" + vProtocoloTLS + ";";

          DataSource dataSource = dataSourceDinamico.createDataSource(vUrlConexao, usuarioSql, senhaSql);
          return dataSource.getConnection();

        } catch (Exception e) {
          ultimaExcecao = e;
        }
      }

			if (ultimaExcecao != null) {
				loggerRobot.warning("\nFALHA Ao Conectar Ao Banco: " + database         +
                                 "\nSeller: -> " + cliente.getIdentificadorCliente() +
                                 "\nMensagem Do Ultimo Erro  -> " + ultimaExcecao.getMessage());
			}

			loggerRobot.severe("Nenhum Protocolo TLS Foi Capaz de Estabelecer Conexão com o Banco Do Seller. " + cliente.getIdentificadorCliente()
                            + " Banco: -> " + cliente.getBanco());
    }
    return null;

  }

  public void fecharConexao(Connection conexao) throws SQLException {
    if (conexao != null) {
      try {
        conexao.close();
      } catch (SQLException e) {
        loggerRobot.log(Level.SEVERE, "Erro Ao Fechar Conexão", e.getMessage());
        throw e;
      }
    }
  }
}