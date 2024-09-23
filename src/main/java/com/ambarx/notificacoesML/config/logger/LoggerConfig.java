package com.ambarx.notificacoesML.config.logger;

import lombok.Getter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.logging.*;

public class LoggerConfig {
	@Getter
	private static final Logger loggerRobot  				= Logger.getLogger("LoggerORobot");
	private static String vUltimaMensagemLog 				= ""; //Armazena Último Log Gerado
	private static final String CAMINHO_ARQUIVO_LOG = "C:/Ambar/Temp/notificacaoMLLogs.log";

	static {
		try {
			FileHandler fileHandler = new FileHandler(CAMINHO_ARQUIVO_LOG, true);
			fileHandler.setFormatter(new FormatadorDeLog());
			fileHandler.setLevel(Level.INFO); //Level Mínimo Que Será Capturado
			loggerRobot.addHandler(fileHandler);
			loggerRobot.setUseParentHandlers(false); //Para Evitar Dupliacação No Console
		} catch (IOException e) {
			loggerRobot.severe("Erro Ao Configurar Arquivo de Log. " + e.getMessage());
		}
	}

	//region Método Para Formatar Os Logs.
	static class FormatadorDeLog extends Formatter {

		@Override
		public String format(LogRecord pLog) {
			StringBuilder vConstruirString = new StringBuilder();
			String vMensagemLog				 = formatMessage(pLog);
			String vMensagemLogCompleta = LocalDateTime.now() + " -> " + pLog.getLevel() + ": " + formatMessage(pLog); // Monta a Mensagem Do Log

			// Verifica Se a Mensagem é Idêntica à Última Registrada
			if (vMensagemLog.equals(vUltimaMensagemLog)) {
				sobrescreverUltimaLinha(vMensagemLogCompleta + "\n"); // Se For Igual, Sobrescreve a Última Linha
				return ""; // Não Adiciona Uma Nova Linha Ao Log
			} else {
				vUltimaMensagemLog = vMensagemLogCompleta; // Atualiza o Último Log Gerado e Constrói o Novo Log
				vConstruirString.append(vMensagemLogCompleta).append("\n");

				if (pLog.getMessage().contains("Finaliza Tarefa")) {
					// Adiciona Separador Para Logs De Finalização De Tarefa
					vConstruirString.append("\n------------------------------------------------------------------\n");
				}
				return vConstruirString.toString();
			}
		}
	}
	//endregion

	//region Método Para Sobrescrever a Última Linha No Arquivo De Log.
	private static void sobrescreverUltimaLinha(String mensagem) {
		try {
			RandomAccessFile arquivoLogAcessoAleatorio = new RandomAccessFile(CAMINHO_ARQUIVO_LOG, "rw");
			long length = arquivoLogAcessoAleatorio.length();
			if (length > 0) {
				// Move o Ponteiro Para o Final Do Arquivo Menos o Tamanho Do Último Log
				arquivoLogAcessoAleatorio.seek(length - vUltimaMensagemLog.length() - 1); // Ultimo Log + Quebra De Linha
				arquivoLogAcessoAleatorio.write(mensagem.getBytes()); // Sobrescreve o Último Log
			}
			arquivoLogAcessoAleatorio.close();
		} catch (IOException e) {
			loggerRobot.severe("Erro Ao Sobrescrever o Log: " + e.getMessage());
		}
	}
	//endregion


}

