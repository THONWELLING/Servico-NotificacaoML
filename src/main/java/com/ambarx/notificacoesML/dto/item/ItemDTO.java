package com.ambarx.notificacoesML.dto.item;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ItemDTO {
  private String id;
  private String title;
  private Long sellerId;
  private String categoryId;
  private String thumbnail;
  private double price;
  private int basePrice;
  private int initialQuantity;
  private int availableQuantity;
  private int soldQuantity;
  private String inventoryId;
  private String buyingMode;
  private String listingTypeId;
  private Date startTime;
  private Date stopTime;
  private Date endTime;
  private Date expirationTime;
  private String condition;
  private String permalink;
  private ArrayList<ImagemDTO> pictures;
  private ArrayList<Object>   descriptions;
  private EnvioDTO shipping;
  private EnderecoVendedorDTO sellerAddress;
  private String sellerContact;
  private ArrayList<AtributoDTO> attributes;
  private ArrayList<VariacaoDTO> variations;
  private String status;
  private ArrayList<String> subStatus;
  private ArrayList<String> tags;
  private Date dateCreated;
  private Date lastUpdated;
  private ArrayList<ItemRelationDTO> itemRelations;
  private ArrayList<String> channels;
  private boolean catalogListing;
}
