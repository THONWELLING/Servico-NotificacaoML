package com.ambarx.notificacoesML.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "acessoapi_cadclientes")
public class AcessoApiCadClientesEntity {

    @Column(name = "autoid")
    private Integer id;
    @Id
    @Column(name = "identificador_cliente")
    private String identificadorCliente;
    @Column(name = "servidor")
    private String servidor;
    @Column(name = "banco")
    private String banco;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AcessoApiCadClientesEntity that)) return false;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getIdentificadorCliente(), that.getIdentificadorCliente()) && Objects.equals(getServidor(), that.getServidor()) && Objects.equals(getBanco(), that.getBanco());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getIdentificadorCliente(), getServidor(), getBanco());
  }
}
