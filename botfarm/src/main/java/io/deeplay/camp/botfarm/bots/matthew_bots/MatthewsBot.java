package io.deeplay.camp.botfarm.bots.matthew_bots;

import io.deeplay.camp.botfarm.bots.Bot;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.PlacementBot;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.mechanics.GameState;
import lombok.Getter;

/** Реализация бота matthew8913. */

@Getter
public class MatthewsBot extends Bot {
  /** Бот этапа расстновки. */
  private final PlacementBot placementBot;

  /** Бот этапа игры. */
  private final MovementBot movementBot;

  /** Анализатор дерева. */
  TreeAnalyzer treeAnalyzer;

  /**
   * Конструктор.
   *
   * @param placementBot бот этапа расстновки.
   * @param movementBot бот этапа игры.
   */
  public MatthewsBot(PlacementBot placementBot, MovementBot movementBot) {
    this.placementBot = placementBot;
    this.movementBot = movementBot;
    treeAnalyzer = movementBot.getTreeAnalyzer();
  }

  /**
   * Метод, генерирующий ход расстановки по игровому состоянию.
   *
   * @param gameState игровое состояние.
   * @return ивент расстановки.
   */
  @Override
  public PlaceUnitEvent generatePlaceUnitEvent(GameState gameState) {
    return placementBot.generatePlaceUnitEvent(gameState.getCopy());
  }

  /**
   * Метод, генерирующий ивент хода по игровому состоянию.
   *
   * @param gameState игровое состояние.
   * @return ивент хода.
   */
  @Override
  public MakeMoveEvent generateMakeMoveEvent(GameState gameState) {
    return movementBot.generateMakeMoveEvent(gameState.getCopy());
  }
}
