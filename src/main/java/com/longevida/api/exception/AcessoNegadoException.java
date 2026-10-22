package com.longevida.api.exception;

/** 403 - o usuário está autenticado, mas não tem permissão para a ação. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
