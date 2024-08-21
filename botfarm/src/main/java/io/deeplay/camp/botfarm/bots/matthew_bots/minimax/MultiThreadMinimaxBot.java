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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import lombok.SneakyThrows;

public class MultiThreadMinimaxBot extends Bot {
  private PlayerType maximizingPlayerType;

  private final int maxDepth;
  final TreeAnalyzer treeAnalyzer;
  private static final double MAX_COST = Double.POSITIVE_INFINITY;
  private static final double MIN_COST = Double.NEGATIVE_INFINITY;

  private final GameStateEvaluator gameStateEvaluator;
  private final ForkJoinPool forkJoinPool;

  public MultiThreadMinimaxBot(int depth, GameStateEvaluator gameStateEvaluator) {
    maxDepth = depth;
    this.gameStateEvaluator = gameStateEvaluator;
    treeAnalyzer = new TreeAnalyzer();
    forkJoinPool = new ForkJoinPool();
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
    EventScore result =
        forkJoinPool.invoke(
            new MinimaxTask(gameState.getCopy(), maxDepth, MIN_COST, MAX_COST, true));
    treeAnalyzer.endMoveStopWatch();
    return (MakeMoveEvent) result.getEvent();
  }

  private class MinimaxTask extends RecursiveTask<EventScore> {
    private final GameState gameState;
    private final int depth;
    private final double alpha;
    private final double beta;
    private boolean maximizing;

    public MinimaxTask(
        GameState gameState, int depth, double alpha, double beta, boolean maximizing) {
      this.gameState = gameState;
      this.depth = depth;
      this.alpha = alpha;
      this.beta = beta;
      this.maximizing = maximizing;
    }

    @Override
    protected EventScore compute() {
      try {
        treeAnalyzer.incrementNodesCount();
        // Базовый случай (Дошли до ограничения глубины или конца игры)
        if (depth == 0 || gameState.getGameStage() == GameStage.ENDED) {
          return new EventScore(
              null, gameStateEvaluator.evaluate(gameState, maximizingPlayerType));
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

        if (maximizing) {
          return maximize(gameState, depth, alpha, beta, possibleMoves);
        } else {
          return minimize(gameState, depth, alpha, beta, possibleMoves);
        }
      } catch (GameException e) {
        throw new RuntimeException(e);
      }
    }

    private EventScore maximize(
        GameState gameState,
        int depth,
        double alpha,
        double beta,
        List<MakeMoveEvent> possibleMoves)
        throws GameException {
      EventScore bestResult = new EventScore(null, MIN_COST);
      for (MakeMoveEvent move : possibleMoves) {
        GameState newGameState = gameState.getCopy();
        newGameState.makeMove(move);
        MinimaxTask task = new MinimaxTask(newGameState, depth - 1, alpha, beta, true);
        task.fork();
        EventScore result = task.join();
        if (result.getScore() > bestResult.getScore()) {
          bestResult = new EventScore(move, result.getScore());
        }
        alpha = Math.max(alpha, bestResult.getScore());
        if (beta <= alpha) {
          break;
        }
      }
      return bestResult;
    }

    private EventScore minimize(
        GameState gameState,
        int depth,
        double alpha,
        double beta,
        List<MakeMoveEvent> possibleMoves)
        throws GameException {
      EventScore bestResult = new EventScore(null, MAX_COST);
      for (MakeMoveEvent move : possibleMoves) {
        GameState newGameState = gameState.getCopy();
        newGameState.makeMove(move);
        MinimaxTask task = new MinimaxTask(newGameState, depth - 1, alpha, beta, false);
        task.fork();
        EventScore result = task.join();
        if (result.getScore() < bestResult.getScore()) {
          bestResult = new EventScore(move, result.getScore());
        }
        beta = Math.min(beta, bestResult.getScore());
        if (beta <= alpha) {
          break; // Alpha cut-off
        }
      }
      return bestResult;
    }
  }


}
