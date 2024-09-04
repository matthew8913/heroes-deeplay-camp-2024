package io.deeplay.camp.botfarm.bots.matthew_bots.evaluate;

import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.RandomMovementBot;
import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;

public class MonteCarloEvaluator implements GameStateEvaluator {
  BaseEvaluator evaluator;
  int gamesCount;

  public MonteCarloEvaluator(int gameCount) {
    this.gamesCount = gameCount;
    evaluator = new BaseEvaluator();
  }

  @Override
  public double evaluate(GameState gameState, PlayerType maximizingPlayer) {
    if (getAliveUnitsSizeOfPlayer(gameState, maximizingPlayer) >= 3) {
      return evaluator.evaluate(gameState, maximizingPlayer);
    } else {
      return monteCarloEvaluate(gameState, maximizingPlayer);
    }
  }

  private int getAliveUnitsSizeOfPlayer(GameState gameState, PlayerType maximizingPlayer) {
    int startRow = maximizingPlayer == PlayerType.FIRST_PLAYER ? 0 : Board.ROWS / 2;
    int endRow = maximizingPlayer == PlayerType.FIRST_PLAYER ? Board.ROWS / 2 : Board.ROWS;
    int unitsNum = 0;
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = startRow; row < endRow; row++) {
        if (gameState.getBoard().getUnit(col, row).isAlive()) {
          unitsNum++;
        }
      }
    }
    return unitsNum;
  }

  public double monteCarloEvaluate(GameState gameState, PlayerType maximizingPlayer) {
    int botWinCount = 0;
    RandomMovementBot randomMovementBot = new RandomMovementBot();
    for (int i = 0; i < gamesCount; i++) {
      GameState gameStateCopy = gameState.getCopy();
      while (gameStateCopy.getGameStage() != GameStage.ENDED) {
        MakeMoveEvent move;
        if (gameStateCopy.getCurrentPlayer() == PlayerType.FIRST_PLAYER) {
          move = randomMovementBot.generateMakeMoveEvent(gameStateCopy);
        } else {
          move = randomMovementBot.generateMakeMoveEvent(gameStateCopy);
        }
        if (move != null) {
          try {
            gameStateCopy.makeMove(move);
          } catch (GameException e) {
            System.out.println(e.getMessage());
          }
        } else {
          gameStateCopy.changeCurrentPlayer();
        }
      }
      if (gameState.getWinner() == maximizingPlayer) {
        botWinCount++;
      }
    }
    return (double) botWinCount / gamesCount;
  }
}
