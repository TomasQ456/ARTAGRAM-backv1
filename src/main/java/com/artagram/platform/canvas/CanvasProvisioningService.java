package com.artagram.platform.canvas;

import com.artagram.platform.community.ComunidadCreadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al crear una comunidad se aprovisionan de una vez su lienzo colaborativo y
 * su banner en blanco (HU-10, nota tecnica).
 */
@Service
public class CanvasProvisioningService {

    private final LienzoRepository lienzoRepository;
    private final BannerComunitarioRepository bannerRepository;
    private final BannerRegionEstadoRepository bannerRegionEstadoRepository;
    private final CanvasProperties properties;

    public CanvasProvisioningService(LienzoRepository lienzoRepository,
                                      BannerComunitarioRepository bannerRepository,
                                      BannerRegionEstadoRepository bannerRegionEstadoRepository,
                                      CanvasProperties properties) {
        this.lienzoRepository = lienzoRepository;
        this.bannerRepository = bannerRepository;
        this.bannerRegionEstadoRepository = bannerRegionEstadoRepository;
        this.properties = properties;
    }

    @EventListener
    @Transactional
    public void alCrearComunidad(ComunidadCreadaEvent evento) {
        lienzoRepository.save(new Lienzo(evento.comunidadId()));
        BannerComunitario banner = bannerRepository.save(
                new BannerComunitario(evento.comunidadId(), properties.getBannerSize(), properties.getBannerRegionSize()));

        int regionesPorLado = properties.regionsPerSide();
        int totalRegiones = regionesPorLado * regionesPorLado;
        for (int i = 0; i < totalRegiones; i++) {
            bannerRegionEstadoRepository.save(new BannerRegionEstado(banner.getId(), i));
        }
    }
}
