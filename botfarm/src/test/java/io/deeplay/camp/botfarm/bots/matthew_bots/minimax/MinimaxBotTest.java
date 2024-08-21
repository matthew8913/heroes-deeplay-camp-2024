package io.deeplay.camp.botfarm.bots.matthew_bots.minimax;

import io.deeplay.camp.botfarm.bots.matthew_bots.BaseEvaluator;
import io.deeplay.camp.game.mechanics.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MinimaxBotTest {

    @Test
    public void minimaxTest(){
        GameState gameState = new GameState();

        System.out.println("DefaultMinimax:");
        gameState.setDefaultPlacement();
        MinimaxBot minimaxBot = new MinimaxBot(6, new BaseEvaluator());
        minimaxBot.generateMakeMoveEvent(gameState);
        minimaxBot.treeAnalyzer.printStatistics();
        long time1 = minimaxBot.treeAnalyzer.getMoveTime();
        int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount();

        gameState = new GameState();
        System.out.println("AB Minimax:");
        gameState.setDefaultPlacement();
        AlphaBetaMinimaxBot minimaxBot1 = new AlphaBetaMinimaxBot(6, new BaseEvaluator());
        minimaxBot1.generateMakeMoveEvent(gameState);
        minimaxBot1.treeAnalyzer.printStatistics();
        long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
        int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount();

        gameState = new GameState();
        System.out.println("AB multi-threaded Minimax:");
        gameState.setDefaultPlacement();
        MultiThreadMinimaxBot minimaxBot2 = new MultiThreadMinimaxBot(6, new BaseEvaluator());
        minimaxBot2.generateMakeMoveEvent(gameState);
        minimaxBot2.treeAnalyzer.printStatistics();
        long time3 = minimaxBot2.treeAnalyzer.getMoveTime();
        int nodesCount3 = minimaxBot2.treeAnalyzer.getNodesCount();

        assertTrue(time1>time2&&time2>time3);
        assertTrue(nodesCount1>nodesCount2&&nodesCount1>nodesCount3);


    }


}
