package ru.practicum.moviehub.http;

public enum ApiError {
    INTERNAL_ERROR(HttpStatus.INTERNAL_ERROR, "Внутренняя ошибка сервера"),
    PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "Ресурс не найден"),
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "Фильм не найден"),
    INVALID_ID(HttpStatus.BAD_REQUEST, "Некорректный ID"),
    INVALID_YEAR_PARAMETER(HttpStatus.BAD_REQUEST, "Некорректный параметр запроса — 'year'"),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "Некорректный JSON"),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "Ошибка валидации"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Неподдерживаемый тип данных, ожидается application/json"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Метод не поддерживается");

    private final HttpStatus status;
    private final String message;

    ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
