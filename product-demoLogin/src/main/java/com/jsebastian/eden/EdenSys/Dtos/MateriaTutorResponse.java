package com.jsebastian.eden.EdenSys.Dtos;

import java.util.List;

public record MateriaTutorResponse(Long id, String nombre, List<TemaResumen> temas) {
    public record TemaResumen(Long id, String nombre) {}
}