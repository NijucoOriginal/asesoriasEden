package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.domain.*;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AsesoriaExpiracionService {

    private static final BigDecimal COMISION = new BigDecimal("5000");

    private final AsesoriaRepository asesoriaRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public void expirar(Long asesoriaId) {
        Asesoria a = asesoriaRepository.findById(asesoriaId).orElse(null);
        // Idempotente: si ya cambió de estado, no hace nada ni notifica de nuevo
        if (a == null || (a.getEstado() != EstadoAsesoria.PENDIENTE
                && a.getEstado() != EstadoAsesoria.CONTRAPROPUESTA)) {
            return;
        }

        a.setEstado(EstadoAsesoria.EXPIRADA);
        a.setActualizadaEn(LocalDateTime.now());
        asesoriaRepository.save(a);

        String texto = "La solicitud de asesoría expiró por no ser respondida a tiempo.";
        notificacionService.crear(a.getAsesor(), a, MotivoNotificacion.ASESORIA_EXPIRADA, texto);
        notificacionService.crear(a.getParticipante(), a, MotivoNotificacion.ASESORIA_EXPIRADA, texto);
    }

    @Transactional
    public void finalizar(Long asesoriaId) {
        Asesoria a = asesoriaRepository.findById(asesoriaId).orElse(null);
        if (a == null || a.getEstado() != EstadoAsesoria.ACEPTADA) {
            return;
        }

        a.setEstado(EstadoAsesoria.FINALIZADA);
        a.setComision(COMISION);
        a.setActualizadaEn(LocalDateTime.now());
        asesoriaRepository.save(a);
    }
}