package io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage;

import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.mechanics.GameState;

/** Абстрактный бота для игрового состояния */
public abstract class PlacementBot {
  public abstract PlaceUnitEvent generatePlaceUnitEvent(GameState gameState);
}
