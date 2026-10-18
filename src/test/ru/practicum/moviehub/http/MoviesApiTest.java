package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:" + MoviesServer.DEFAULT_PORT;
    private static final String CT_JSON = "application/json; charset=UTF-8";
    private static final Gson GSON = new Gson();
    private static MoviesStore store;
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, MoviesServer.DEFAULT_PORT);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    private static HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder().uri(URI.create(BASE + path)).GET().build());
    }

    private static HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder().uri(URI.create(BASE + path)).DELETE().build());
    }

    private static HttpResponse<String> post(String body, String contentType) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
    }

    private static HttpResponse<String> postJson(String body) throws IOException, InterruptedException {
        return post(body, CT_JSON);
    }

    private static String contentType(HttpResponse<String> resp) {
        return resp.headers().firstValue("Content-Type").orElse("");
    }

    private static ErrorResponse error(HttpResponse<String> resp) {
        ErrorResponse error = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertNotNull(error, "Тело ответа должно содержать объект ошибки");
        assertNotNull(error.getError(), "Объект ошибки должен содержать поле error");
        return error;
    }

    @Test
    @DisplayName("GET /movies: пустое хранилище возвращает пустой массив")
    void getMoviesWhenEmptyReturnsEmptyArray() throws Exception {
        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertEquals(CT_JSON, contentType(resp), "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        assertEquals("[]", body, "Список должен быть пустым");
    }

    @Test
    @DisplayName("GET /movies: возвращает ранее добавленные фильмы")
    void getMoviesWhenMoviesAddedReturnsThem() throws Exception {
        store.add("Матрица", 1999);
        store.add("Интерстеллар", 2014);

        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        List<Movie> movies = GSON.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size(), "Должны вернуться оба фильма");
        assertEquals("Матрица", movies.get(0).getTitle());
        assertEquals(1999, movies.get(0).getYear());
        assertEquals("Интерстеллар", movies.get(1).getTitle());
        assertEquals(2014, movies.get(1).getYear());
    }

    @Test
    @DisplayName("POST /movies: корректные данные создают фильм и возвращают 201")
    void postMoviesWithValidDataReturnsCreatedMovie() throws Exception {
        HttpResponse<String> resp = postJson("{\"title\":\"Начало\",\"year\":2010}");

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertEquals(CT_JSON, contentType(resp));
        Movie movie = GSON.fromJson(resp.body(), Movie.class);
        assertTrue(movie.getId() > 0, "Фильму должен быть присвоен id");
        assertEquals("Начало", movie.getTitle());
        assertEquals(2010, movie.getYear());
        assertEquals(1, store.getAll().size(), "Фильм должен попасть в хранилище");
    }

    @Test
    @DisplayName("POST /movies: пустой title возвращает 422")
    void postMoviesWithEmptyTitleReturnsValidationError() throws Exception {
        HttpResponse<String> resp = postJson("{\"title\":\"\",\"year\":2010}");

        assertEquals(422, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        ErrorResponse error = error(resp);
        assertNotNull(error.getDetails(), "Ожидается список деталей ошибки");
        assertFalse(error.getDetails().isEmpty());
        assertTrue(store.getAll().isEmpty(), "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("POST /movies: отсутствующий title возвращает 422")
    void postMoviesWithMissingTitleReturnsValidationError() throws Exception {
        HttpResponse<String> resp = postJson("{\"year\":2010}");

        assertEquals(422, resp.statusCode());
        assertFalse(error(resp).getDetails().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: title длиннее 100 символов возвращает 422")
    void postMoviesWithTooLongTitleReturnsValidationError() throws Exception {
        String title = "а".repeat(101);

        HttpResponse<String> resp = postJson("{\"title\":\"" + title + "\",\"year\":2010}");

        assertEquals(422, resp.statusCode());
        assertFalse(error(resp).getDetails().isEmpty());
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: title из 100 символов допустим")
    void postMoviesWithTitleOfMaxLengthReturnsCreated() throws Exception {
        String title = "а".repeat(100);

        HttpResponse<String> resp = postJson("{\"title\":\"" + title + "\",\"year\":2010}");

        assertEquals(201, resp.statusCode());
    }

    @Test
    @DisplayName("POST /movies: год меньше 1888 возвращает 422")
    void postMoviesWithYearBeforeFirstFilmReturnsValidationError() throws Exception {
        HttpResponse<String> resp = postJson("{\"title\":\"Фильм\",\"year\":1887}");

        assertEquals(422, resp.statusCode());
        assertFalse(error(resp).getDetails().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: год больше текущего + 1 возвращает 422")
    void postMoviesWithYearTooFarInFutureReturnsValidationError() throws Exception {
        int year = Year.now().getValue() + 2;

        HttpResponse<String> resp = postJson("{\"title\":\"Фильм\",\"year\":" + year + "}");

        assertEquals(422, resp.statusCode());
        assertFalse(error(resp).getDetails().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: граничные значения года допустимы")
    void postMoviesWithBoundaryYearsReturnsCreated() throws Exception {
        int maxYear = Year.now().getValue() + 1;

        assertEquals(201, postJson("{\"title\":\"Первый\",\"year\":1888}").statusCode());
        assertEquals(201, postJson("{\"title\":\"Будущий\",\"year\":" + maxYear + "}").statusCode());
    }

    @Test
    @DisplayName("POST /movies: при нескольких ошибках возвращаются все details")
    void postMoviesWithInvalidTitleAndYearReturnsAllDetails() throws Exception {
        HttpResponse<String> resp = postJson("{\"title\":\"\",\"year\":1500}");

        assertEquals(422, resp.statusCode());
        assertEquals(2, error(resp).getDetails().size(), "Должны вернуться обе ошибки");
    }

    @Test
    @DisplayName("POST /movies: неверный Content-Type возвращает 415")
    void postMoviesWithWrongContentTypeReturnsUnsupportedMediaType() throws Exception {
        HttpResponse<String> resp = post("{\"title\":\"Фильм\",\"year\":2010}", "text/plain");

        assertEquals(415, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: отсутствие Content-Type возвращает 415")
    void postMoviesWithoutContentTypeReturnsUnsupportedMediaType() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Фильм\",\"year\":2010}"))
                .build();

        assertEquals(415, send(req).statusCode());
    }

    @Test
    @DisplayName("POST /movies: некорректный JSON возвращает 400")
    void postMoviesWithInvalidJsonReturnsBadRequest() throws Exception {
        HttpResponse<String> resp = postJson("{\"title\":");

        assertEquals(400, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    @DisplayName("POST /movies: пустое тело возвращает 400")
    void postMoviesWithEmptyBodyReturnsBadRequest() throws Exception {
        assertEquals(400, postJson("").statusCode());
    }

    @Test
    @DisplayName("GET /movies/{id}: существующий фильм возвращается с 200")
    void getMovieByIdWhenExistsReturnsMovie() throws Exception {
        Movie saved = store.add("Побег из Шоушенка", 1994);

        HttpResponse<String> resp = get("/movies/" + saved.getId());

        assertEquals(200, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        Movie movie = GSON.fromJson(resp.body(), Movie.class);
        assertEquals(saved.getId(), movie.getId());
        assertEquals("Побег из Шоушенка", movie.getTitle());
        assertEquals(1994, movie.getYear());
    }

    @Test
    @DisplayName("GET /movies/{id}/: слэш в конце пути допустим")
    void getMovieByIdWithTrailingSlashReturnsMovie() throws Exception {
        Movie saved = store.add("Матрица", 1999);

        HttpResponse<String> resp = get("/movies/" + saved.getId() + "/");

        assertEquals(200, resp.statusCode());
        assertEquals(saved, GSON.fromJson(resp.body(), Movie.class));
    }

    @Test
    @DisplayName("Неизвестный путь возвращает 404")
    void unknownPathReturnsNotFound() throws Exception {
        HttpResponse<String> resp = get("/movies/1/2");

        assertEquals(404, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("GET /movies/{id}: несуществующий фильм возвращает 404")
    void getMovieByIdWhenNotFoundReturnsNotFound() throws Exception {
        HttpResponse<String> resp = get("/movies/999");

        assertEquals(404, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("GET /movies/{id}: нечисловой id возвращает 400")
    void getMovieByIdWhenIdIsNotNumberReturnsBadRequest() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");

        assertEquals(400, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("DELETE /movies/{id}: существующий фильм удаляется с 204")
    void deleteMovieWhenExistsReturnsNoContentAndRemovesMovie() throws Exception {
        Movie saved = store.add("Форрест Гамп", 1994);

        HttpResponse<String> resp = delete("/movies/" + saved.getId());

        assertEquals(204, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        assertTrue(resp.body().isEmpty(), "Тело ответа 204 должно быть пустым");
        assertEquals(404, get("/movies/" + saved.getId()).statusCode(), "Фильм должен быть удалён");
    }

    @Test
    @DisplayName("DELETE /movies/{id}: несуществующий фильм возвращает 404")
    void deleteMovieWhenNotFoundReturnsNotFound() throws Exception {
        HttpResponse<String> resp = delete("/movies/999");

        assertEquals(404, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("DELETE /movies/{id}: нечисловой id возвращает 400")
    void deleteMovieWhenIdIsNotNumberReturnsBadRequest() throws Exception {
        HttpResponse<String> resp = delete("/movies/abc");

        assertEquals(400, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("GET /movies?year=: возвращает фильмы указанного года")
    void getMoviesWithYearFilterReturnsMoviesOfThatYear() throws Exception {
        store.add("Матрица", 1999);
        store.add("Бойцовский клуб", 1999);
        store.add("Интерстеллар", 2014);

        HttpResponse<String> resp = get("/movies?year=1999");

        assertEquals(200, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        List<Movie> movies = GSON.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(movie -> movie.getYear() == 1999));
    }

    @Test
    @DisplayName("GET /movies?year=: нет совпадений возвращает пустой массив")
    void getMoviesWithYearFilterWithoutMatchesReturnsEmptyArray() throws Exception {
        store.add("Матрица", 1999);

        HttpResponse<String> resp = get("/movies?year=2000");

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim());
    }

    @Test
    @DisplayName("GET /movies?year=: нечисловой year возвращает 400")
    void getMoviesWithNonNumericYearReturnsBadRequest() throws Exception {
        HttpResponse<String> resp = get("/movies?year=abc");

        assertEquals(400, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("GET /movies: неизвестный параметр запроса возвращает 400")
    void getMoviesWithUnknownQueryParameterReturnsBadRequest() throws Exception {
        assertEquals(400, get("/movies?genre=drama").statusCode());
    }

    @Test
    @DisplayName("/movies: неподдерживаемый метод возвращает 405")
    void unsupportedMethodOnCollectionReturnsMethodNotAllowed() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(405, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
        error(resp);
    }

    @Test
    @DisplayName("/movies/{id}: неподдерживаемый метод возвращает 405")
    void unsupportedMethodOnItemReturnsMethodNotAllowed() throws Exception {
        Movie saved = store.add("Матрица", 1999);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + saved.getId()))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(405, resp.statusCode());
        assertEquals(CT_JSON, contentType(resp));
    }
}
