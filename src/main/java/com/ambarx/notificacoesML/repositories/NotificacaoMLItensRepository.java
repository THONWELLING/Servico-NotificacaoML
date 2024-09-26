package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.NotificacaoMLItensEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoMLItensRepository extends JpaRepository<NotificacaoMLItensEntity, Long> {

//region Query Antiga.
/*@Query(value = """
				SELECT *
				FROM (
							SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY received DESC) AS posicao
							FROM notificacao_mercadolivre_items
						 ) AS limitada
				WHERE posicao <= :limit""", nativeQuery = true)*/
//endregion

	@Query(value = """
        SELECT notificacoesLimitadas.*
        FROM (
              SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY received DESC) AS posicao
              FROM notificacao_mercadolivre_items
              ) AS notificacoesLimitadas
        JOIN sellers_mercadolivre ON notificacoesLimitadas.user_id = sellers_mercadolivre.seller_id
        WHERE sellers_mercadolivre.identificador_cliente <> 'desconhecido'
          AND notificacoesLimitadas.posicao <= :limit
        ORDER BY notificacoesLimitadas.posicao
        """, nativeQuery = true)
	List<NotificacaoMLItensEntity> findTopNotificacoesPorUserId(@Param("limit") int pLimit);

	@Query(value = "SELECT * FROM notificacao_mercadolivre_items WHERE user_id = 532496825", nativeQuery = true)
	List<NotificacaoMLItensEntity> findNotificacoesPorUserIda();
}