package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.*;
import com.jsebastian.eden.EdenSys.domain.*;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.mappers.UserMapper;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import com.jsebastian.eden.EdenSys.services.interfaces.UserService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsebastian.eden.EdenSys.domain.Asesoria;
import com.jsebastian.eden.EdenSys.domain.EstadoAsesoria;
import com.jsebastian.eden.EdenSys.domain.MotivoNotificacion;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MINUTOS_VIGENCIA_CODIGO = 10;
    private static final String CORREO_CONTACTO = "inmobiliariaedenco@gmail.com"; // confirmar correo oficial
    private static final String PATRON_CONTRASENA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    private final CorreoService correoService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AsesoriaRepository asesoriaRepository;
    private final NotificacionService notificacionService;
    private final ProgramaService programaService;



    private String fromEmail;

    // ---------- Registro y activación ----------

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioDto dto) {
        String email = normalizar(dto.email());

        if (!esContrasenaSegura(dto.contrasena())) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.");
        }

        Optional<User> existente = userRepository.findByEmail(email);

        if (existente.isPresent()) {
            User u = existente.get();

            if (u.getEstado() == EstadoUsuario.ACTIVO) {
                throw new ValueConflictException("Ya existe un usuario activo con este email.");
            }
            if (u.getEstado() == EstadoUsuario.INACTIVO) {
                throw new ValueConflictException("Esta cuenta está desactivada. Inicia sesión para reactivarla.");
            }

            // PENDIENTE: actualiza datos, cambia contraseña y reenvía código
            if (!u.getDocumentoIdentidad().equals(dto.documentoIdentidad())
                    && userRepository.existsByDocumentoIdentidad(dto.documentoIdentidad())) {
                throw new ValueConflictException("Ya existe un usuario con esta cédula.");
            }
            aplicarDatos(u, dto);
            u.setContrasena(passwordEncoder.encode(dto.contrasena()));
            generarYEnviarCodigoActivacion(u);
            userRepository.save(u);
            log.info("Usuario pendiente actualizado y código reenviado: {}", email);
            return userMapper.toUsuarioResponse(u);
        }

        if (userRepository.existsByDocumentoIdentidad(dto.documentoIdentidad())) {
            throw new ValueConflictException("Ya existe un usuario con esta cédula.");
        }

        User nuevo = new User();
        aplicarDatos(nuevo, dto);
        nuevo.setProgramaAcademico(programaService.buscarObligatorio(dto.programaId()));
        nuevo.setEmail(email);
        nuevo.setContrasena(passwordEncoder.encode(dto.contrasena()));
        nuevo.setRol(Rol.ESTUDIANTE);
        nuevo.setEstado(EstadoUsuario.PENDIENTE);
        generarYEnviarCodigoActivacion(nuevo);
        userRepository.save(nuevo);
        log.info("Nuevo usuario creado: {}", email);
        return userMapper.toUsuarioResponse(nuevo);
    }

    @Override
    @Transactional
    public boolean activarUsuario(String codigo) {
        Optional<User> opt = userRepository.findByCodigoActivacion(codigo);
        if (opt.isEmpty()) {
            log.warn("Código de activación inválido: {}", codigo);
            return false;
        }

        User u = opt.get();
        if (u.getEstado() != EstadoUsuario.PENDIENTE) {
            log.warn("La cuenta {} ya está activa o inactiva", u.getEmail());
            return false;
        }

        if (codigoVencido(u)) {
            u.setCodigoActivacion(null);
            u.setFechaCreacionCodigo(null);
            userRepository.save(u);
            return false;
        }

        u.setEstado(EstadoUsuario.ACTIVO);
        u.setCodigoActivacion(null);
        u.setFechaCreacionCodigo(null);
        userRepository.save(u);
        log.info("Usuario activado: {}", u.getEmail());
        return true;
    }

    // ---------- Login ----------

    @Override
    public String validarCredencialesYGenerarToken(String email, String contrasena) {
        User u = userRepository.findByEmail(normalizar(email))
                .orElseThrow(() -> new ValueConflictException("Usuario no encontrado con el email proporcionado."));

        if (!passwordEncoder.matches(contrasena, u.getContrasena())) {
            throw new ValueConflictException("Contraseña incorrecta.");
        }
        if (u.getEstado() == EstadoUsuario.PENDIENTE) {
            throw new ValueConflictException("Debes activar tu cuenta antes de iniciar sesión.");
        }
        return generarToken(u);
    }

    @Override
    public String generarToken(User usuario) {
        return jwtService.generateToken(usuario);
    }

    // ---------- Recuperación de contraseña ----------

    @Override
    @Transactional
    public void enviarCodigoRecuperacionContrasena(SolicitarRecuperacionDto dto) {
        User u = userRepository.findByEmail(normalizar(dto.email()))
                .orElseThrow(() -> new ValueConflictException(
                        "El correo proporcionado no se encuentra registrado en nuestro sistema."));

        if (u.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ValueConflictException("Esta cuenta está desactivada. Reactívala para recuperar tu contraseña.");
        }

        String codigo = generarCodigo();
        u.setCodigoActivacion(codigo);
        u.setFechaCreacionCodigo(LocalDateTime.now());
        correoService.enviarCorreo(u.getEmail(), "Recuperación de contraseña",
                "Hola " + u.getNombre() + ", tu código es: " + codigo);
        userRepository.save(u);
    }

    @Override
    @Transactional
    public boolean cambiarContrasenaConCodigo(CambiarContrasenaDto dto) {
        Optional<User> opt = userRepository.findByEmail(normalizar(dto.email()));
        if (opt.isEmpty()) return false;

        User u = opt.get();
        if (u.getCodigoActivacion() == null || !u.getCodigoActivacion().equals(dto.codigo())) {
            return false;
        }
        if (codigoVencido(u)) {
            u.setCodigoActivacion(null);
            u.setFechaCreacionCodigo(null);
            userRepository.save(u);
            return false;
        }
        if (!esContrasenaSegura(dto.nuevaContrasena())) {
            throw new IllegalArgumentException(
                    "La contraseña no cumple los requisitos: mínimo 8 caracteres, una minúscula, una mayúscula, un número y un símbolo.");
        }

        u.setContrasena(passwordEncoder.encode(dto.nuevaContrasena()));
        u.setCodigoActivacion(null);
        u.setFechaCreacionCodigo(null);
        userRepository.save(u);
        return true;
    }

    // ---------- Perfil y administración ----------

    @Override
    @Transactional
    public String actualizarUsuarioEmail(String email, CrearUsuarioDto dto) {
        User u = userRepository.findByEmail(normalizar(email))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el email: " + email));

        // La cédula no se modifica desde el perfil (así lo indica el frontend)
        if (dto.nombre() != null) u.setNombre(dto.nombre());
        if (dto.apellido() != null) u.setApellido(dto.apellido());
        if (dto.telefono() != null) u.setTelefono(dto.telefono());

        userRepository.save(u);
        return generarToken(u);
    }

    @Override
    @Transactional
    public void desvincularUsuario(String email) {
        User u = userRepository.findByEmail(normalizar(email))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el email: " + email));

        if (u.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ValueConflictException("El usuario ya se encuentra desvinculado.");
        }

        for (Asesoria a : asesoriaRepository.findAbiertasDeUsuario(u)) {
            boolean esTutor = a.getAsesor().getId().equals(u.getId());
            User contraparte = esTutor ? a.getParticipante() : a.getAsesor();

            // Decisión antes de cambiar el estado, para que el texto sea el correcto
            boolean estabaAceptada = a.getEstado() == EstadoAsesoria.ACEPTADA;
            String estadoTexto = estabaAceptada ? "cancelada" : "rechazada";

            if (estabaAceptada) {
                a.setEstado(EstadoAsesoria.CANCELADA);
                a.setCanceladaPor(u);
            } else {
                a.setEstado(EstadoAsesoria.RECHAZADA);
                a.setRechazadaPor(u);
            }
            a.setActualizadaEn(LocalDateTime.now());
            asesoriaRepository.save(a);

            notificacionService.crear(contraparte, a, MotivoNotificacion.USUARIO_DESACTIVADO,
                    u.getNombre() + " " + u.getApellido() + " se desactivó, por lo que la asesoría "
                            + a.getId() + " quedó " + estadoTexto + ".");
        }

        u.setEstado(EstadoUsuario.INACTIVO);
        userRepository.save(u);
    }

    @Override
    public Optional<User> buscarPorId(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(normalizar(email));
    }

    @Override
    public Optional<User> buscarPorCedula(String cedula) {
        return userRepository.findByDocumentoIdentidad(cedula);
    }

    @Override
    public List<User> obtenerTodosLosUsuarios() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
        userRepository.delete(u);
    }

    @Override
    @Transactional
    public void eliminarUsuarioPorEmail(String email) {
        userRepository.findByEmail(normalizar(email)).ifPresent(userRepository::delete);
    }

    @Override
    public boolean existePorEmail(String email) {
        return userRepository.existsByEmail(normalizar(email));
    }

    @Override
    public boolean existePorCedula(String cedula) {
        return userRepository.existsByDocumentoIdentidad(cedula);
    }

    @Override
    public void procesarContacto(ContactoDto contacto) {
        String mensajeFinal = "Nuevo mensaje de contacto\n\n"
                + "Nombre: " + contacto.nombre() + "\n"
                + "Teléfono: " + contacto.telefono() + "\n"
                + "Correo: " + contacto.correo() + "\n\n"
                + "Mensaje:\n" + contacto.mensaje();

        correoService.enviarCorreo(CORREO_CONTACTO, "Nuevo contacto - " + contacto.asunto(), mensajeFinal);
        correoService.enviarCorreo(contacto.correo(), "Recibimos su correo", "Pronto te responderemos tus inquietudes.");
    }

    // ---------- Helpers ----------

    private void aplicarDatos(User u, CrearUsuarioDto dto) {
        u.setNombre(dto.nombre());
        u.setApellido(dto.apellido());
        u.setDocumentoIdentidad(dto.documentoIdentidad());
        u.setTelefono(dto.telefono());
    }

    private void generarYEnviarCodigoActivacion(User u) {
        String codigo = generarCodigo();
        u.setCodigoActivacion(codigo);
        u.setFechaCreacionCodigo(LocalDateTime.now());
        correoService.enviarCorreo(u.getEmail(), "Código de activación de tu cuenta",
                "Hola, " + u.getNombre() + ".\n\nTu código de activación es: " + codigo);
    }

    private String generarCodigo() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private boolean codigoVencido(User u) {
        return u.getFechaCreacionCodigo() == null
                || u.getFechaCreacionCodigo().isBefore(LocalDateTime.now().minusMinutes(MINUTOS_VIGENCIA_CODIGO));
    }

    private boolean esContrasenaSegura(String contrasena) {
        return contrasena != null && contrasena.matches(PATRON_CONTRASENA);
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

}