package com.longevida.api.controller;

import com.longevida.api.dto.AuthDto;
import com.longevida.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.AuthResponse cadastro(@Valid @RequestBody AuthDto.CadastroRequest req) {
        return authService.cadastrar(req);
    }

    @PostMapping("/login")
    public AuthDto.AuthResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/esqueci-senha")
    public AuthDto.MensagemResposta esqueciSenha(@Valid @RequestBody AuthDto.EsqueciSenhaRequest req) {
        authService.esqueciSenha(req);
        return new AuthDto.MensagemResposta("Se o e-mail estiver cadastrado, você receberá um código para redefinir a senha.");
    }

    @PostMapping("/redefinir-senha")
    public AuthDto.MensagemResposta redefinirSenha(@Valid @RequestBody AuthDto.RedefinirSenhaRequest req) {
        authService.redefinirSenha(req);
        return new AuthDto.MensagemResposta("Senha redefinida com sucesso. Faça login com a nova senha.");
    }
}
