package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Link;

public record LinkResponseDto(Integer id, String name, String url) {
    public static LinkResponseDto fromEntity(Link l) {
        return new LinkResponseDto(l.getId(), l.getName(), l.getUrl());
    }
}
