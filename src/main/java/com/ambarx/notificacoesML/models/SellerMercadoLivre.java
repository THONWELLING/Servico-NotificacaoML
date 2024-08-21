package com.ambarx.notificacoesML.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "sellers_mercadolivre")
public class SellerMercadoLivre implements Serializable {
  @Id
  @Column(name = "autoid")
  private Integer id;
  @Column(name = "seller_id")
  private String sellerId;
  @Column(name = "identificador_cliente")
  private String identificadorCliente;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SellerMercadoLivre that)) return false;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getSellerId(), that.getSellerId()) && Objects.equals(getIdentificadorCliente(), that.getIdentificadorCliente());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getSellerId(), getIdentificadorCliente());
  }
}