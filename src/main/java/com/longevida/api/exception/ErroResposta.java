package com.longevida.api.exception;

import java.time.Instant;
import java.util.Map;

/** Formato único de erro devolvido pela API. */
public record ErroResposta(String timestamp, int status, String erro, String mensagem, Map<String, String> campos) {

    public static ErroResposta de(int status, String erro, String mensagem) {
        return new ErroResposta(Instant.now().toString(), status, erro, mensagem, null);
    }

    public static ErroResposta de(int status, String erro, String mensagem, Map<String, String> campos) {
        return new ErroResposta(Instant.now().toString(), status, erro, mensagem, campos);
    }
}
