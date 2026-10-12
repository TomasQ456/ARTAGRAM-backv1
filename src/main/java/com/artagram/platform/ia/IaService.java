package com.artagram.platform.ia;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.Publicacion;
import com.artagram.platform.feed.PublicacionRepository;
import com.artagram.platform.ia.estrategia.AnalisisTecnicoEstrategia;
import com.artagram.platform.ia.estrategia.AnalisisTecnicoResultado;
import com.artagram.platform.ia.estrategia.EstrategiaAnalisis;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasEstrategia;
import com.artagram.platform.ia.estrategia.SugerenciaEtiquetasResultado;
import com.artagram.platform.ia.proveedor.ProveedorIa;
import com.artagram.platform.profile.StorageService;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IaService {

    private static final Logger log = LoggerFactory.getLogger(IaService.class);

    private final AnalisisObraRepository analisisRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final StorageService storageService;
    private final ProveedorIa proveedorIa;
    private final Map<TipoAnalisis, EstrategiaAnalisis<?>> estrategiasPorTipo;

    public IaService(AnalisisObraRepository analisisRepository,
                     PublicacionRepository publicacionRepository,
                     UsuarioRepository usuarioRepository,
                     StorageService storageService,
                     ProveedorIa proveedorIa,
                     List<EstrategiaAnalisis<?>> estrategias) {
        this.analisisRepository = analisisRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.storageService = storageService;
        this.proveedorIa = proveedorIa;
        this.estrategiasPorTipo = estrategias.stream()
                .collect(Collectors.toMap(EstrategiaAnalisis::getTipo, Function.identity()));
    }

    @Transactional
    public AnalisisObraDto analizarPublicacion(UUID usuarioId, UUID publicacionId, TipoAnalisis tipo) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "Esa publicación no existe"));
        Usuario solicitante = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        String dataUri = cargarImagenDataUri(publicacion.getObraFinalRuta());

        EstrategiaAnalisis<?> estrategia = estrategiasPorTipo.get(tipo);
        if (estrategia == null) {
            throw ApiException.badRequest("TIPO_ANALISIS_NO_SOPORTADO", "Tipo de análisis no soportado: " + tipo);
        }

        log.info("Iniciando análisis {} para publicación {} con modelo {}", tipo, publicacionId, proveedorIa.getNombreModelo());
        Object resultado = estrategia.analizar(dataUri, proveedorIa);
        String resultadoJson = serializar(estrategia, resultado);

        AnalisisObra analisis = new AnalisisObra(publicacion, solicitante, tipo, resultadoJson, proveedorIa.getNombreModelo());
        analisis = analisisRepository.save(analisis);

        log.info("Análisis de IA guardado con éxito: id={} tipo={}", analisis.getId(), tipo);
        return aDto(analisis);
    }

    @Transactional(readOnly = true)
    public AnalisisObraDto obtenerUltimoAnalisis(UUID publicacionId, TipoAnalisis tipo) {
        TipoAnalisis tipoBuscado = tipo != null ? tipo : TipoAnalisis.TECNICO;
        return analisisRepository.findFirstByPublicacion_IdAndTipoOrderByCreadoEnDesc(publicacionId, tipoBuscado)
                .map(this::aDto)
                .orElseThrow(() -> ApiException.notFound("ANALISIS_NO_ENCONTRADO", "No hay análisis previo registrado para esta obra"));
    }

    @Transactional(readOnly = true)
    public List<AnalisisObraDto> historial(UUID publicacionId) {
        return analisisRepository.findByPublicacion_IdOrderByCreadoEnDesc(publicacionId)
                .stream()
                .map(this::aDto)
                .toList();
    }

    public SugerenciaEtiquetasResultado sugerirMetadatos(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw ApiException.badRequest("IMAGEN_REQUERIDA", "Se requiere una imagen para sugerir etiquetas");
        }
        try {
            byte[] bytes = imagen.getBytes();
            String mime = imagen.getContentType() != null ? imagen.getContentType() : "image/png";
            String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);

            @SuppressWarnings("unchecked")
            EstrategiaAnalisis<SugerenciaEtiquetasResultado> estrategia =
                    (EstrategiaAnalisis<SugerenciaEtiquetasResultado>) estrategiasPorTipo.get(TipoAnalisis.SUGERENCIA_ETIQUETAS);

            return estrategia.analizar(dataUri, proveedorIa);
        } catch (IOException e) {
            log.error("Error leyendo imagen para sugerencias de IA", e);
            throw ApiException.badRequest("IMAGEN_ILEGIBLE", "No se pudo leer la imagen proporcionada");
        }
    }

    private String cargarImagenDataUri(String rutaRelativa) {
        try {
            Resource recurso = storageService.leer(rutaRelativa);
            byte[] bytes = recurso.getContentAsByteArray();
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            log.error("No se pudo cargar la imagen para análisis de IA: ruta={}", rutaRelativa, e);
            throw ApiException.badRequest("IMAGEN_NO_DISPONIBLE", "No se pudo acceder a la imagen de la obra");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> String serializar(EstrategiaAnalisis<T> estrategia, Object resultado) {
        return estrategia.aJson((T) resultado);
    }

    private AnalisisObraDto aDto(AnalisisObra entidad) {
        return new AnalisisObraDto(
                entidad.getId(),
                entidad.getPublicacion().getId(),
                entidad.getSolicitante().getId(),
                entidad.getTipo(),
                entidad.getResultadoJson(),
                entidad.getModelo(),
                entidad.getCreadoEn()
        );
    }
}
