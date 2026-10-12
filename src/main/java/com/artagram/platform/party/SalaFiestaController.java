package com.artagram.platform.party;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.common.lock.DistributedLockService;
import com.artagram.platform.party.dto.CrearSalaRequest;
import com.artagram.platform.party.dto.EstadoRondaResponse;
import com.artagram.platform.party.dto.SalaFiestaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-18 (crear/entrar) y disparo de rondas de HU-19. */
@RestController
@Tag(name = "Modo fiesta", description = "HU-18/19/20: salas, rondas cronometradas y borrador disputado")
public class SalaFiestaController {

    private final SalaFiestaService salaFiestaService;
    private final DistributedLockService lockService;

    public SalaFiestaController(SalaFiestaService salaFiestaService, DistributedLockService lockService) {
        this.salaFiestaService = salaFiestaService;
        this.lockService = lockService;
    }

    @PostMapping("/api/fiestas")
    public ResponseEntity<SalaFiestaResponse> crear(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                                      @Valid @RequestBody CrearSalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaFiestaService.crear(principal.usuarioId(), request));
    }

    @PostMapping("/api/fiestas/unirse")
    public SalaFiestaResponse unirse(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                      @RequestParam String codigo) {
        return salaFiestaService.unirse(principal.usuarioId(), codigo);
    }

    @PostMapping("/api/fiestas/{salaId}/rondas")
    public ResponseEntity<Void> iniciarRonda(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                              @PathVariable UUID salaId) {
        salaFiestaService.iniciarRonda(principal.usuarioId(), salaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/fiestas/{salaId}")
    public SalaFiestaResponse ver(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                   @PathVariable UUID salaId) {
        if (!salaFiestaService.esParticipanteActivo(principal.usuarioId(), salaId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        return salaFiestaService.obtenerRespuesta(salaId);
    }

    @GetMapping("/api/fiestas/{salaId}/estado")
    public EstadoRondaResponse estado(@AuthenticationPrincipal JwtService.TokenPrincipal principal,
                                       @PathVariable UUID salaId) {
        if (!salaFiestaService.esParticipanteActivo(principal.usuarioId(), salaId)) {
            throw ApiException.forbidden("NO_ERES_PARTICIPANTE", "No perteneces a esta sala");
        }
        String propietarioBorrador = lockService.propietarioActual("fiesta-borrador:" + salaId)
                .map(p -> p.substring(p.indexOf("::") + 2))
                .orElse(null);
        return salaFiestaService.obtenerEstado(salaId, propietarioBorrador);
    }
}
