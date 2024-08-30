package io.deeplay.camp.botfarm.bots.matthew_bots.clusterization;

import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

import java.util.List;

/** Интерфейс кластеризации */
public interface Clusterization {
  /**
   * Метод, кластеризующий список состояний.
   *
   * @param states список состояний
   * @param clustersAmount необходимое количество кластеров
   * @return список кластеров
   */
  List<CentroidCluster<StateClusterable>> clusterize(
      final List<StateClusterable> states, final int clustersAmount);

  /**
   * Метод, достающий представителей из одного кластера.
   *
   * @param cluster кластер
   * @param amount требуемое количество представителей
   * @return список представителей
   */
  List<State> pickRepresentativesFromCluster(
      final CentroidCluster<StateClusterable> cluster, final int amount);

  /**
   * Метод, достающий из списка кластеров множество представителей, распределенных между кластерами.
   *
   * @param clusters список кластеров
   * @param amount количество представителей
   * @return список представителей
   */
  List<State> pickRepresentatives(
      final List<CentroidCluster<StateClusterable>> clusters, final int amount);
}
