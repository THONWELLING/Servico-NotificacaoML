package com.ambarx.notificacoesML.httpclients;

import com.ambarx.notificacoesML.utils.FuncoesUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MercadoLivreHttpClient {
  private final RestTemplate restTemplate;
  private final Logger logger = Logger.getLogger(FuncoesUtils.class.getName());
  public MercadoLivreHttpClient(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

  public <T> T fazerRequisicao (String urlDaRequisicao, String tokenSeller, Class<T> tipoDoRetornoEsperado) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("Accept", "application/json");
    headers.setBearerAuth(tokenSeller);

    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(
          URI.create(urlDaRequisicao),
          HttpMethod.GET,
          requestEntity,
          tipoDoRetornoEsperado
      );

      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        return responseEntity.getBody();
      } else {
        logger.log(Level.WARNING,"FALHA: Requisição, Status Code :  " + responseEntity.getStatusCode());
      }
    } catch (RestClientException excecao) {
      logger.log(Level.SEVERE, "Erro Ao Fazer Requisição Http " + excecao.getMessage());
    }
    return null;
  }

}
