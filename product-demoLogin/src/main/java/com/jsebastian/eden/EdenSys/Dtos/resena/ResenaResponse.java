package com.jsebastian.eden.EdenSys.Dtos.resena;

import com.jsebastian.eden.EdenSys.domain.Resena;

public record ResenaResponse(
        Long id,
        Long asesoriaId,
        String materia,
        String autorNombre,
        int calificacion,
        String descripcion
) {
    public static ResenaResponse from(Resena r) {
        return new ResenaResponse(
                r.getId(),
                r.getAsesoria().getId(),
                r.getAsesoria().getMateria().getNombreMateria(),
                r.getAsesoria().getParticipante().getNombre() + " " + r.getAsesoria().getParticipante().getApellido(),
                r.getCalificacion(),
                r.getDescripcion()
        );
    }
}