package io.deeplay.camp.botfarm.bots.matthew_bots.clusterization;

import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State;
import lombok.Getter;
import org.apache.commons.math3.ml.clustering.Clusterable;

/** Игровое состояние + его оценка. */
public class StateClusterable implements Clusterable {
  /** Игровое состояние. */
  @Getter private State state;

  /** Координаты. В нашем случае пространство - одномерное. */
  private final double[] coordinates;

  /**
   * Конструктор
   *
   * @param value оценка состояния
   * @param state состояние
   */
  public StateClusterable(double value, State state) {
    this.coordinates = new double[] {value};
    this.state = state;
  }

  @Override
  public double[] getPoint() {
    return coordinates;
  }

  @Override
  public String toString() {
    return "(" + coordinates[0] + ")";
  }
}
