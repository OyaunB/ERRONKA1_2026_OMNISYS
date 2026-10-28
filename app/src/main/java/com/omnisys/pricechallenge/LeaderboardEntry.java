package com.omnisys.pricechallenge;

public class LeaderboardEntry {

    public final long userId;
    public final String username;
    public final int bestScore;
    public final long achievedAt;

    public LeaderboardEntry(
            long userId,
            String username,
            int bestScore,
            long achievedAt) {

        this.userId = userId;
        this.username = username;
        this.bestScore = bestScore;
        this.achievedAt = achievedAt;
    }
}