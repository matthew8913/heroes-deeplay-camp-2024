package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import lombok.Getter;

/** Класс, содержащий геймстейт и связанные с ним данные. */
@Getter
public class State {

  /** Игровое состояние. */
  private final GameState gameState;

  /** Ход который приводит к состоянию. */
  private final MakeMoveEvent lastMove;

  /** Вероятность, с которой ход приводит именно к данному состоянию. */
  private final double probability;

  public State(GameState gameState, double chance, MakeMoveEvent move) {
    this.gameState = gameState;
    this.probability = chance;
    this.lastMove = move;
  }
}
