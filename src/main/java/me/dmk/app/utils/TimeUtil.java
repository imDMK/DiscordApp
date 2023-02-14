package me.dmk.app.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Created by DMK on 07.12.2022
 */

@UtilityClass
public class TimeUtil {

    public static Optional<Instant> stringToInstant(@NonNull String date) {
        if (date.isEmpty() || date.length() == 1)
            return Optional.empty();

        long difference;
        try {
            difference = Long.parseLong(date.substring(0, date.length() - 1));
        } catch (Exception exception) {
            return Optional.empty();
        }

        char charAt = date.toUpperCase().charAt(date.length() - 1);

        return switch (charAt) {
            case 'S' -> Optional.of(Instant.now().plus(difference, ChronoUnit.SECONDS));
            case 'M' -> Optional.of(Instant.now().plus(difference, ChronoUnit.MINUTES));
            case 'H' -> Optional.of(Instant.now().plus(difference, ChronoUnit.HOURS));
            case 'D' -> Optional.of(Instant.now().plus(difference, ChronoUnit.DAYS));
            default -> Optional.empty();
        };
    }
}
