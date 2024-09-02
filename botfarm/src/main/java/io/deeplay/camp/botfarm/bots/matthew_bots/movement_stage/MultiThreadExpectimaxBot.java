package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import static io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBotUtil.collectPossibleStates;
import static io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBotUtil.removeUnnecessaryMoves;

import io.deeplay.camp.botfarm.bots.matthew_bots.TreeAnalyzer;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.BaseEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.EventScore;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.GameStateEvaluator;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiThreadExpectimaxBot extends MovementBot {
  private static final Logger logger = LoggerFactory.getLogger(MultiThreadExpectimaxBot.class);

  /** Минимальная оценка игрового состояния. */
  private static final double MIN_COST = GameStateEvaluator.MIN_COST;

  /** Оценщик игровых состояний. */
  private final GameStateEvaluator gameStateEvaluator;

  /** Максимальная глубина. */
  private final int maxDepth;

  /** Максимизирующий игрок, т.е. сторона, за которую играет бот. */
  private PlayerType maximizingPlayerType;

  private final ForkJoinPool forkJoinPool;

  /**
   * Конструктор.
   *
   * @param maxDepth Максимальная глубина дерева.
   */
  public MultiThreadExpectimaxBot(int maxDepth) {
    super(new TreeAnalyzer());
    this.maxDepth = maxDepth;
    this.gameStateEvaluator = new BaseEvaluator();
    treeAnalyzer = new TreeAnalyzer();
    forkJoinPool = new ForkJoinPool();
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
    ExpectimaxTask expectimaxTask =
        new ExpectimaxTask(new State(gameState.getCopy(), 1, null), maxDepth, true);
    EventScore result = forkJoinPool.invoke(expectimaxTask);
    treeAnalyzer.endMoveStopWatch();
    return (MakeMoveEvent) result.getEvent();
  }

  public class ExpectimaxTask extends RecursiveTask<EventScore> {
    private final GameState gameState;
    private final int depth;
    private final MakeMoveEvent lastMove;
    private final double probability;
    private boolean maximizing;

    public ExpectimaxTask(
            io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State state, int depth, boolean maximizing) {
      gameState = state.getGameState();
      this.depth = depth;
      lastMove = (MakeMoveEvent)state.getLastMove();
      probability = state.getProbability();
      this.maximizing = maximizing;
    }

    /**
     * Метод, исполняющий алгоритм экспектимакс.
     *
     * @return ивент и его оценку.
     */
    @Override
    protected EventScore compute() {
      treeAnalyzer.incrementNodesCount();
      // Базовый случай (Дошли до ограничения глубины или конца игры)
      if (depth == 0 || gameState.getGameStage() == GameStage.ENDED) {
        return new EventScore(null, gameStateEvaluator.evaluate(gameState, maximizingPlayerType));
      }

      List<MakeMoveEvent> possibleMoves = gameState.getPossibleMoves();
      if (possibleMoves.isEmpty()) {
        if (depth == maxDepth) {
          return new EventScore(null, maximizing ? MIN_COST : 0);
        }
        gameState.changeCurrentPlayer();
        possibleMoves = gameState.getPossibleMoves();
        maximizing = !maximizing;
      }
      removeUnnecessaryMoves(possibleMoves);

      return maximizing
          ? maximize(gameState, depth, possibleMoves)
          : expect(gameState, depth, possibleMoves);
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
        List<io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State> possibleStates = collectPossibleStates(gameState, possibleMoves);
        List<ExpectimaxTask> tasks = new ArrayList<>();
        for (io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State state : possibleStates) {
          ExpectimaxTask task = new ExpectimaxTask(state, depth - 1, true);
          tasks.add(task);
          task.fork();
        }
        for (ExpectimaxTask task : tasks) {
          EventScore result = task.join();
          result.setScore(result.getScore() * task.probability);
          if (result.getScore() > bestResult.getScore()) {
            bestResult = new EventScore(task.lastMove, result.getScore());
          }
        }
      } catch (GameException e) {
        logger.error("Ошибка в применении хода к игровому состоянию!", e);
      }
      return bestResult;
    }

    /**
     * Метод, отвечающий за игрока оппонента.
     *
     * @param gameState Игровое состояние.
     * @param depth Максимальная глубина.
     * @param possibleMoves Возможные ходы.
     * @return ивент и его оценку.
     */
    private EventScore expect(GameState gameState, int depth, List<MakeMoveEvent> possibleMoves) {
      double expectedValue = 0;
      EventScore bestResult = new EventScore(null, 0);
      try {
        List<io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State> possibleStates = collectPossibleStates(gameState, possibleMoves);
        for (io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State state : possibleStates) {
          ExpectimaxTask task = new ExpectimaxTask(state, depth - 1, false);
          EventScore result = task.compute();
          expectedValue += result.getScore() * state.getProbability();
        }
        expectedValue /= possibleStates.size();
        bestResult.setScore(expectedValue);
      } catch (GameException e) {
        logger.error("Ошибка в применении хода к игровому состоянию!", e);
      }
      return bestResult;
    }
  }
}
