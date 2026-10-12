package com.artagram.platform.observability;

import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.observability.dto.KpiDetalleDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula los 4 KPIs del business case (HU-37, seccion 7 del backlog) a
 * partir de los eventos minimos que deja EventoNegocioRecorder. Cada KPI
 * expone tambien su formula, periodo y cuantos eventos se contaron, para que
 * HU-37 ("quiero saber de donde sale el numero") no sea una caja negra.
 */
@Service
public class KpiService {

    private static final double META_TASA_COLABORACION = 0.40;
    private static final double META_DURACION_SESION_MINUTOS = 12.0;
    private static final double META_RETENCION_7_DIAS = 0.25;
    private static final double META_CONVERSION_PREMIUM = 0.05;

    private final EventoNegocioRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public KpiService(EventoNegocioRepository eventoRepository, UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<KpiDetalleDto> calcularTodos() {
        return List.of(
                calcular("tasa-colaboracion"),
                calcular("duracion-sesion"),
                calcular("retencion-7-dias"),
                calcular("conversion-premium")
        );
    }

    @Transactional(readOnly = true)
    public KpiDetalleDto calcular(String id) {
        Instant ahora = Instant.now();
        return switch (id) {
            case "tasa-colaboracion" -> tasaColaboracion(ahora.minus(7, ChronoUnit.DAYS), ahora);
            case "duracion-sesion" -> duracionSesion(ahora.minus(7, ChronoUnit.DAYS), ahora);
            case "retencion-7-dias" -> retencion7Dias(ahora);
            case "conversion-premium" -> conversionPremium(ahora.minus(30, ChronoUnit.DAYS), ahora);
            default -> throw ApiException.notFound("KPI_NO_ENCONTRADO", "No existe ese KPI");
        };
    }

    private KpiDetalleDto tasaColaboracion(Instant desde, Instant hasta) {
        long activos = eventoRepository.contarUsuariosDistintos(TipoEventoNegocio.LOGIN, desde, hasta);
        long colaborativos = eventoRepository.contarUsuariosDistintos(TipoEventoNegocio.ENTRO_COLABORATIVO, desde, hasta);
        double valor = activos == 0 ? 0 : (double) colaborativos / activos;
        double anterior = tasaColaboracionValor(desde.minus(7, ChronoUnit.DAYS), desde);
        return new KpiDetalleDto("tasa-colaboracion", "Tasa de colaboracion", valor, META_TASA_COLABORACION,
                tendencia(valor, anterior),
                "usuarios activos que entraron a un lienzo o sala de fiesta / usuarios activos, en la semana",
                desde, hasta, activos);
    }

    private double tasaColaboracionValor(Instant desde, Instant hasta) {
        long activos = eventoRepository.contarUsuariosDistintos(TipoEventoNegocio.LOGIN, desde, hasta);
        long colaborativos = eventoRepository.contarUsuariosDistintos(TipoEventoNegocio.ENTRO_COLABORATIVO, desde, hasta);
        return activos == 0 ? 0 : (double) colaborativos / activos;
    }

    private KpiDetalleDto duracionSesion(Instant desde, Instant hasta) {
        Double promedioMs = eventoRepository.promedioDuracionMs(TipoEventoNegocio.SALIO_COLABORATIVO, desde, hasta);
        long sesiones = eventoRepository.contarEventos(TipoEventoNegocio.SALIO_COLABORATIVO, desde, hasta);
        double minutos = promedioMs == null ? 0 : promedioMs / 60000.0;
        Instant desdeAnterior = desde.minus(7, ChronoUnit.DAYS);
        Double promedioAnteriorMs = eventoRepository.promedioDuracionMs(TipoEventoNegocio.SALIO_COLABORATIVO, desdeAnterior, desde);
        double minutosAnterior = promedioAnteriorMs == null ? 0 : promedioAnteriorMs / 60000.0;
        return new KpiDetalleDto("duracion-sesion", "Duracion de la sesion colaborativa", minutos, META_DURACION_SESION_MINUTOS,
                tendencia(minutos, minutosAnterior),
                "tiempo total de las sesiones de lienzo / numero de sesiones, en minutos, en la semana",
                desde, hasta, sesiones);
    }

    private KpiDetalleDto retencion7Dias(Instant ahora) {
        Instant cohorteDesde = ahora.minus(14, ChronoUnit.DAYS);
        Instant cohorteHasta = ahora.minus(7, ChronoUnit.DAYS);
        double valor = retencionValor(cohorteDesde, cohorteHasta);
        long tamanoCohorte = usuarioRepository.findAll().stream()
                .filter(u -> !u.getCreadoEn().isBefore(cohorteDesde) && u.getCreadoEn().isBefore(cohorteHasta))
                .count();
        Instant cohorteAnteriorDesde = cohorteDesde.minus(7, ChronoUnit.DAYS);
        double valorAnterior = retencionValor(cohorteAnteriorDesde, cohorteDesde);
        return new KpiDetalleDto("retencion-7-dias", "Retencion a 7 dias", valor, META_RETENCION_7_DIAS,
                tendencia(valor, valorAnterior),
                "artistas que volvieron a entrar entre el dia 1 y el dia 7 / artistas registrados en el periodo",
                cohorteDesde, cohorteHasta, tamanoCohorte);
    }

    private double retencionValor(Instant cohorteDesde, Instant cohorteHasta) {
        List<UUID> cohorte = usuarioRepository.findAll().stream()
                .filter(u -> !u.getCreadoEn().isBefore(cohorteDesde) && u.getCreadoEn().isBefore(cohorteHasta))
                .map(u -> u.getId())
                .toList();
        if (cohorte.isEmpty()) {
            return 0;
        }
        long retenidos = cohorte.stream()
                .filter(id -> {
                    var usuario = usuarioRepository.findById(id).orElseThrow();
                    Instant ventanaDesde = usuario.getCreadoEn().plus(1, ChronoUnit.DAYS);
                    Instant ventanaHasta = usuario.getCreadoEn().plus(7, ChronoUnit.DAYS);
                    return eventoRepository.existsByUsuarioIdAndTipoAndOcurridoEnBetween(id, TipoEventoNegocio.LOGIN, ventanaDesde, ventanaHasta);
                })
                .count();
        return (double) retenidos / cohorte.size();
    }

    private KpiDetalleDto conversionPremium(Instant desde, Instant hasta) {
        List<UUID> activos = eventoRepository.usuariosDistintos(TipoEventoNegocio.LOGIN, desde, hasta);
        long totalActivos = activos.size();
        long premium = activos.stream().filter(id -> usuarioRepository.findById(id).map(u -> u.isPremium()).orElse(false)).count();
        double valor = totalActivos == 0 ? 0 : (double) premium / totalActivos;
        Instant desdeAnterior = desde.minus(30, ChronoUnit.DAYS);
        List<UUID> activosAnterior = eventoRepository.usuariosDistintos(TipoEventoNegocio.LOGIN, desdeAnterior, desde);
        long premiumAnterior = activosAnterior.stream().filter(id -> usuarioRepository.findById(id).map(u -> u.isPremium()).orElse(false)).count();
        double valorAnterior = activosAnterior.isEmpty() ? 0 : (double) premiumAnterior / activosAnterior.size();
        return new KpiDetalleDto("conversion-premium", "Conversion a premium", valor, META_CONVERSION_PREMIUM,
                tendencia(valor, valorAnterior),
                "usuarios activos del mes con premium activo / usuarios activos del mes",
                desde, hasta, totalActivos);
    }

    private String tendencia(double actual, double anterior) {
        if (actual > anterior) {
            return "SUBE";
        }
        if (actual < anterior) {
            return "BAJA";
        }
        return "IGUAL";
    }
}
