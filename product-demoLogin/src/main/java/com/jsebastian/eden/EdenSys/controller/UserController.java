package com.jsebastian.eden.EdenSys.controller;

import com.jsebastian.eden.EdenSys.Dtos.*;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.mappers.UserMapper;
import com.jsebastian.eden.EdenSys.services.interfaces.CaptchaService;
import com.jsebastian.eden.EdenSys.services.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final CaptchaService captchaService;

    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody CrearUsuarioDto dto) {
        if (!captchaService.verificar(dto.recaptchaToken())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Seguridad",
                    "message", "La verificación de reCAPTCHA ha fallado o ha expirado."));
        }
        try {
            return ResponseEntity.ok(userService.crearUsuario(dto));
        } catch (ValueConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Conflicto de datos",
                    "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Validación",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> obtenerPorId(@PathVariable Long id) {
        return responder(userService.buscarPorId(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> obtenerPorEmail(@PathVariable String email) {
        return responder(userService.buscarPorEmail(email));
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<UserResponse> obtenerPorCedula(@PathVariable String cedula) {
        return responder(userService.buscarPorCedula(cedula));
    }

    @PutMapping("/actualizar/{email}")
    public ResponseEntity<AuthResponse> actualizarUsuarioPorEmail(@PathVariable String email,
                                                                  @RequestBody CrearUsuarioDto dto) {
        try {
            return ResponseEntity.ok(new AuthResponse(userService.actualizarUsuarioEmail(email, dto)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        try {
            userService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/email/{email}")
    public ResponseEntity<Void> eliminarUsuarioPorEmail(@PathVariable String email) {
        userService.eliminarUsuarioPorEmail(email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/existe/email/{email}")
    public ResponseEntity<Boolean> existeEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.existePorEmail(email));
    }

    @GetMapping("/existe/cedula/{cedula}")
    public ResponseEntity<Boolean> existeCedula(@PathVariable String cedula) {
        return ResponseEntity.ok(userService.existePorCedula(cedula));
    }

    @PutMapping("/desvincular/{email}")
    public ResponseEntity<String> desvincularUsuario(@PathVariable String email) {
        userService.desvincularUsuario(email);
        return ResponseEntity.ok("Usuario desvinculado correctamente");
    }

    private ResponseEntity<UserResponse> responder(Optional<User> usuario) {
        return usuario.map(u -> ResponseEntity.ok(userMapper.toUserResponse(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}