package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.config.CloudinaryConfig;
import com.jsebastian.eden.EdenSys.services.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final CloudinaryConfig cloudinaryConfig;

    @Override
    public String subirImagen(MultipartFile archivo) {
        try {
            Map<?, ?> resultado = cloudinaryConfig.upload(archivo);
            return resultado.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al subir la imagen a Cloudinary", e);
        }
    }
}