package com.ambarx.notificacoesML.dto.infodobanco;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class SellerMercadoLivreDTO {
  @Id
  @Column(name = "seller_id")
  private Long sellerId;
  @Column(name = "identificador_cliente")
  private String identificadorCliente;

}