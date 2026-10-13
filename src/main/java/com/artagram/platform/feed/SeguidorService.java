package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.feed.dto.ArtistaResumenDto;
import com.artagram.platform.feed.dto.SeguimientoDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soporte minimo para que HU-06 tenga de donde sacar el feed. La llave unica
 * (seguidor, seguido) hace que seguir dos veces seguidas sea idempotente.
 */
@Service
public class SeguidorService {

    private final SeguidorRepository seguidorRepository;
    private final UsuarioRepository usuarioRepository;

    public SeguidorService(SeguidorRepository seguidorRepository, UsuarioRepository usuarioRepository) {
        this.seguidorRepository = seguidorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void seguir(UUID seguidorId, UUID seguidoId) {
        if (seguidorId.equals(seguidoId)) {
            throw ApiException.badRequest("NO_TE_PUEDES_SEGUIR", "No te puedes seguir a ti mismo");
        }
        if (seguidorRepository.existsBySeguidor_IdAndSeguido_Id(seguidorId, seguidoId)) {
            return;
        }
        Usuario seguidor = usuarioRepository.getReferenceById(seguidorId);
        Usuario seguido = usuarioRepository.findById(seguidoId)
                .orElseThrow(() -> ApiException.notFound("ARTISTA_NO_ENCONTRADO", "Ese artista no existe"));
        try {
            seguidorRepository.save(new Seguidor(seguidor, seguido));
        } catch (DataIntegrityViolationException e) {
            // Doble clic casi simultaneo: la segunda solicitud no crea un segundo seguimiento.
        }
    }

    @Transactional
    public void dejarDeSeguir(UUID seguidorId, UUID seguidoId) {
        seguidorRepository.deleteBySeguidor_IdAndSeguido_Id(seguidorId, seguidoId);
    }

    /** Busca artistas por nombre (vacio = los primeros en orden alfabetico) e indica a cuales ya sigue. */
    @Transactional(readOnly = true)
    public List<ArtistaResumenDto> buscar(UUID solicitanteId, String texto) {
        String filtro = texto == null ? "" : texto.trim();
        Set<UUID> seguidos = seguidorRepository.findBySeguidor_Id(solicitanteId).stream()
                .map(s -> s.getSeguido().getId())
                .collect(Collectors.toSet());
        return usuarioRepository
                .findTop10ByNombreArtistaContainingIgnoreCaseAndVerificadoTrueAndIdNotOrderByNombreArtistaAsc(filtro, solicitanteId)
                .stream()
                .map(u -> new ArtistaResumenDto(u.getId(), u.getNombreArtista(), seguidos.contains(u.getId()),
                        seguidorRepository.countBySeguido_Id(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SeguimientoDto seguimiento(UUID solicitanteId, UUID artistaId) {
        return new SeguimientoDto(seguidorRepository.existsBySeguidor_IdAndSeguido_Id(solicitanteId, artistaId),
                seguidorRepository.countBySeguido_Id(artistaId));
    }
}
