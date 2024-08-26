package io.deeplay.camp.botfarm.bots.matthew_bots;

import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.BaseEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.AlphaBetaMovementMinimaxBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MinimaxMovementBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MultiThreadMinimax;
import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.RandomPlacementBot;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BotsTest {

    @Test
    public void minimaxVsAbMinimaxTest(){
        GameState gameState = new GameState();
        System.out.println("DefaultMinimax:");
        gameState.setDefaultPlacementWithoutMage();
        MatthewsBot minimaxBot = new MatthewsBot(new RandomPlacementBot(), new MinimaxMovementBot(5));
        MakeMoveEvent makeMoveEvent1 = minimaxBot.generateMakeMoveEvent(gameState);
        minimaxBot.treeAnalyzer.printStatistics();
        long time1 = minimaxBot.treeAnalyzer.getMoveTime();
        int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount();

        gameState = new GameState();
        System.out.println("AB Minimax:");
        gameState.setDefaultPlacementWithoutMage();
        MatthewsBot minimaxBot1 = new MatthewsBot(new RandomPlacementBot(), new AlphaBetaMovementMinimaxBot(5));
        MakeMoveEvent makeMoveEvent2 = minimaxBot1.generateMakeMoveEvent(gameState);
        minimaxBot1.treeAnalyzer.printStatistics();
        long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
        int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount();

        assertTrue(nodesCount1 > nodesCount2);
        assertTrue(time1 > time2);
        assertEquals(makeMoveEvent1, makeMoveEvent2);
    }
    @Test
    public void multiThreadMinimaxVsAbMinimaxTest(){
        GameState gameState = new GameState();
        System.out.println("MultiMinimax:");
        gameState.setDefaultPlacementWithoutMage();
        MatthewsBot minimaxBot = new MatthewsBot(new RandomPlacementBot(), new MultiThreadMinimax(5));
        MakeMoveEvent makeMoveEvent1 = minimaxBot.generateMakeMoveEvent(gameState);
        minimaxBot.treeAnalyzer.printStatistics();
        long time1 = minimaxBot.treeAnalyzer.getMoveTime();
        int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount();

        gameState = new GameState();
        System.out.println("AB Minimax:");
        gameState.setDefaultPlacementWithoutMage();
        MatthewsBot minimaxBot1 = new MatthewsBot(new RandomPlacementBot(), new AlphaBetaMovementMinimaxBot(5));
        MakeMoveEvent makeMoveEvent2 = minimaxBot1.generateMakeMoveEvent(gameState);
        minimaxBot1.treeAnalyzer.printStatistics();
        long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
        int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount();

        assertTrue(nodesCount1 == nodesCount2);
        assertTrue(time1 < time2);
        assertEquals(makeMoveEvent1, makeMoveEvent2);
    }

}
