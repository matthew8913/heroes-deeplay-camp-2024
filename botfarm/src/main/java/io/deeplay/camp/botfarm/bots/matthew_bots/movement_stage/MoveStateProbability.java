package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Класс, содержащий геймстейт и связанные с ним данные.
 */
@Getter
public class MoveStateProbability {

  /**
   * Игровое состояние.
   */
  private GameState gameState;

  /**
   * Ход который приводит к состоянию.
   */
  private MakeMoveEvent lastMove;
  /**
   * Вероятность, с которой ход приводит именно к данному состоянию.
   */
  private double probability;


  public MoveStateProbability(GameState gameState, double chance, MakeMoveEvent move) {
    this.gameState = gameState;
    this.probability = chance;
    this.lastMove = move;
  }
}
