package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import static io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBotUtil.BAD_BRANCH_PROBABILITY;
import static io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBotUtil.removeUnnecessaryMoves;

import io.deeplay.camp.botfarm.bots.matthew_bots.TreeAnalyzer;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.BaseEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.EventScore;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.GameStateEvaluator;
import io.deeplay.camp.game.entities.StateChance;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Бот, использующий алгоритм минимакс с альфа-бета отсечениями в рамках игрового состояния
 * movement.
 */
public class AlphaBetaMinimaxBot extends MovementBot {
  private static final Logger logger = LoggerFactory.getLogger(AlphaBetaMinimaxBot.class);

  /** Максимальная оценка игрового состояния. */
  private static final double MAX_COST = GameStateEvaluator.MAX_COST;

  /** Минимальная оценка игрового состояния. */
  private static final double MIN_COST = GameStateEvaluator.MIN_COST;

  /** Оценщик игровых состояний. */
  private final GameStateEvaluator gameStateEvaluator;

  /** Максимизирующий игрок, т.е. сторона, за которую играет бот. */
  private PlayerType maximizingPlayerType;

  /**
   * Конструктор.
   *
   * @param maxDepth Максимальная глубина дерева.
   */
  public AlphaBetaMinimaxBot(int maxDepth) {
    super(new TreeAnalyzer(), maxDepth);
    gameStateEvaluator = new BaseEvaluator();
  }

  /**
   * Метод, генерирующий ход для текущего игрока игрового состояния.
   *
   * @param gameState Игровое состояние.
   * @return ивент с ходом.
   */
  @Override
  public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
    maximizingPlayerType = gameState.getCurrentPlayer();
    treeAnalyzer.startMoveStopWatch();
    EventScore result = minimax(gameState.getCopy(), maxDepth, MIN_COST, MAX_COST, true);
    treeAnalyzer.endMoveStopWatch();
    return (MakeMoveEvent) result.getEvent();
  }

  /**
   * Метод, исполняющий алгоритм минимакс с альфа-бета отсечениями.
   *
   * @param gameState Игровое состояние.
   * @param depth Максимальная глубина.
   * @param alpha Значение альфа.
   * @param beta Значение бета.
   * @param maximizing Флаг, обозначающий максимизирующего игрока.
   * @return ивент и его оценку.
   */
  private EventScore minimax(
      GameState gameState, int depth, double alpha, double beta, boolean maximizing) {
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

    removeUnnecessaryMoves(possibleMoves);

    return maximizing
        ? maximize(gameState, depth, alpha, beta, possibleMoves)
        : minimize(gameState, depth, alpha, beta, possibleMoves);
  }

  /**
   * Метод, отвечающий за максимизирующего игрока.
   *
   * @param gameState Игровое состояние.
   * @param depth Максимальная глубина.
   * @param alpha Значение альфа.
   * @param beta Значение бета.
   * @param possibleMoves Возможные ходы.
   * @return ивент и его оценку.
   */
  private EventScore maximize(
      GameState gameState,
      int depth,
      double alpha,
      double beta,
      List<MakeMoveEvent> possibleMoves) {
    EventScore bestResult = new EventScore(null, MIN_COST);
    try {
      for (MakeMoveEvent move : possibleMoves) {
        List<StateChance> possibleStates = gameState.getPossibleState(move);
        for (StateChance stateChance : possibleStates) {
          if (stateChance.chance() < BAD_BRANCH_PROBABILITY) {
            continue;
          }
          EventScore result = minimax(stateChance.gameState(), depth - 1, alpha, beta, true);
          result.setScore(result.getScore() * stateChance.chance());
          if (result.getScore() > bestResult.getScore()) {
            bestResult = new EventScore(move, result.getScore());
          }
          alpha = Math.max(alpha, bestResult.getScore());
          if (beta <= alpha) {
            break;
          }
        }
        if (beta <= alpha) {
          break;
        }
      }
    } catch (GameException e) {
      logger.error("Ошибка в применении хода к игровому состоянию!");
    }
    return bestResult;
  }

  /**
   * Метод, отвечающий за минимизирующего игрока.
   *
   * @param gameState Игровое состояние.
   * @param depth Максимальная глубина.
   * @param alpha Значение альфа.
   * @param beta Значение бета.
   * @param possibleMoves Возможные ходы.
   * @return ивент и его оценку.
   */
  private EventScore minimize(
      GameState gameState,
      int depth,
      double alpha,
      double beta,
      List<MakeMoveEvent> possibleMoves) {
    EventScore bestResult = new EventScore(null, MAX_COST);
    try {
      for (MakeMoveEvent move : possibleMoves) {
        List<StateChance> possibleStates = gameState.getPossibleState(move);
        for (StateChance stateChance : possibleStates) {
          if (stateChance.chance() < BAD_BRANCH_PROBABILITY) {
            continue;
          }
          EventScore result = minimax(stateChance.gameState(), depth - 1, alpha, beta, false);
          result.setScore(result.getScore() * stateChance.chance());
          if (result.getScore() < bestResult.getScore()) {
            bestResult = new EventScore(move, result.getScore());
          }
          beta = Math.min(beta, bestResult.getScore());
          if (beta <= alpha) {
            break;
          }
        }
        if (beta <= alpha) {
          break;
        }
      }
    } catch (GameException e) {
      logger.error("Ошибка в применении хода к игровому состоянию!");
    }
    return bestResult;
  }
}
