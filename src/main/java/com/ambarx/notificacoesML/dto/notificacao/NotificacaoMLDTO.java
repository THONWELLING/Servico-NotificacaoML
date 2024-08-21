package com.ambarx.notificacoesML.dto.notificacao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificacaoMLDTO {
  private Long   id;
  private String userId;
  private String resource;
  private String topic;
  private String received;
  private String sent;
  private int quantidadeNotificacoes;


}
