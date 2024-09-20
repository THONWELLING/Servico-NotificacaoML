package com.ambarx.notificacoesML.config.logger;

import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.*;

public class LoggerConfig {
	@Getter
	private static final Logger loggerRobot = Logger.getLogger("LoggerORobot");
	static {
		try {
			FileHandler fileHandler = new FileHandler("C:/Ambar/Temp/notificacaoMLLogs.log", true);
			fileHandler.setFormatter(new FormatadorDeLog());
			fileHandler.setLevel(Level.INFO);
			loggerRobot.addHandler(fileHandler);
			loggerRobot.setUseParentHandlers(false); //Para Evitar Dupliacação Nko Console
		} catch (IOException e) {
			loggerRobot.severe("Erro Ao Configurar Arquivo de Log. " + e.getMessage());
		}
	}

	static class FormatadorDeLog extends Formatter {
		@Override
		public String format(LogRecord pLog) {
			StringBuilder vConstruirString = new StringBuilder();
			vConstruirString.append(LocalDateTime.now()).append(" -> ");       //Adiciona o Timestamp Da Mensagem De Log
			vConstruirString.append(pLog.getLevel()).append(": ");//Adiciona o Nível Do Log()
			vConstruirString.append(formatMessage(pLog)).append("\n"); //Adiciona A Mensagem Do Log

			if (pLog.getMessage().contains("Finaliza Tarefa")) {
				vConstruirString.append("\n------------------------------------------------------------------\n");
			}
			return vConstruirString.toString();
		}
	}
}
