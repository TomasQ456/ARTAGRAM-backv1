package com.artagram.platform.canvas;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.canvas.dto.BannerTrazoEventoDto;
import com.artagram.platform.canvas.dto.PintarRegionRequest;
import com.artagram.platform.canvas.dto.RegionEstadoDto;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.common.lock.ConcurrencyMetrics;
import com.artagram.platform.community.ComunidadService;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pintar el banner comunitario particionado en regiones (HU-16). Cada region
 * se serializa con un bloqueo pesimista de fila sobre su contador (igual
 * patron que el lienzo, HU-12); regiones distintas nunca se esperan entre si
 * porque son filas distintas.
 */
@Service
public class BannerService {

    private static final Logger log = LoggerFactory.getLogger(BannerService.class);

    private final BannerComunitarioRepository bannerRepository;
    private final BannerRegionEstadoRepository regionEstadoRepository;
    private final BannerTrazoRepository bannerTrazoRepository;
    private final BannerSnapshotRepository snapshotRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadService comunidadService;
    private final BannerRateLimiter rateLimiter;
    private final ConcurrencyMetrics metrics;
    private final CanvasProperties properties;

    public BannerService(BannerComunitarioRepository bannerRepository, BannerRegionEstadoRepository regionEstadoRepository,
                          BannerTrazoRepository bannerTrazoRepository, BannerSnapshotRepository snapshotRepository,
                          UsuarioRepository usuarioRepository, ComunidadService comunidadService,
                          BannerRateLimiter rateLimiter, ConcurrencyMetrics metrics, CanvasProperties properties) {
        this.bannerRepository = bannerRepository;
        this.regionEstadoRepository = regionEstadoRepository;
        this.bannerTrazoRepository = bannerTrazoRepository;
        this.snapshotRepository = snapshotRepository;
        this.usuarioRepository = usuarioRepository;
        this.comunidadService = comunidadService;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Transactional
    public BannerTrazoEventoDto pintar(UUID comunidadId, UUID usuarioId, String nombreUsuario, PintarRegionRequest request) {
        if (!comunidadService.esMiembroActivo(usuarioId, comunidadId)) {
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }
        BannerComunitario banner = bannerRepository.findByComunidadId(comunidadId)
                .orElseThrow(() -> ApiException.notFound("BANNER_NO_ENCONTRADO", "Esta comunidad no tiene banner"));

        int maxRegion = properties.regionsPerSide() * properties.regionsPerSide();
        if (request.regionIndex() >= maxRegion) {
            throw ApiException.badRequest("REGION_INVALIDA", "Esa region no existe");
        }
        if (!rateLimiter.permitir(comunidadId, usuarioId)) {
            metrics.registrarBloqueoNegado();
            throw ApiException.tooManyRequests("LIMITE_DE_RITMO",
                    "Vas muy rapido pintando el banner, baja el ritmo (maximo "
                            + properties.getBannerRateLimitPerSecond() + " operaciones por segundo)");
        }

        BannerRegionEstado region = regionEstadoRepository.buscarParaActualizar(banner.getId(), request.regionIndex())
                .orElseThrow(() -> ApiException.notFound("REGION_NO_ENCONTRADA", "Esa region no existe"));
        long secuencia = region.siguienteSecuencia();

        Usuario autor = usuarioRepository.getReferenceById(usuarioId);
        BannerTrazo trazo = new BannerTrazo(banner.getId(), request.regionIndex(), secuencia, autor,
                request.tipoPincel(), request.colorHex(), request.grosor(), request.opacidad(), request.puntos());
        bannerTrazoRepository.save(trazo);
        log.debug("Banner pintado: comunidad={} region={} secuencia={} autor={}",
                comunidadId, request.regionIndex(), secuencia, usuarioId);

        return new BannerTrazoEventoDto(request.regionIndex(), secuencia, usuarioId, nombreUsuario,
                request.tipoPincel(), request.colorHex(), request.grosor(), request.opacidad(), request.puntos());
    }

    @Transactional(readOnly = true)
    public List<RegionEstadoDto> obtenerEstado(UUID comunidadId) {
        BannerComunitario banner = bannerRepository.findByComunidadId(comunidadId)
                .orElseThrow(() -> ApiException.notFound("BANNER_NO_ENCONTRADO", "Esta comunidad no tiene banner"));
        int totalRegiones = properties.regionsPerSide() * properties.regionsPerSide();

        return java.util.stream.IntStream.range(0, totalRegiones).mapToObj(regionIndex -> {
            Optional<BannerSnapshot> snapshot = snapshotRepository.findByBannerIdAndRegionIndex(banner.getId(), regionIndex);
            long desde = snapshot.map(BannerSnapshot::getUltimaSecuenciaIncluida).orElse(0L);
            List<BannerTrazoEventoDto> pendientes = bannerTrazoRepository
                    .findByBannerIdAndRegionIndexAndSecuenciaGreaterThanOrderBySecuenciaAsc(banner.getId(), regionIndex, desde)
                    .stream()
                    .map(t -> new BannerTrazoEventoDto(t.getRegionIndex(), t.getSecuencia(), t.getAutor().getId(),
                            t.getAutor().getNombreArtista(), t.getTipoPincel(), t.getColorHex(), t.getGrosor(),
                            t.getOpacidad(), t.getPuntos()))
                    .toList();
            String base64 = snapshot.map(s -> Base64.getEncoder().encodeToString(s.getPng())).orElse(null);
            return new RegionEstadoDto(regionIndex, base64, pendientes);
        }).toList();
    }
}
