package com.jsebastian.eden.EdenSys.Dtos;

import com.jsebastian.eden.EdenSys.domain.EstadoUsuario;
import com.jsebastian.eden.EdenSys.domain.Rol;
import com.jsebastian.eden.EdenSys.domain.User;

public record PerfilResponse(
        Long id,
        String email,
        Rol rol,
        EstadoUsuario estado,
        String nombre,
        String apellido,
        String documentoIdentidad,
        String telefono,
        String imagenPerfil,
        Long programaId,
        String programa
) {
    public static PerfilResponse from(User u) {
        return new PerfilResponse(
                u.getId(), u.getEmail(), u.getRol(), u.getEstado(),
                u.getNombre(), u.getApellido(), u.getDocumentoIdentidad(),
                u.getTelefono(), u.getImagenPerfil(),
                u.getProgramaAcademico() != null ? u.getProgramaAcademico().getId() : null,
                u.getProgramaAcademico() != null ? u.getProgramaAcademico().getNombrePrograma() : null);
    }
}