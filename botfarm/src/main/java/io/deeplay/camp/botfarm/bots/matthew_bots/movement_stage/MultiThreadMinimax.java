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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Многопоточная версия минимакс бота с альфа-бета отсечениями.
 */
public class MultiThreadMinimax extends MovementBot {
    private static final Logger logger = LoggerFactory.getLogger(MultiThreadMinimax.class);
    /**
     * Максимальная оценка игрового состояния.
     */
    private static final double MAX_COST = GameStateEvaluator.MAX_COST;
    /**
     * Минимальная оценка игрового состояния.
     */
    private static final double MIN_COST = GameStateEvaluator.MIN_COST;
    /**
     * Оценщик игровых состояний.
     */
    private final GameStateEvaluator gameStateEvaluator;
    /**
     * Максимальная глубина.
     */
    private final int maxDepth;
    private final ForkJoinPool forkJoinPool;
    /**
     * Максимизирующий игрок, т.е. сторона, за которую играет бот.
     */
    private PlayerType maximizingPlayerType;

    /**
     * Конструктор.
     *
     * @param maxDepth Максимальная глубина дерева.
     */
    public MultiThreadMinimax(int maxDepth){
        super(new TreeAnalyzer());
        this.maxDepth = maxDepth;
        this.gameStateEvaluator = new BaseEvaluator();
        treeAnalyzer = new TreeAnalyzer();
        forkJoinPool = new ForkJoinPool();
    }

    /**
     * Метод, генерирующий ход для текущего игрока игрового состояния.
     * @param gameState Игровое состояние.
     * @return ивент с ходом.
     */
    @Override
    public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
        maximizingPlayerType = gameState.getCurrentPlayer();
        treeAnalyzer.startMoveStopWatch();
        EventScore result = forkJoinPool.invoke(new MinimaxTask(gameState.getCopy(), maxDepth, MIN_COST, MAX_COST, true));
        treeAnalyzer.endMoveStopWatch();
        return (MakeMoveEvent) result.getEvent();
    }

    /**
     * Класс задачи для fjp.
     */
    private class MinimaxTask extends RecursiveTask<EventScore> {
        private final GameState gameState;
        private final int depth;
        private final Double alpha;
        private final double beta;
        private boolean maximizing;

        public MinimaxTask(GameState gameState, int depth, double alpha, double beta, boolean maximizing) {
            this.gameState = gameState;
            this.depth = depth;
            this.alpha = alpha;
            this.beta = beta;
            this.maximizing = maximizing;
        }

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
                    return new EventScore(null, maximizing ? MIN_COST : MAX_COST);
                }
                gameState.changeCurrentPlayer();
                possibleMoves = gameState.getPossibleMoves();
                maximizing = !maximizing;
            }

            return maximizing
                    ? maximize(gameState, depth, alpha, beta, possibleMoves)
                    : minimize(gameState, depth, alpha, beta, possibleMoves);
        }

        /**
         * Метод, отвечающий за максимизирующего игрока.
         * @param gameState Игровое состояние.
         * @param depth Максимальная глубина.
         * @param alpha Коэффициент альфа.
         * @param beta Коэффициент бета.
         * @param possibleMoves Возможные ходы.
         * @return ивент и его оценку.
         */
        private EventScore maximize(
                GameState gameState, int depth, double alpha, double beta, List<MakeMoveEvent> possibleMoves) {
            EventScore bestResult = new EventScore(null, MIN_COST);
            try{
                List<MinimaxTask> tasks = new ArrayList<>();

                for (MakeMoveEvent move : possibleMoves) {
                    List<StateChance> possibleStates = gameState.getPossibleState(move);
                    for (StateChance stateChance : possibleStates) {
                        MinimaxTask task = new MinimaxTask(stateChance.gameState(), depth - 1, alpha, beta, true);
                        tasks.add(task);
                        task.fork();
                    }
                }

                for (int i = 0; i < tasks.size(); i++) {
                    MinimaxTask task = tasks.get(i);
                    EventScore result = task.join();
                    if (result.getScore() > bestResult.getScore()) {
                        bestResult = new EventScore(possibleMoves.get(i / 2), result.getScore());
                    }
                    alpha = Math.max(alpha, bestResult.getScore());
                    if (beta <= alpha) {
                        break;
                    }
                }
            }catch (GameException e){
                logger.error("Ошибка в применении хода к игровому состоянию!");

            }

            return bestResult;
        }

        /**
         * Метод, отвечающий за минимизирующего игрока.
         * @param gameState Игровое состояние.
         * @param depth Максимальная глубина.
         * @param alpha Коэффициент альфа.
         * @param beta Коэффициент бета.
         * @param possibleMoves Возможные ходы.
         * @return ивент и его оценку.
         */
        private EventScore minimize(
                GameState gameState, int depth, double alpha, double beta, List<MakeMoveEvent> possibleMoves) {
            EventScore bestResult = new EventScore(null, MAX_COST);
            try{
                List<MinimaxTask> tasks = new ArrayList<>();

                for (MakeMoveEvent move : possibleMoves) {
                    List<StateChance> possibleStates = gameState.getPossibleState(move);
                    for (StateChance stateChance : possibleStates) {
                        MinimaxTask task = new MinimaxTask(stateChance.gameState(), depth - 1, alpha, beta, false);
                        tasks.add(task);
                        task.fork();
                    }
                }

                for (int i = 0; i < tasks.size(); i++) {
                    MinimaxTask task = tasks.get(i);
                    EventScore result = task.join();
                    if (result.getScore() < bestResult.getScore()) {
                        bestResult = new EventScore(possibleMoves.get(i / 2), result.getScore());
                    }
                    beta = Math.min(beta, bestResult.getScore());
                    if (beta <= alpha) {
                        break;
                    }
                }
            }catch (GameException e){
                logger.error("Ошибка в применении хода к игровому состоянию!");

            }



            return bestResult;
        }
    }
}
