package com.gepardec.rest.impl;

import com.gepardec.core.services.StatisticsService;
import com.gepardec.rest.api.StatsResource;
import com.gepardec.rest.model.dto.ErrorRestDto;
import com.gepardec.rest.model.dto.HeadToHeadRestDto;
import com.gepardec.rest.model.dto.PlayerFormRestDto;
import com.gepardec.rest.model.dto.PlayerGameStatsRestDto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class StatsResourceImpl implements StatsResource {

    private final Logger logger = LoggerFactory.getLogger(StatsResourceImpl.class);

    @Inject
    private StatisticsService statisticsService;

    /**
     * Retrieves a player's statistics for a game.
     *
     * @param userToken the token identifying the player
     * @param gameToken the token identifying the game
     * @return a successful response containing the player's statistics, or a 404 response if the user or game is not found
     */
    @Override
    public Response getPlayerGameStats(String userToken, String gameToken) {
        logger.info("Getting stats for userToken: %s and gameToken: %s".formatted(userToken, gameToken));

        return statisticsService.getPlayerGameStats(userToken, gameToken)
                .map(PlayerGameStatsRestDto::new)
                .map(Response::ok)
                .orElseGet(() -> unknownUserOrGame())
                .build();
    }

    /**
     * Retrieves a player's recent form for a game.
     *
     * @param userToken the player's token
     * @param gameToken the game's token
     * @param limit the maximum number of form results to include
     * @return an HTTP 200 response with the player's form, an HTTP 400 response
     *         when the limit is not positive, or an HTTP 404 response when the
     *         user or game is not found
     */
    @Override
    public Response getPlayerForm(String userToken, String gameToken, int limit) {
        logger.info("Getting form for userToken: %s and gameToken: %s with limit: %s"
                .formatted(userToken, gameToken, limit));

        if (limit <= 0) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ErrorRestDto("limit must be greater than 0"))
                    .build();
        }

        return statisticsService.getPlayerForm(userToken, gameToken, Math.min(limit, MAX_FORM_RESULTS))
                .map(PlayerFormRestDto::new)
                .map(Response::ok)
                .orElseGet(() -> unknownUserOrGame())
                .build();
    }

    /**
     * Retrieves head-to-head statistics for two different users in a game.
     *
     * @param firstUserToken  the token identifying the first user
     * @param secondUserToken the token identifying the second user
     * @param gameToken       the token identifying the game
     * @return an HTTP 200 response containing the statistics, or an HTTP 400 or 404 response when the request is invalid or data is unavailable
     */
    @Override
    public Response getHeadToHead(String firstUserToken, String secondUserToken, String gameToken) {
        logger.info("Getting head-to-head for userTokens: %s and %s and gameToken: %s"
                .formatted(firstUserToken, secondUserToken, gameToken));

        if (firstUserToken == null || firstUserToken.isBlank()
                || secondUserToken == null || secondUserToken.isBlank()
                || gameToken == null || gameToken.isBlank()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ErrorRestDto(
                            "firstUserToken, secondUserToken and gameToken must be provided"))
                    .build();
        }

        if (firstUserToken.equals(secondUserToken)) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ErrorRestDto("firstUserToken and secondUserToken must be different"))
                    .build();
        }

        return statisticsService.getHeadToHead(firstUserToken, secondUserToken, gameToken)
                .map(HeadToHeadRestDto::new)
                .map(Response::ok)
                .orElseGet(() -> unknownUserOrGame())
                .build();
    }

    /**
     * Creates a not-found response for an unknown user or game.
     *
     * @return a response builder configured with HTTP status 404 and an error entity
     */
    private static Response.ResponseBuilder unknownUserOrGame() {
        return Response.status(Status.NOT_FOUND)
                .entity(new ErrorRestDto("User or game not found"));
    }
}
