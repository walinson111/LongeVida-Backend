package com.longevida.api.exception;

/** 409 - conflito com o estado atual (ex.: e-mail já cadastrado). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
