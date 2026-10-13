package com.artagram.platform.seed;

import com.artagram.platform.auth.RolGlobal;
import com.artagram.platform.auth.Usuario;
import com.artagram.platform.auth.UsuarioRepository;
import com.artagram.platform.canvas.BannerService;
import com.artagram.platform.canvas.Punto;
import com.artagram.platform.canvas.TipoPincel;
import com.artagram.platform.canvas.dto.PintarRegionRequest;
import com.artagram.platform.chat.ChatPersonalService;
import com.artagram.platform.chat.ComunidadChatService;
import com.artagram.platform.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.platform.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.platform.community.ComunidadService;
import com.artagram.platform.community.dto.ComunidadResponse;
import com.artagram.platform.community.dto.CrearComunidadRequest;
import com.artagram.platform.feed.PublicacionService;
import com.artagram.platform.feed.SeguidorService;
import com.artagram.platform.feed.dto.PublicacionDto;
import com.artagram.platform.profile.PerfilService;
import com.artagram.platform.profile.dto.ActualizarPerfilRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Llena la base con usuarios, comunidades, publicaciones, likes, seguidores,
 * chat y regiones de banner pintadas — para que la app no se sienta vacía en
 * una demo. Corre una sola vez al arrancar, solo con el perfil "seed" activo:
 *
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=seed</pre>
 *
 * o, contra la imagen de Docker: <pre>SPRING_PROFILES_ACTIVE=seed</pre>.
 * Reemplaza el antiguo script de Node que hacía lo mismo por HTTP — así todo
 * el backend, seeding incluido, queda en Java y reusa los Service reales en
 * vez de reimplementar sus reglas de negocio contra la API.
 */
@Component
@Profile("seed")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String PASSWORD = "clavesegura123";
    private static final String ADMIN_CORREO = "prueba.artagram@correo.com";
    private final Random random = new Random(7);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PerfilService perfilService;
    private final ComunidadService comunidadService;
    private final PublicacionService publicacionService;
    private final SeguidorService seguidorService;
    private final ChatPersonalService chatPersonalService;
    private final ComunidadChatService comunidadChatService;
    private final BannerService bannerService;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           PerfilService perfilService, ComunidadService comunidadService,
                           PublicacionService publicacionService, SeguidorService seguidorService,
                           ChatPersonalService chatPersonalService, ComunidadChatService comunidadChatService,
                           BannerService bannerService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.perfilService = perfilService;
        this.comunidadService = comunidadService;
        this.publicacionService = publicacionService;
        this.seguidorService = seguidorService;
        this.chatPersonalService = chatPersonalService;
        this.comunidadChatService = comunidadChatService;
        this.bannerService = bannerService;
    }

    private record UsuarioDemo(String correo, String nombreArtista, String bio, String avatar) {
    }

    private record PublicacionDemo(String autor, String titulo, String descripcion, List<String> etiquetas,
                                    String imagen, List<String> proceso) {
    }

    private static final List<UsuarioDemo> USUARIOS = List.of(
            new UsuarioDemo("lucia.mora@correo.com", "lucia_acuarela", "Acuarelista. Pinto jardines y luz de tarde. Bogotá 🌿", "avatar_0.jpg"),
            new UsuarioDemo("sam.rivera@correo.com", "sam_pixel", "Arte digital y pixel art. Ex-diseñador de videojuegos.", "avatar_1.jpg"),
            new UsuarioDemo("noa.delgado@correo.com", "noa_tinta", "Tinta china y retrato. Publico el proceso completo siempre.", "avatar_2.jpg"),
            new UsuarioDemo("ivan.rueda@correo.com", "ivan_urbano", "Muralista. Me interesa el arte colaborativo en espacios públicos.", "avatar_3.jpg"),
            new UsuarioDemo("maria.paz@correo.com", "maria_paz_art", "Ilustradora freelance. Talleres los sábados.", "avatar_4.jpg"),
            new UsuarioDemo("diego.leon@correo.com", "diego_grabado", "Grabado y xilografía. Lento pero seguro.", "avatar_5.jpg")
    );

    private static final List<CrearComunidadRequest> COMUNIDADES = List.of(
            new CrearComunidadRequest("Jardines Colaborativos", "Pintemos jardines y paisajes juntos, a cualquier hora.", true),
            new CrearComunidadRequest("Pixel Studio", "Arte digital, pixel art y práctica diaria.", true),
            new CrearComunidadRequest("Tinta y Papel", "Tinta china, grabado y técnicas tradicionales.", true)
    );

    private static final List<PublicacionDemo> PUBLICACIONES = List.of(
            new PublicacionDemo("lucia_acuarela", "Jardín al amanecer", "Acuarela sobre papel, 30x40cm. Tercera vez que repito esta composición.",
                    List.of("acuarela", "jardines", "paisaje"), "jardin_abstracto.jpg", List.of("jardin_abstracto_paso1.jpg", "jardin_abstracto_paso2.jpg")),
            new PublicacionDemo("sam_pixel", "Atardecer en 8-bit", "Práctica diaria #47. Paleta limitada a 6 colores.",
                    List.of("pixelart", "digital"), "atardecer_coral.jpg", List.of("atardecer_coral_paso1.jpg", "atardecer_coral_paso2.jpg")),
            new PublicacionDemo("noa_tinta", "Estudio en violeta", "Tinta china y una gota de acuarela. Retrato de estudio.",
                    List.of("tinta", "retrato"), "estudio_violeta.jpg", List.of("estudio_violeta_paso1.jpg", "estudio_violeta_paso2.jpg")),
            new PublicacionDemo("ivan_urbano", "Mural cálido", "Boceto para el mural comunitario del barrio.",
                    List.of("mural", "boceto"), "acuarela_calida.jpg", List.of()),
            new PublicacionDemo("maria_paz_art", "Lienzo nocturno", "Ilustración para portada de fanzine.",
                    List.of("ilustracion"), "lienzo_nocturno.jpg", List.of()),
            new PublicacionDemo("diego_grabado", "Brotes de color", "Grabado experimental, primera prueba de color.",
                    List.of("grabado", "experimental"), "brotes_de_color.jpg", List.of())
    );

    private static final List<List<String>> MENSAJES_COMUNIDAD = List.of(
            List.of("¡Bienvenidos! Cuelguen lo que estén pintando esta semana.", "¿Alguien se une a dibujar hoy en el lienzo?", "Recién publiqué el proceso completo de mi última pieza 🎨"),
            List.of("Practicando paleta limitada, cualquier feedback sirve", "¿Qué tablet usan para pixel art?", "Buen ritmo esta semana, sigamos así"),
            List.of("Nueva plancha de grabado lista para probar", "La tinta china perdona poco jaja", "¿Quedamos el sábado para el taller?")
    );

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByCorreoIgnoreCase(USUARIOS.get(0).correo())) {
            usuarioRepository.findByNombreArtistaIgnoreCase("sam_pixel").ifPresent(u -> {
                if (!u.isPremium()) {
                    u.setPremium(true);
                    usuarioRepository.save(u);
                    log.info("Usuario sam_pixel actualizado a premium");
                }
            });
            log.info("Datos de ejemplo ya presentes, no se vuelve a sembrar (perfil 'seed')");
            return;
        }
        log.info("Sembrando datos de ejemplo (perfil 'seed')...");

        Map<String, Usuario> usuarios = crearUsuarios();
        Usuario admin = promoverAdmin();
        usuarios.put(admin.getNombreArtista(), admin);

        actualizarPerfiles(usuarios);
        Map<String, UUID> comunidadIds = crearComunidades(usuarios);
        unirATodos(usuarios, comunidadIds);
        List<UUID> publicacionIds = crearPublicaciones(usuarios);
        repartirLikes(usuarios, publicacionIds);
        crearSeguidores(usuarios);
        enviarChatsDeComunidad(usuarios, comunidadIds);
        enviarMensajesPersonales(usuarios);
        pintarBanners(usuarios, comunidadIds);

        log.info("Datos de ejemplo listos: {} usuarios, {} comunidades, {} publicaciones",
                usuarios.size(), comunidadIds.size(), publicacionIds.size());
    }

    private Map<String, Usuario> crearUsuarios() {
        Map<String, Usuario> creados = new LinkedHashMap<>();
        for (UsuarioDemo u : USUARIOS) {
            Usuario usuario = new Usuario(u.correo(), u.nombreArtista(), passwordEncoder.encode(PASSWORD));
            usuario.marcarVerificado();
            if ("sam_pixel".equalsIgnoreCase(u.nombreArtista())) {
                usuario.setPremium(true);
            }
            usuario = usuarioRepository.save(usuario);
            creados.put(u.nombreArtista(), usuario);
        }
        log.info("{} usuarios de ejemplo creados y verificados", creados.size());
        return creados;
    }

    /** La cuenta que ya conoce quien esté probando la app pasa a ADMIN, para que pueda entrar directo a /admin. */
    private Usuario promoverAdmin() {
        return usuarioRepository.findByCorreoIgnoreCase(ADMIN_CORREO)
                .map(u -> {
                    u.setRolGlobal(RolGlobal.ADMIN);
                    log.info("Cuenta {} promovida a ADMIN", u.getNombreArtista());
                    return u;
                })
                .orElseGet(() -> {
                    Usuario u = new Usuario(ADMIN_CORREO, "pincel_artagram", passwordEncoder.encode(PASSWORD));
                    u.marcarVerificado();
                    u.setRolGlobal(RolGlobal.ADMIN);
                    u = usuarioRepository.save(u);
                    log.info("Cuenta admin {} creada", u.getNombreArtista());
                    return u;
                });
    }

    private void actualizarPerfiles(Map<String, Usuario> usuarios) {
        for (UsuarioDemo u : USUARIOS) {
            perfilService.actualizar(usuarios.get(u.nombreArtista()).getId(),
                    new ActualizarPerfilRequest(u.bio(), List.of(), List.of()));
            perfilService.subirFoto(usuarios.get(u.nombreArtista()).getId(), imagenClasspath(u.avatar()));
        }
        log.info("Perfiles actualizados con bio y foto");
    }

    private Map<String, UUID> crearComunidades(Map<String, Usuario> usuarios) {
        List<String> creadores = List.of("lucia_acuarela", "sam_pixel", "noa_tinta");
        Map<String, UUID> ids = new LinkedHashMap<>();
        for (int i = 0; i < COMUNIDADES.size(); i++) {
            ComunidadResponse respuesta = comunidadService.crear(usuarios.get(creadores.get(i)).getId(), COMUNIDADES.get(i));
            ids.put(COMUNIDADES.get(i).nombre(), respuesta.id());
        }
        log.info("Comunidades creadas: {}", ids.keySet());
        return ids;
    }

    private void unirATodos(Map<String, Usuario> usuarios, Map<String, UUID> comunidadIds) {
        List<String> creadores = List.of("lucia_acuarela", "sam_pixel", "noa_tinta");
        int i = 0;
        for (UUID comunidadId : comunidadIds.values()) {
            String creador = creadores.get(i++);
            for (Usuario u : usuarios.values()) {
                if (!u.getNombreArtista().equals(creador)) {
                    comunidadService.unirse(u.getId(), comunidadId);
                }
            }
        }
        log.info("Todos los usuarios (incluido el admin) se unieron a las {} comunidades", comunidadIds.size());
    }

    private List<UUID> crearPublicaciones(Map<String, Usuario> usuarios) {
        List<UUID> ids = new ArrayList<>();
        for (PublicacionDemo p : PUBLICACIONES) {
            List<MultipartFile> proceso = p.proceso().stream().map(this::imagenClasspath).toList();
            PublicacionDto dto = publicacionService.publicar(usuarios.get(p.autor()).getId(), p.titulo(), p.descripcion(),
                    new java.util.LinkedHashSet<>(p.etiquetas()), imagenClasspath(p.imagen()), proceso);
            ids.add(dto.id());
        }
        log.info("{} publicaciones creadas", ids.size());
        return ids;
    }

    private void repartirLikes(Map<String, Usuario> usuarios, List<UUID> publicacionIds) {
        List<Usuario> todos = new ArrayList<>(usuarios.values());
        for (UUID pubId : publicacionIds) {
            int cuantos = 2 + random.nextInt(Math.max(1, todos.size() - 2));
            List<Usuario> mezclados = new ArrayList<>(todos);
            java.util.Collections.shuffle(mezclados, random);
            for (Usuario u : mezclados.subList(0, Math.min(cuantos, mezclados.size()))) {
                publicacionService.alternarLike(u.getId(), pubId);
            }
        }
        log.info("Likes repartidos entre las publicaciones");
    }

    private void crearSeguidores(Map<String, Usuario> usuarios) {
        List<Usuario> todos = new ArrayList<>(usuarios.values());
        for (Usuario u : todos) {
            List<Usuario> otros = new ArrayList<>(todos);
            otros.remove(u);
            java.util.Collections.shuffle(otros, random);
            int cuantos = Math.min(2 + random.nextInt(2), otros.size());
            for (Usuario destino : otros.subList(0, cuantos)) {
                try {
                    seguidorService.seguir(u.getId(), destino.getId());
                } catch (RuntimeException ignorado) {
                    // ya se seguían por otra combinación al azar; no rompe el seeding
                }
            }
        }
        log.info("Relaciones de seguidor creadas");
    }

    private void enviarChatsDeComunidad(Map<String, Usuario> usuarios, Map<String, UUID> comunidadIds) {
        List<Usuario> todos = new ArrayList<>(usuarios.values());
        int i = 0;
        for (UUID comunidadId : comunidadIds.values()) {
            for (String texto : MENSAJES_COMUNIDAD.get(i++)) {
                Usuario autor = todos.get(random.nextInt(todos.size()));
                comunidadChatService.enviar(comunidadId, autor.getId(), new EnviarMensajeComunidadRequest(texto));
            }
        }
        log.info("Chat de comunidad poblado");
    }

    private void enviarMensajesPersonales(Map<String, Usuario> usuarios) {
        List<String[]> pares = List.of(
                new String[] {"lucia_acuarela", "noa_tinta"},
                new String[] {"sam_pixel", "diego_grabado"}
        );
        for (String[] par : pares) {
            Usuario a = usuarios.get(par[0]);
            Usuario b = usuarios.get(par[1]);
            if (a == null || b == null) continue;
            chatPersonalService.enviar(a.getId(), a.getNombreArtista(),
                    new EnviarMensajePersonalRequest(b.getId(), "¡Hola! Vi tu último trabajo, me encantó la paleta que usaste."));
            chatPersonalService.enviar(b.getId(), b.getNombreArtista(),
                    new EnviarMensajePersonalRequest(a.getId(), "¡Gracias! Fue un experimento, contento con el resultado."));
        }
        log.info("Mensajes personales enviados");
    }

    private void pintarBanners(Map<String, Usuario> usuarios, Map<String, UUID> comunidadIds) {
        TipoPincel[] pinceles = TipoPincel.values();
        String[] colores = {"#FF6B57", "#2FD9C4", "#FFC24B", "#8B5CF6", "#F5F1EA"};
        List<Usuario> todos = new ArrayList<>(usuarios.values());
        for (UUID comunidadId : comunidadIds.values()) {
            for (int region = 0; region < 14; region++) {
                Usuario autor = todos.get(random.nextInt(todos.size()));
                List<Punto> puntos = new ArrayList<>();
                int cantidad = 3 + random.nextInt(4);
                for (int p = 0; p < cantidad; p++) {
                    puntos.add(new Punto(random.nextDouble(), random.nextDouble()));
                }
                PintarRegionRequest request = new PintarRegionRequest(
                        random.nextInt(100), pinceles[random.nextInt(pinceles.length)],
                        colores[random.nextInt(colores.length)], 8 + random.nextDouble() * 20,
                        0.5 + random.nextDouble() * 0.5, puntos);
                bannerService.pintar(comunidadId, autor.getId(), autor.getNombreArtista(), request);
            }
        }
        log.info("Banners con regiones pintadas");
    }

    private MultipartFile imagenClasspath(String nombre) {
        try (InputStream in = new ClassPathResource("seed-images/" + nombre).getInputStream()) {
            return new ByteArrayMultipartFile(nombre, in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen de ejemplo " + nombre, e);
        }
    }
}
