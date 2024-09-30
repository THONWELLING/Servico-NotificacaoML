package com.ambarx.notificacoesML.dto.prices;

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
public class PrecoDTO {

	private String id;
	private String type;
	private double amount;
	private double regularAmount;
	private String currencyId;
	private String lastUpdated;
	private CondicoesMLDTO conditions;
}
