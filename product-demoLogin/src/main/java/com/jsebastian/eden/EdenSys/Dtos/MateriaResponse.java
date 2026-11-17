package com.jsebastian.eden.EdenSys.Dtos;

import java.util.List;

public record MateriaResponse(Long id, String nombre, Long programaId, String programa, List<TemaResumen> temas) {
    public record TemaResumen(Long id, String nombre) {}
}