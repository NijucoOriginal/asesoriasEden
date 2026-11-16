package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.asesoria.CrearAsesoriaRequest;
import com.jsebastian.eden.EdenSys.Dtos.asesoria.ProponerAsesoriaRequest;
import com.jsebastian.eden.EdenSys.domain.*;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import com.jsebastian.eden.EdenSys.repository.MateriaRepository;
import com.jsebastian.eden.EdenSys.repository.NotificacionRepository;
import com.jsebastian.eden.EdenSys.repository.TemaRepository;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AsesoriaService {

    private static final int HORAS_LIMITE_CANCELACION = 12;

    private static final Set<EstadoAsesoria> ESTADOS_CERRADOS =
            EnumSet.of(EstadoAsesoria.RECHAZADA, EstadoAsesoria.CANCELADA,
                    EstadoAsesoria.EXPIRADA, EstadoAsesoria.FINALIZADA);

    private final AsesoriaRepository asesoriaRepository;
    private final UserRepository userRepository;
    private final MateriaRepository materiaRepository;
    private final TemaRepository temaRepository;
    private final NotificacionService notificacionService;

    // ---------- Crear (estudiante) ----------

    @Transactional
    public Asesoria crear(String emailEstudiante, CrearAsesoriaRequest req) {
        User participante = usuarioActivo(emailEstudiante);
        User asesor = usuarioActivo(req.asesorId());

        if (participante.getId().equals(asesor.getId())) {
            throw new ValueConflictException("No puedes solicitarte una asesoría a ti mismo.");
        }

        Materia materia = materiaRepository.findById(req.materiaId())
                .orElseThrow(() -> new ValueConflictException("Materia no encontrada."));

        if (asesor.getMateriasQueDicta().stream().noneMatch(m -> m.getId().equals(materia.getId()))) {
            throw new ValueConflictException("El tutor no dicta esa materia.");
        }

        List<Tema> temas = req.temasIds() == null ? new ArrayList<>() : temaRepository.findAllById(req.temasIds());
        for (Tema t : temas) {
            if (t.getMateriasPertenecientes().stream().noneMatch(m -> m.getId().equals(materia.getId()))) {
                throw new ValueConflictException("El tema '" + t.getNombreTema() + "' no pertenece a la materia.");
            }
        }

        validarFechas(req.fechaInicioPropuesta(), req.fechaCierrePropuesta());
        validarPrecio(req.precio());

        Asesoria a = new Asesoria();
        a.setAsesor(asesor);
        a.setParticipante(participante);
        a.setMateria(materia);
        a.setTemas(temas);
        a.setFechaInicioPropuesta(req.fechaInicioPropuesta());
        a.setFechaCierrePropuesta(req.fechaCierrePropuesta());
        a.setPrecio(req.precio());
        a.setEstado(EstadoAsesoria.PENDIENTE);
        a = asesoriaRepository.save(a);

        notificar(a, MotivoNotificacion.ASESORIA_CREADA, participante,
                "Enviaste una solicitud de asesoría a " + asesor.getNombre() + ".");
        notificar(a, MotivoNotificacion.ASESORIA_CREADA, asesor,
                participante.getNombre() + " te envió una solicitud de asesoría.");
        return a;
    }

    @Transactional
    public Asesoria proponer(String email, Long asesoriaId, ProponerAsesoriaRequest req) {
        Asesoria a = buscarAbierta(asesoriaId);
        User yo = usuarioActivo(email);
        validarFechas(req.fechaInicioPropuesta(), req.fechaCierrePropuesta());
        validarPrecio(req.precio());

        boolean esTutor = yo.getId().equals(a.getAsesor().getId());
        boolean esEstudiante = yo.getId().equals(a.getParticipante().getId());
        if (!esTutor && !esEstudiante) {
            throw new SecurityException("No participas en esta asesoría.");
        }

        BigDecimal precioAnterior = a.getPrecio();
        boolean precioCambio = req.precio().compareTo(precioAnterior) != 0;

        if (esTutor) {
            // El tutor solo contrapropone desde PENDIENTE
            if (a.getEstado() != EstadoAsesoria.PENDIENTE) {
                throw new ValueConflictException("Solo puedes contraproponer cuando la solicitud está pendiente.");
            }
            a.setEstado(EstadoAsesoria.CONTRAPROPUESTA);
            a.setMotivoContrapropuesta(req.motivo() != null ? req.motivo()
                    : (precioCambio ? MotivoContrapropuesta.PRECIO : MotivoContrapropuesta.FECHA));
        } else {
            // El estudiante reenvía solo desde CONTRAPROPUESTA
            if (a.getEstado() != EstadoAsesoria.CONTRAPROPUESTA) {
                throw new ValueConflictException("Solo puedes reenviar tu propuesta cuando recibiste una contrapropuesta.");
            }
            a.setEstado(EstadoAsesoria.PENDIENTE);
            a.setMotivoContrapropuesta(req.motivo() != null ? req.motivo()
                    : (precioCambio ? MotivoContrapropuesta.PRECIO : null));
        }

        a.setFechaInicioPropuesta(req.fechaInicioPropuesta());
        a.setFechaCierrePropuesta(req.fechaCierrePropuesta());
        a.setPrecio(req.precio());
        a.setActualizadaEn(LocalDateTime.now());
        a = asesoriaRepository.save(a);

        String detalle = a.getMotivoContrapropuesta() == MotivoContrapropuesta.PRECIO ? "por precio" : "por horario";
        MotivoNotificacion motivo = a.getEstado() == EstadoAsesoria.CONTRAPROPUESTA
                ? MotivoNotificacion.ASESORIA_CONTRAPROPUESTA
                : MotivoNotificacion.ASESORIA_CREADA;

        notificar(a, motivo, otra(a, yo), yo.getNombre() + " envió una nueva propuesta (" + detalle + ").");
        notificar(a, motivo, yo, "Enviaste una nueva propuesta (" + detalle + ").");
        return a;
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new ValueConflictException("El precio debe ser mayor que cero.");
        }
    }

    // ---------- Aceptar ----------

    @Transactional
    public Asesoria aceptar(String email, Long asesoriaId) {
        Asesoria a = buscarAbierta(asesoriaId);
        User yo = usuarioActivo(email);

        // PENDIENTE: lo acepta el tutor. CONTRAPROPUESTA: lo acepta el estudiante.
        User quienDebeResponder = a.getEstado() == EstadoAsesoria.PENDIENTE
                ? a.getAsesor() : a.getParticipante();
        if (a.getEstado() != EstadoAsesoria.PENDIENTE && a.getEstado() != EstadoAsesoria.CONTRAPROPUESTA) {
            throw new ValueConflictException("Esta asesoría no está en un estado aceptable.");
        }
        if (!yo.getId().equals(quienDebeResponder.getId())) {
            throw new SecurityException("Solo quien recibió la última propuesta puede aceptarla.");
        }

        a.setEstado(EstadoAsesoria.ACEPTADA);
        a.setFechaInicioConfirmada(a.getFechaInicioPropuesta());
        a.setFechaCierreConfirmada(a.getFechaCierrePropuesta());
        a.setAceptadaEn(LocalDateTime.now());
        a.setActualizadaEn(LocalDateTime.now());
        a.setMotivoContrapropuesta(null);
        a = asesoriaRepository.save(a);

        notificar(a, MotivoNotificacion.ASESORIA_ACEPTADA, otra(a, yo), yo.getNombre() + " aceptó la asesoría.");
        notificar(a, MotivoNotificacion.ASESORIA_ACEPTADA, yo, "Aceptaste la asesoría.");
        return a;
    }

    // ---------- Rechazar ----------

    @Transactional
    public Asesoria rechazar(String email, Long asesoriaId) {
        Asesoria a = buscarAbierta(asesoriaId);
        User yo = usuarioActivo(email);

        User quienDebeResponder = a.getEstado() == EstadoAsesoria.PENDIENTE
                ? a.getAsesor() : a.getParticipante();
        if (!yo.getId().equals(quienDebeResponder.getId())) {
            throw new SecurityException("Solo quien recibió la última propuesta puede rechazarla.");
        }

        a.setEstado(EstadoAsesoria.RECHAZADA);
        a.setRechazadaPor(yo);
        a.setActualizadaEn(LocalDateTime.now());
        a = asesoriaRepository.save(a);

        notificar(a, MotivoNotificacion.ASESORIA_RECHAZADA, otra(a, yo), yo.getNombre() + " rechazó la asesoría.");
        notificar(a, MotivoNotificacion.ASESORIA_RECHAZADA, yo, "Rechazaste la asesoría.");
        return a;
    }

    // ---------- Cancelar ----------

    @Transactional
    public Asesoria cancelar(String email, Long asesoriaId) {
        Asesoria a = buscarAbierta(asesoriaId);
        User yo = usuarioActivo(email);

        boolean esParticipante = yo.getId().equals(a.getParticipante().getId());
        boolean esTutor = yo.getId().equals(a.getAsesor().getId());
        if (!esParticipante && !esTutor) {
            throw new SecurityException("No participas en esta asesoría.");
        }

        if (a.getEstado() == EstadoAsesoria.PENDIENTE) {
            // En PENDIENTE solo cancela el estudiante
            if (!esParticipante) {
                throw new SecurityException("En estado pendiente solo el estudiante puede cancelar.");
            }
        } else if (a.getEstado() == EstadoAsesoria.ACEPTADA) {
            validarVentanaCancelacion(a);
        } else {
            throw new ValueConflictException("Esta asesoría no se puede cancelar en su estado actual.");
        }

        a.setEstado(EstadoAsesoria.CANCELADA);
        a.setCanceladaPor(yo);
        a.setActualizadaEn(LocalDateTime.now());
        a = asesoriaRepository.save(a);

        notificar(a, MotivoNotificacion.ASESORIA_CANCELADA, otra(a, yo), yo.getNombre() + " canceló la asesoría.");
        notificar(a, MotivoNotificacion.ASESORIA_CANCELADA, yo, "Cancelaste la asesoría.");
        return a;
    }

    // ---------- Reseña ----------

    /**
     * La reseña solo es posible cuando la asesoría está FINALIZADA y la hace el participante.
     * La creación de la entidad Resena se hace en ResenaService.
     */
    @Transactional(readOnly = true)
    public void validarPuedeResenar(String email, Long asesoriaId) {
        Asesoria a = asesoriaRepository.findById(asesoriaId)
                .orElseThrow(() -> new ValueConflictException("Asesoría no encontrada."));
        User yo = usuarioActivo(email);
        if (a.getEstado() != EstadoAsesoria.FINALIZADA) {
            throw new ValueConflictException("Solo puedes reseñar una asesoría finalizada.");
        }
        if (!yo.getId().equals(a.getParticipante().getId())) {
            throw new SecurityException("Solo el estudiante puede reseñar la asesoría.");
        }
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<Asesoria> listarDeUsuario(String email) {
        User yo = usuarioActivo(email);
        return asesoriaRepository.findByAsesorOrParticipanteOrderByActualizadaEnDesc(yo, yo);
    }

    // ---------- Helpers ----------

    private Asesoria buscarAbierta(Long id) {
        Asesoria a = asesoriaRepository.findById(id)
                .orElseThrow(() -> new ValueConflictException("Asesoría no encontrada."));
        if (ESTADOS_CERRADOS.contains(a.getEstado())) {
            throw new ValueConflictException("La asesoría está cerrada y no admite más acciones.");
        }
        return a;
    }

    private void validarVentanaCancelacion(Asesoria a) {
        LocalDateTime limite = a.getFechaInicioConfirmada().minusHours(HORAS_LIMITE_CANCELACION);
        if (LocalDateTime.now().isAfter(limite)) {
            throw new ValueConflictException(
                    "Solo puedes cancelar hasta " + HORAS_LIMITE_CANCELACION + " horas antes de la asesoría.");
        }
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime cierre) {
        if (!cierre.isAfter(inicio)) {
            throw new ValueConflictException("La fecha de cierre debe ser posterior a la de inicio.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ValueConflictException("La fecha de inicio no puede estar en el pasado.");
        }
    }

    private User usuarioActivo(String email) {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValueConflictException("Usuario no encontrado."));
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ValueConflictException("Tu cuenta no está activa.");
        }
        return u;
    }

    private User usuarioActivo(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ValueConflictException("Tutor no encontrado."));
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ValueConflictException("El tutor no está activo.");
        }
        return u;
    }

    private User otra(Asesoria a, User yo) {
        return yo.getId().equals(a.getAsesor().getId()) ? a.getParticipante() : a.getAsesor();
    }

    private void notificar(Asesoria a, MotivoNotificacion motivo, User destinatario, String descripcion) {
        notificacionService.crear(destinatario, a, motivo, descripcion);
    }
}