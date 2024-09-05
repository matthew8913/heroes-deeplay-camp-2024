package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import java.util.List;
import lombok.Getter;

/** Класс, содержащий геймстейт и связанные с ним данные. */
@Getter
public class State<T> {

  /** Игровое состояние. */
  private final GameState gameState;
  /** Вероятность, с которой ход(ы) приводят именно к данному состоянию. */
  private final double probability;
  /** Ход(ы) которые приводят к состоянию. */
  private T lastMove;

  public State(GameState gameState, double chance, T move) {
    this.gameState = gameState;
    this.probability = chance;
    this.lastMove = move;
  }

  public State(GameState gameState, double probability) {
    this.gameState = gameState;
    this.probability = probability;
  }

  public T getLastMove() {
    return lastMove;
  }

  public void setLastMove(T move) {
    this.lastMove = move;
  }

  public boolean isLastMoveList() {
    return lastMove instanceof List;
  }

  public List<MakeMoveEvent> getLastMoveAsList() {
    if (isLastMoveList()) {
      return (List<MakeMoveEvent>) lastMove;
    } else {
      throw new ClassCastException("lastMove is not a List");
    }
  }
}