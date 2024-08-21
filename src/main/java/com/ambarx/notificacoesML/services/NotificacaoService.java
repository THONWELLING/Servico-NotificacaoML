package com.ambarx.notificacoesML.services;

import com.ambarx.notificacoesML.repositories.NotificacaoMercadoLivreRepository;

public interface NotificacaoService {
    void buscarTodasNotificacoes(NotificacaoMercadoLivreRepository notificacaoMercadoLivreRepository) throws Exception;

}