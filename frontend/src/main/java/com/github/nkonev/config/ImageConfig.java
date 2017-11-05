package com.github.nkonev.config;

import org.springframework.http.MediaType;

import java.util.List;

public interface ImageConfig {
    List<MediaType> getAllowedMimeTypes();

    public long getMaxBytes();

    /**
     * in seconds
     * @return
     */
    public long getMaxAge();
}
