package com.ambarx.notificacoesML.utils;

import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FuncoesUtils {
private final Logger logger = Logger.getLogger(FuncoesUtils.class.getName());

//region Função Para Agrupar As Notificações De Itens e Filtrar Para Evitar Requisições Desnecessárias
  /**
   * A Função Abaixo Foi Criada Com O Intuito De Eliminar Requisições Desnecessárias Mediante A Eliminação De Notificações Exatamente Iguais Dentro De Um Período De 2 Minutos.
   * Ou Seja, Se Existirem 10 Notificações Exatamente Iguais De Um Mesmo Seller(Userid) E O Período De Recebimento Da Mesma(Received) For Menor Que 2 Min. Entre Elas, Serão Eliminadas
   * As Notificações Repetidas. Isso Vai Resultar Na Diminuição De Requisições Feitas Para A Api Externa Diminuindo Assim O Tráfego De Dados E A Intermitência No Processamento.
   * Como A Função Funciona
   * Agrupamento Por Userid:
   * <p>
   * A Função Começa Agrupando Todas As Notificações Pelo Userid Usando Collectors.groupingby(Notificacaomldto::getuserid). Isso Cria Um Map<String, List<NotificacaoMLDTO>> Onde A Chave É O Userid E O Valor É Uma Lista De Notificações Associadas A Esse Userid.
   * Iteração Sobre Os Grupos De Userid:
   * <p>
   * Depois De Agrupar As Notificações, A Função Itera Sobre Cada Entrada Desse Mapa (Entryset().stream()), O Que Significa Que Ela Processa Cada Grupo De Notificações De Forma Independente.
   * Filtragem Dentro De Cada Grupo De Userid:
   * <p>
   * Para Cada Userid E Sua Lista De Notificações (Entry.getvalue()), A Função:
   * Ordena As Notificações Pela Data De Recebimento (Received).
   * Cria Uma Lista Notificacoesfiltradas Para Armazenar As Notificações Após A Filtragem.
   * Utiliza Um Hashmap Chamado Ultimasnotificacoes Para Rastrear A Última Data De Recebimento De Cada Resource Dentro Desse Grupo.
   * Filtra As Notificações Dentro Da Lista Específica De Um Userid Para Remover Notificações Repetidas Com Base No Intervalo De 2 Minutos.
   * Resumo
   * A Função Proposta Faz Exatamente O Que Você Descreveu:
   * <p>
   * Agrupa Notificações Por Userid.
   * Filtra Notificações Repetidas Dentro De Cada Grupo De Userid Com Base No Resource E No Intervalo De Tempo De 2 Minutos.
   * Portanto, A Função Atende Aos Requisitos De Agrupar Por Userid E Filtrar Notificações Repetidas Apenas Dentro Das Notificações Pertencentes A Cada Userid Individualmente.
   * <p>
   * @Param: pNotificacoesml -> Lista Contendo Todas As Notificações Do Mercado Livre.
   * @Return: A Função Agruparefiltrarnotificacoes Retorna Um Map<String, List<NotificacaoMLDTO>>, Onde:
   * <p>
   * Chave (String): Representa O Userid De Cada Usuário.
   * <p>
   * Valor (List<NotificacaoMLDTO>): É Uma Lista De Objetos Notificacaomldto Que Foram Filtrados Para Esse Userid Específico. Cada Lista Contém Apenas Notificações Únicas, Ou Seja, Notificações Que Não São Repetidas Dentro De Um Intervalo De 2 Minutos Para O Mesmo Resource.
   * @Author: Thonwelling*/

  public Map<String, List<NotificacaoMLDTO>> agruparEFiltrarNotificacoes(List<NotificacaoMLDTO> pNotificacoesML) {

  //Converte strings(ISO 8601) da tag received das em objetos LocalDateTime. Isso facilita a comparação de datas que faremos para filtrar as notificações.
  DateTimeFormatter formataData = DateTimeFormatter.ISO_DATE_TIME;

  // Agrupando Notificações Por User_Id
  return pNotificacoesML
      .stream()// Inicia um fluxo (stream) das notificações.
      .collect(Collectors.groupingBy(NotificacaoMLDTO::getUserId))//Agrupa as notificações em um Map onde a chave é o user_id e o valor é uma lista de notificações(List<NotificacaoMLDTO>) desse user_id.
      .entrySet().stream()//Converte o conjunto de entradas do mapa (Map) da etapa anterior em um fluxo(stream) para poder iterar sobre cada grupo de notificações(cada par user_id -> lista de notificações).
      .collect(Collectors.toMap(
    // Esse Coletor cria um novo Map onde:
    Map.Entry::getKey, // O (user_id) é a chave
    entry -> {
      List<NotificacaoMLDTO> vNotificacoesDoUsuario = entry.getValue(); //Obtém a lista de notificações associadas ao user_id atual.

      //Ordenar por data de recebimento
      vNotificacoesDoUsuario.sort(Comparator.comparing(NotificacaoMLDTO::getReceived));//Ordena as notificações dentro desse grupo em ordem crescente, com base na data de recebimento (received).

      List<NotificacaoMLDTO> vNotificacoesFiltradas = new ArrayList<>();
      Map<String, LocalDateTime> vUltimasNotificacoes = new HashMap<>(); //Armazena a Ultima Notificação Por Resource.

      for (NotificacaoMLDTO vNotificacao : vNotificacoesDoUsuario) {
        LocalDateTime vDataRecebida = LocalDateTime.parse(vNotificacao.getReceived(), formataData);
        String vResource = vNotificacao.getResource();
        if (! vUltimasNotificacoes.containsKey(vResource) || Duration.between(vUltimasNotificacoes.get(vResource), vDataRecebida).toMinutes() >= 2) {
          //Adiciona a Notifgicação a Lista Filtrada Se Não Houver Repetição Recente
          vNotificacoesFiltradas.add(vNotificacao);
          vUltimasNotificacoes.put(vResource, vDataRecebida); //Atualiza a Última Notificação Desse Resource
        }
      }
      return vNotificacoesFiltradas;
    }
  ));
}
//endregion

//region Função Para Apagar As Notificações No Banco MySql.
public List<Long> pegarIDsDasNotificacoesDoUsuario(List<NotificacaoMLDTO> pArrNotificacoes) throws SQLException {
  List<Long> vIDsNotificacoes = pArrNotificacoes
  .stream()
  .map(NotificacaoMLDTO::getId)
  .collect(Collectors.toList());
  logger.info(" Apagando " + vIDsNotificacoes.size() + " Notificações !!");
  return  vIDsNotificacoes;
}
//endregion

//region Função Para Extrair o SKU Da Tag Resource da Notificação.
public String extrairSkuMLDasNotificacoes(String resource) {
  if (resource != null && resource.startsWith("/items/")) {
    logger.info("Extraindo o SKU Da Notificação");
    return resource.substring("/items/".length());
  }
  return null;
}
//endregion

//region Função Para Arredondar Casas Decimais Dos Centavos.
public static double arredondarParaDuasCasasDecimais(double valor) {
  BigDecimal resultado = new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
  return resultado.doubleValue();
}

public double arredondarCentavos(double valor) {
  return Math.round(valor * 100) / 100.0;
}
//endregion

//region Função Para Calcular a Comissão Mercado Livre.
public double calcularComissaoML(double vComissao, double preco){
  if (vComissao > 0) {
    double percentualDeComissao = (vComissao / preco) * 100;
    double resultado            = arredondarCentavos(arredondarParaDuasCasasDecimais((percentualDeComissao)));
    if (preco < 79 && resultado > 18) {
      int mlCustoAdicional = 6;
      vComissao -= mlCustoAdicional;
      percentualDeComissao = (vComissao / preco) * 100;
      return resultado = arredondarCentavos(arredondarParaDuasCasasDecimais((percentualDeComissao)));
    }
  }
  return 0.0;
}
//endregion

//region Função Para Calcular o Custo Adicional.
public double calculaCustoAdicional(double pComissao, double pPreco, String pSupermercado) {
  double vCustoAdicional = 0.00;
	if (pComissao > 0) {
    double vComissao = arredondarParaDuasCasasDecimais(pComissao);
		if (!"S".equalsIgnoreCase(pSupermercado)) {
			if (pPreco < 79) {
				if (vComissao > 18){
          vCustoAdicional = 6;
        }
			}
		} else {
      if (pPreco < 30) {
        vCustoAdicional = 1;
      } else if (pPreco >= 30 && pPreco < 50) {
        vCustoAdicional = 2;
      } else if (pPreco >= 50 && pPreco < 100) {
        vCustoAdicional = 4;
      } else if (pPreco >= 100 && pPreco < 199) {
        vCustoAdicional = 6;
      }
    }
	}
  return vCustoAdicional;
}
//endregion

//region Função Para Salvar JSON em Arquivo.
public void gravaJSON(Object pObjeto, String pCaminhoArquivo) throws IOException {
  ObjectMapper vMapper = new ObjectMapper();
  try (FileWriter gravaJson = new FileWriter(pCaminhoArquivo)) {
    vMapper.enable(SerializationFeature.INDENT_OUTPUT);
    vMapper.writeValue(gravaJson, pObjeto);
  } catch (Exception excecaoGravaJson) {
    logger.log(Level.SEVERE, "Erro Ao Gravar JSON Na Temp.");
    throw excecaoGravaJson;
  }
}
//endregion

}