package com.longevida.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige uma senha forte: entre 8 e 64 caracteres, com pelo menos uma letra maiúscula,
 * uma letra minúscula, um número e um caractere especial, e sem espaços.
 *
 * <p>Valores nulos ou em branco são considerados válidos aqui, para que a mensagem de
 * {@code @NotBlank} prevaleça.
 */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default "A senha não atende aos requisitos de segurança.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
