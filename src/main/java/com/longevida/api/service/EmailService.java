package com.longevida.api.service;

/** Envio de e-mails transacionais. Implementações: {@link SmtpEmailService} e {@link LogEmailService}. */
public interface EmailService {

    void enviarCodigoRecuperacao(String para, String nome, String codigo, int validadeMinutos);
}
