package io.deeplay.camp.botfarm.bots.matthew_bots.clusterization;

import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueClusterization implements Clusterization {

  private static final Logger logger = LoggerFactory.getLogger(ValueClusterization.class);

  /**
   * Метод кластеризации, использующий Kmeans++.
   *
   * @param statesClusterable список состояний
   * @param clustersAmount необходимое количество кластеров
   * @return список кластеров
   */
  @Override
  public List<CentroidCluster<StateClusterable>> clusterize(
      List<StateClusterable> statesClusterable, int clustersAmount) {
    try {
      List<CentroidCluster<StateClusterable>> centroidClusters;
      KMeansPlusPlusClusterer<StateClusterable> clusterer =
          new KMeansPlusPlusClusterer<>(clustersAmount, 100);
      centroidClusters = clusterer.cluster(statesClusterable);
      return centroidClusters;

    } catch (Exception e) {
      logger.error("Ошибка кластеризации!");
    }
    return null;
  }

  /**
   * Метод определяет первые amount состояний, самых близких к центроиду.
   *
   * @param cluster кластер
   * @param amount требуемое количество представителей
   * @return список состояний
   */
  @Override
  public List<State> pickRepresentativesFromCluster(
      CentroidCluster<StateClusterable> cluster, int amount) {
    List<StateClusterable> statesClusterable = cluster.getPoints();
    if (statesClusterable.isEmpty()) {
      return Collections.emptyList();
    }
    double[] centroid = cluster.getCenter().getPoint();
    EuclideanDistance distanceCalculator = new EuclideanDistance();
    statesClusterable.sort(
        Comparator.comparingDouble(
            state -> distanceCalculator.compute(state.getPoint(), centroid)));
    return statesClusterable.stream().map((StateClusterable::getState)).toList().subList(0, amount);
  }

  /**
   * Метод вычисляет количество запрашиваемых у каждого кластера представителей и возвращает их.
   *
   * @param clusters список кластеров
   * @param amount количество представителей
   * @return список представителей с кластеров
   */
  @Override
  public List<State> pickRepresentatives(
      List<CentroidCluster<StateClusterable>> clusters, int amount) {
    List<State> representatives = new ArrayList<>();
    int summaryStatesAmount =
        clusters.stream().mapToInt(cluster -> cluster.getPoints().size()).sum();
    for (CentroidCluster<StateClusterable> centroidCluster : clusters) {
      double clusterRepresentativesAmountDouble =
          ((double) centroidCluster.getPoints().size() / summaryStatesAmount) * amount;
      int clusterRepresentativesAmount = (int) Math.round(clusterRepresentativesAmountDouble);
      List<State> centroidRepresentatives =
          pickRepresentativesFromCluster(centroidCluster, clusterRepresentativesAmount);
      representatives.addAll(centroidRepresentatives);
    }
    return representatives;
  }
}
