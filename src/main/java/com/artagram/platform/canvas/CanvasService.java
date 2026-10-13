package com.artagram.platform.canvas;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.canvas.dto.EnviarPuntosRequest;
import com.artagram.platform.canvas.dto.TrazoEventoDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.community.ComunidadService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasService {

    private static final Logger log = LoggerFactory.getLogger(CanvasService.class);

    private final Map<String, Long> cacheMiembros = new ConcurrentHashMap<>();

    private final LienzoRepository lienzoRepository;
    private final TrazoRepository trazoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadService comunidadService;
    private final CanvasMetrics metrics;

    public CanvasService(LienzoRepository lienzoRepository, TrazoRepository trazoRepository,
                          UsuarioRepository usuarioRepository, ComunidadService comunidadService,
                          CanvasMetrics metrics) {
        this.lienzoRepository = lienzoRepository;
        this.trazoRepository = trazoRepository;
        this.usuarioRepository = usuarioRepository;
        this.comunidadService = comunidadService;
        this.metrics = metrics;
    }

    /**
     * Solo relay: no se persiste nada mientras el trazo esta en progreso, para
     * que dibujar se sienta instantaneo y el lienzo no crezca con basura si
     * alguien nunca suelta el pincel (HU-12, "el trazo viaja como una secuencia
     * de puntos").
     */
    public TrazoEventoDto procesarParcial(UUID comunidadId, UUID autorId, String autorNombre, EnviarPuntosRequest request) {
        exigirMiembroActivo(autorId, comunidadId);
        return TrazoEventoDto.parcial(request.trazoEnProgresoId(), autorId, autorNombre, request);
    }

    @Transactional
    public TrazoEventoDto finalizarTrazo(UUID comunidadId, UUID autorId, String autorNombre, EnviarPuntosRequest request) {
        exigirMiembroActivo(autorId, comunidadId);
        Lienzo lienzo = lienzoRepository.buscarParaActualizar(comunidadId)
                .orElseThrow(() -> ApiException.notFound("LIENZO_NO_ENCONTRADO", "Esta comunidad no tiene lienzo"));
        long secuencia = lienzo.siguienteSecuencia();

        Usuario autor = usuarioRepository.getReferenceById(autorId);
        Trazo trazo = new Trazo(lienzo.getId(), autor, secuencia, request.tipoPincel(), request.colorHex(),
                request.grosor(), request.opacidad(), request.capa(), request.puntos());
        trazoRepository.save(trazo);
        metrics.incrementarTrazoFinalizado();
        log.debug("Trazo finalizado: comunidad={} secuencia={} autor={}", comunidadId, secuencia, autorId);

        return TrazoEventoDto.finalizado(request.trazoEnProgresoId(), trazo.getId(), secuencia, autorId, autorNombre, request);
    }

    public void registrarLatencia(long millis) {
        metrics.registrarLatenciaMs(millis);
    }

    @Transactional(readOnly = true)
    public List<Trazo> obtenerTrazosDesde(UUID comunidadId, long desdeSecuencia) {
        Lienzo lienzo = lienzoRepository.findByComunidadId(comunidadId)
                .orElseThrow(() -> ApiException.notFound("LIENZO_NO_ENCONTRADO", "Esta comunidad no tiene lienzo"));
        return trazoRepository.findByLienzoIdAndSecuenciaGreaterThanOrderBySecuenciaAsc(lienzo.getId(), desdeSecuencia);
    }

    private void exigirMiembroActivo(UUID usuarioId, UUID comunidadId) {
        String clave = usuarioId + ":" + comunidadId;
        Long expira = cacheMiembros.get(clave);
        long ahora = System.currentTimeMillis();
        if (expira != null && ahora < expira) {
            return;
        }
        if (!comunidadService.esMiembroActivo(usuarioId, comunidadId)) {
            cacheMiembros.remove(clave);
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }
        cacheMiembros.put(clave, ahora + 30_000L);
    }
}
