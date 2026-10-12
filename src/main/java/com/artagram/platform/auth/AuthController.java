package com.artagram.platform.auth;

import com.artagram.platform.auth.dto.LoginRequest;
import com.artagram.platform.auth.dto.LoginResponse;
import com.artagram.platform.auth.dto.NombreDisponibleResponse;
import com.artagram.platform.auth.dto.RegistroRequest;
import com.artagram.platform.auth.dto.RegistroResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticación", description = "HU-01: registro, verificación de correo y login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/registro")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @GetMapping("/api/auth/verificar")
    public ResponseEntity<Void> verificar(@RequestParam String token) {
        authService.verificar(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/api/auth/nombre-disponible")
    public ResponseEntity<NombreDisponibleResponse> nombreDisponible(@RequestParam String nombre) {
        return ResponseEntity.ok(authService.verificarNombreDisponible(nombre));
    }
}
