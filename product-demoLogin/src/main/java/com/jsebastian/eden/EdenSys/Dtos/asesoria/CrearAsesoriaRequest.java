package com.jsebastian.eden.EdenSys.Dtos.asesoria;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CrearAsesoriaRequest(
        @NotNull Long asesorId,
        @NotNull Long materiaId,
        List<Long> temasIds,
        @NotNull LocalDateTime fechaInicioPropuesta,
        @NotNull LocalDateTime fechaCierrePropuesta,
        @NotNull BigDecimal precio
) {}