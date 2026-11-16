package com.jsebastian.eden.EdenSys.Dtos.asesoria;

import com.jsebastian.eden.EdenSys.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AsesoriaResponse(
        Long id,
        EstadoAsesoria estado,
        MotivoContrapropuesta motivoContrapropuesta,
        UsuarioResumen asesor,
        UsuarioResumen participante,
        Long materiaId,
        String materiaNombre,
        List<String> temas,
        LocalDateTime fechaInicioPropuesta,
        LocalDateTime fechaCierrePropuesta,
        LocalDateTime fechaInicioConfirmada,
        LocalDateTime fechaCierreConfirmada,
        BigDecimal precio,
        BigDecimal comision,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn,
        LocalDateTime aceptadaEn,
        Long rechazadaPorId,
        Long canceladaPorId
) {
    public record UsuarioResumen(Long id, String nombre, String apellido) {}

    public static AsesoriaResponse from(Asesoria a) {
        return new AsesoriaResponse(
                a.getId(),
                a.getEstado(),
                a.getMotivoContrapropuesta(),
                new UsuarioResumen(a.getAsesor().getId(), a.getAsesor().getNombre(), a.getAsesor().getApellido()),
                new UsuarioResumen(a.getParticipante().getId(), a.getParticipante().getNombre(), a.getParticipante().getApellido()),
                a.getMateria().getId(),
                a.getMateria().getNombreMateria(),
                a.getTemas().stream().map(Tema::getNombreTema).toList(),
                a.getFechaInicioPropuesta(),
                a.getFechaCierrePropuesta(),
                a.getFechaInicioConfirmada(),
                a.getFechaCierreConfirmada(),
                a.getPrecio(),
                a.getComision(),
                a.getCreadaEn(),
                a.getActualizadaEn(),
                a.getAceptadaEn(),
                a.getRechazadaPor() != null ? a.getRechazadaPor().getId() : null,
                a.getCanceladaPor() != null ? a.getCanceladaPor().getId() : null
        );
    }
}