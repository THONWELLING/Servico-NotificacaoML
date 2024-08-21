package com.ambarx.notificacoesML.repositories;

import com.ambarx.notificacoesML.dto.conexao.ConexaoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcessoApiCadClientesRepository extends JpaRepository<ConexaoDTO, Integer> {
    ConexaoDTO findByIdentificadorCliente(String identificadorCliente);

}