package com.javaisland.score;

public final class ScoreCalculator {
  public static final int HINT_PENALTY = 100;
  public static final int ATTEMPT_PENALTY = 50;
  public static final int MIN_SCORE = 100;

  public static int taskScore(int levelMaxScore, int hintsUsed, int attempts) {
    int extraAttempts = Math.max(0, attempts - 1);
    int raw = levelMaxScore - (hintsUsed * HINT_PENALTY) - (extraAttempts * ATTEMPT_PENALTY);
    return Math.max(MIN_SCORE, raw);
  }

  private ScoreCalculator() {}
}