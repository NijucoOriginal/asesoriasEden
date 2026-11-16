package com.jsebastian.eden.EdenSys.Dtos.resena;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ResenaRequest(
        @Min(1) @Max(5) int calificacion,
        String descripcion
) {}