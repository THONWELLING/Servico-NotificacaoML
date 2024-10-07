package com.ambarx.notificacoesML.utils;

import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FuncoesUtilsTest {

	private FuncoesUtils funcoesUtils;

	@BeforeEach
	public void setUp() {
		funcoesUtils = new FuncoesUtils();
	}

	//region Função Para Gerar Notificações.
	private List<NotificacaoMLDTO> gerarNotificacoes() {
		List<NotificacaoMLDTO> notificacoes = new ArrayList<>();
		int totalUsers 						 = 5;
		int totalResources 				 = 10;
		int notificacoesPorUsuario = 320;

		// Gerar 320 notificações por usuário, totalizando 1600 notificações
		for (int userId = 1; userId <= totalUsers; userId++) {
			for (int i = 1; i <= notificacoesPorUsuario; i++) {
				String resource = "resource" + (i % totalResources + 1); // Alterna entre 10 recursos
				notificacoes.add(new NotificacaoMLDTO(
						(long) notificacoes.size() + 1, // ID único para cada notificação
						"user" + userId, // Alterna entre os 5 usuários
						resource, // Alterna entre 10 recursos
						"Items",
						new Date(System.currentTimeMillis() - (300 * i)).toString(),
						new Date(System.currentTimeMillis() - (1000 * i)).toString(),
						1
				));
			}
		}

		return notificacoes;
	}

	//endregion

	@Test
	void agruparEFiltrarNotificacoes() {
		// Setup
		List<NotificacaoMLDTO> notificacoes = gerarNotificacoes();

		// Execute
		Map<String, List<NotificacaoMLDTO>> resultado = funcoesUtils.agruparEFiltrarNotificacoes(notificacoes);

		// Verifique o resultado
		assertEquals(5, resultado.size()); // Deve ter 5 usuários
		for (int userId = 1; userId <= 5; userId++) {
			String userkey = "user" + userId;
			assertTrue(resultado.containsKey(userkey), "Faltando notificações para o " + userkey);
			assertEquals(10,resultado.get(userkey).size(), "Número incorreto de notificações para o " + userkey);
		}
	}


	@Test
	void pegarIDsDasNotificacoesDoUsuario() throws Exception {
		// Setup
		List<NotificacaoMLDTO> notificacoes = Arrays.asList(
				new NotificacaoMLDTO(1L, "user1", "resource1", "items", new Date().toString(), new Date().toString(), 3),
				new NotificacaoMLDTO(2L, "user1", "resource2", "items", new Date().toString(), new Date().toString(), 3)
																											 );

		// Execute
		List<Long> ids = funcoesUtils.pegarIDsDasNotificacoesDoUsuario(notificacoes);

		// Verifique o resultado
		assertEquals(2, ids.size());
		assertTrue(ids.contains(1L));
		assertTrue(ids.contains(2L));
	}

	@Test
	void extrairSkuMLDasNotificacoes() {
		// Execute
		String sku = funcoesUtils.extrairSkuMLDasNotificacoes("/items/ABC123");

		// Verifique o resultado
		assertEquals("ABC123", sku);

		// Teste caso nulo
		assertNull(funcoesUtils.extrairSkuMLDasNotificacoes(null));

		// Teste caso não correspondente
		assertNull(funcoesUtils.extrairSkuMLDasNotificacoes("/other/ABC123"));
	}

	@Test
	void calcularComissaoML() {
		// Execute
		double comissao = funcoesUtils.calcularComissaoML(12.0, 100.0);

		// Verifique o resultado
		assertEquals(0.00, comissao, 0.01);

		// Teste para preço abaixo de 79
		double comissaoAlta = funcoesUtils.calcularComissaoML(25.0, 70.0);
		assertEquals(27.14, comissaoAlta, 0.01); // Deveria aplicar custo adicional
	}

	@Test
	void calculaCustoAdicional() {
		// Execute
		double custo = funcoesUtils.calculaCustoAdicional(20.0, 50.0, "N");

		// Verifique o resultado
		assertEquals(6.0, custo);

		// Teste para supermercado
		double custoSupermercado = funcoesUtils.calculaCustoAdicional(20.0, 25.0, "S");
		assertEquals(1.0, custoSupermercado);
	}
}