package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.botfarm.bots.matthew_bots.TreeAnalyzer;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import java.util.List;

public class RandomMovementBot extends MovementBot {

    public RandomMovementBot() {
        super(new TreeAnalyzer(),0);
    }

    @Override
    public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
        List<MakeMoveEvent> makeMoveEvents = gameState.getPossibleMoves();
        if(!makeMoveEvents.isEmpty()){
            return makeMoveEvents.get((int)(Math.random()*makeMoveEvents.size()));
        }
        else{
            return null;
        }
    }
}
