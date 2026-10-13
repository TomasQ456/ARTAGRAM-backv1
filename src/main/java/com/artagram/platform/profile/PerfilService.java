package com.artagram.platform.profile;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRegistradoEvent;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.profile.dto.ActualizarPerfilRequest;
import com.artagram.platform.profile.dto.EnlaceDto;
import com.artagram.platform.profile.dto.ObraDestacadaDto;
import com.artagram.platform.profile.dto.PerfilResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PerfilService {

    private static final long TAMANO_MAX_FOTO_PERFIL = 2L * 1024 * 1024;
    private static final long TAMANO_MAX_IMAGEN_OBRA = 15L * 1024 * 1024;

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final StorageService storageService;

    public PerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository, StorageService storageService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.storageService = storageService;
    }

    @EventListener
    @Transactional
    public void alRegistrarUsuario(UsuarioRegistradoEvent evento) {
        obtenerOCrear(evento.usuarioId());
    }

    @Transactional
    public Perfil obtenerOCrear(UUID usuarioId) {
        return perfilRepository.findByUsuario_Id(usuarioId).orElseGet(() -> {
            Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
            return perfilRepository.save(new Perfil(usuario));
        });
    }

    @Transactional
    public PerfilResponse actualizar(UUID usuarioId, ActualizarPerfilRequest request) {
        Perfil perfil = obtenerOCrear(usuarioId);
        if (request.biografia() != null) {
            perfil.setBiografia(request.biografia());
        }
        if (request.enlaces() != null) {
            List<Enlace> enlaces = request.enlaces().stream()
                    .map(e -> new Enlace(e.etiqueta(), e.url(), request.enlaces().indexOf(e)))
                    .toList();
            perfil.reemplazarEnlaces(enlaces);
        }
        if (request.obrasDestacadas() != null) {
            List<ObraDestacada> obras = request.obrasDestacadas().stream()
                    .map(o -> new ObraDestacada(o.titulo(), o.rutaImagen(), request.obrasDestacadas().indexOf(o)))
                    .toList();
            perfil.reemplazarObrasDestacadas(obras);
        }
        return aRespuesta(perfil, true);
    }

    @Transactional
    public String subirFoto(UUID usuarioId, MultipartFile archivo) {
        Perfil perfil = obtenerOCrear(usuarioId);
        String ruta = storageService.guardar(archivo, "perfiles/" + usuarioId, TAMANO_MAX_FOTO_PERFIL);
        perfil.setFotoUrl(ruta);
        return storageService.urlFirmada(ruta);
    }

    public String subirImagenObraDestacada(UUID usuarioId, MultipartFile archivo) {
        return storageService.guardar(archivo, "perfiles/" + usuarioId + "/obras", TAMANO_MAX_IMAGEN_OBRA);
    }

    @Transactional(readOnly = true)
    public PerfilResponse verPropio(UUID usuarioId) {
        Perfil perfil = obtenerOCrear(usuarioId);
        return aRespuesta(perfil, true);
    }

    @Transactional(readOnly = true)
    public PerfilResponse verPublico(String nombreArtista, UUID usuarioIdSolicitante) {
        Perfil perfil = perfilRepository.findByUsuario_NombreArtistaIgnoreCase(nombreArtista)
                .orElseThrow(() -> ApiException.notFound("PERFIL_NO_ENCONTRADO", "Ese artista no existe"));
        boolean esPropio = usuarioIdSolicitante != null && perfil.getUsuario().getId().equals(usuarioIdSolicitante);
        return aRespuesta(perfil, esPropio);
    }

    @Transactional
    public PerfilResponse otorgarPremium(String nombreArtista, boolean activo) {
        Usuario usuario = usuarioRepository.findByNombreArtistaIgnoreCase(nombreArtista)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "No existe ese usuario"));
        usuario.setPremium(activo);
        usuarioRepository.save(usuario);
        Perfil perfil = obtenerOCrear(usuario.getId());
        return aRespuesta(perfil, false);
    }

    private PerfilResponse aRespuesta(Perfil perfil, boolean esPropio) {
        List<EnlaceDto> enlaces = perfil.getEnlaces().stream()
                .map(e -> new EnlaceDto(e.getEtiqueta(), e.getUrl()))
                .toList();
        List<PerfilResponse.ObraDestacadaResumen> obras = perfil.getObrasDestacadas().stream()
                .map(o -> new PerfilResponse.ObraDestacadaResumen(o.getTitulo(), storageService.urlFirmada(o.getImagenUrl())))
                .toList();
        return new PerfilResponse(
                perfil.getUsuario().getId(),
                perfil.getUsuario().getNombreArtista(),
                perfil.getBiografia(),
                storageService.urlFirmada(perfil.getFotoUrl()),
                enlaces,
                obras,
                esPropio,
                perfil.getUsuario().isPremium()
        );
    }
}
