package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.domain.Asesoria;
import com.jsebastian.eden.EdenSys.domain.MotivoNotificacion;
import com.jsebastian.eden.EdenSys.domain.Notificacion;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public Notificacion crear(User destinatario, Asesoria asesoria,
                              MotivoNotificacion motivo, String descripcion) {
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setAsesoria(asesoria);
        n.setMotivo(motivo);
        n.setDescripcion(descripcion);
        return notificacionRepository.save(n);
    }
}