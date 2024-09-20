package com.ambarx.notificacoesML.httpclients;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

@AllArgsConstructor
public class MercadoLivreHttpClient {
  private final RestTemplate restTemplate;
  private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();

  public <T> T fazerRequisicao (String pUrlDaRequisicao, String pTokenSeller,  String pIdentificadorSeller,Class<T> tipoDoRetornoEsperado) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("Accept", "application/json");
    headers.setBearerAuth(pTokenSeller);

    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(
          URI.create(pUrlDaRequisicao),
          HttpMethod.GET,
          requestEntity,
          tipoDoRetornoEsperado
      );

      if (responseEntity.getStatusCode() == HttpStatus.OK) { return responseEntity.getBody();
      } else { loggerRobot.warning("FALHA: Requisição, Status Code: -> " + responseEntity.getStatusCode());}

    } catch (RestClientException excecao) {
      loggerRobot.severe("FALHA: Erro Ao Fazer Requisição Http. ->\n Seller: -> " + pIdentificadorSeller + "\nMensagem: -> " + excecao.getMessage());
    }
    return null;
  }

}
