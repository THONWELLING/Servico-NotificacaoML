package com.ambarx.notificacoesML.httpclients;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.customizedExceptions.LimiteRequisicaoMLException;
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

  public <T> RespostaAPI<T> fazerRequisicao(String pUrlDaRequisicao, String pTokenSeller, String pIdentificadorSeller, Class<T> tipoDoRetornoEsperado, String pApi) throws IOException, LimiteRequisicaoMLException {
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
      }
      if (objResposta.getStatusCode() != HttpStatus.OK) {loggerRobot.warning("FALHA: Requisição, Status Code: -> " + objResposta.getStatusCode());}

      return new RespostaAPI<>(objResposta.getBody(), objResposta.getStatusCode());

    } catch (RestClientException excecao) {
      if (excecao.getMessage().contains("429")) {
        throw new LimiteRequisicaoMLException("ATENCAO: Recebido Status 429 Too Many Requests.", pApi, pIdentificadorSeller);
      }
      loggerRobot.severe("FALHA: Erro Ao Fazer Requisicao Http. ->\n Seller: -> " + pIdentificadorSeller + "\nMensagem: -> " + excecao.getMessage());
    }
    return new RespostaAPI<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
