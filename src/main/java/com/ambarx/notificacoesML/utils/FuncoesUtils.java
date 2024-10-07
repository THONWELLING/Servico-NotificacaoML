package com.ambarx.notificacoesML.utils;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.dto.notificacao.NotificacaoMLDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class FuncoesUtils {
  private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();

  //region Função Para Agrupar As Notificações De Itens e Filtrar Para Evitar Requisições Desnecessárias
  /**
     * A Função Abaixo Foi Criada Com O Intuito De Eliminar Requisições Desnecessárias Mediante A Eliminação De Notificações Exatamente Iguais Dentro De Um Período De 2 Minutos.
     * Ou Seja, Se Existirem 10 Notificações Exatamente Iguais De Um Mesmo Seller(Userid) E O Período De Recebimento Da Mesma(Received) For Menor Que 2 Min. Entre Elas, Serão Eliminadas
     * As Notificações Repetidas. Isso Vai Resultar Na Diminuição De Requisições Feitas Para A Api Externa Diminuindo Assim O Tráfego De Dados E A Intermitência No Processamento.
     * Como A Função Funciona
     * Agrupamento Por Userid:
     * <p>
     * A Função Começa Agrupando Todas As Notificações Pelo UserId Usando Collectors.groupingby(Notificacaomldto::getuserid). Isso Cria Um Map<String, List<NotificacaoMLDTO>> Onde A Chave É O Userid E O Valor É Uma Lista De Notificações Associadas A Esse Userid.
     * Iteração Sobre Os Grupos De Userid:
     * <p>
     * Depois De Agrupar As Notificações, A Função Itera Sobre Cada Entrada Desse Mapa (Entryset().stream()), O Que Significa Que Ela Processa Cada Grupo De Notificações De Forma Independente.
     * Filtragem Dentro De Cada Grupo De Userid:
     * <p>
     * Para Cada Userid E Sua Lista De Notificações (Entry.getvalue()), A Função:
     * Ordena As Notificações Pela Data De Recebimento (Received).
     * Utiliza Um Hashmap Chamado <arrNotificacoesPorResource> Para Que a Notificação Mais Recente Cara Cada Resource Seja Mantida.
     * Filtra As Notificações Dentro Da Lista Específica De Um Userid Para Remover Notificações Repetidas.
     * Resumo
     * A Função Proposta Faz Exatamente O Que Você Descreveu:
     * <p>
     * Agrupa Notificações Por Userid.
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

     // Agrupando Notificações Por User_Id
     return pNotificacoesML
        .stream()// Inicia um fluxo (stream) das notificações.
        .collect(Collectors.groupingBy(NotificacaoMLDTO::getUserId))//Agrupa as notificações em um Map onde a chave é o user_id e o valor é uma lista de notificações(List<NotificacaoMLDTO>) desse user_id.
        .entrySet().stream()//Converte o conjunto de entradas do (Map) da etapa anterior em um fluxo(stream) para iterar sobre cada grupo de notificações(cada par user_id -> lista de notificações).
        .map(entrada -> {
          List<NotificacaoMLDTO> arrNotificacoesDoUsuario = entrada.getValue(); //Obtém a lista de notificações do user_id atual.

          //Ordenar por data de recebimento(Mais Nova)
          arrNotificacoesDoUsuario.sort(Comparator.comparing(NotificacaoMLDTO::getReceived).reversed());

          Map<String, NotificacaoMLDTO> arrNotificacoesPorResource = new HashMap<>();

          for (NotificacaoMLDTO vNotificacao : arrNotificacoesDoUsuario) {
            String vResource = vNotificacao.getResource();
            arrNotificacoesPorResource.putIfAbsent(vResource, vNotificacao); //Atualiza a Última Notificação Desse Resource
          }
          return Map.entry(entrada.getKey(), new ArrayList<>(arrNotificacoesPorResource.values())); // Retorna a chave (user_id) e a lista filtrada com as notificações mais recentes por resource
        })
        .sorted((entrada1, entrada2) -> Integer.compare(entrada2.getValue().size(), entrada1.getValue().size()))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (e1, e2) -> e1,
            LinkedHashMap::new
        )
    );
  }
  //endregion

  //region Função Para Pegar IDs Das Notificações No Banco MySql.
  public List<Long> pegarIDsDasNotificacoesDoUsuario(List<NotificacaoMLDTO> pArrNotificacoes) throws SQLException {
    return pArrNotificacoes
    .stream()
    .map(NotificacaoMLDTO::getId)
    .collect(Collectors.toList());
  }
  //endregion

  //region Função Para Extrair o SKU Da Tag Resource da Notificação.
  public String extrairSkuMLDasNotificacoes(String resource) {
    if (resource != null && resource.startsWith("/items/")) {
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
      loggerRobot.severe("Erro Ao Gravar JSON Na Pasta Temp. -> " + excecaoGravaJson.getMessage());
      throw excecaoGravaJson;
    }
  }
  //endregion



}