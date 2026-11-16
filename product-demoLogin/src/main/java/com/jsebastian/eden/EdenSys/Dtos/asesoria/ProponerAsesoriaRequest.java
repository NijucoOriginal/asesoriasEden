package com.jsebastian.eden.EdenSys.Dtos.asesoria;

import com.jsebastian.eden.EdenSys.domain.MotivoContrapropuesta;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProponerAsesoriaRequest(
        @NotNull LocalDateTime fechaInicioPropuesta,
        @NotNull LocalDateTime fechaCierrePropuesta,
        @NotNull BigDecimal precio,
        MotivoContrapropuesta motivo
) {}