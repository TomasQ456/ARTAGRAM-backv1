package com.artagram.platform.billing;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRegistradoEvent;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuotas del plan gratis (HU-26). La verificacion de cupo y el incremento van
 * en la MISMA transaccion, con bloqueo pesimista sobre la fila de uso del
 * usuario, para que varias solicitudes concurrentes no se cuelen por encima
 * del tope (ver UsoCuentaRepository.buscarParaActualizar).
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final UsoCuentaRepository usoCuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuotaProperties properties;

    public QuotaService(UsoCuentaRepository usoCuentaRepository, UsuarioRepository usuarioRepository,
                         QuotaProperties properties) {
        this.usoCuentaRepository = usoCuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.properties = properties;
    }

    @EventListener
    @Transactional
    public void alRegistrarUsuario(UsuarioRegistradoEvent evento) {
        if (usoCuentaRepository.findById(evento.usuarioId()).isEmpty()) {
            usoCuentaRepository.save(new UsoCuenta(evento.usuarioId()));
        }
    }

    @Transactional
    public void reservarCupoComunidad(UUID usuarioId) {
        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        UsoCuenta uso = usoCuentaRepository.buscarParaActualizar(usuarioId)
                .orElseGet(() -> usoCuentaRepository.save(new UsoCuenta(usuarioId)));
        if (!usuario.isPremium() && uso.getComunidadesCreadas() >= properties.getFreeMaxCommunities()) {
            log.info("Tope de comunidades alcanzado: usuario={} limite={}", usuarioId, properties.getFreeMaxCommunities());
            throw new ApiException(HttpStatus.FORBIDDEN, "TOPE_COMUNIDADES",
                    "Llegaste al limite de " + properties.getFreeMaxCommunities()
                            + " comunidades del plan gratis. Pasate a premium para crear comunidades ilimitadas");
        }
        uso.incrementarComunidades();
    }

    @Transactional
    public void liberarCupoComunidad(UUID usuarioId) {
        usoCuentaRepository.buscarParaActualizar(usuarioId).ifPresent(UsoCuenta::decrementarComunidades);
    }

    @Transactional
    public void reservarCupoSalaFiesta(UUID usuarioId) {
        UsoCuenta uso = usoCuentaRepository.buscarParaActualizar(usuarioId)
                .orElseGet(() -> usoCuentaRepository.save(new UsoCuenta(usuarioId)));
        uso.incrementarSalasFiesta();
        log.debug("Cupo sala de fiesta reservado (sin limite): usuario={}", usuarioId);
    }

    @Transactional
    public void liberarCupoSalaFiesta(UUID usuarioId) {
        usoCuentaRepository.buscarParaActualizar(usuarioId).ifPresent(UsoCuenta::decrementarSalasFiesta);
    }
}
