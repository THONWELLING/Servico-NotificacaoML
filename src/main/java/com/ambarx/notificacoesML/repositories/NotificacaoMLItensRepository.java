package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.NotificacaoMLItens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoMLItensRepository extends JpaRepository<NotificacaoMLItens, Long> {
}
