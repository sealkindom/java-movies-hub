package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new LinkedHashMap<>();
    private int nextId = 1;

    public Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean remove(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}
