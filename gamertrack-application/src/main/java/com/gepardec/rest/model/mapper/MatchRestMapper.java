package com.gepardec.rest.model.mapper;

import com.gepardec.model.Match;
import com.gepardec.rest.model.command.CreateMatchCommand;
import com.gepardec.rest.model.command.UpdateMatchCommand;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.IntStream;

@ApplicationScoped
public class MatchRestMapper {

  public Match updateMatchCommandtoMatch(Long id, String token,
      UpdateMatchCommand updateMatchCommand) {
    return new Match(id, token, updateMatchCommand.game(),
        updateMatchCommand.users(), placementsOrDefault(updateMatchCommand.users(), updateMatchCommand.placements()));
  }

  public Match createMatchCommandtoMatch(CreateMatchCommand createMatchCommand) {
    return new Match(null, null, createMatchCommand.game(), createMatchCommand.users(),
        placementsOrDefault(createMatchCommand.users(), createMatchCommand.placements()));
  }

  private static List<Integer> placementsOrDefault(List<?> users, List<Integer> placements) {
    return placements == null
        ? IntStream.range(0, users.size()).boxed().toList()
        : placements;
  }
}
