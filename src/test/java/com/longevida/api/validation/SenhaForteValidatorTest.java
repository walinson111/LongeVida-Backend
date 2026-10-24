package com.longevida.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.longevida.api.dto.AuthDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SenhaForteValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<ConstraintViolation<AuthDto.CadastroRequest>> validar(String senha) {
        return validator.validateProperty(
                new AuthDto.CadastroRequest("Maria", "maria@ex.com", senha, 65, null), "senha");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Senha-Forte-123", "Abcdef1!", "Çãoç#2024Xy", "P@ssw0rd${x}"})
    void aceitaSenhasFortes(String senha) {
        assertThat(validar(senha)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!",              
            "senhafraca1!",      
            "SENHAFRACA1!",      
            "SenhaFraca!!",      
            "SenhaFraca12",      
            "Senha Forte 1!",    
            "12345678"           
    })
    void recusaSenhasFracas(String senha) {
        assertThat(validar(senha)).hasSize(1);
    }

    @Test
    void recusaSenhaAcimaDe64Caracteres() {
        assertThat(validar("Aa1!" + "x".repeat(61))).hasSize(1);
    }

    @Test
    void mensagemListaTodosOsRequisitosFaltando() {
        String msg = validar("abc").iterator().next().getMessage();
        assertThat(msg).isEqualTo("A senha precisa ter entre 8 e 64 caracteres, uma letra maiúscula, um número"
                + " e um caractere especial (ex.: ! @ # $ % & * - _).");
    }

    @Test
    void senhaEmBrancoFicaParaONotBlank() {
        Set<ConstraintViolation<AuthDto.CadastroRequest>> violacoes = validar("");
        assertThat(violacoes).hasSize(1);
        assertThat(violacoes.iterator().next().getMessage()).isEqualTo("Informe a senha.");
    }
}
