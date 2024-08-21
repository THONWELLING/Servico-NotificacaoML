package com.ambarx.notificacoesML.dto.notificacao;

import com.ambarx.notificacoesML.models.NotificacaoML;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class NotificacaoCompletaDTO {
  private Long   id;
  private String user_id;
  private String resource;
  private String topic;
  private String identificadorCliente;
  private String servidor;
  private String banco;
  private String mensagemRetorno;


  public NotificacaoCompletaDTO(NotificacaoML notificacao, String identificadorCliente, String servidor, String banco) {
    this.id                   = (long) notificacao.getId();
    this.user_id              = notificacao.getUserId();
    this.resource             = notificacao.getResource();
    this.topic                = notificacao.getTopic();
    this.identificadorCliente = identificadorCliente;
    this.servidor             = servidor;
    this.banco                = banco;
  }

  public NotificacaoCompletaDTO(String mensagemRetorno) { this.mensagemRetorno = mensagemRetorno; }

}