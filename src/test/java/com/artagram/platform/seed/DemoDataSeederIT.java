package com.artagram.platform.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.artagram.platform.auth.RolGlobal;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.community.ComunidadRepository;
import com.artagram.platform.feed.PublicacionRepository;
import com.artagram.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * El perfil "seed" arranca {@link DemoDataSeeder} automaticamente como
 * CommandLineRunner al levantar el contexto (por eso esta clase no lo invoca
 * a mano en el primer test) — asi se prueba el mismo camino que corre en
 * Docker con SPRING_PROFILES_ACTIVE=seed, no una llamada sintetica aparte.
 */
@ActiveProfiles("seed")
class DemoDataSeederIT extends IntegrationTestBase {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ComunidadRepository comunidadRepository;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Test
    void sembroLosDatosDeEjemploAlArrancarElContexto() {
        assertThat(usuarioRepository.existsByCorreoIgnoreCase("lucia.mora@correo.com")).isTrue();
        assertThat(usuarioRepository.existsByCorreoIgnoreCase("prueba.artagram@correo.com")).isTrue();

        Usuario admin = usuarioRepository.findByCorreoIgnoreCase("prueba.artagram@correo.com").orElseThrow();
        assertThat(admin.getRolGlobal()).isEqualTo(RolGlobal.ADMIN);
        assertThat(admin.isVerificado()).isTrue();

        assertThat(comunidadRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(publicacionRepository.count()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void correrloDeNuevoNoDuplicaNada() {
        long usuariosAntes = usuarioRepository.count();
        long comunidadesAntes = comunidadRepository.count();
        long publicacionesAntes = publicacionRepository.count();

        seeder.run();

        assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
        assertThat(comunidadRepository.count()).isEqualTo(comunidadesAntes);
        assertThat(publicacionRepository.count()).isEqualTo(publicacionesAntes);
    }
}
