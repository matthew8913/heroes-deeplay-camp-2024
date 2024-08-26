package io.deeplay.camp.botfarm.bots.matthew_bots.evaluate;

import io.deeplay.camp.game.entities.Archer;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseEvaluatorTest {

    @Test
    void evaluateTest() {
        GameState gameState = new GameState();
        gameState.setDefaultPlacement();
        PlayerType maximizingPlayerType = PlayerType.FIRST_PLAYER;
        GameStateEvaluator baseEvaluator = new BaseEvaluator();
        double costWhenMaximizingIsCurrent = baseEvaluator.evaluate(gameState, maximizingPlayerType);
        gameState.changeCurrentPlayer();
        double costWhenMaximizingIsNotCurrent = baseEvaluator.evaluate(gameState, maximizingPlayerType);
        assertEquals(costWhenMaximizingIsCurrent, costWhenMaximizingIsNotCurrent, 0);
    }

    @Test
    void evaluateAfterKillTest() {
        GameState gameState = new GameState();
        gameState.setDefaultPlacement();
        PlayerType maximizingPlayerType = PlayerType.FIRST_PLAYER;
        GameStateEvaluator baseEvaluator = new BaseEvaluator();
        double costBeforeKill = baseEvaluator.evaluate(gameState, maximizingPlayerType);
        gameState.getBoard().getUnit(0,0).setCurrentHp(0);
        double costAfterKill = baseEvaluator.evaluate(gameState, maximizingPlayerType);
        assertTrue(costAfterKill < costBeforeKill);
    }

    @Test
    void wrongRowTest(){
        GameStateEvaluator baseEvaluator = new BaseEvaluator();
        GameState gameState = new GameState();
        gameState.setDefaultPlacement();
        double originalCost = baseEvaluator.evaluate(gameState, PlayerType.FIRST_PLAYER);
        gameState.getBoard().setUnit(1,1, new Archer(PlayerType.FIRST_PLAYER));
        double costWhenUnitOnWrongRow = baseEvaluator.evaluate(gameState, PlayerType.FIRST_PLAYER);
        assertTrue(originalCost> costWhenUnitOnWrongRow);
    }

    @Test
    void noDefTest(){
        GameStateEvaluator baseEvaluator = new BaseEvaluator();
        GameState gameState = new GameState();
        gameState.setDefaultPlacement();
        double originalCost = baseEvaluator.evaluate(gameState, PlayerType.FIRST_PLAYER);
        gameState.getBoard().getUnit(1,1).setCurrentHp(0);
        double costWhenUnitWithoutDef = baseEvaluator.evaluate(gameState, PlayerType.FIRST_PLAYER);
        assertTrue(originalCost> costWhenUnitWithoutDef);
    }
}