package com.gepardec.impl.service;

import com.gepardec.core.repository.GameRepository;
import com.gepardec.core.repository.MatchRepository;
import com.gepardec.core.repository.UserRepository;
import com.gepardec.core.services.StatisticsService;
import com.gepardec.model.HeadToHead;
import com.gepardec.model.Match;
import com.gepardec.model.MatchOutcome;
import com.gepardec.model.PlayerForm;
import com.gepardec.model.PlayerGameStats;
import com.gepardec.model.Streak;
import com.gepardec.model.User;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Computes read-only player statistics from stored matches.
 * <p>
 * A match result is derived from the persisted placement at the same index as
 * each participant. Equal placements represent a draw. A match only counts for
 * statistics if it has at least two participants and a placement for each of
 * them; all other matches are excluded and reported via excludedMatches.
 */
@ApplicationScoped
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Inject
    private MatchRepository matchRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private GameRepository gameRepository;

    /**
     * Computes the specified user's game statistics from their recorded matches.
     *
     * @param userToken the user's token
     * @param gameToken the game's token
     * @return the user's statistics, or an empty optional if the user or game is unknown
     */
    @Override
    public Optional<PlayerGameStats> getPlayerGameStats(String userToken, String gameToken) {
        if (userOrGameMissing(userToken, gameToken)) {
            return Optional.empty();
        }

        logger.info("Computing stats for userToken %s and gameToken %s".formatted(userToken, gameToken));

        List<Match> matches = findMatchesNewestFirst(userToken, gameToken);
        List<MatchOutcome> outcomes = outcomesNewestFirst(matches, userToken);

        return Optional.of(
                buildPlayerGameStats(userToken, gameToken, outcomes, countMatchesWithoutResult(matches)));
    }

    /**
     * Retrieves a player's recent match form for a game.
     *
     * @param userToken  the player's user token
     * @param gameToken  the game's token
     * @param maxResults the maximum number of valid outcomes to include
     * @return the player's form, or an empty optional if the user or game is unknown
     */
    @Override
    public Optional<PlayerForm> getPlayerForm(String userToken, String gameToken, int maxResults) {
        if (userOrGameMissing(userToken, gameToken)) {
            return Optional.empty();
        }

        logger.info("Computing form for userToken %s and gameToken %s with maxResults %s"
                .formatted(userToken, gameToken, maxResults));

        List<Match> matches = findMatchesNewestFirst(userToken, gameToken);
        List<MatchOutcome> outcomes = outcomesNewestFirst(matches, userToken).stream()
                .limit(Math.max(0, maxResults))
                .toList();

        return Optional.of(
                new PlayerForm(userToken, gameToken, outcomes, countMatchesWithoutResult(matches)));
    }

    /**
     * Computes the head-to-head statistics between two users for a game.
     *
     * @param firstUserToken  the token identifying the first user
     * @param secondUserToken the token identifying the second user
     * @param gameToken       the token identifying the game
     * @return the head-to-head statistics, or an empty optional if either user or the game is unknown
     */
    @Override
    public Optional<HeadToHead> getHeadToHead(String firstUserToken, String secondUserToken, String gameToken) {
        if (userOrGameMissing(firstUserToken, gameToken)
                || userRepository.findUserByToken(secondUserToken).isEmpty()) {
            return Optional.empty();
        }

        logger.info("Computing head-to-head for userTokens %s and %s and gameToken %s"
                .formatted(firstUserToken, secondUserToken, gameToken));

        List<Match> mutualMatches = findMatchesNewestFirst(firstUserToken, gameToken).stream()
                .filter(match -> participantIndexOf(match, secondUserToken).isPresent())
                .toList();

        List<PlacementPair> placements = mutualMatches.stream()
                .filter(StatisticsServiceImpl::hasStoredResult)
                .map(match -> new PlacementPair(
                        placementOf(match, firstUserToken).orElseThrow(),
                        placementOf(match, secondUserToken).orElseThrow()))
                .toList();

        return Optional.of(buildHeadToHead(gameToken, firstUserToken, secondUserToken, placements,
                countMatchesWithoutResult(mutualMatches)));
    }

    /**
     * Builds aggregate game statistics for a player from their match outcomes.
     *
     * @param userToken        the player's user token
     * @param gameToken        the game's token
     * @param outcomesNewestFirst the player's outcomes ordered from newest to oldest
     * @param excludedMatches  the number of matches excluded from the statistics
     * @return the player's aggregated game statistics
     */
    PlayerGameStats buildPlayerGameStats(String userToken, String gameToken,
                                         List<MatchOutcome> outcomesNewestFirst, long excludedMatches) {
        long wins = outcomesNewestFirst.stream().filter(MatchOutcome.WIN::equals).count();
        long draws = outcomesNewestFirst.stream().filter(MatchOutcome.DRAW::equals).count();
        long losses = outcomesNewestFirst.stream().filter(MatchOutcome.LOSS::equals).count();
        long played = outcomesNewestFirst.size();

        return new PlayerGameStats(userToken, gameToken, played, wins, draws, losses,
                played == 0 ? 0.0 : (double) wins / played,
                currentStreak(outcomesNewestFirst),
                longestWinStreak(outcomesNewestFirst),
                excludedMatches);
    }

    /**
     * Builds a head-to-head summary from the players' placement pairs.
     *
     * @param gameToken         the game identifier
     * @param firstUserToken    the first player's identifier
     * @param secondUserToken   the second player's identifier
     * @param placements        placement pairs from matches involving both players
     * @param excludedMatches   the number of matches without a valid result
     * @return                  the aggregated head-to-head statistics
     */
    HeadToHead buildHeadToHead(String gameToken, String firstUserToken, String secondUserToken,
                               List<PlacementPair> placements, long excludedMatches) {
        long firstUserWins = placements.stream()
                .filter(pair -> pair.firstPlacement() < pair.secondPlacement()).count();
        long secondUserWins = placements.stream()
                .filter(pair -> pair.secondPlacement() < pair.firstPlacement()).count();
        long draws = placements.size() - firstUserWins - secondUserWins;

        return new HeadToHead(gameToken, firstUserToken, secondUserToken, placements.size(),
                firstUserWins, secondUserWins, draws, excludedMatches);
    }

    /**
     * Determines whether the specified user or game cannot be found.
     *
     * @param userToken the user's token
     * @param gameToken the game's token
     * @return {@code true} if either the user or game is missing, {@code false} otherwise
     */
    private boolean userOrGameMissing(String userToken, String gameToken) {
        return userRepository.findUserByToken(userToken).isEmpty()
                || gameRepository.findGameByToken(gameToken).isEmpty();
    }

    /**
     * Finds the user's matches for a game, ordered from newest to oldest.
     *
     * @param userToken the user's token
     * @param gameToken the game's token
     * @return the matching matches in newest-first order
     */
    private List<Match> findMatchesNewestFirst(String userToken, String gameToken) {
        return matchRepository.findAllMatchesOrFilteredByGameTokenAndUserToken(gameToken, userToken,
                PageRequest.ofPage(1L, Integer.MAX_VALUE, true));
    }

    /**
     * Converts eligible matches into outcomes for the specified user, preserving newest-first order.
     *
     * @param matchesNewestFirst matches ordered from newest to oldest
     * @param userToken          token identifying the user whose outcomes are calculated
     * @return the user's outcomes for matches with at least two participants
     */
    private static List<MatchOutcome> outcomesNewestFirst(List<Match> matchesNewestFirst, String userToken) {
        return matchesNewestFirst.stream()
                .filter(StatisticsServiceImpl::hasStoredResult)
                .map(match -> outcomeFor(match, userToken))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Counts matches that do not contain a stored result.
     *
     * @param matches the matches to inspect
     * @return the number of matches with fewer than two participants
     */
    private static long countMatchesWithoutResult(List<Match> matches) {
        return matches.stream().filter(match -> !hasStoredResult(match)).count();
    }

    /**
     * Determines whether a match contains enough participants to produce a result.
     *
     * @param match the match to evaluate
     * @return {@code true} if the match has at least two users, {@code false} otherwise
     */
    private static boolean hasStoredResult(Match match) {
        return match.getUsers() != null
                && match.getUsers().size() >= 2
                && match.getPlacements() != null
                && match.getPlacements().size() == match.getUsers().size()
                && match.getPlacements().stream().allMatch(Objects::nonNull);
    }

    /**
     * Determines the specified user's outcome in a match based on their placement.
     *
     * @param match    the match to evaluate
     * @param userToken the user whose outcome is being determined
     * @return the user's win or loss, or an empty result when the user did not participate
     */
    private static Optional<MatchOutcome> outcomeFor(Match match, String userToken) {
        OptionalInt participantIndex = participantIndexOf(match, userToken);
        if (participantIndex.isEmpty()) {
            return Optional.empty();
        }

        int placement = match.getPlacements().get(participantIndex.getAsInt());
        int bestPlacement = match.getPlacements().stream().mapToInt(Integer::intValue).min().orElseThrow();
        if (placement != bestPlacement) {
            return Optional.of(MatchOutcome.LOSS);
        }

        long playersAtBestPlacement = match.getPlacements().stream()
                .filter(value -> value == bestPlacement)
                .count();
        return Optional.of(playersAtBestPlacement > 1 ? MatchOutcome.DRAW : MatchOutcome.WIN);
    }

    /**
     * Finds a user's zero-based placement in a match.
     *
     * @param match    the match whose participants are searched
     * @param userToken the user's token
     * @return the user's zero-based placement, or an empty result if the user or participant list is absent
     */
    private static OptionalInt placementOf(Match match, String userToken) {
        OptionalInt participantIndex = participantIndexOf(match, userToken);
        if (participantIndex.isEmpty() || !hasStoredResult(match)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(match.getPlacements().get(participantIndex.getAsInt()));
    }

    private static OptionalInt participantIndexOf(Match match, String userToken) {
        List<User> users = match.getUsers();
        if (users == null) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < users.size(); i++) {
            if (userToken.equals(users.get(i).getToken())) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Determines the consecutive outcome streak beginning with the newest result.
     *
     * @param outcomesNewestFirst match outcomes ordered from newest to oldest
     * @return the current streak, or {@code null} when no outcomes are provided
     */
    private static Streak currentStreak(List<MatchOutcome> outcomesNewestFirst) {
        if (outcomesNewestFirst.isEmpty()) {
            return null;
        }
        MatchOutcome type = outcomesNewestFirst.getFirst();
        int length = 0;
        for (MatchOutcome outcome : outcomesNewestFirst) {
            if (outcome != type) {
                break;
            }
            length++;
        }
        return new Streak(type, length);
    }

    /**
     * Determines the longest consecutive sequence of wins in the outcomes.
     *
     * @param outcomes the outcomes to evaluate in sequence
     * @return the maximum number of consecutive wins
     */
    private static int longestWinStreak(List<MatchOutcome> outcomes) {
        int longest = 0;
        int current = 0;
        for (MatchOutcome outcome : outcomes) {
            current = outcome == MatchOutcome.WIN ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    record PlacementPair(int firstPlacement, int secondPlacement) {
    }
}
