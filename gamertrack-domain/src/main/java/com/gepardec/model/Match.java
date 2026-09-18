package com.gepardec.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class Match {

    private Long id;
    private String token;
    @NotNull(message = "Game must not be null")
    private Game game;
    @NotEmpty(message = "User List must not be null or Empty")
    private List<User> users;
    private List<Integer> placements;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    public Match() {
    }

    public Match(Long id, String token, LocalDateTime createdOn, LocalDateTime updatedOn, Game game, List<User> users) {
        this(id, token, createdOn, updatedOn, game, users, defaultPlacements(users));
    }

    public Match(Long id, String token, LocalDateTime createdOn, LocalDateTime updatedOn, Game game,
                 List<User> users, List<Integer> placements) {
        this.id = id;
        this.token = token;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.game = game;
        this.users = users;
        this.placements = placements;
    }

    public Match(Long id, String token, Game game, List<User> users) {
        this(id, token, game, users, defaultPlacements(users));
    }

    public Match(Long id, String token, Game game, List<User> users, List<Integer> placements) {
        this.id = id;
        this.token = token;
        this.game = game;
        this.users = users;
        this.placements = placements;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Integer> getPlacements() {
        return placements;
    }

    public void setPlacements(List<Integer> placements) {
        this.placements = placements;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", key='" + token + '\'' +
                ", game=" + game +
                ", users=" + users +
                ", placements=" + placements +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return Objects.equals(id, match.id) && Objects.equals(token, match.token) && Objects.equals(game, match.game)
                && Objects.equals(users, match.users) && Objects.equals(placements, match.placements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token, game, users, placements);
    }

    private static List<Integer> defaultPlacements(List<User> users) {
        return users == null ? List.of() : IntStream.range(0, users.size()).boxed().toList();
    }
}
