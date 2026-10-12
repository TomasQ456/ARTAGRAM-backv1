package com.artagram.platform.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeComunidadRepository extends JpaRepository<MensajeComunidad, UUID> {

    List<MensajeComunidad> findByComunidadIdOrderByOrdenAsc(UUID comunidadId);
}
