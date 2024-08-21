package io.deeplay.camp.botfarm.bots.matthew_bots.minimax;

import io.deeplay.camp.botfarm.bots.Bot;
import io.deeplay.camp.botfarm.bots.matthew_bots.GameStateEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.TreeAnalyzer;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.util.List;
import java.util.Random;
import lombok.SneakyThrows;

public class MinimaxBot extends Bot {
  private PlayerType maximizingPlayerType;

  private final int maxDepth;
  final TreeAnalyzer treeAnalyzer;
  private static final double MAX_COST = Double.POSITIVE_INFINITY;
  private static final double MIN_COST = Double.NEGATIVE_INFINITY;

  private final GameStateEvaluator gameStateEvaluator;

  public MinimaxBot(int depth, GameStateEvaluator gameStateEvaluator) {
    maxDepth = depth;
    this.gameStateEvaluator = gameStateEvaluator;
    treeAnalyzer = new TreeAnalyzer();
  }

  @Override
  public PlaceUnitEvent generatePlaceUnitEvent(GameState gameState) {
    List<PlaceUnitEvent> placeUnitEvents = gameState.getPossiblePlaces();
    Random random = new Random();
    int randomIndex = random.nextInt(placeUnitEvents.size());
    return placeUnitEvents.get(randomIndex);
  }

  @Override
  @SneakyThrows
  public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
    maximizingPlayerType = gameState.getCurrentPlayer();
    treeAnalyzer.startMoveStopWatch();
    EventScore result = minimax(gameState.getCopy(), maxDepth, true);
    treeAnalyzer.endMoveStopWatch();
    return (MakeMoveEvent) result.getEvent();
  }

  private EventScore minimax(GameState gameState, int depth, boolean maximizing)
      throws GameException {
    treeAnalyzer.incrementNodesCount();
    // Базовый случай (Дошли до ограничения глубины или конца игры)
    if (depth == 0 || gameState.getGameStage() == GameStage.ENDED) {
      return new EventScore(null, gameStateEvaluator.evaluate(gameState, maximizingPlayerType));
    }

    List<MakeMoveEvent> possibleMoves = gameState.getPossibleMoves();
    if (possibleMoves.isEmpty()) {
      if (depth == maxDepth) {
        return new EventScore(null, maximizing ? MIN_COST : MAX_COST);
      }
      gameState.changeCurrentPlayer();
      possibleMoves = gameState.getPossibleMoves();
      maximizing = !maximizing;
    }

    return maximizing
        ? maximize(gameState, depth, possibleMoves)
        : minimize(gameState, depth, possibleMoves);
  }

  private EventScore maximize(GameState gameState, int depth, List<MakeMoveEvent> possibleMoves)
      throws GameException {
    EventScore bestResult = new EventScore(null, MIN_COST);
    for (MakeMoveEvent move : possibleMoves) {
      GameState newGameState = gameState.getCopy();
      newGameState.makeMove(move);
      EventScore result = minimax(newGameState, depth - 1, true);
      if (result.getScore() > bestResult.getScore()) {
        bestResult = new EventScore(move, result.getScore());
      }
    }
    return bestResult;
  }

  private EventScore minimize(GameState gameState, int depth, List<MakeMoveEvent> possibleMoves)
      throws GameException {
    EventScore bestResult = new EventScore(null, MAX_COST);
    for (MakeMoveEvent move : possibleMoves) {
      GameState newGameState = gameState.getCopy();
      newGameState.makeMove(move);
      EventScore result = minimax(newGameState, depth - 1, false);
      if (result.getScore() < bestResult.getScore()) {
        bestResult = new EventScore(move, result.getScore());
      }
    }
    return bestResult;
  }
}
