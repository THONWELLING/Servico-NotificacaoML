package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.NotificacaoMLItensEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoMLItensRepository extends JpaRepository<NotificacaoMLItensEntity, Long> {

	@Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY received DESC) AS posicao FROM notificacao_mercadolivre_items ) AS limitada WHERE posicao <= :limit", nativeQuery = true)
	List<NotificacaoMLItensEntity> findTopNotificacoesPorUserId(@Param("limit") int pLimit);
}
