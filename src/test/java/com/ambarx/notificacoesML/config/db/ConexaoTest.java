package com.ambarx.notificacoesML.config.db;

import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConexaoTest {
	@InjectMocks
	private Conexao conexao;
	@Mock
	private ConfigDataSourceDinamico dataSourceDinamico;
	@Mock
	private DataSource dataSource;
	@Mock
	private Connection connection;
	@BeforeEach
	public void setUp () {
		MockitoAnnotations.openMocks(this);
		System.setProperty("USUARIO_SQLSERVER", "usuarioTeste");
		System.setProperty("SENHA_SQLSERVER"  , "senhaTeste");

	}


	//region Teste De Conexão Com Sucesso.
	@Test
	void conectarSucesso() throws Exception {
		ConexaoDTO cliente = new ConexaoDTO(41116311202111895L, "idCliente", "ativo", "remoto",
																				"servidorTeste", "bancoTeste", System.getProperty("USUARIO_SQLSERVER"),
																				System.getProperty("SENHA_SQLSERVER"), "ambar", "tokenMD5", "tokeSHA");


		when(dataSourceDinamico.createDataSource(anyString(), anyString(), anyString())).thenReturn(mock(DataSource.class));
		when(dataSourceDinamico.createDataSource(anyString(), anyString(), anyString()).getConnection()).thenReturn(connection);
		Connection conectar = conexao.conectar(cliente);

		assertNotNull(conectar);
		verify(dataSourceDinamico, times(1)).createDataSource(anyString(), eq("usuarioTeste"), eq("senhaTeste"));
	}
	//endregion

	//region Teste De Conexão Com Falha.
	@Test
	void conectarFalha() throws Exception {
		ConexaoDTO cliente = new ConexaoDTO(41116311202111895L, "idCliente", "ativo", "remoto",
																				"servidorTeste", "bancoTeste", System.getProperty("USUARIO_SQLSERVER"),
																				System.getProperty("SENHA_SQLSERVER"), "ambar", "tokenMD5", "tokeSHA");


			when(dataSourceDinamico.createDataSource(anyString(), anyString(), anyString())).thenReturn(mock(DataSource.class));
		doThrow(new SQLException("Falha Na Conexão.")).when(dataSource).getConnection();
		Connection conectar = conexao.conectar(cliente);

		assertNull(conectar);
	}
	//endregion

	//region Teste Fechar Conexão Com Sucesso.
	@Test
	public void testFecharConexaoSucesso() throws SQLException {
		conexao.fecharConexao(connection);
		verify(connection, times(1)).close();
	}
	//endregion

	//region Teste Fechar Conexão Com Falha
	@Test
	void fecharConexaoFalha() throws SQLException {
		doThrow(new SQLException("Erro ao fechar")).when(connection).close();

		SQLException exception = assertThrows(SQLException.class, () -> {
			conexao.fecharConexao(connection);
		});

		assertEquals("Erro ao fechar", exception.getMessage());
	}
	//endregion
}