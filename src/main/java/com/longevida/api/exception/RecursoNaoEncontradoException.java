package com.longevida.api.exception;

/** 404 - também usada quando o recurso existe mas pertence a outro usuário (evita enumeração). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
