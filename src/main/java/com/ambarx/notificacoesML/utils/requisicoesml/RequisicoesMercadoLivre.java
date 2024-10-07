package com.ambarx.notificacoesML.utils.requisicoesml;

import com.ambarx.notificacoesML.customizedExceptions.LimiteRequisicaoMLException;
import com.ambarx.notificacoesML.dto.comissao.ComissaoDTO;
import com.ambarx.notificacoesML.dto.frete.FreteDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.httpclients.MercadoLivreHttpClient;
import com.ambarx.notificacoesML.utils.RespostaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

@Component
public class RequisicoesMercadoLivre {
/**Essa Classe Tem Como Objetivo Centralizar Todas As Requisições Que Serão Feitas Para a API Do Mercado Livre
 *
 * @Author Thonwelling*/

private static MercadoLivreHttpClient mercadoLivreHttpClient = null;

	@Autowired
public RequisicoesMercadoLivre(RestTemplate restTemplate) {
  mercadoLivreHttpClient = new MercadoLivreHttpClient(restTemplate);
}

	//region Função Para Fazer a Requisição De Items.
	public static RespostaAPI<ItemDTO> fazerRequisicaoGetItem(String pSkuML, String pTokenSeller, String pIdentificadorSeller, String pApi) throws IOException, LimiteRequisicaoMLException {
		String urlGetItems = "https://api.mercadolibre.com/items/" + pSkuML + "?include_attributes=all";
		return mercadoLivreHttpClient.fazerRequisicao(urlGetItems, pTokenSeller, pIdentificadorSeller, ItemDTO.class, pApi);

		/*utils.gravaJSON(respostaAPI, "C:/Ambar/Temp/RetornoGetItemMl.txt");*/
	}
	//endregion

	//region Função Para Fazer a Requisição De Comissão.
	public static RespostaAPI<ComissaoDTO> fazerRequisicaoGetComissaoML(String pTipoDeAnuncio, double pPreco, String pCategoria, String pTokenSeller, String pIdentificadorSeller, String pApi) throws IOException, LimiteRequisicaoMLException {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat formatoValor   = new DecimalFormat("0.00", symbols);
		String        urlGetComissao = "https://api.mercadolibre.com/sites/MLB/listing_prices/" + pTipoDeAnuncio + "?price=" + formatoValor.format(pPreco) + "&category_id=" + pCategoria;

		return mercadoLivreHttpClient.fazerRequisicao(urlGetComissao, pTokenSeller, pIdentificadorSeller, ComissaoDTO.class, pApi);
	}
	//endregion

	//region Função Para Fazer a Requisição De Frete.
	public static RespostaAPI<FreteDTO> fazerRequisicaoGetFrete(String pUserId, String pSkuML, String pTokenSeller, String pIdentificadorSeller, String pApi) throws IOException, LimiteRequisicaoMLException {
		String urlFrete = "https://api.mercadolibre.com/users/" + pUserId + "/shipping_options/free?item_id=" + pSkuML;
		return mercadoLivreHttpClient.fazerRequisicao(urlFrete, pTokenSeller, pIdentificadorSeller, FreteDTO.class, pApi);
	}
	//endregion

}
