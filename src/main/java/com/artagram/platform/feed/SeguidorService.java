package com.artagram.platform.feed;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.common.error.ApiException;
import java.util.UUID;
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
}
