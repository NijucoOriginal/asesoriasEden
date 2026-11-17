package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.MateriaResponse;
import com.jsebastian.eden.EdenSys.services.MateriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;

    @GetMapping
    public List<MateriaResponse> catalogo() {
        return materiaService.catalogo();
    }
}