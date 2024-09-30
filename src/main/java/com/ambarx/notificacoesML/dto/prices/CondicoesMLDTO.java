package com.ambarx.notificacoesML.dto.prices;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CondicoesMLDTO {

	private List<String> contextRestrictions;
	private String startTime;
	private String endTime;

}
