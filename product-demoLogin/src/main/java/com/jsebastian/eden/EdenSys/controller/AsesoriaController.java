package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.asesoria.AsesoriaResponse;
import com.jsebastian.eden.EdenSys.Dtos.asesoria.CrearAsesoriaRequest;
import com.jsebastian.eden.EdenSys.Dtos.asesoria.ProponerAsesoriaRequest;
import com.jsebastian.eden.EdenSys.services.AsesoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asesorias")
@RequiredArgsConstructor
public class AsesoriaController {

    private final AsesoriaService asesoriaService;

    @GetMapping
    public ResponseEntity<List<AsesoriaResponse>> listar(@AuthenticationPrincipal UserDetails u) {
        return ResponseEntity.ok(asesoriaService.listarDeUsuario(u.getUsername())
                .stream().map(AsesoriaResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<AsesoriaResponse> crear(@AuthenticationPrincipal UserDetails u,
                                                  @RequestBody CrearAsesoriaRequest req) {
        return ResponseEntity.ok(AsesoriaResponse.from(asesoriaService.crear(u.getUsername(), req)));
    }

    @PutMapping("/{id}/proponer")
    public ResponseEntity<AsesoriaResponse> proponer(@AuthenticationPrincipal UserDetails u,
                                                     @PathVariable Long id,
                                                     @RequestBody ProponerAsesoriaRequest req) {
        return ResponseEntity.ok(AsesoriaResponse.from(asesoriaService.proponer(u.getUsername(), id, req)));
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<AsesoriaResponse> aceptar(@AuthenticationPrincipal UserDetails u, @PathVariable Long id) {
        return ResponseEntity.ok(AsesoriaResponse.from(asesoriaService.aceptar(u.getUsername(), id)));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<AsesoriaResponse> rechazar(@AuthenticationPrincipal UserDetails u, @PathVariable Long id) {
        return ResponseEntity.ok(AsesoriaResponse.from(asesoriaService.rechazar(u.getUsername(), id)));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<AsesoriaResponse> cancelar(@AuthenticationPrincipal UserDetails u, @PathVariable Long id) {
        return ResponseEntity.ok(AsesoriaResponse.from(asesoriaService.cancelar(u.getUsername(), id)));
    }
}