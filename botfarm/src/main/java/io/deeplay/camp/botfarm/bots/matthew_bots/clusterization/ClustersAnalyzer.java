package io.deeplay.camp.botfarm.bots.matthew_bots.clusterization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.deeplay.camp.botfarm.bots.matthew_bots.evaluate.BaseEvaluator;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.MovementBotUtil;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.State;
import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.entities.Unit;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс для анализа кластеров и построения графика среднего WCSS для различного количества
 * кластеров.
 */
public class ClustersAnalyzer {
  private static final Logger logger = LoggerFactory.getLogger(ClustersAnalyzer.class);

  /**
   * Метод для вычисления WCSS для одного кластера.
   *
   * @param cluster кластер, для которого вычисляется WCSS
   * @return значение WCSS для данного кластера
   */
  public static double calculateWCSS(CentroidCluster<StateClusterable> cluster) {
    EuclideanDistance distance = new EuclideanDistance();
    double wcss = 0.0;
    double[] centroid = cluster.getCenter().getPoint();
    for (StateClusterable point : cluster.getPoints()) {
      wcss += Math.pow(distance.compute(point.getPoint(), centroid), 2);
    }
    return wcss;
  }

  public static double calculateDaviesBouldinIndex(List<CentroidCluster<StateClusterable>> clusters) {
    EuclideanDistance distance = new EuclideanDistance();
    int k = clusters.size();
    double[] avgDistanceWithinCluster = new double[k];
    double[][] distanceBetweenCentroids = new double[k][k];
    double[] maxRatios = new double[k];

    // Calculate average distance within each cluster
    for (int i = 0; i < k; i++) {
      CentroidCluster<StateClusterable> cluster = clusters.get(i);
      double totalDistance = 0.0;
      for (StateClusterable point : cluster.getPoints()) {
        totalDistance += distance.compute(point.getPoint(), cluster.getCenter().getPoint());
      }
      avgDistanceWithinCluster[i] = totalDistance / cluster.getPoints().size();
    }

    // Calculate distance between centroids
    for (int i = 0; i < k; i++) {
      for (int j = i + 1; j < k; j++) {
        distanceBetweenCentroids[i][j] = distance.compute(clusters.get(i).getCenter().getPoint(), clusters.get(j).getCenter().getPoint());
        distanceBetweenCentroids[j][i] = distanceBetweenCentroids[i][j];
      }
    }

    // Calculate Davies-Bouldin Index
    for (int i = 0; i < k; i++) {
      maxRatios[i] = 0.0;
      for (int j = 0; j < k; j++) {
        if (i != j) {
          double ratio = (avgDistanceWithinCluster[i] + avgDistanceWithinCluster[j]) / distanceBetweenCentroids[i][j];
          if (ratio > maxRatios[i]) {
            maxRatios[i] = ratio;
          }
        }
      }
    }

    double dbi = 0.0;
    for (int i = 0; i < k; i++) {
      dbi += maxRatios[i];
    }
    return dbi / k;
  }

  /**
   * Метод для чтения игровых состояний из файла.
   *
   * @return список игровых состояний
   */
  public static List<GameState> readGameStatesFromFile(String filePath) {
    ObjectMapper objectMapper = new ObjectMapper();
    List<GameState> gameStates = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        Unit[][] units = objectMapper.readValue(line, Unit[][].class);
        GameState gameState = new GameState();
        gameState.setDefaultPlacementWithoutMage();
        Board board = gameState.getBoard();
        for (int col = 0; col < Board.COLUMNS; col++) {
          for (int row = 0; row < Board.ROWS / 2; row++) {
            board.setUnit(col, row, units[col][row]);
          }
        }
        gameStates.add(gameState);
      }
    } catch (IOException e) {
      logger.error("Ошибка чтения файла");
    }
    return gameStates;
  }

  /**
   * Метод для вычисления среднего WCSS для заданного количества кластеров.
   *
   * @param gameStates список игровых состояний
   * @param clustersAmount количество кластеров
   * @return среднее значение WCSS
   */
  public static double calculateAverageWCSS(List<GameState> gameStates, int clustersAmount)
      throws GameException {
    double totalWCSS = 0.0;
    for (GameState gameState : gameStates) {
      GameState gameStateCopy = gameState.getCopy();
      gameStateCopy.setDefaultPlacementWithoutMage();
      List<MakeMoveEvent> possibleMoves = gameStateCopy.getPossibleMoves();
      MovementBotUtil.removeUnnecessaryMoves(possibleMoves);
      List<State> possibleStates =
          MovementBotUtil.collectPossibleStates(gameStateCopy, possibleMoves);
      List<StateClusterable> statesClusterables =
          ClusterizationUtil.getClusterableStates(
              possibleStates, new BaseEvaluator(), PlayerType.FIRST_PLAYER);
      Clusterization clusterization = new ValueClusterization();
      List<CentroidCluster<StateClusterable>> clusters =
          clusterization.clusterize(statesClusterables, clustersAmount);
      for (CentroidCluster<StateClusterable> cluster : clusters) {
        double wcss = calculateWCSS(cluster);
        totalWCSS += wcss;
      }
    }
    return totalWCSS / (gameStates.size() * clustersAmount);
  }

  /**
   * Метод для построения графика среднего WCSS.
   *
   * @param averageWCSS список средних значений WCSS для различного количества кластеров
   */
  public static void plotAverageWCSS(List<Double> averageWCSS) {
    XYSeries series = new XYSeries("Average WCSS");
    for (int i = 0; i < averageWCSS.size(); i++) {
      series.add(i + 2, averageWCSS.get(i));
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);

    org.jfree.chart.JFreeChart chart =
        ChartFactory.createXYLineChart(
            "Average WCSS for Clusters",
            "Number of Clusters",
            "Average WCSS",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false);

    ChartFrame frame = new ChartFrame("Average WCSS Chart", chart);
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) throws GameException {
    String filePath = "botfarm/src/main/resources/first-placements.json";
    List<GameState> gameStates = readGameStatesFromFile(filePath);

    List<Double> averageWCSS = new ArrayList<>();
    List<Double> daviesBouldinIndices = new ArrayList<>();

    for (int clustersAmount = 2; clustersAmount <= 10; clustersAmount++) {
      double totalWCSS = 0.0;
      double totalDaviesBouldinIndex = 0.0;
      for (GameState gameState : gameStates) {
        GameState gameStateCopy = gameState.getCopy();
        gameStateCopy.setDefaultPlacementWithoutMage();
        List<MakeMoveEvent> possibleMoves = gameStateCopy.getPossibleMoves();
        MovementBotUtil.removeUnnecessaryMoves(possibleMoves);
        List<State> possibleStates =
                MovementBotUtil.collectPossibleStates(gameStateCopy, possibleMoves);
        List<StateClusterable> statesClusterables =
                ClusterizationUtil.getClusterableStates(
                        possibleStates, new BaseEvaluator(), PlayerType.FIRST_PLAYER);
        Clusterization clusterization = new ValueClusterization();
        List<CentroidCluster<StateClusterable>> clusters =
                clusterization.clusterize(statesClusterables, clustersAmount);

        double wcss = 0.0;
        for (CentroidCluster<StateClusterable> cluster : clusters) {
          wcss += calculateWCSS(cluster);
        }
        totalWCSS += wcss;
        totalDaviesBouldinIndex += calculateDaviesBouldinIndex(clusters);
      }
      averageWCSS.add(totalWCSS / (gameStates.size() * clustersAmount));
      daviesBouldinIndices.add(totalDaviesBouldinIndex / gameStates.size());
    }

    plotAverageWCSS(averageWCSS);

    for (int i = 0; i < daviesBouldinIndices.size(); i++) {
      System.out.println("Clusters: " + (i + 2) + " Davies-Bouldin Index: " + daviesBouldinIndices.get(i));
    }
  }
}
