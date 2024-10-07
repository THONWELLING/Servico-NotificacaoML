package com.ambarx.notificacoesML.customizedExceptions;

import lombok.Getter;

@Getter
public class LimiteRequisicaoMLException extends Exception {
	private final String vApi;
	private final String vIdentificadorCliente;

	public LimiteRequisicaoMLException(String message, String vApi, String vIdentificadorCliente) {
		super(message);
		this.vApi = vApi;
		this.vIdentificadorCliente = vIdentificadorCliente;
	}
}
