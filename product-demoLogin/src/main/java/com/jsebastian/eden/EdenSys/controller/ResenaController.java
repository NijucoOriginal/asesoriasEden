package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.resena.ResenaRequest;
import com.jsebastian.eden.EdenSys.Dtos.resena.ResenaResponse;
import com.jsebastian.eden.EdenSys.services.ResenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;

    @PostMapping("/api/asesorias/{asesoriaId}/resena")
    public ResponseEntity<ResenaResponse> crear(@AuthenticationPrincipal UserDetails u,
                                                @PathVariable Long asesoriaId,
                                                @RequestBody ResenaRequest req) {
        return ResponseEntity.ok(ResenaResponse.from(
                resenaService.crear(u.getUsername(), asesoriaId, req.calificacion(), req.descripcion())));
    }

    @PutMapping("/api/asesorias/{asesoriaId}/resena")
    public ResponseEntity<ResenaResponse> editar(@AuthenticationPrincipal UserDetails u,
                                                 @PathVariable Long asesoriaId,
                                                 @RequestBody ResenaRequest req) {
        return ResponseEntity.ok(ResenaResponse.from(
                resenaService.editar(u.getUsername(), asesoriaId, req.calificacion(), req.descripcion())));
    }

    @DeleteMapping("/api/asesorias/{asesoriaId}/resena")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserDetails u, @PathVariable Long asesoriaId) {
        resenaService.eliminar(u.getUsername(), asesoriaId);
        return ResponseEntity.noContent().build();
    }

    // Reseñas recibidas por un tutor (visibles para cualquier usuario autenticado)
    @GetMapping("/api/tutores/{tutorId}/resenas")
    public ResponseEntity<List<ResenaResponse>> listarDeTutor(@PathVariable Long tutorId) {
        return ResponseEntity.ok(resenaService.listarDeTutor(tutorId).stream()
                .map(ResenaResponse::from).toList());
    }
}