package io.deeplay.camp.botfarm.bots.matthew_bots.evaluate;

import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;

public interface GameStateEvaluator {
  double MAX_COST = 1000;
  double MIN_COST = -1000;

  double evaluate(GameState gameState, PlayerType maximizingPlayer);
}
