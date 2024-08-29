package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

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

/** Бот, использующий классический алгоритм минимакс в рамках игрового состояния movement. */
public class MultiThreadMinimaxBot extends MovementBot {
    private static final Logger logger = LoggerFactory.getLogger(MultiThreadMinimaxBot.class);
    private static final double MAX_COST = GameStateEvaluator.MAX_COST;
    private static final double MIN_COST = GameStateEvaluator.MIN_COST;
    private final GameStateEvaluator gameStateEvaluator;
    private final int maxDepth;
    private final ForkJoinPool forkJoinPool;
    private PlayerType maximizingPlayerType;

    public MultiThreadMinimaxBot(int maxDepth) {
        super(new TreeAnalyzer());
        this.maxDepth = maxDepth;
        this.gameStateEvaluator = new BaseEvaluator();
        this.forkJoinPool = new ForkJoinPool();
    }

    @Override
    public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
        maximizingPlayerType = gameState.getCurrentPlayer();
        treeAnalyzer.startMoveStopWatch();
        EventScore result = forkJoinPool.invoke(new MinimaxTask(new MoveStateProbability(gameState.getCopy(),1, null), maxDepth, true));
        treeAnalyzer.endMoveStopWatch();
        return (MakeMoveEvent) result.getEvent();
    }

    private class MinimaxTask extends RecursiveTask<EventScore> {
        private final GameState gameState;
        private final int depth;
        private final MoveStateProbability moveStateProbability;
        private boolean maximizing;

        public MinimaxTask(MoveStateProbability moveStateProbability, int depth,  boolean maximizing) {
            gameState = moveStateProbability.getGameState();
            this.depth = depth;
            this.moveStateProbability = moveStateProbability;
            this.maximizing = maximizing;
        }

        /**
         * Метод, исполняющий обычный алгоритм минимакс.
         *
         * @return ивент и его оценку.
         */
        @Override
        protected EventScore compute() {
            treeAnalyzer.incrementNodesCount();
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

            return maximizing ? maximize(gameState, depth, possibleMoves) : minimize(gameState, depth, possibleMoves);
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
                List<MoveStateProbability> possibleStates = collectPossibleStates(gameState, possibleMoves);
                List<MinimaxTask> tasks = new ArrayList<>();
                for (MoveStateProbability state : possibleStates) {
                    MinimaxTask task = new MinimaxTask(state, depth - 1, true);
                    tasks.add(task);
                    task.fork();
                }
                for (MinimaxTask task : tasks) {
                    EventScore result = task.join();
                    result.setScore(result.getScore() * task.moveStateProbability.getProbability());
                    if (result.getScore() > bestResult.getScore()) {
                        bestResult = new EventScore(task.moveStateProbability.getLastMove(), result.getScore());
                    }
                }
            } catch (GameException e) {
                logger.error("Ошибка в применении хода к игровому состоянию!", e);
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
                List<MoveStateProbability> possibleStates = collectPossibleStates(gameState, possibleMoves);
                List<MinimaxTask> tasks = new ArrayList<>();
                for (MoveStateProbability state : possibleStates) {
                    MinimaxTask task = new MinimaxTask(state, depth - 1, false);
                    tasks.add(task);
                    task.fork();
                }
                for (MinimaxTask task : tasks) {
                    EventScore result = task.join();
                    result.setScore(result.getScore() * task.moveStateProbability.getProbability());
                    if (result.getScore() < bestResult.getScore()) {
                        bestResult = new EventScore(task.moveStateProbability.getLastMove(), result.getScore());
                    }
                }
            } catch (GameException e) {
                logger.error("Ошибка в применении хода к игровому состоянию!", e);
            }
            return bestResult;
        }

    }
}