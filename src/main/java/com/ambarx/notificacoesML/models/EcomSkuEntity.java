package com.ambarx.notificacoesML.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Setter
@Table(name = "ECOM_SKU")
public class EcomSkuEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "AUTOID")
private int autoId;

@Column(name = "MATERIAL_ID")
private int materialId;

@Column(name = "ECOM_ID")
private int ecomId;

@Column(name = "SKU")
private String sku;

@Column(name = "ID_SKU")
private int idSku;

@Column(name = "VLR_SITE1")
private BigDecimal vlrSite1;

@Column(name = "VLR_SITE2")
private BigDecimal vlrSite2;

@Column(name = "TITULO")
private String titulo;

@Column(name = "TEXTO")
private String texto;

@Column(name = "ESTOQUE")
private BigDecimal estoque;

@Column(name = "EAN")
private String ean;

@Column(name = "ATIVO")
private String ativo;

@Column(name = "EXISTE")
private String existe;

@Column(name = "ORIGEM_ID")
private int origemId;

@Column(name = "TIPO_ANUNCIO")
private String tipoAnuncio;

@Column(name = "TIPO_ENVIO")
private String tipoEnvio;

@Column(name = "STCODE")
private int stCode;

@Column(name = "RETORNO")
private String retorno;

@Column(name = "LINKURL")
private String linkUrl;

@Column(name = "PRODMKTP_ID")
private String prodMktpId;

@Column(name = "BUYBOX")
private int buybox;

@Column(name = "ATUALIZA")
private String atualiza;

@Column(name = "CROSSDOCKING")
private int crossdocking;

@Column(name = "SKUVARIACAO_MASTER")
private String skuVariacaoMaster;

@Column(name = "MARGEM")
private BigDecimal margem;

@Column(name = "UPDT")
private String updt;

@Column(name = "CUSTO_ADICIONAL")
private BigDecimal custoAdicional;

@Column(name = "ARMAZEM")
private int armazem;

@Column(name = "FULFILLMENT")
private String fulfillment;

@Column(name = "COMISSAO_SKU")
private BigDecimal comissaoSku;

@Column(name = "VLR_CUSTO")
private BigDecimal vlrCusto;

@Column(name = "CUSTO_FRETE")
private BigDecimal custoFrete;

@Column(name = "MARGEM_MAX")
private BigDecimal margemMax;

@Column(name = "DESCARTE")
private int descarte;

@Column(name = "DESCONTO_VLR")
private BigDecimal descontoVlr;

@Column(name = "IMPOUTRAS")
private BigDecimal impOutras;

@Column(name = "CD_TIPO")
private String cdTipo;

@Column(name = "CD_MODELO")
private String cdModelo;

@Column(name = "LEVELSKU")
private BigDecimal levelSku;

@Column(name = "CD_ID")
private String cdId;

@Column(name = "ESTOQUE_ND")
private BigDecimal estoqueNd;

@Column(name = "CATALOGO")
private String catalogo;

@Column(name = "PRICE_BEST")
private BigDecimal priceBest;

@Column(name = "REBATE")
private BigDecimal rebate;

@Column(name = "MSHOPS")
private String mshops;

@Column(name = "DTCRIACAO")
private LocalDateTime dtCriacao;

@Column(name = "UPDSTOCKFULL")
private String updStockFull;

@Column(name = "FLEX")
private String flex;

@Column(name = "VLR_SITE3")
private BigDecimal vlrSite3;

@Column(name = "DT_GET")
private LocalDateTime dtGet;

@Column(name = "SKUMKTP_ID")
private String skuMktpId;

@Column(name = "LEVELCOMPRA")
private int levelCompra;

@Column(name = "CATALOGOSYNC")
private String catalogoSync;

@Column(name = "ML_FAMILY_ID")
private String mlFamilyId;
}