package com.gepardec.rest.model.dto;

import com.gepardec.model.HeadToHead;

public record HeadToHeadRestDto(String gameToken,
                                String firstUserToken,
                                String secondUserToken,
                                long matchesPlayed,
                                long firstUserWins,
                                long secondUserWins,
                                long draws,
                                long excludedMatches) {

    /**
     * Creates a REST DTO from head-to-head match statistics.
     *
     * @param headToHead the head-to-head statistics to copy
     */
    public HeadToHeadRestDto(HeadToHead headToHead) {
        this(headToHead.gameToken(), headToHead.firstUserToken(), headToHead.secondUserToken(),
                headToHead.matchesPlayed(), headToHead.firstUserWins(), headToHead.secondUserWins(),
                headToHead.draws(), headToHead.excludedMatches());
    }
}
