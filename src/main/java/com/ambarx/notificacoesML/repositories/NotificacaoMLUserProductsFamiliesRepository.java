package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.NotificacaoUserProductFamiliesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoMLUserProductsFamiliesRepository extends JpaRepository<NotificacaoUserProductFamiliesEntity, Long> {
}
