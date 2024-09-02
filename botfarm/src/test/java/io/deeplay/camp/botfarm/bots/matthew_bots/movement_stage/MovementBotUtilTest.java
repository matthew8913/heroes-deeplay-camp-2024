package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.entities.Archer;
import io.deeplay.camp.game.entities.Healer;
import io.deeplay.camp.game.entities.Knight;
import io.deeplay.camp.game.entities.Position;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovementBotUtilTest {


    @Test
    void collectPossibleStatesFromComplexMoves() {
        GameState gameState = new GameState();
        gameState.setDefaultPlacementWithoutMage();

        long startTime = System.currentTimeMillis();
        List<List<MakeMoveEvent>> complexMoves = gameState.getCopy().getPossibleComplexMoves();
        long endTime = System.currentTimeMillis();
        long durationGetPossibleComplexMoves = (endTime - startTime);

        startTime = System.currentTimeMillis();
        List<State> possibleStates = MovementBotUtil.collectPossibleStatesFromComplexMoves(gameState.getCopy(), complexMoves);
        endTime = System.currentTimeMillis();
        long durationCollectPossibleStatesComplex = (endTime - startTime);

        List<State> possibleStates1 = MovementBotUtil.collectPossibleStatesFromComplexMoves(gameState.getCopy(), complexMoves);
        assertEquals(possibleStates1.size(), possibleStates.size());
        System.out.println("Количество состояний, к которым могут привести комплексные ходы: " + possibleStates.size());
        System.out.println("Время поиска всех комбинаций ходов: " + durationGetPossibleComplexMoves + " ms");
        System.out.println("Время поиска возможных состояний: " + durationCollectPossibleStatesComplex + " ms");
    }
    @Test
    void removeSimilarMovesTest() {
        List<MakeMoveEvent> similarMoves = new ArrayList<>();
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 0), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 1), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
        MovementBotUtil.removeUnnecessaryMoves(similarMoves);
        assertEquals(1, similarMoves.size());
    }

    @Test
    void removeNotSimilarMovesTest() {
        List<MakeMoveEvent> similarMoves = new ArrayList<>();
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 0), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 1), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
        MovementBotUtil.removeUnnecessaryMoves(similarMoves);
        assertEquals(2, similarMoves.size());
    }

    @Test
    void removeUnnecessaryMovesTest() {
        List<MakeMoveEvent> similarMoves = new ArrayList<>();
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 0), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 1), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 0), new Position(2, 1), new Archer(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 1), new Position(2, 1), new Archer(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 0), new Position(2, 1), new Healer(PlayerType.FIRST_PLAYER)));
        similarMoves.add(
                new MakeMoveEvent(
                        new Position(0, 1), new Position(2, 1), new Healer(PlayerType.FIRST_PLAYER)));
        MovementBotUtil.removeUnnecessaryMoves(similarMoves);
        assertEquals(3, similarMoves.size());
    }
}