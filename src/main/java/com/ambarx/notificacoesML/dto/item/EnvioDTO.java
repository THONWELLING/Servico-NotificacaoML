package com.ambarx.notificacoesML.dto.item;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EnvioDTO {
  private String            mode;
  private ArrayList<String> methods;
  private ArrayList<String> tags;
  private String            dimensions;
  private boolean           localPickUp;
  private boolean           freeShipping;
  private String            logisticType;
  private boolean           storePickUp;
}

