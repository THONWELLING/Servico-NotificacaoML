package com.ambarx.notificacoesML.customizedExceptions;

import lombok.Getter;

@Getter
public class NotificacaoMLException extends Exception {
	private final String userId;
	private final String vIdentificadorCliente;

	public NotificacaoMLException(String message, Throwable cause, String userId, String vIdentificadorCliente) {
		super(message, cause);
		this.userId = userId;
		this.vIdentificadorCliente = vIdentificadorCliente;
	}
}
