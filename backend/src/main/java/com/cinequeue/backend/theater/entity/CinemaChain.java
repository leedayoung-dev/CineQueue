package com.cinequeue.backend.theater.entity;

public enum CinemaChain {
    CGV,
    MEGABOX,
    LOTTE;

    public static CinemaChain fromName(String name) {
        if (name == null) {
            return null;
        }
        if (name.startsWith("CGV ")) {
            return CGV;
        }
        if (name.startsWith("메가박스 ")) {
            return MEGABOX;
        }
        if (name.startsWith("롯데시네마 ")) {
            return LOTTE;
        }
        return null;
    }

    public static String keyword(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceFirst("^(CGV|메가박스|롯데시네마)\\s+", "");
    }
}
