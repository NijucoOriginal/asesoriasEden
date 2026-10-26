package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String codigoActivacion;

    private LocalDateTime fechaCreacionCodigo;

    private String email;

    private String contrasena;

    private String nombre;

    private String apellido;

    private String documentoIdentidad;

    private String telefono;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;
}