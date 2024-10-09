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
        JOIN sellers_mercadolivre sellers_ml ON notificacoesLimitadas.user_id = sellers_ml.seller_id
        WHERE sellers_ml.identificador_cliente NOT IN ('desconhecido')
          AND notificacoesLimitadas.posicao <= :limit
        ORDER BY notificacoesLimitadas.posicao
        """, nativeQuery = true)
	List<NotificacaoMLItensEntity> findTopNotificacoesPorUserId(@Param("limit") int pLimit);

	//region Buscar Notificações Somente da vgshop.
/*	@Query(value = "SELECT * FROM notificacao_mercadolivre_items WHERE user_id IN (242864502, 462118052, 532496825, 239750995, 1113952563, 1612067589, 1614118256)", nativeQuery = true)
	List<NotificacaoMLItensEntity> findNotificacoesPorUserIda();*/
	//endregion

	//region Busca Notificações Somente da CMC
/*	@Query(value = "SELECT * FROM notificacao_mercadolivre_items WHERE user_id IN (14001958, 36127327, 65426154, 1005162109, 1155851487, 44056971, 1201249297, 746615311, 764521626, 404143232, 340132258, 169792382, 1463843468, 1524398555, 735327437, 1941226791, 1959039009)", nativeQuery = true)
		List<NotificacaoMLItensEntity> findNotificacoesPorUserIda();*/
	//endregion

	//region Busca Notificações Somente da maisvantagens
	@Query(value = "SELECT * FROM notificacao_mercadolivre_items WHERE user_id IN (69240948, 175914219)", nativeQuery = true)
	List<NotificacaoMLItensEntity> findNotificacoesPorUserIda();
	//endregion

}