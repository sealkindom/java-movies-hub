package ru.practicum.moviehub.api;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MovieValidator {
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    public List<String> validate(MovieRequest request) {
        List<String> details = new ArrayList<>();
        String title = request.getTitle();
        Integer year = request.getYear();

        if (title == null || title.isBlank()) {
            details.add("название не должно быть пустым");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            details.add("название не должно быть длиннее " + MAX_TITLE_LENGTH + " символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (year == null || year < MIN_YEAR || year > maxYear) {
            details.add("год должен быть между " + MIN_YEAR + " и " + maxYear);
        }
        return details;
    }
}
