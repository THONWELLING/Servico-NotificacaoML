package com.ambarx.notificacoesML.utils.requisicoesml;

import com.ambarx.notificacoesML.config.logger.LoggerConfig;
import com.ambarx.notificacoesML.dto.comissao.ComissaoDTO;
import com.ambarx.notificacoesML.dto.frete.FreteDTO;
import com.ambarx.notificacoesML.dto.item.ItemDTO;
import com.ambarx.notificacoesML.dto.prices.PrecosMLDTO;
import com.ambarx.notificacoesML.httpclients.MercadoLivreHttpClient;
import com.ambarx.notificacoesML.mapper.ModelMapperMapping;
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

private static final Logger loggerRobot = LoggerConfig.getLoggerRobot();
private static final Logger logger = Logger.getLogger(RequisicoesMercadoLivre.class.getName());
private static MercadoLivreHttpClient mercadoLivreHttpClient = null;

	@Autowired
public RequisicoesMercadoLivre(RestTemplate restTemplate) {
  mercadoLivreHttpClient = new MercadoLivreHttpClient(restTemplate);
}

	//region Função Para Fazer a Requisição De Items.
	public static ItemDTO fazerRequisicaoGetItem(String pSkuML, String pTokenSeller, String pIdentificadorSeller) throws IOException {
		String urlGetItems = "https://api.mercadolibre.com/items/" + pSkuML + "?include_attributes=all";
		try {
			logger.log(Level.INFO, "Fazendo GET De Item No Mercado Livre.");
			return ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlGetItems, pTokenSeller, pIdentificadorSeller, ItemDTO.class), ItemDTO.class);
			/*utils.gravaJSON(respostaAPI, "C:/Ambar/Temp/RetornoGetItemMl.txt");*/
		} catch (IOException excecaoMlItens) {
			loggerRobot.severe("FALHA: Erro Ao Buscar Dados Na API De Itens. -> \n Seller: -> " + pIdentificadorSeller + "\nMensagem: -> " + excecaoMlItens.getMessage());
			return null;
		}
	}
	//endregion

	//region Função Para Fazer a Requisição De Comissão.
	public static double fazerRequisicaoGetComissaoML(String pTipoDeAnuncio, double pPreco, String pCategoria, String pTokenSeller, String pIdentificadorSeller) throws IOException {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat formatoValor   = new DecimalFormat("0.00", symbols);
		String        urlGetComissao = "https://api.mercadolibre.com/sites/MLB/listing_prices/" + pTipoDeAnuncio + "?price=" + formatoValor.format(pPreco) + "&category_id=" + pCategoria;
		logger.log(Level.INFO, "Fazendo GET Da Comissão No Mercado Livre!!!");
		try {
			ComissaoDTO respostaAPIComissao = ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlGetComissao, pTokenSeller, pIdentificadorSeller, ComissaoDTO.class), ComissaoDTO.class);

			// Extrair valor da comissão
			return respostaAPIComissao != null ? respostaAPIComissao.getSaleFeeDetails().getPercentageFee() : 0.00;
		} catch (IOException excecaoComissao) {
			loggerRobot.severe("FALHA: Erro Ao Buscar Dados Na API De Comissão.\nSeller: -> " +pIdentificadorSeller + "\nMensagem: -> " + excecaoComissao.getMessage());
			return 0.00;
		}

	}
	//endregion

	//region Função Para Fazer a Requisição De Frete.
	public static double fazerRequisicaoGetFrete(String pUserId, String pSkuML, String pTokenSeller, String pIdentificadorSeller) throws IOException {
		String urlFrete = "https://api.mercadolibre.com/users/" + pUserId + "/shipping_options/free?item_id=" + pSkuML;
		logger.log(Level.INFO, "Fazendo GET Do Frete No Mercado Livre!!!");

		try {
			FreteDTO respostaAPIFrete = ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlFrete, pTokenSeller, pIdentificadorSeller, FreteDTO.class), FreteDTO.class);
			return respostaAPIFrete != null ? respostaAPIFrete.getCoverage().getAllCountry().getListCost() : 0.00;
		} catch (Exception excecaoFrete) {
			loggerRobot.severe("FALHA: Erro Ao Buscar Dados Na API De FreTe. \nSeller: -> " + pIdentificadorSeller + "\nMensagem: -> " + excecaoFrete.getMessage());
			return 0.00;
		}
	}
//endregion

	//region Função Para Fazer a Requisição De Items.
	public static PrecosMLDTO fazerRequisicaoGetPrices(String pSkuML, String pTokenSeller, String pIdentificadorSeller) throws IOException {
		String urlGetItems = "https://api.mercadolibre.com/items/" + pSkuML + "/prices";
		try {
			logger.log(Level.INFO, "Fazendo GET De Preços Mercado Livre.");
			return ModelMapperMapping.parseObject(mercadoLivreHttpClient.fazerRequisicao(urlGetItems, pTokenSeller, pIdentificadorSeller, PrecosMLDTO.class), PrecosMLDTO.class);
		} catch (IOException excecaoMlItens) {
			loggerRobot.severe("FALHA: Erro Ao Buscar Dados Na API De Itens. -> \n Seller: -> " + pIdentificadorSeller + "\nMensagem: -> " + excecaoMlItens.getMessage());
			return null;
		}
	}
//endregion
}
