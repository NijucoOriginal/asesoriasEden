package com.jsebastian.eden.EdenSys.mappers;

import com.jsebastian.eden.EdenSys.Dtos.CrearUsuarioDto;
import com.jsebastian.eden.EdenSys.Dtos.UserResponse;
import com.jsebastian.eden.EdenSys.Dtos.UsuarioResponse;
import com.jsebastian.eden.EdenSys.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Solo mapea lo que viene del registro. El resto (rol, estado, contraseña hash) lo asigna el servicio.
    User toEntity(CrearUsuarioDto dto);

    UsuarioResponse toUsuarioResponse(User user);

    UserResponse toUserResponse(User user);
}