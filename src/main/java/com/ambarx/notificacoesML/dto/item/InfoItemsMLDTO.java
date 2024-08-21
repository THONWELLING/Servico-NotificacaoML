package com.ambarx.notificacoesML.dto.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class InfoItemsMLDTO {
  private int    codid;
  private String skuNoBanco;
  private String estaAtivo;
  private String eFulfillment;


}
