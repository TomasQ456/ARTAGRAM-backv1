package com.artagram.platform.billing;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UsoCuentaRepository extends JpaRepository<UsoCuenta, UUID> {

    /**
     * SELECT ... FOR UPDATE: la fila de uso del usuario es el punto de
     * serializacion para que 5 solicitudes concurrentes de creacion no
     * revienten la cuota (HU-26).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsoCuenta u where u.usuarioId = :usuarioId")
    Optional<UsoCuenta> buscarParaActualizar(UUID usuarioId);
}
