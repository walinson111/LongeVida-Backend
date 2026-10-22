package com.longevida.api.service;

import com.longevida.api.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Usado quando o e-mail está desativado (app.mail.enabled=false, o padrão).
 * Nenhum e-mail é enviado. Somente com app.mail.log-codigo=true (perfil "dev") o código é
 * escrito no log, para permitir testar a recuperação de senha localmente.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LogEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LogEmailService.class);

    private final AppProperties props;

    public LogEmailService(AppProperties props) {
        this.props = props;
    }

    @Override
    public void enviarCodigoRecuperacao(String para, String nome, String codigo, int validadeMinutos) {
        if (props.mail().logCodigo()) {
            log.warn("[DEV] E-mail desativado. Código de recuperação de senha de {}: {} (vale {} min)",
                    para, codigo, validadeMinutos);
        } else {
            log.warn("E-mail desativado (app.mail.enabled=false): código de recuperação NÃO foi enviado. "
                    + "Configure o SMTP para habilitar a recuperação de senha.");
        }
    }
}
