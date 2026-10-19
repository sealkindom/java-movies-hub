package ru.practicum.moviehub.http;

public enum HttpMethod {
    GET,
    POST,
    DELETE,
    OTHER;

    public static HttpMethod from(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
