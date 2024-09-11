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
@Table(name = "notificacao_mercadolivre_user_products_families")
public class NotificacaoUserProductFamiliesMLDTO implements Serializable {
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
    if (o == null || getClass() != o.getClass()) return false;
    NotificacaoUserProductFamiliesMLDTO that = (NotificacaoUserProductFamiliesMLDTO) o;
    return Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(resource, that.resource) && Objects.equals(topic, that.topic) && Objects.equals(received, that.received) && Objects.equals(sent, that.sent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, resource, topic, received, sent);
  }
}