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
    for (int clustersAmount = 2; clustersAmount <= 10; clustersAmount++) {
      double avgWCSS = calculateAverageWCSS(gameStates, clustersAmount);
      averageWCSS.add(avgWCSS);
    }

    plotAverageWCSS(averageWCSS);
  }
}
