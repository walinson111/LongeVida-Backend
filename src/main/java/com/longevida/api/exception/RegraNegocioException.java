package com.longevida.api.exception;

/** 422 - a requisição é válida, mas viola uma regra de negócio. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
