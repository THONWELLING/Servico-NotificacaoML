package com.ambarx.notificacoesML.utils.requisicoesml;

import com.ambarx.notificacoesML.dto.comissao.ComissaoDTO;
import com.ambarx.notificacoesML.dto.frete.FreteDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.httpclients.MercadoLivreHttpClient;
import com.ambarx.notificacoesML.mapper.ModelMapperMapping;
import com.ambarx.notificacoesML.utils.FuncoesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class RequisicoesMercadoLivre {
/**Essa Classe Tem Como Objetivo Centralizar Todas As Requisições Que Serão Feitas Para a API Do Mercado Livre
 *
 * @Author Thonwelling*/

private static final Logger logger = Logger.getLogger(RequisicoesMercadoLivre.class.getName());
private static MercadoLivreHttpClient mercadoLivreHttpClient = null;
@Autowired
private static FuncoesUtils utils = new FuncoesUtils();

@Autowired
public RequisicoesMercadoLivre(RestTemplate restTemplate) {
  mercadoLivreHttpClient = new MercadoLivreHttpClient(restTemplate);
}

//region Função Para Fazer a Requisição De Items.
public static ItemDTO fazerRequisicaoGetItem(String skuML, String tokenSeller) throws IOException {
  String urlGetItems = "https://api.mercadolibre.com/items/" + skuML + "?include_attributes=all";
  logger.log(Level.INFO, "Fazendo GET Do SKU No Mercado Livre!!!");
  ItemDTO respostaAPI = ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlGetItems, tokenSeller, ItemDTO.class), ItemDTO.class);

  utils.gravaJSON(respostaAPI, "C:/Ambar/Temp/RetornoGetItemMl.txt");
  return respostaAPI;
}
//endregion

//region Função Para Fazer a Requisição De Comissão.
public static double fazerRequisicaoGetComissaoML(String tipoDeAnuncio, double preco, String categoria, String tokenSeller) throws IOException {
  DecimalFormatSymbols symbols = new DecimalFormatSymbols();
  symbols.setDecimalSeparator('.');
  DecimalFormat formatoValor   = new DecimalFormat("0.00", symbols);
  String        urlGetComissao = "https://api.mercadolibre.com/sites/MLB/listing_prices/" + tipoDeAnuncio + "?price=" + formatoValor.format(preco) + "&category_id=" + categoria;
  logger.log(Level.INFO, "Fazendo GET Da Comissão No Mercado Livre!!!");
  ComissaoDTO  respostaAPIComissao = ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlGetComissao, tokenSeller, ComissaoDTO.class), ComissaoDTO.class);
  utils.gravaJSON(respostaAPIComissao, "C:/Ambar/Temp/RetornoGetComissaoMl.txt");

  // Extrair valor da comissão
  return respostaAPIComissao.getSaleFeeDetails().getPercentageFee();
}
//endregion

//region Função Para Fazer a Requisição De Frete.
public static double fazerRequisicaoGetFrete(String userId, String skuML, String tokenSeller) throws IOException {
  String urlFrete = "https://api.mercadolibre.com/users/" + userId + "/shipping_options/free?item_id=" + skuML;
  logger.log(Level.INFO, "Fazendo GET Do Frete No Mercado Livre!!!");

  try {
    FreteDTO respostaAPIFrete = ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlFrete, tokenSeller, FreteDTO.class), FreteDTO.class);
    utils.gravaJSON(respostaAPIFrete, "C:/Ambar/Temp/RetornoGetFreteMl.txt");
    return respostaAPIFrete != null ? respostaAPIFrete.getCoverage().getAllCountry().getListCost() : 0.00;
  } catch (Exception excecao) {
    excecao.printStackTrace();
    return 0.00;
  }
}
//endregion

}
