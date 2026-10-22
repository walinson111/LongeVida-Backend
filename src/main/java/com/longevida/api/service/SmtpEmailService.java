package com.longevida.api.service;

import com.longevida.api.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia e-mails de verdade via SMTP. Só é criado com app.mail.enabled=true e exige
 * a configuração spring.mail.* (SPRING_MAIL_HOST, SPRING_MAIL_USERNAME, ...). Ver README.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public SmtpEmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public void enviarCodigoRecuperacao(String para, String nome, String codigo, int validadeMinutos) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(props.mail().from());
        mensagem.setTo(para);
        mensagem.setSubject("LongeVida - código para redefinir sua senha");
        mensagem.setText("Olá, " + nome + "!\n\n"
                + "Seu código para redefinir a senha é: " + codigo + "\n\n"
                + "Ele vale por " + validadeMinutos + " minutos. "
                + "Se você não pediu a redefinição, ignore esta mensagem.\n\n"
                + "Equipe LongeVida");
        mailSender.send(mensagem);
    }
}
