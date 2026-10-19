package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public abstract class BaseHttpHandler implements HttpHandler {
    private static final String CT_JSON = "application/json; charset=UTF-8";
    private static final int MAX_BODY_SIZE = 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    protected void sendJson(HttpExchange ex, HttpStatus status, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status.getCode(), bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendError(HttpExchange ex, ApiError error) throws IOException {
        sendJson(ex, error.getStatus(), new ErrorResponse(error.getMessage()));
    }

    protected void sendError(HttpExchange ex, List<String> details) throws IOException {
        sendJson(ex, ApiError.VALIDATION_ERROR.getStatus(), new ErrorResponse(ApiError.VALIDATION_ERROR.getMessage(), details));
    }

    protected <T> Optional<T> readJson(HttpExchange ex, Class<T> type) throws IOException {
        byte[] bytes = ex.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);
        if (bytes.length > MAX_BODY_SIZE) {
            throw new PayloadTooLargeException();
        }
        String body = new String(bytes, StandardCharsets.UTF_8);
        try {
            return Optional.ofNullable(GSON.fromJson(body, type));
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(HttpStatus.NO_CONTENT.getCode(), -1);
        ex.close();
    }
}
