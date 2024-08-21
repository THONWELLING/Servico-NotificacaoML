package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.NotificacaoML;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoMercadoLivreRepository extends JpaRepository<NotificacaoML, Long> {
}
