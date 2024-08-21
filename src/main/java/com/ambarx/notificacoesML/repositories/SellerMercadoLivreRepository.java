package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.models.SellerMercadoLivre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerMercadoLivreRepository extends JpaRepository<SellerMercadoLivre, String> {
    Optional<SellerMercadoLivre> findIdentificadorClienteBySellerId(String sellerId);

}
