package io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage;

import io.deeplay.camp.game.entities.Knight;
import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementCacheGeneratorTest {
    @Test
    void generatePossiblePlacementsTest() throws GameException {
        GameState gameState = new GameState();
        PlacementCacheGenerator placementCacheGenerator = new PlacementCacheGenerator();
        gameState.makePlacement(new PlaceUnitEvent(0, 1, new Knight(PlayerType.FIRST_PLAYER), PlayerType.FIRST_PLAYER, true, false));
        gameState.makePlacement(new PlaceUnitEvent(1, 1, new Knight(PlayerType.FIRST_PLAYER), PlayerType.FIRST_PLAYER, true, false));
        placementCacheGenerator.generatePossiblePlacements(gameState);
        assertEquals(256, placementCacheGenerator.possiblePlacements.size());
    }
}