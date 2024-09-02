package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import lombok.Getter;

import java.util.List;

/** Класс, содержащий геймстейт и связанные с ним данные. */
@Getter
public class State<T> {

  /** Игровое состояние. */
  private final GameState gameState;

  /** Ход(ы) которые приводят к состоянию. */
  private T lastMove;

  /** Вероятность, с которой ход(ы) приводят именно к данному состоянию. */
  private final double probability;

  public State(GameState gameState, double chance, T move) {
    this.gameState = gameState;
    this.probability = chance;
    this.lastMove = move;
  }

  public State(GameState gameState, double probability) {
    this.gameState = gameState;
    this.probability = probability;
  }

  // Метод для установки lastMove, если это необходимо
  public void setLastMove(T move) {
    this.lastMove = move;
  }

  // Метод для получения lastMove
  public T getLastMove() {
    return lastMove;
  }

  // Метод для проверки, является ли lastMove списком
  public boolean isLastMoveList() {
    return lastMove instanceof List;
  }

  // Метод для получения lastMove как списка, если это возможно
  @SuppressWarnings("unchecked")
  public List<MakeMoveEvent> getLastMoveAsList() {
    if (isLastMoveList()) {
      return (List<MakeMoveEvent>) lastMove;
    } else {
      throw new ClassCastException("lastMove is not a List");
    }
  }
}