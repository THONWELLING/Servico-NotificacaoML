package com.ambarx.notificacoesML.customizedExceptions;


public class LimiteRequisicaoMLException extends Exception {
	private final String vApi;
	private final String vIdentificadorCliente;

	public LimiteRequisicaoMLException(String message, String vApi, String vIdentificadorCliente) {
		super(message);
		this.vApi = vApi;
		this.vIdentificadorCliente = vIdentificadorCliente;
	}

	public String getvApi() {
		return vApi;
	}

	public String getvIdentificadorCliente() {
		return vIdentificadorCliente;
	}

}
