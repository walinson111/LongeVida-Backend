package com.longevida.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

/** Valida {@link SenhaForte} e informa exatamente quais requisitos faltam. */
public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    public static final int TAMANHO_MINIMO = 8;
    public static final int TAMANHO_MAXIMO = 64;

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        if (senha == null || senha.isBlank()) {
            return true; // @NotBlank cuida deste caso
        }

        List<String> faltando = requisitosFaltando(senha);
        if (faltando.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(montarMensagem(faltando)).addConstraintViolation();
        return false;
    }

    /** Lista, em português, os requisitos que a senha não cumpre (vazia se a senha é forte). */
    public static List<String> requisitosFaltando(String senha) {
        List<String> faltando = new ArrayList<>();
        int tamanho = senha.codePointCount(0, senha.length());
        if (tamanho < TAMANHO_MINIMO || tamanho > TAMANHO_MAXIMO) {
            faltando.add("entre " + TAMANHO_MINIMO + " e " + TAMANHO_MAXIMO + " caracteres");
        }
        if (senha.codePoints().noneMatch(Character::isUpperCase)) {
            faltando.add("uma letra maiúscula");
        }
        if (senha.codePoints().noneMatch(Character::isLowerCase)) {
            faltando.add("uma letra minúscula");
        }
        if (senha.codePoints().noneMatch(Character::isDigit)) {
            faltando.add("um número");
        }
        if (senha.codePoints().noneMatch(SenhaForteValidator::isEspecial)) {
            faltando.add("um caractere especial (ex.: ! @ # $ % & * - _)");
        }
        if (senha.codePoints().anyMatch(Character::isWhitespace)) {
            faltando.add("nenhum espaço");
        }
        return faltando;
    }

    private static boolean isEspecial(int c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    private static String montarMensagem(List<String> faltando) {
        String lista = faltando.size() == 1
                ? faltando.get(0)
                : String.join(", ", faltando.subList(0, faltando.size() - 1)) + " e " + faltando.get(faltando.size() - 1);
        // Escapa caracteres que o interpolador de mensagens trataria como expressões.
        return ("A senha precisa ter " + lista + ".")
                .replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$");
    }
}
