package com.jsebastian.eden.EdenSys.Dtos;

import java.util.List;

public record TutorResponse(
        Long id,
        String nombre,
        String apellido,
        String carrera,
        String imagenPerfil,
        List<String> materias,
        double horasTutoria,
        Double calificacionPromedio,
        int totalResenas
) {}