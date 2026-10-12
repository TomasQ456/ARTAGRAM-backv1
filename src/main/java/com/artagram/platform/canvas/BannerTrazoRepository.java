package com.artagram.platform.canvas;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerTrazoRepository extends JpaRepository<BannerTrazo, UUID> {

    List<BannerTrazo> findByBannerIdAndRegionIndexAndSecuenciaGreaterThanOrderBySecuenciaAsc(
            UUID bannerId, int regionIndex, long secuencia);

    long countByBannerIdAndRegionIndexAndSecuenciaGreaterThan(UUID bannerId, int regionIndex, long secuencia);
}
