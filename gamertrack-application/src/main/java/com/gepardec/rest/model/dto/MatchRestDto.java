package com.gepardec.rest.model.dto;

import com.gepardec.model.Match;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public record MatchRestDto(@NotBlank String token, @NotNull String createdOn, String updatedOn, @NotNull GameRestDto game,
                           @NotNull List<UserRestDto> users, @NotNull List<Integer> placements) {

    public MatchRestDto(String token, String createdOn, String updatedOn, GameRestDto game,
                        List<UserRestDto> users) {
        this(token, createdOn, updatedOn, game, users,
                IntStream.range(0, users.size()).boxed().toList());
    }

    public MatchRestDto(Match match) {
        this(match.getToken(),
                match.getUpdatedOn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                match.getUpdatedOn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                new GameRestDto(match.getGame()),
                new ArrayList<>(
                        match.getUsers().stream()
                                .map(UserRestDto::new)
                                .toList()
                ), match.getPlacements());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchRestDto that = (MatchRestDto) o;
        return Objects.equals(game, that.game) && Objects.equals(token, that.token)
                && Objects.equals(users, that.users) && Objects.equals(placements, that.placements)
                && Objects.equals(createdOn, that.createdOn) && Objects.equals(updatedOn, that.updatedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, game, users, placements, createdOn, updatedOn);
    }

    @Override
    public String toString() {
        return "MatchRestDto{" +
                "token='" + token + '\'' +
                ", game=" + game +
                ", users=" + users +
                ", placements=" + placements +
                '}';
    }
}
