package com.ambarx.notificacoesML.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class RespostaAPI <T>{
	private T              corpoResposta;
	private HttpStatusCode statusCode;
}
