package com.ambarx.notificacoesML.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Setter
public class EcomSkuDTO {
private int autoId;
private int materialId;
private int ecomId;
private String sku;
private int idSku;
private BigDecimal vlrSite1;
private BigDecimal vlrSite2;
private String titulo;
private String texto;
private BigDecimal estoque;
private String ean;
private String ativo;
private String existe;
private int origemId;
private String tipoAnuncio;
private String tipoEnvio;
private int stCode;
private String retorno;
private String linkUrl;
private String prodMktpId;

private int buybox;
private String atualiza;
private int crossdocking;
private String skuVariacaoMaster;
private BigDecimal margem;
private String updt;
private BigDecimal custoAdicional;
private int armazem;
private String fulfillment;
private BigDecimal comissaoSku;
private BigDecimal vlrCusto;
private BigDecimal custoFrete;
private BigDecimal margemMax;
private int descarte;
private BigDecimal descontoVlr;
private BigDecimal impOutras;
private String cdTipo;
private String cdModelo;
private BigDecimal levelSku;
private String cdId;
private BigDecimal estoqueNd;
private String catalogo;
private BigDecimal priceBest;
private BigDecimal rebate;
private String mshops;
private LocalDateTime dtCriacao;
private String updStockFull;
private String flex;
private BigDecimal vlrSite3;
private LocalDateTime dtGet;
private String skuMktpId;
private int levelCompra;
private String catalogoSync;
private String mlFamilyId;
}