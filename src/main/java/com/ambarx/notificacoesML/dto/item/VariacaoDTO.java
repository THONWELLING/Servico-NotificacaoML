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
public class VariacaoDTO {
  private String id;
  private double price;
  private int availableQuantity;
  private int soldQuantity;
  private ArrayList<String>      pictureIds;
  private ArrayList<AtributoDTO> attributes;
}
