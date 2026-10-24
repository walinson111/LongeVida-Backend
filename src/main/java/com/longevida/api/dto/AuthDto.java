package com.longevida.api.dto;

import com.longevida.api.validation.SenhaForte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDto {

    private AuthDto() {}

    public record PerfilProfissionalCadastro(
            @NotBlank(message = "Informe a especialidade.") @Size(max = 60, message = "Especialidade muito longa.") String especialidade,
            @NotBlank(message = "Informe o registro profissional.") @Size(max = 30, message = "Registro muito longo.") String registro,
            @Size(max = 500, message = "Descrição muito longa.") String descricao) {}

    public record CadastroRequest(
            @NotBlank(message = "Informe o nome.") @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 160, message = "E-mail muito longo.") String email,
            @NotBlank(message = "Informe a senha.") @SenhaForte String senha,
            @NotNull(message = "Informe a idade.") @Min(value = 1, message = "Idade inválida.") @Max(value = 120, message = "Idade inválida.") Integer idade,
            @Valid PerfilProfissionalCadastro perfilProfissional) {}

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {}

    public record AuthResponse(String token, String tipo, Instant expiraEm, UsuarioDto.Resposta usuario) {}

    public record EsqueciSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email) {}

    public record RedefinirSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe o código.") @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos.") String codigo,
            @NotBlank(message = "Informe a nova senha.") @SenhaForte String novaSenha) {}

    public record MensagemResposta(String mensagem) {}
}
