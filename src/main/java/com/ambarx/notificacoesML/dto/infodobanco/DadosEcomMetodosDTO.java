package com.ambarx.notificacoesML.dto.infodobanco;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class DadosEcomMetodosDTO {
  private String tokenTemp;
  private int    origem;
  private LocalDateTime tokenExpira;
}
