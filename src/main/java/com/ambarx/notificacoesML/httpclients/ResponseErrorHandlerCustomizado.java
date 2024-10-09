package com.ambarx.notificacoesML.httpclients;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

public class ResponseErrorHandlerCustomizado implements ResponseErrorHandler {
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return !response.getStatusCode().is2xxSuccessful();
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
			return;
		}
		if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
			return;
		}
		throw new RestClientException("Erro Durante A Requisicao HTTP: " + response.getStatusCode());
	}
}
