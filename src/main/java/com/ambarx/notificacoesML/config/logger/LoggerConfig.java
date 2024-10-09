package com.ambarx.notificacoesML.config.logger;

import lombok.Getter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.logging.*;

public class LoggerConfig {
	@Getter
	private static final Logger loggerRobot 				 = Logger.getLogger("LoggerORobot");
	private static String ultimaMensagemSemTimestamp = ""; // Armazena a última mensagem sem timestamp
	private static final String CAMINHO_ARQUIVO_LOG  = "C:/Ambar/Temp/notificacaoMLItemsLogs.log"; // C:/Ambar/Temp/MLLogsDebug.log

	static {
		try {
			FileHandler fileHandler = new FileHandler(CAMINHO_ARQUIVO_LOG, true);
			fileHandler.setFormatter(new FormatadorDeLog());
			fileHandler.setLevel(Level.INFO); // Define o nível mínimo que será capturado
			loggerRobot.addHandler(fileHandler);
			loggerRobot.setUseParentHandlers(false); // Para evitar duplicação no console
		} catch (IOException e) {
			loggerRobot.severe("Erro Ao Configurar Arquivo de Log. " + e.getMessage());
		}
	}

	//region Método Para Formatar Os Logs.
	static class FormatadorDeLog extends Formatter {

		@Override
		public String format(LogRecord log) {
			StringBuilder logBuilder 		= new StringBuilder();
			String mensagemSemTimestamp = (formatMessage(log) != null && !formatMessage(log).trim().isEmpty()) ? formatMessage(log) : ""; // Apenas a Mensagem, Sem Timestamp
			String mensagemCompleta			= LocalDateTime.now() + " -> " + log.getLevel() + ": " + mensagemSemTimestamp; // Monta a Mensagem Do Log Com Timestamp

			// Verifica Se a Mensagem, Sem o Timestamp, é Idêntica à Última Registrada
			if (mensagemSemTimestamp.equals(ultimaMensagemSemTimestamp)) {
				// Se For Igual, Sobrescreve o Log Anterior, Mas Atualiza o Timestamp
				sobrescreverUltimaLinha("\n" + mensagemCompleta + "\n");
				return ""; // Não Adiciona Uma Nova Linha Ao Log
			} else {
				ultimaMensagemSemTimestamp = mensagemSemTimestamp; // Atualiza a Última Mensagem Gerada
				logBuilder.append(mensagemCompleta).append("\n");

				if (log.getMessage().contains("Finaliza Tarefa")) {
					// Adiciona Separador Para Logs De Finalização De Tarefa
					logBuilder.append("\n------------------------------------------------------------------\n");
				}
				return logBuilder.toString();
			}
		}
	}
	//endregion

	//region Método Para Sobrescrever a Última Linha No Arquivo De Log.
	private static void sobrescreverUltimaLinha(String mensagem) {
		try {
			RandomAccessFile acessoArquivoLog = new RandomAccessFile(CAMINHO_ARQUIVO_LOG, "rw");
			long tamanhoArquivo = acessoArquivoLog.length();

			if (tamanhoArquivo > 0) {
				// Move o ponteiro para o final do arquivo, menos o tamanho da última mensagem registrada
				acessoArquivoLog.seek(tamanhoArquivo - ultimaMensagemSemTimestamp.length() - 1); // Última mensagem + quebra de linha
				acessoArquivoLog.write(mensagem.getBytes()); // Sobrescreve o último log com o novo timestamp
			}
			acessoArquivoLog.close();
		} catch (IOException e) {
			loggerRobot.severe("Erro Ao Sobrescrever o Log: " + e.getMessage());
		}
	}
	//endregion

}
