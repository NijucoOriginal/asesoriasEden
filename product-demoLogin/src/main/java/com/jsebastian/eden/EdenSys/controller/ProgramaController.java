package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.ProgramaResponse;
import com.jsebastian.eden.EdenSys.services.ProgramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/programas")
@RequiredArgsConstructor
public class ProgramaController {

    private final ProgramaService programaService;

    @GetMapping
    public List<ProgramaResponse> catalogo() {
        return programaService.catalogo();
    }
}