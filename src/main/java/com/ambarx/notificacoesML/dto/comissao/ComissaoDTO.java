package com.ambarx.notificacoesML.dto.comissao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ComissaoDTO {
  /*private String               currencyId;
  private boolean              freeRelist;
  private String               listingExposure;
  private double               listingFeeAmount;
  private String               listingTypeId;
  private String               listingTypeName;
  private boolean              requiresPicture;
  private double               saleFeeAmount;
  private String               stopTime;*/
  private ListingFeeDetailsDTO listingFeeDetailsDTO;
  private SaleFeeDetailsDTO    saleFeeDetails;
}
