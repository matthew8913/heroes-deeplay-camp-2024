package io.deeplay.camp.botfarm.bots.matthew_bots.evaluate;

import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.entities.Unit;
import io.deeplay.camp.game.entities.UnitType;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Базовая реализация "оценщика" состояний. */
public class BaseEvaluator implements GameStateEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(BaseEvaluator.class);

  /**
   * Двумерный массив, содержащий стоимости юнитов. Соответствует доске внутри игрового состояния.
   */
  private final double[][] unitsCosts = new double[Board.COLUMNS][Board.ROWS];

  /** Коэффициент-бонус для юнитов-генералов. */
  private final double isGeneralBonus;

  /** Коэффициент-штраф для юнитов, находящихся не в своей полосе. */
  private final double rowPenalty;

  /** Коэффициент штраф за отсутствие защиты перед юнитом. */
  private final double noDefPenalty;

  /** Максимальная стоимость одного юнита. */
  private final double maxUnitCost;

  /** Минимальная стоимость одного юнита. */
  private final double minUnitCost;

  /** Конструктор. Загружает коэффициенты и считает крайние стоимости юнитов. */
  public BaseEvaluator() {
    Properties props = new Properties();
    try (InputStream inputStream =
        BaseEvaluator.class.getClassLoader().getResourceAsStream("coefficients.properties")) {
      if (inputStream == null) {
        throw new IOException("coefficients.properties not found");
      }
      props.load(inputStream);
    } catch (IOException e) {
      logger.error("Ошибка загрузки файла с коэффициентами!");
    }
    isGeneralBonus = Double.parseDouble(props.getProperty("isGeneralBonus"));
    rowPenalty = Double.parseDouble(props.getProperty("rowPenalty"));
    noDefPenalty = Double.parseDouble(props.getProperty("noDefPenalty"));

    maxUnitCost = isGeneralBonus;
    minUnitCost = rowPenalty * noDefPenalty;
  }

  /**
   * Метод оценки игрового сстояния.
   *
   * @param gameState Игровое состояние.
   * @param maximizingPlayerType Максимизирующий игрок.
   * @return оценку состояния.
   */
  @Override
  synchronized public double evaluate(GameState gameState, PlayerType maximizingPlayerType) {
    if (gameState.getGameStage() == GameStage.ENDED) {
      return evaluateGameEnd(gameState, maximizingPlayerType);
    }

    evaluateUnitsCost(gameState);
    double sum = sumUnitsCosts(maximizingPlayerType);
    return normalize(sum);
  }

  /**
   * Метод нормировки итоговой оценки.
   *
   * @param sum Итоговая оценка.
   * @return нормированное значение(от -1 до 1).
   */
  private double normalize(double sum) {
    double maxPossibleValue = maxUnitCost > minUnitCost ? 6 * maxUnitCost : 6 * minUnitCost;
    double minPossibleValue = maxUnitCost > minUnitCost ? -6 * maxUnitCost : -6 * minUnitCost;
    return MAX_COST * ((sum - minPossibleValue) / (maxPossibleValue - minPossibleValue) * 2 - 1);
  }

  /**
   * Метод суммирования стоимостей юнитов.
   *
   * @param maximizingPlayerType Максимизирующий агент.
   * @return итоговая сумма стоимостей юнитов.
   */
  private double sumUnitsCosts(PlayerType maximizingPlayerType) {
    int sign = maximizingPlayerType == PlayerType.FIRST_PLAYER ? 1 : -1;
    double sum = 0;
    for (int row = 0; row < Board.ROWS; row++) {
      if (row == Board.ROWS / 2) {
        sign *= -1;
      }
      for (int col = 0; col < Board.COLUMNS; col++) {
        sum += unitsCosts[col][row] * sign;
      }
    }
    return sum;
  }

  /**
   * Метод оценивания стоимостей юнитов.
   *
   * @param gameState Игровое состояние.
   */
  private void evaluateUnitsCost(GameState gameState) {
    Board board = gameState.getBoard();
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS; row++) {
        evaluateBaseHpCost(col, row, board);
        evaluateIsGeneral(col, row, board);
        evaluateRowPenalty(col, row, board);
        evaluateNoDefPenalty(col, row, board);
      }
    }
  }

  /**
   * Метод считает базовую стоимость юнитов, отталкиваясь от их хп.
   *
   * @param col Колонка юнита.
   * @param row Полоса юнита.
   * @param board Доска, где находятся юниты.
   */
  private void evaluateBaseHpCost(int col, int row, Board board) {
    Unit unit = board.getUnit(col, row);
    if (unit.isAlive()) {
      unitsCosts[col][row] = (double) unit.getCurrentHp() / unit.getMaxHp();
    } else {
      unitsCosts[col][row] = 0;
    }
  }

  /**
   * Метод, штрафующий юнитов за отсутствие защиты.
   *
   * @param col Колонка юнита.
   * @param row Полоса юнита.
   * @param board Доска, где находятся юниты.
   */
  private void evaluateNoDefPenalty(int col, int row, Board board) {
    if (row == 0) {
      if (!board.getUnit(col, row + 1).isAlive()) {
        unitsCosts[col][row] *= noDefPenalty;
      }
    }
    if (row == Board.ROWS - 1) {
      if (!board.getUnit(col, row - 1).isAlive()) {
        unitsCosts[col][row] *= noDefPenalty;
      }
    }
  }

  /**
   * Метод, штрафующий юнитов за нахождение не в своей полосе.
   *
   * @param col Колонка юнита.
   * @param row Полоса юнита.
   * @param board Доска, где находятся юниты.
   */
  private void evaluateRowPenalty(int col, int row, Board board) {
    Unit unit = board.getUnit(col, row);
    if (isLongRangeUnit(unit)) {
      if (row == Board.ROWS - 2 || row == Board.ROWS - 3) {
        unitsCosts[col][row] *= rowPenalty;
      }
    } else {
      if (row == Board.ROWS - 4 || row == Board.ROWS - 1) {
        unitsCosts[col][row] *= rowPenalty;
      }
    }
  }

  /**
   * Метод проверяет дальнобойность юнита.
   *
   * @param unit Юнит для проверки.
   * @return Дальнобойный/нет.
   */
  private boolean isLongRangeUnit(Unit unit) {
    return unit.getUnitType() != UnitType.KNIGHT;
  }

  /**
   * Метод, дающий бонус генералам.
   *
   * @param col Колонка юнита.
   * @param row Полоса юнита.
   * @param board Доска, где находятся юниты.
   */
  private void evaluateIsGeneral(int col, int row, Board board) {
    Unit unit = board.getUnit(col, row);
    if (unit.isGeneral()) {
      unitsCosts[col][row] *= isGeneralBonus;
    }
  }

  /**
   * Метод оценки конца игры.
   *
   * @param gameState Игровое состояние.
   * @param maximizingPlayer Максимизирующий игрок.
   * @return Оценка терминального узла.
   */
  private double evaluateGameEnd(GameState gameState, PlayerType maximizingPlayer) {
    if (gameState.getWinner() == maximizingPlayer) {
      return GameStateEvaluator.MAX_COST;
    } else if (gameState.getWinner() == null) {
      return 0;
    } else {
      return GameStateEvaluator.MIN_COST;
    }
  }
}
