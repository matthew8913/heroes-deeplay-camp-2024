package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

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

/** Бот, использующий классический алгоритм минимакс в рамках игрового состояния movement. */
public class MinimaxMovementBot extends MovementBot {
  private static final Logger logger = LoggerFactory.getLogger(MinimaxMovementBot.class);
  /** Максимальная оценка игрового состояния. */
  private static final double MAX_COST = GameStateEvaluator.MAX_COST;
  /** Минимальная оценка игрового состояния. */
  private static final double MIN_COST = GameStateEvaluator.MIN_COST;
  /** Оценщик игровых состояний. */
  private final GameStateEvaluator gameStateEvaluator;
  /** Максимальная глубина. */
  private final int maxDepth;
  /** Максимизирующий игрок, т.е. сторона, за которую играет бот. */
  private PlayerType maximizingPlayerType;

  /**
   * Конструктор.
   *
   * @param maxDepth Максимальная глубина дерева.
   */
  public MinimaxMovementBot(int maxDepth) {
    super(new TreeAnalyzer());
    this.maxDepth = maxDepth;
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
    EventScore result = minimax(gameState.getCopy(), maxDepth, true);
    treeAnalyzer.endMoveStopWatch();
    return (MakeMoveEvent) result.getEvent();
  }

  /**
   * Метод, исполняющий обычный алгоритм минимакс.
   *
   * @param gameState Игровое состояние.
   * @param depth Максимальная глубина.
   * @param maximizing Флаг, обозначающий максимизирующего игрока.
   * @return ивент и его оценку.
   */
  private EventScore minimax(GameState gameState, int depth, boolean maximizing) {
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
        ? maximize(gameState, depth, possibleMoves)
        : minimize(gameState, depth, possibleMoves);
  }

  /**
   * Метод, отвечающий за максимизирующего игрока.
   *
   * @param gameState Игровое состояние.
   * @param depth Максимальная глубина.
   * @param possibleMoves Возможные ходы.
   * @return ивент и его оценку.
   */
  private EventScore maximize(GameState gameState, int depth, List<MakeMoveEvent> possibleMoves) {
    EventScore bestResult = new EventScore(null, MIN_COST);
    try {
      for (MakeMoveEvent move : possibleMoves) {
        List<StateChance> possibleStates = gameState.getPossibleState(move);
        for (StateChance stateChance : possibleStates) {
          if(stateChance.chance()<BAD_BRANCH_PROBABILITY){
            continue;
          }
          EventScore result = minimax(stateChance.gameState(), depth - 1, true);
          result.setScore(result.getScore() * stateChance.chance());
          if (result.getScore() > bestResult.getScore()) {
            bestResult = new EventScore(move, result.getScore());
          }
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
   * @param possibleMoves Возможные ходы.
   * @return ивент и его оценку.
   */
  private EventScore minimize(GameState gameState, int depth, List<MakeMoveEvent> possibleMoves) {
    EventScore bestResult = new EventScore(null, MAX_COST);
    try {
      for (MakeMoveEvent move : possibleMoves) {
        List<StateChance> possibleStates = gameState.getPossibleState(move);
        for (StateChance stateChance : possibleStates) {
          if(stateChance.chance()<BAD_BRANCH_PROBABILITY){
            continue;
          }
          EventScore result = minimax(stateChance.gameState(), depth - 1, false);
          result.setScore(result.getScore() * stateChance.chance());
          if (result.getScore() < bestResult.getScore()) {
            bestResult = new EventScore(move, result.getScore());
          }
        }
      }
    } catch (GameException e) {
      logger.error("Ошибка в применении хода к игровому состоянию!");
    }
    return bestResult;
  }
}
