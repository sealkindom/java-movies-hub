package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.MovieRequest;
import ru.practicum.moviehub.api.MovieValidator;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    static final String MOVIES_PATH = "/movies";

    private static final String JSON_MEDIA_TYPE = "application/json";
    private static final String YEAR_PARAMETER_PREFIX = "year=";
    private static final String ALLOWED_ON_COLLECTION = "GET, POST";
    private static final String ALLOWED_ON_ITEM = "GET, DELETE";

    private final MoviesStore store;
    private final MovieValidator validator = new MovieValidator();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            route(ex);
        } catch (RuntimeException e) {
            sendError(ex, ApiError.INTERNAL_ERROR);
        }
    }

    private void route(HttpExchange ex) throws IOException {
        HttpMethod method = HttpMethod.from(ex.getRequestMethod());
        String path = normalize(ex.getRequestURI().getPath());

        if (path.equals(MOVIES_PATH)) {
            handleCollection(ex, method);
        } else if (path.startsWith(MOVIES_PATH + "/") && path.indexOf('/', MOVIES_PATH.length() + 1) < 0) {
            handleItem(ex, method, path.substring(MOVIES_PATH.length() + 1));
        } else {
            sendError(ex, ApiError.PATH_NOT_FOUND);
        }
    }

    private void handleCollection(HttpExchange ex, HttpMethod method) throws IOException {
        switch (method) {
            case GET -> getMovies(ex);
            case POST -> addMovie(ex);
            default -> sendMethodNotAllowed(ex, ALLOWED_ON_COLLECTION);
        }
    }

    private void handleItem(HttpExchange ex, HttpMethod method, String rawId) throws IOException {
        switch (method) {
            case GET -> getMovie(ex, rawId);
            case DELETE -> deleteMovie(ex, rawId);
            default -> sendMethodNotAllowed(ex, ALLOWED_ON_ITEM);
        }
    }

    private void getMovies(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) {
            sendJson(ex, HttpStatus.OK, store.getAll());
            return;
        }
        Optional<Integer> year = parseYearParameter(query);
        if (year.isEmpty()) {
            sendError(ex, ApiError.INVALID_YEAR_PARAMETER);
            return;
        }
        sendJson(ex, HttpStatus.OK, store.getByYear(year.get()));
    }

    private void addMovie(HttpExchange ex) throws IOException {
        if (!hasJsonContentType(ex)) {
            sendError(ex, ApiError.UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        Optional<MovieRequest> parsed = readJson(ex, MovieRequest.class);
        if (parsed.isEmpty()) {
            sendError(ex, ApiError.INVALID_JSON);
            return;
        }

        MovieRequest request = parsed.get();
        List<String> details = validator.validate(request);
        if (!details.isEmpty()) {
            sendError(ex, details);
            return;
        }
        sendJson(ex, HttpStatus.CREATED, store.add(request.getTitle(), request.getYear()));
    }

    private void getMovie(HttpExchange ex, String rawId) throws IOException {
        Optional<Integer> id = parseInt(rawId);
        if (id.isEmpty()) {
            sendError(ex, ApiError.INVALID_ID);
            return;
        }
        Optional<Movie> movie = store.getById(id.get());
        if (movie.isEmpty()) {
            sendError(ex, ApiError.MOVIE_NOT_FOUND);
            return;
        }
        sendJson(ex, HttpStatus.OK, movie.get());
    }

    private void deleteMovie(HttpExchange ex, String rawId) throws IOException {
        Optional<Integer> id = parseInt(rawId);
        if (id.isEmpty()) {
            sendError(ex, ApiError.INVALID_ID);
            return;
        }
        if (!store.remove(id.get())) {
            sendError(ex, ApiError.MOVIE_NOT_FOUND);
            return;
        }
        sendNoContent(ex);
    }

    private void sendMethodNotAllowed(HttpExchange ex, String allowedMethods) throws IOException {
        ex.getResponseHeaders().set("Allow", allowedMethods);
        sendError(ex, ApiError.METHOD_NOT_ALLOWED);
    }

    private String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private boolean hasJsonContentType(HttpExchange ex) {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.toLowerCase().startsWith(JSON_MEDIA_TYPE);
    }

    private Optional<Integer> parseYearParameter(String query) {
        if (!query.startsWith(YEAR_PARAMETER_PREFIX) || query.contains("&")) {
            return Optional.empty();
        }
        return parseInt(query.substring(YEAR_PARAMETER_PREFIX.length()));
    }

    private Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
