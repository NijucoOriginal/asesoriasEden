package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.chat.*;
import com.jsebastian.eden.EdenSys.domain.Conversacion;
import com.jsebastian.eden.EdenSys.domain.Mensaje;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.repository.ConversacionRepository;
import com.jsebastian.eden.EdenSys.repository.MensajeRepository;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Obtener o crear conversación entre dos usuarios ──────────────────
    @Transactional
    public ConversacionDetalleDto obtenerOCrearConversacion(String emailActual, Long otroUsuarioId) {
        User yo = obtenerUsuarioPorEmail(emailActual);
        User otro = obtenerUsuarioPorId(otroUsuarioId);

        if (yo.getId().equals(otro.getId())) {
            throw new IllegalArgumentException("No puedes iniciar una conversación contigo mismo.");
        }

        Conversacion conversacion = conversacionRepository
                .findEntreUsuarios(yo, otro)
                .orElseGet(() -> crearConversacion(yo, otro));

        mensajeRepository.marcarComoLeidos(conversacion.getId(), yo.getId());

        return construirDetalle(conversacion, yo);
    }

    // ─── Lista de conversaciones del usuario actual ───────────────────────
    @Transactional(readOnly = true)
    public List<ConversacionDto> listarConversaciones(String emailActual) {
        User yo = obtenerUsuarioPorEmail(emailActual);
        List<Conversacion> conversaciones = conversacionRepository.findByUsuario(yo);

        return conversaciones.stream().map(c -> {
            User otro = otroParticipante(c, yo);

            List<Mensaje> msgs = mensajeRepository.findByConversacionIdOrderByEnviadoEnAsc(c.getId());

            String ultimoContenido = msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getContenido();

            long noLeidos = msgs.stream()
                    .filter(m -> m.getReceptor().getId().equals(yo.getId()) && !m.isLeido())
                    .count();

            return new ConversacionDto(
                    c.getId(),
                    otro.getId(),
                    otro.getNombre(),
                    otro.getApellido(),
                    ultimoContenido,
                    c.getUltimoMensajeEn(),
                    noLeidos
            );
        }).toList();
    }

    // ─── Enviar mensaje (llamado desde ChatWebSocketController) ───────────
    @Transactional
    public MensajeDto enviarMensaje(String emailEmisor, EnviarMensajeRequest request) {
        User emisor = obtenerUsuarioPorEmail(emailEmisor);
        User receptor = obtenerUsuarioPorId(request.receptorId());

        if (emisor.getId().equals(receptor.getId())) {
            throw new IllegalArgumentException("No puedes enviarte mensajes a ti mismo.");
        }

        Conversacion conversacion;
        if (request.conversacionId() != null) {
            conversacion = conversacionRepository.findById(request.conversacionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Conversación no encontrada: " + request.conversacionId()));

            if (!esParticipante(conversacion, emisor)) {
                throw new SecurityException("No tienes acceso a esta conversación.");
            }
        } else {
            conversacion = conversacionRepository
                    .findEntreUsuarios(emisor, receptor)
                    .orElseGet(() -> crearConversacion(emisor, receptor));
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        mensaje.setContenido(request.contenido());
        mensaje.setLeido(false);
        mensaje = mensajeRepository.save(mensaje);

        conversacion.setUltimoMensajeEn(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        MensajeDto mensajeDto = toMensajeDto(mensaje);

        // Canal personal del receptor: /user/{receptorId}/queue/mensajes
        messagingTemplate.convertAndSendToUser(
                receptor.getId().toString(),
                "/queue/mensajes",
                mensajeDto
        );

        long totalNoLeidos = mensajeRepository.contarNoLeidos(receptor.getId());
        NotificacionDto notificacion = new NotificacionDto(
                conversacion.getId(),
                emisor.getId(),
                emisor.getNombre() + " " + emisor.getApellido(),
                request.contenido().length() > 50
                        ? request.contenido().substring(0, 50) + "..."
                        : request.contenido(),
                totalNoLeidos
        );
        messagingTemplate.convertAndSendToUser(
                receptor.getId().toString(),
                "/queue/notificaciones",
                notificacion
        );

        return mensajeDto;
    }

    // ─── Marcar mensajes como leídos ─────────────────────────────────────
    @Transactional
    public void marcarLeidos(String emailActual, Long conversacionId) {
        User yo = obtenerUsuarioPorEmail(emailActual);
        mensajeRepository.marcarComoLeidos(conversacionId, yo.getId());
    }

    // ─── Cargar una conversación existente por id ────────────────────────
    @Transactional
    public ConversacionDetalleDto obtenerConversacionPorId(String emailActual, Long conversacionId) {
        User yo = obtenerUsuarioPorEmail(emailActual);

        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RuntimeException("Conversación no encontrada: " + conversacionId));

        if (!esParticipante(conversacion, yo)) {
            throw new SecurityException("No tienes acceso a esta conversación.");
        }

        mensajeRepository.marcarComoLeidos(conversacionId, yo.getId());

        return construirDetalle(conversacion, yo);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────

    /**
     * Crea la conversación guardando siempre el id menor en usuario1,
     * para que la restricción única (usuario1_id, usuario2_id) funcione.
     */
    private Conversacion crearConversacion(User a, User b) {
        User menor = a.getId() < b.getId() ? a : b;
        User mayor = menor == a ? b : a;

        Conversacion nueva = new Conversacion();
        nueva.setUsuario1(menor);
        nueva.setUsuario2(mayor);
        return conversacionRepository.save(nueva);
    }

    private ConversacionDetalleDto construirDetalle(Conversacion conversacion, User yo) {
        List<MensajeDto> mensajes = mensajeRepository
                .findByConversacionIdOrderByEnviadoEnAsc(conversacion.getId())
                .stream()
                .map(this::toMensajeDto)
                .toList();

        User otro = otroParticipante(conversacion, yo);

        return new ConversacionDetalleDto(
                conversacion.getId(),
                otro.getId(),
                otro.getNombre(),
                otro.getApellido(),
                mensajes
        );
    }

    private User otroParticipante(Conversacion c, User yo) {
        return c.getUsuario1().getId().equals(yo.getId()) ? c.getUsuario2() : c.getUsuario1();
    }

    private boolean esParticipante(Conversacion c, User u) {
        return c.getUsuario1().getId().equals(u.getId())
                || c.getUsuario2().getId().equals(u.getId());
    }

    private User obtenerUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    private User obtenerUsuarioPorId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    private MensajeDto toMensajeDto(Mensaje m) {
        return new MensajeDto(
                m.getId(),
                m.getConversacion().getId(),
                m.getEmisor().getId(),
                m.getEmisor().getNombre(),
                m.getEmisor().getApellido(),
                m.getReceptor().getId(),
                m.getContenido(),
                m.getEnviadoEn(),
                m.isLeido()
        );
    }
}