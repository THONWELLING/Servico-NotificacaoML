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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
@Service
public class FuncoesUtils {
private final Logger logger = Logger.getLogger(FuncoesUtils.class.getName());


//region Função Para Agrupar As Notificações e Filtrar Para Evitar Requisições Desnecessárias
public Map<String, List<NotificacaoMLDTO>> agruparEFiltrarNotificacoes(List<NotificacaoMLDTO> notificacoesML) {
  /**
   * Esse Função Foi Criada Co o Intuito De Eliminar Requisições Desnecessárias Mediante a Eliminação de Notificações Exatamente Iguais Eme um Período de 2 Minutos.
   * Ou Seja, Se Existirem 10 Notificações Exatamente Iguais de Um Mesmo Seller e o Período de Recebimento da Mesma For Menor que 2 Min. Entre elas, Serão Eliminadas
   * as Notificações Repetidas. Isso vai Resultar Na Diminuição De Requisições Feitas Para a API Externa Diminuindo Assim o Tráfego de Dados.

   * @Author: Thonwelling*/

  //Converte strings(ISO 8601) da tag received das em objetos LocalDateTime. Isso facilita a comparação de datas que faremos para filtrar as notificações.
  DateTimeFormatter formataData = DateTimeFormatter.ISO_DATE_TIME;

  // Agrupando Notificações Por User_Id
  return notificacoesML.stream()// Inicia um fluxo (stream) das notificações.
    .collect(Collectors.groupingBy(NotificacaoMLDTO::getUserId))//Agrupa as notificações em um Map onde a chave é o user_id e o valor é uma lista de notificações(List<NotificacaoMLDTO>) desse user_id.
    .entrySet().stream()//Converte o conjunto de entradas do mapa (Map) da etapa anterior em um fluxo(stream) para poder iterar sobre cada grupo de notificações(cada par user_id -> lista de notificações).
    .collect(Collectors.toMap(
      // Esse Coletor cria um novo Map onde:
      Map.Entry::getKey, // O (user_id) é a chave
      entry -> {
        List<NotificacaoMLDTO> notificacoesDoUsuario = entry.getValue(); //Obtém a lista de notificações associadas ao user_id atual.

        //Ordenar por data de recebimento
        notificacoesDoUsuario.sort(Comparator.comparing(NotificacaoMLDTO::getReceived));//Ordena as notificações dentro desse grupo em ordem crescente, com base na data de recebimento (received).

        //Filtrar Notificações Repetidas
        return notificacoesDoUsuario.stream()
          /**Filtra a lista de notificações para remover as repetidas.*/
          .filter(notificacao -> {
            LocalDateTime dataRecebida = LocalDateTime.parse(notificacao.getReceived(), formataData);//Converte a data de recebimento da notificação atual em um objeto LocalDateTime.
            return notificacoesDoUsuario.stream()
              .noneMatch(other -> {
                if (notificacao == other) return false;
                LocalDateTime otherDataRecebida = LocalDateTime.parse(other.getReceived(), formataData);
                return other.getResource().equals(notificacao.getResource()) // Verifica se as duas notificações têm o mesmo resource.
                  && Duration.between(otherDataRecebida, dataRecebida).abs().toMinutes() < 2; //Verifica se o intervalo entre as datas de recebimento (received) das duas notificações é menor que 2 minutos.
              });//Se noneMatch encontrar uma notificação repetida, a notificação atual será filtrada e não incluída na lista resultante.
          })
          .collect(Collectors.toList());
      }
    ));
}
//endregion


//region Função Para Apagar As Notificações No Banco MySql.
public List<Long> apagarNotificacoes(List<NotificacaoMLDTO> notificacoes) throws SQLException {
  List<Long> idsNotificacoes = notificacoes.stream()
    .map(NotificacaoMLDTO::getId)
    .collect(Collectors.toList());
  logger.info(" Apagando >------> " + idsNotificacoes.size() + " <------< Notificações !!");
  return  idsNotificacoes;
}
//endregion


//region Função Para Extrair o SKU Da tag resource da BNotificação.
public String extrairSkuMLDasNotificacoes(String resource) {
  if (resource != null && resource.startsWith("/items/")) {
    logger.info(" Extraindo o SKU Da Notificação!! ");
    return resource.substring("/items/".length());
  }
  return null;
}
//endregion

//region Função Para Arredondar Casas Decimais Dos Centavos.
public double arredondarParaDuasCasasDecimais(double valor) {
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

//region Função Para Salvar JSON em Arquivo.
public void gravarJson( Object object, String caminhoArquivo) throws IOException {
  ObjectMapper mapper = new ObjectMapper();
  try (FileWriter gravaJson = new FileWriter(caminhoArquivo)) {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(gravaJson, object);
  } catch (Exception excecao) {
    excecao.printStackTrace();
  }
}
//endregion

}