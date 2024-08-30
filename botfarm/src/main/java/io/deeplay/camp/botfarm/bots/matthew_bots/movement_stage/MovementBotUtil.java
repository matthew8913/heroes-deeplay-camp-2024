package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.entities.Position;
import io.deeplay.camp.game.entities.StateChance;
import io.deeplay.camp.game.entities.UnitType;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameState;

import java.util.*;

public class MovementBotUtil {
  public static final double BAD_BRANCH_PROBABILITY = 0.1;

  /**
   * Метод, удаляющий из списка ходов похожие ходы.
   *
   * @param possibleMoves возможные ходы
   */
  public static void removeUnnecessaryMoves(List<MakeMoveEvent> possibleMoves) {
    removeSimilarMovesOfUnitType(possibleMoves, UnitType.ARCHER);
    removeSimilarMovesOfUnitType(possibleMoves, UnitType.KNIGHT);
    removeSimilarMovesOfUnitType(possibleMoves, UnitType.HEALER);
  }

  /**
   * Метод, удаляющий из списка похожие ходы юнитов одного типа.
   *
   * @param moves список ходов.
   * @param attackerUnitType тип юнитов, чьи ходы фильтруем.
   */
  public static void removeSimilarMovesOfUnitType(
      List<MakeMoveEvent> moves, UnitType attackerUnitType) {
    Set<Position> targetUnitPositions = new HashSet<>();
    Iterator<MakeMoveEvent> iterator = moves.iterator();
    while (iterator.hasNext()) {
      MakeMoveEvent moveEvent = iterator.next();
      if (attackerUnitType.equals(moveEvent.getAttacker().getUnitType())) {
        if (!targetUnitPositions.add(moveEvent.getTo())) {
          iterator.remove();
        }
      }
    }
  }

  /**
   * Метод, собирающий список возможных состояний на следующем уровне по PossibleMoves. Также
   * отсекает плохие варианты.
   *
   * @param gameState игровое состояние
   * @param possibleMoves возможные ходы
   * @return сформированный список возможных состояний
   * @throws GameException если getPossibleState не может применить ход
   */
  public static List<State> collectPossibleStates(
      GameState gameState, List<MakeMoveEvent> possibleMoves) throws GameException {
    List<State> possibleStates = new ArrayList<>();
    for (MakeMoveEvent move : possibleMoves) {
      List<StateChance> possibleOptions = gameState.getPossibleState(move);
      for (StateChance stateChance : possibleOptions) {
        if ((stateChance.chance() > BAD_BRANCH_PROBABILITY)) {
          possibleStates.add(
              new State(
                  stateChance.gameState(),
                  stateChance.chance(),
                  new MakeMoveEvent(move.getFrom(), move.getTo(), move.getAttacker().getCopy())));
        }
      }
    }
    return possibleStates;
  }
}
