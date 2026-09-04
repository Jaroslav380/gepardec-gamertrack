package com.gepardec.rest.model.dto;

import com.gepardec.model.Streak;

public record StreakRestDto(String type, int length) {

    /**
     * Converts a streak to its REST representation.
     *
     * @param streak the streak to convert; {@code null} represents no streak
     * @return the streak type and length, or {@code "NONE"} with length {@code 0} when the streak is {@code null}
     */
    public static StreakRestDto of(Streak streak) {
        return streak == null
                ? new StreakRestDto("NONE", 0)
                : new StreakRestDto(streak.type().name(), streak.length());
    }
}
