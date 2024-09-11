package com.ambarx.notificacoesML.services;

import com.ambarx.notificacoesML.repositories.NotificacaoMLItensRepository;
import com.ambarx.notificacoesML.repositories.NotificacaoMLUserProductsFamiliesRepository;

public interface NotificacaoMLItensService {
    void buscarTodasNotificacoes(NotificacaoMLItensRepository notificacaoMLItensRepository) throws Exception;

    void processaNotificacoesUserProductsFamilies(NotificacaoMLUserProductsFamiliesRepository notificacaoMLUserProductsFamiliesRepository) throws Exception;
}