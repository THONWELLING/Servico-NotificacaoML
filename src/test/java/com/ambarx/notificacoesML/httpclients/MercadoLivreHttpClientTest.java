package com.ambarx.notificacoesML.httpclients;

import com.ambarx.notificacoesML.customizedExceptions.LimiteRequisicaoMLException;
import com.ambarx.notificacoesML.customizedExceptions.NotFoundMLException;
import com.ambarx.notificacoesML.utils.RespostaAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MercadoLivreHttpClientTest {

	@InjectMocks
	private MercadoLivreHttpClient mercadoLivreHttpClient;

	@Mock
	private RestTemplate restTemplate;
	// Método auxiliar para verificar headers
	private HttpEntity<String> matchHttpEntity() {
		return argThat(entity -> {
			HttpHeaders headers = entity.getHeaders();
			if (! Objects.equals(headers.getContentType(), MediaType.APPLICATION_JSON) || !headers.getAccept().contains(MediaType.APPLICATION_JSON)) return false;
			headers.getAccessControlAllowHeaders();
			return headers.getAccessControlAllowCredentials();
		});
	}

	@Test
	public void testFazerRequisicao_200OK() throws IOException, LimiteRequisicaoMLException, NotFoundMLException {
		// Configurando Resposta Simulada para 200 OK
		when(restTemplate.exchange(
						 any(URI.class),
						 any(HttpMethod.class),
						 any(HttpEntity.class),
						 eq(String.class))
				).thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));

		// Chamando o método a ser testado
		RespostaAPI<String> resposta = mercadoLivreHttpClient.fazerRequisicao("https://api.mercadolivre.com", "token", "seller", String.class, "api");

		// Validando a resposta
		assertEquals(HttpStatus.OK, resposta.getStatusCode());
		assertEquals("{\"status\":\"ok\"}", resposta.getCorpoResposta());
	}

	@Test
	public void testFazerRequisicao_400BadRequest() throws IOException, LimiteRequisicaoMLException, NotFoundMLException {
		// Configurando Resposta Simulada para 400 Bad Request
		when(restTemplate.exchange(
						 any(URI.class),
						 any(HttpMethod.class),
						 any(HttpEntity.class),
						 eq(String.class))
				).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

		// Chamando o método a ser testado
		RespostaAPI<String> resposta = mercadoLivreHttpClient.fazerRequisicao("https://api.mercadolivre.com", "token", "seller", String.class, "api");

		// Validando a resposta
		assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
		assertNull(resposta.getCorpoResposta());
	}

	@Test
	public void testFazerRequisicao_429TooManyRequests() throws IOException {
		// Configurando Resposta Simulada para 429 Too Many Requests
		when(restTemplate.exchange(
						 any(URI.class),
						 any(HttpMethod.class),
						 any(HttpEntity.class),
						 eq(String.class))
				).thenReturn(new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS));

		// Validando que a exceção LimiteRequisicaoMLException é lançada
		LimiteRequisicaoMLException exception = assertThrows(
				LimiteRequisicaoMLException.class,
				() -> mercadoLivreHttpClient.fazerRequisicao("https://api.mercadolivre.com", "token", "seller", String.class, "api")
																												);

		assertEquals("ATENCAO: Recebido Status 429 Too Many Requests.", exception.getMessage());
	}

	@Test
	public void testFazerRequisicao_ThrowRestClientException() throws IOException, LimiteRequisicaoMLException, NotFoundMLException {
		// Simulando um erro de rede (RestClientException)
		when(restTemplate.exchange(
						 any(URI.class),
						 any(HttpMethod.class),
						 any(HttpEntity.class),
						 eq(String.class))
				).thenThrow(new RestClientException("Erro na comunicação"));

		// Chamando o método a ser testado
		RespostaAPI<String> resposta = mercadoLivreHttpClient.fazerRequisicao("https://api.mercadolivre.com", "token", "seller", String.class, "api");

		// Validando a resposta (deve retornar INTERNAL_SERVER_ERROR)
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
		assertNull(resposta.getCorpoResposta());
	}
}
