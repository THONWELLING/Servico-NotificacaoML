package com.ambarx.notificacoesML.httpclients;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.customizedExceptions.LimiteRequisicaoMLException;
import com.ambarx.notificacoesML.customizedExceptions.NotFoundMLException;
import com.ambarx.notificacoesML.utils.RespostaAPI;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class MercadoLivreHttpClient {
  private final RestTemplate restTemplate;
  private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();

  public MercadoLivreHttpClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.restTemplate.setErrorHandler(new ResponseErrorHandlerCustomizado());
  }

  public <T> RespostaAPI<T> fazerRequisicao(String pUrlDaRequisicao, String pTokenSeller, String pIdentificadorSeller, Class<T> tipoDoRetornoEsperado, String pApi
                                           ) throws IOException, LimiteRequisicaoMLException, NotFoundMLException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("Accept", "application/json");
    headers.setBearerAuth(pTokenSeller);

    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    try {
      ResponseEntity<T> objResposta = restTemplate.exchange(
          URI.create(pUrlDaRequisicao),
          HttpMethod.GET,
          requestEntity,
          tipoDoRetornoEsperado
      );
      if (objResposta.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        throw new LimiteRequisicaoMLException("ATENCAO: Recebido Status 429 Too Many Requests.", pApi, pIdentificadorSeller);
      } else if (objResposta.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new NotFoundMLException("ATENCAO: Recebido Status 404 Not Found.", pApi, pIdentificadorSeller);
      }

      if (objResposta.getStatusCode() != HttpStatus.OK) {loggerRobot.warning("FALHA: Requisição, Status Code: -> " + objResposta.getStatusCode());}

      return new RespostaAPI<>(objResposta.getBody(), objResposta.getStatusCode());

    } catch (LimiteRequisicaoMLException  excecaoLimiteRequisicoes) {
      loggerRobot.severe("FALHA: Erro Ao Fazer Requisicao Http. ->\n" +
                               "Seller: -> " + pIdentificadorSeller + "\n" +
                               "API: -> " + excecaoLimiteRequisicoes.getvApi()  + "\n" +
                               "Identificador: -> " + excecaoLimiteRequisicoes.getvIdentificadorCliente() + "\n" +
                               "Mensagem: -> " + excecaoLimiteRequisicoes.getMessage());
      throw excecaoLimiteRequisicoes;
    } catch (NotFoundMLException excecaoRecursoNaoEncontrado) {
      loggerRobot.severe("FALHA: Erro Ao Fazer Requisicao Http. ->\n" +
                               "Seller: -> " + pIdentificadorSeller + "\n" +
                               "API: -> " + excecaoRecursoNaoEncontrado.getvApi()  + "\n" +
                               "Identificador: -> " + excecaoRecursoNaoEncontrado.getvIdentificadorCliente() + "\n" +
                               "Mensagem: -> " + excecaoRecursoNaoEncontrado.getMessage());
      throw excecaoRecursoNaoEncontrado;
    } catch (RestClientException excecao) {
      // Captura outros erros da API
      loggerRobot.severe("FALHA: Erro Ao Fazer Requisicao Http. ->\n" +
                              "Seller: -> " + pIdentificadorSeller + "\n" +
                              "Mensagem: -> " + excecao.getMessage());
    }
    return new RespostaAPI<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
