package com.gepardec.core.services;

import com.gepardec.model.HeadToHead;
import com.gepardec.model.PlayerForm;
import com.gepardec.model.PlayerGameStats;

import java.util.Optional;

public interface StatisticsService {

    /**
 * Retrieves game statistics for a player.
 *
 * @param userToken the token identifying the user
 * @param gameToken the token identifying the game
 * @return optional player game statistics, if available
 */
Optional<PlayerGameStats> getPlayerGameStats(String userToken, String gameToken);

    /**
 * Retrieves a player's form data for a game.
 *
 * @param userToken  the player's user token
 * @param gameToken  the game token
 * @param maxResults the maximum number of form results to include
 * @return the player's form data, if available
 */
Optional<PlayerForm> getPlayerForm(String userToken, String gameToken, int maxResults);

    /**
 * Retrieves head-to-head statistics for two users within a game.
 *
 * @param firstUserToken  the token identifying the first user
 * @param secondUserToken the token identifying the second user
 * @param gameToken       the token identifying the game
 * @return the available head-to-head statistics, or an empty optional if unavailable
 */
Optional<HeadToHead> getHeadToHead(String firstUserToken, String secondUserToken, String gameToken);
}
