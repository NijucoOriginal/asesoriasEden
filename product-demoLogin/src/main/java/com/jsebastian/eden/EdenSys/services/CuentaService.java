package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.PerfilResponse;
import com.jsebastian.eden.EdenSys.domain.EstadoUsuario;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import com.jsebastian.eden.EdenSys.services.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MINUTOS_VIGENCIA = 10;

    private final UserRepository userRepository;
    private final CorreoService correoService;
    private final JwtService jwtService;
    private final CloudinaryService cloudinaryService;

    // ---------- Perfil ----------

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(String email) {
        return PerfilResponse.from(buscar(email));
    }

    // ---------- Reenvío de código de activación (solo PENDIENTE) ----------

    @Transactional
    public void reenviarCodigoActivacion(String email) {
        userRepository.findByEmail(normalizar(email)).ifPresent(u -> {
            if (u.getEstado() != EstadoUsuario.PENDIENTE) {
                return;
            }
            asignarCodigo(u);
            correoService.enviarCorreo(u.getEmail(), "Nuevo código de activación",
                    "Hola, " + u.getNombre() + ".\n\nTu nuevo código de activación es: "
                            + u.getCodigoActivacion()
                            + "\n\nVence en " + MINUTOS_VIGENCIA + " minutos.");
            userRepository.save(u);
        });
    }

    // ---------- Reactivación (solo INACTIVO) ----------

    @Transactional
    public void solicitarReactivacion(String email) {
        User u = buscar(email);
        if (u.getEstado() != EstadoUsuario.INACTIVO) {
            throw new ValueConflictException("Tu cuenta ya está activa.");
        }
        asignarCodigo(u);
        correoService.enviarCorreo(u.getEmail(), "Código para reactivar tu cuenta",
                "Hola, " + u.getNombre() + ".\n\nTu código para reactivar tu cuenta es: "
                        + u.getCodigoActivacion()
                        + "\n\nVence en " + MINUTOS_VIGENCIA + " minutos.");
        userRepository.save(u);
    }

    /**
     * Valida el código y devuelve un token nuevo, para que el claim "estado"
     * ya llegue como ACTIVO sin tener que volver a iniciar sesión.
     */
    @Transactional
    public String confirmarReactivacion(String email, String codigo) {
        User u = buscar(email);
        if (u.getEstado() != EstadoUsuario.INACTIVO) {
            throw new ValueConflictException("Tu cuenta ya está activa.");
        }
        if (u.getCodigoActivacion() == null || !u.getCodigoActivacion().equals(codigo)) {
            throw new ValueConflictException("El código es inválido.");
        }
        if (codigoVencido(u)) {
            throw new ValueConflictException("El código expiró. Solicita uno nuevo.");
        }

        u.setEstado(EstadoUsuario.ACTIVO);
        limpiarCodigo(u);
        userRepository.save(u);
        return jwtService.generateToken(u);
    }

    // ---------- Imagen de perfil ----------

    @Transactional
    public String actualizarImagenPerfil(String email, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar una imagen.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen.");
        }

        User u = buscar(email);
        String url = cloudinaryService.subirImagen(archivo);
        u.setImagenPerfil(url);
        userRepository.save(u);
        return url;
    }

    // ---------- Helpers ----------

    private User buscar(String email) {
        return userRepository.findByEmail(normalizar(email))
                .orElseThrow(() -> new ValueConflictException("Usuario no encontrado."));
    }

    private void asignarCodigo(User u) {
        u.setCodigoActivacion(String.format("%06d", RANDOM.nextInt(1_000_000)));
        u.setFechaCreacionCodigo(LocalDateTime.now());
    }

    private void limpiarCodigo(User u) {
        u.setCodigoActivacion(null);
        u.setFechaCreacionCodigo(null);
    }

    private boolean codigoVencido(User u) {
        return u.getFechaCreacionCodigo() == null
                || u.getFechaCreacionCodigo().isBefore(LocalDateTime.now().minusMinutes(MINUTOS_VIGENCIA));
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}