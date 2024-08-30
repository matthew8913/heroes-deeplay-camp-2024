package io.deeplay.camp.botfarm.bots.matthew_bots.clusterization;

import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.GameStateEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.util.ArrayList;
import java.util.List;

/** Вспомогательный класс для работы с кластеризацией. */
public class ClusterizationUtil {

  /**
   * Метод, приводящий список игровых состояний к списку кластеризуемых состояний для дальнейшей
   * работы с кластеризацией.
   *
   * @param states список состояний
   * @param evaluator оцениватель состояний
   * @param maximizingPlayer максимизирующий игрок для оценки состояния
   * @return список кластеризуемых состояний
   */
  public static List<StateClusterable> getClusterableStates(
      List<State> states, GameStateEvaluator evaluator, PlayerType maximizingPlayer) {
    List<StateClusterable> statesClustarable = new ArrayList<>();
    for (State state : states) {
      statesClustarable.add(
          new StateClusterable(
              evaluator.evaluate(state.getGameState(), maximizingPlayer) * state.getProbability(),
              state));
    }
    return statesClustarable;
  }
}
