package com.ambarx.notificacoesML.dto.conexao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "acessoapi_cadclientes")
public class ConexaoDTO {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "autoid", nullable = false)
      private Long id;
      @Column(name ="identificador_cliente")
      private String identificadorCliente;
      @Column(name = "status_cliente")
      private String statusCliente;
      @Column(name = "tipo_acesso")
      private String tipoAcesso;
      @Column(name = "servidor")
      private String servidor;
      @Column(name = "banco")
      private String banco;
      @Column(name = "usuario_sql")
      private String usuarioSql;
      @Column(name = "senha_sql")
      private String senhaSql;
      @Column(name = "unidade_acesso")
      private String unidadeAcesso;
      @Column(name = "acessoapi_tokenmd5")
      private String tokenMd5;
      @Column(name = "acessoapi_tokensha")
      private String tokenSha;


}
