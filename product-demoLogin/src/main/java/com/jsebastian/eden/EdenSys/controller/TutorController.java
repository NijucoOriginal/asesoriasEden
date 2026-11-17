package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.MateriaTutorResponse;
import com.jsebastian.eden.EdenSys.Dtos.TutorResponse;
import com.jsebastian.eden.EdenSys.services.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutores")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @GetMapping
    public List<TutorResponse> listar() {
        return tutorService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutorResponse> obtener(@PathVariable Long id) {
        return tutorService.obtener(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/materias")
    public List<MateriaTutorResponse> materias(@PathVariable Long id) {
        return tutorService.materiasDe(id);
    }
}