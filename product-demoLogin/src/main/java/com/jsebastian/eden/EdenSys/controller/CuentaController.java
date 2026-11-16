package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.AuthResponse;
import com.jsebastian.eden.EdenSys.Dtos.CodigoRequest;
import com.jsebastian.eden.EdenSys.Dtos.PerfilResponse;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.services.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> miPerfil(@AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(cuentaService.obtenerPerfil(usuario.getUsername()));
    }

    @PutMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> actualizarImagen(@AuthenticationPrincipal UserDetails usuario,
                                                                @RequestPart("imagen") MultipartFile imagen) {
        String url = cuentaService.actualizarImagenPerfil(usuario.getUsername(), imagen);
        return ResponseEntity.ok(Map.of("imagenPerfil", url));
    }

    @PostMapping("/reactivar/solicitar")
    public ResponseEntity<Map<String, String>> solicitarReactivacion(@AuthenticationPrincipal UserDetails usuario) {
        cuentaService.solicitarReactivacion(usuario.getUsername());
        return ResponseEntity.ok(Map.of("message", "Te enviamos un código a tu correo para reactivar tu cuenta."));
    }

    @PostMapping("/reactivar/confirmar")
    public ResponseEntity<AuthResponse> confirmarReactivacion(@AuthenticationPrincipal UserDetails usuario,
                                                              @RequestBody CodigoRequest req) {
        String token = cuentaService.confirmarReactivacion(usuario.getUsername(), req.codigo());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @ExceptionHandler(ValueConflictException.class)
    public ResponseEntity<Map<String, String>> conflicto(ValueConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}