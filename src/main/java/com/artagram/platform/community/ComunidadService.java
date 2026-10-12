package com.artagram.platform.community;

import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.billing.QuotaService;
import com.artagram.platform.common.error.ApiException;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComunidadService {

    private static final Logger log = LoggerFactory.getLogger(ComunidadService.class);

    private final ComunidadRepository comunidadRepository;
    private final MembresiaComunidadRepository membresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final QuotaService quotaService;
    private final ApplicationEventPublisher eventPublisher;

    public ComunidadService(ComunidadRepository comunidadRepository,
                             MembresiaComunidadRepository membresiaRepository,
                             UsuarioRepository usuarioRepository,
                             QuotaService quotaService,
                             ApplicationEventPublisher eventPublisher) {
        this.comunidadRepository = comunidadRepository;
        this.membresiaRepository = membresiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.quotaService = quotaService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ComunidadResponse crear(UUID usuarioId, CrearComunidadRequest request) {
        if (comunidadRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw ApiException.conflict("NOMBRE_COMUNIDAD_TOMADO", "Ese nombre de comunidad ya esta tomado");
        }
        // El cupo se reserva ANTES de crear: si falla, no queda comunidad huerfana.
        quotaService.reservarCupoComunidad(usuarioId);

        Usuario creador = usuarioRepository.getReferenceById(usuarioId);
        Comunidad comunidad;
        try {
            comunidad = comunidadRepository.save(new Comunidad(
                    request.nombre(), request.descripcion() == null ? "" : request.descripcion(),
                    request.publica(), creador));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("NOMBRE_COMUNIDAD_TOMADO", "Ese nombre de comunidad ya esta tomado");
        }
        membresiaRepository.save(new MembresiaComunidad(comunidad, creador, RolComunidad.ADMIN));
        eventPublisher.publishEvent(new ComunidadCreadaEvent(comunidad.getId()));
        log.info("Comunidad creada: id={} nombre='{}' creador={}", comunidad.getId(), comunidad.getNombre(), usuarioId);

        return aRespuesta(comunidad, RolComunidad.ADMIN);
    }

    @Transactional
    public ComunidadResponse unirse(UUID usuarioId, UUID comunidadId) {
        Comunidad comunidad = comunidadRepository.findById(comunidadId)
                .orElseThrow(() -> ApiException.notFound("COMUNIDAD_NO_ENCONTRADA", "Esa comunidad no existe"));
        if (!comunidad.isPublica()) {
            throw ApiException.forbidden("COMUNIDAD_PRIVADA", "Esta comunidad es privada, necesitas invitacion");
        }
        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        MembresiaComunidad membresia = membresiaRepository.findByComunidad_IdAndUsuario_Id(comunidadId, usuarioId)
                .map(existing -> {
                    existing.reactivar();
                    return existing;
                })
                .orElseGet(() -> membresiaRepository.save(new MembresiaComunidad(comunidad, usuario, RolComunidad.MIEMBRO)));
        log.info("Usuario {} se unió a la comunidad {} con rol {}", usuarioId, comunidadId, membresia.getRol());

        return aRespuesta(comunidad, membresia.getRol());
    }

    @Transactional
    public void cambiarRol(UUID solicitanteId, UUID comunidadId, UUID usuarioObjetivoId, RolComunidad nuevoRol) {
        RolComunidad rolSolicitante = obtenerRol(solicitanteId, comunidadId)
                .orElseThrow(() -> ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad"));
        if (rolSolicitante != RolComunidad.ADMIN) {
            throw ApiException.forbidden("SOLO_ADMIN", "Solo un administrador puede cambiar roles");
        }
        MembresiaComunidad membresia = membresiaRepository.findByComunidad_IdAndUsuario_Id(comunidadId, usuarioObjetivoId)
                .orElseThrow(() -> ApiException.notFound("MIEMBRO_NO_ENCONTRADO", "Ese usuario no es miembro de la comunidad"));
        membresia.setRol(nuevoRol);
        log.info("Rol cambiado en comunidad {}: usuario={} nuevoRol={} (por {})", comunidadId, usuarioObjetivoId, nuevoRol, solicitanteId);
    }

    @Transactional
    public void expulsar(UUID solicitanteId, UUID comunidadId, UUID usuarioObjetivoId) {
        RolComunidad rolSolicitante = obtenerRol(solicitanteId, comunidadId)
                .orElseThrow(() -> ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad"));
        if (!rolSolicitante.puedeModerar()) {
            throw ApiException.forbidden("SIN_PERMISO_MODERACION", "No tienes permisos de moderacion en esta comunidad");
        }
        MembresiaComunidad membresia = membresiaRepository.findByComunidad_IdAndUsuario_Id(comunidadId, usuarioObjetivoId)
                .orElseThrow(() -> ApiException.notFound("MIEMBRO_NO_ENCONTRADO", "Ese usuario no es miembro de la comunidad"));
        membresia.expulsar();
        log.info("Usuario {} expulsado de la comunidad {} (por {})", usuarioObjetivoId, comunidadId, solicitanteId);
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID comunidadId) {
        Comunidad comunidad = comunidadRepository.findById(comunidadId)
                .orElseThrow(() -> ApiException.notFound("COMUNIDAD_NO_ENCONTRADA", "Esa comunidad no existe"));
        if (!comunidad.getCreador().getId().equals(usuarioId)) {
            throw ApiException.forbidden("SOLO_CREADOR", "Solo quien creo la comunidad puede eliminarla");
        }
        comunidadRepository.delete(comunidad);
        quotaService.liberarCupoComunidad(usuarioId);
        log.info("Comunidad eliminada: id={} nombre='{}' por={}", comunidadId, comunidad.getNombre(), usuarioId);
    }

    @Transactional(readOnly = true)
    public Optional<RolComunidad> obtenerRol(UUID usuarioId, UUID comunidadId) {
        return membresiaRepository.findByComunidad_IdAndUsuario_Id(comunidadId, usuarioId)
                .filter(MembresiaComunidad::isActivo)
                .map(MembresiaComunidad::getRol);
    }

    @Transactional(readOnly = true)
    public boolean esMiembroActivo(UUID usuarioId, UUID comunidadId) {
        return membresiaRepository.existsByComunidad_IdAndUsuario_IdAndActivoTrue(comunidadId, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<ComunidadResponse> misComunidades(UUID usuarioId) {
        return membresiaRepository.findByUsuario_IdAndActivoTrue(usuarioId).stream()
                .map(m -> aRespuesta(m.getComunidad(), m.getRol()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ComunidadResponse ver(UUID comunidadId, UUID usuarioId) {
        Comunidad comunidad = comunidadRepository.findById(comunidadId)
                .orElseThrow(() -> ApiException.notFound("COMUNIDAD_NO_ENCONTRADA", "Esa comunidad no existe"));
        RolComunidad rol = usuarioId == null ? null : obtenerRol(usuarioId, comunidadId).orElse(null);
        return aRespuesta(comunidad, rol);
    }

    private ComunidadResponse aRespuesta(Comunidad comunidad, RolComunidad miRol) {
        long totalMiembros = membresiaRepository.countByComunidad_IdAndActivoTrue(comunidad.getId());
        return new ComunidadResponse(
                comunidad.getId(),
                comunidad.getNombre(),
                comunidad.getDescripcion(),
                comunidad.isPublica(),
                comunidad.getCreador().getNombreArtista(),
                totalMiembros,
                miRol == null ? null : miRol.name()
        );
    }
}
