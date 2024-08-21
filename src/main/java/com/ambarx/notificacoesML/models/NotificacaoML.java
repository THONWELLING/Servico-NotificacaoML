package com.ambarx.notificacoesML.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "notificacao_mercadolivre_items")
public class NotificacaoML implements Serializable {
  @Id
  @Column(name = "id")
  private Long   id;
  @Column(name = "user_id")
  private String userId;
  @Column(name = "resource")
  private String resource;
  @Column(name = "topic")
  private String topic;
  @Column(name = "received")
  private String received;
  @Column(name = "sent")
  private String sent;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NotificacaoML that)) return false;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getResource(), that.getResource()) && Objects.equals(getTopic(), that.getTopic()) && Objects.equals(getReceived(), that.getReceived()) && Objects.equals(getSent(), that.getSent());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getUserId(), getResource(), getTopic(), getReceived(), getSent());
  }
}