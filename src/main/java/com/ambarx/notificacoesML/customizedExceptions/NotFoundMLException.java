package com.ambarx.notificacoesML.customizedExceptions;


public class NotFoundMLException extends Exception {
	private final String vApi;
	private final String vIdentificadorCliente;

	public NotFoundMLException(String message, String vApi, String vIdentificadorCliente) {
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