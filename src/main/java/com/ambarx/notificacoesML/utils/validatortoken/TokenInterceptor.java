package com.ambarx.notificacoesML.utils.validatortoken;

import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import com.ambarx.notificacoesML.utils.enviroment.EnvProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class TokenInterceptor {
   public static void validarToken(String token) throws Exception {

    if (token == null || token.isEmpty()) {
        throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido!!!");
    }
    String tokenSemBearer = token.replace("Bearer ", "");
    if (tokenSemBearer.length() != 50) {
      throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Inválido!!!");
    }
  }
  public static String validarCliente(ConexaoDTO cliente) throws Exception {
    String mensagemRetorno;

    if (cliente != null) {
      if (cliente.getStatusCliente().equalsIgnoreCase("ativo") ||
          cliente.getStatusCliente().equalsIgnoreCase("prospect")){
        if (cliente.getTipoAcesso() .equalsIgnoreCase("remoto")){
          // Atribuindo o endereço IP correto ao campo servidor
          String servidor = cliente.getServidor();
          if        (servidor.equalsIgnoreCase("ambarcloud")) {
            cliente.setServidor(EnvProperties.getValue("AMBARCLOUD"));
          } else if (servidor.equalsIgnoreCase("ambarcloud2")) {
            cliente.setServidor(EnvProperties.getValue("AMBARCLOUD2"));
          } else if (servidor.equalsIgnoreCase("ambarcloud3")) {
            cliente.setServidor(EnvProperties.getValue("AMBARCLOUD3"));
          }

          //Atribuindo o valore correto ao usuário_sql
          String usuarioSql = cliente.getUsuarioSql();
          if (usuarioSql.equalsIgnoreCase("padrao")){
            cliente.setUsuarioSql(EnvProperties.getValue("USUARIO_SQLSERVER"));
          }

          //Atribuindo o valor correto a senha_sql
          String senhaSql = cliente.getSenhaSql();
          if (senhaSql.equalsIgnoreCase("padrao")) {
            cliente.setSenhaSql(EnvProperties.getValue("SENHA_SQLSERVER"));
          }
          String url = "jdbc:sqlserver://" + cliente.getServidor() + ":1433;databaseName=" + cliente.getBanco()
              + ";encrypt=true;trustServerCertificate=true;";
          try{
            Connection conexao = DriverManager.getConnection(url, cliente.getUsuarioSql(), cliente.getSenhaSql());
            mensagemRetorno    = "Conectado Com Sucesso!! Bem Vindo Ao Banco Do Cliente " + cliente.getIdentificadorCliente().toUpperCase();
            conexao.close();
          } catch(SQLException exception) {
            mensagemRetorno = "Erro Ao Conectar Ao Banco De Dados!!";
            exception.printStackTrace();
          }
        } else {
          mensagemRetorno = "Cliente Não possui Acesso Remoto Configurado!!";
          throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, mensagemRetorno);
        }
      } else {
        mensagemRetorno = "Status Do Cliente Não Permite Conexão!!";
        throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, mensagemRetorno);
      }
    } else {
      mensagemRetorno = "Cliente Não Encontrado Ou Inexistente";
      throw  new ResponseStatusException(HttpStatus.NOT_FOUND, mensagemRetorno);
    }
    return mensagemRetorno;
  }
}