package io.deeplay.camp.botfarm.bots.matthew_bots;

import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.*;
import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.RandomPlacementBot;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotsTest {

  @Test
  public void minimaxVsAbMinimaxTest() {
    GameState gameState = new GameState();
    System.out.println("DefaultMinimax:");
    gameState.setDefaultPlacementWithoutMage();
    MatthewsBot minimaxBot = new MatthewsBot(new RandomPlacementBot(), new MinimaxBot(5));
    MakeMoveEvent makeMoveEvent1 = minimaxBot.generateMakeMoveEvent(gameState);
    minimaxBot.treeAnalyzer.printStatistics();
    long time1 = minimaxBot.treeAnalyzer.getMoveTime();
    int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount().get();

    gameState = new GameState();
    System.out.println("AB Minimax:");
    gameState.setDefaultPlacementWithoutMage();
    MatthewsBot minimaxBot1 = new MatthewsBot(new RandomPlacementBot(), new AlphaBetaMinimaxBot(5));
    MakeMoveEvent makeMoveEvent2 = minimaxBot1.generateMakeMoveEvent(gameState);
    minimaxBot1.treeAnalyzer.printStatistics();
    long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
    int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount().get();

    assertTrue(nodesCount1 > nodesCount2);
    assertTrue(time1 > time2);
    assertEquals(makeMoveEvent1, makeMoveEvent2);
  }

  @Test
  public void multiThreadMinimaxVsMinimaxTest() {
    GameState gameState = new GameState();
    System.out.println("DefaultMinimax:");
    gameState.setDefaultPlacementWithoutMage();
    MatthewsBot minimaxBot = new MatthewsBot(new RandomPlacementBot(), new MinimaxBot(5));
    MakeMoveEvent makeMoveEvent1 = minimaxBot.generateMakeMoveEvent(gameState);
    minimaxBot.treeAnalyzer.printStatistics();
    long time1 = minimaxBot.treeAnalyzer.getMoveTime();
    int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount().get();

    gameState = new GameState();
    System.out.println("MultiThreadMinimax:");
    gameState.setDefaultPlacementWithoutMage();
    MatthewsBot minimaxBot1 =
        new MatthewsBot(new RandomPlacementBot(), new MultiThreadMinimaxBot(5));
    MakeMoveEvent makeMoveEvent2 = minimaxBot1.generateMakeMoveEvent(gameState);
    minimaxBot1.treeAnalyzer.printStatistics();
    long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
    int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount().get();

    assertTrue(nodesCount1 == nodesCount2);
    assertTrue(time1 > time2);
    assertEquals(makeMoveEvent1, makeMoveEvent2);
  }

    @Test
    public void multiThreadExpectimaxVsExpectimaxTest() {
        GameState gameState = new GameState();
        System.out.println("DefaultMinimax:");
        gameState.setDefaultPlacementWithoutMage();
    MatthewsBot minimaxBot = new MatthewsBot(new RandomPlacementBot(), new ExpectimaxBot(5));
        MakeMoveEvent makeMoveEvent1 = minimaxBot.generateMakeMoveEvent(gameState);
        minimaxBot.treeAnalyzer.printStatistics();
        long time1 = minimaxBot.treeAnalyzer.getMoveTime();
        int nodesCount1 = minimaxBot.treeAnalyzer.getNodesCount().get();

        gameState = new GameState();
        System.out.println("MultiThreadMinimax:");
        gameState.setDefaultPlacementWithoutMage();
        MatthewsBot minimaxBot1 =
                new MatthewsBot(new RandomPlacementBot(), new MultiThreadExpectimaxBot(5));
        MakeMoveEvent makeMoveEvent2 = minimaxBot1.generateMakeMoveEvent(gameState);
        minimaxBot1.treeAnalyzer.printStatistics();
        long time2 = minimaxBot1.treeAnalyzer.getMoveTime();
        int nodesCount2 = minimaxBot1.treeAnalyzer.getNodesCount().get();

        assertTrue(nodesCount1 == nodesCount2);
        assertTrue(time1 > time2);
        assertEquals(makeMoveEvent1, makeMoveEvent2);
    }
}
