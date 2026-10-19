package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    public static final int DEFAULT_PORT = 8080;

    private final HttpServer server;

    public MoviesServer(MoviesStore store, int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
        server.createContext(MoviesHandler.MOVIES_PATH, new MoviesHandler(store));
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(1);
        System.out.println("Сервер остановлен");
    }
}
