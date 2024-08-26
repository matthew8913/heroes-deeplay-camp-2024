package io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage;

import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.mechanics.GameState;
import java.util.List;

public class RandomPlacementBot extends PlacementBot {

    @Override
    public PlaceUnitEvent generatePlaceUnitEvent(GameState gameState) {
        List<PlaceUnitEvent> placeUnitEvents = gameState.getPossiblePlaces();
        if(!placeUnitEvents.isEmpty()){
            return placeUnitEvents.get((int)(Math.random()*placeUnitEvents.size()));
        }
        else{
            return null;
        }
    }
}
