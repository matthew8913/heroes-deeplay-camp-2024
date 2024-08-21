package io.deeplay.camp.botfarm.bots.matthew_bots;

import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.entities.Unit;
import io.deeplay.camp.game.entities.UnitType;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BaseEvaluator implements GameStateEvaluator {

  private final double[][] unitsCosts = new double[Board.COLUMNS][Board.ROWS];
  private final double isGeneralBonus;
  private final double rowPenalty;
  private final double noDefPenalty;
  private final double maxUnitCost;
  private final double minUnitCost;

  public BaseEvaluator() {
    Properties props = new Properties();
    try (InputStream inputStream =
        BaseEvaluator.class.getClassLoader().getResourceAsStream("coefficients.properties")) {
      if (inputStream == null) {
        throw new IOException("coefficients.properties not found");
      }
      props.load(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
    isGeneralBonus = Double.parseDouble(props.getProperty("isGeneralBonus"));
    rowPenalty = Double.parseDouble(props.getProperty("rowPenalty"));
    noDefPenalty = Double.parseDouble(props.getProperty("noDefPenalty"));

    maxUnitCost = isGeneralBonus;
    minUnitCost = rowPenalty * noDefPenalty;
  }

  @Override
  public double evaluate(GameState gameState, PlayerType maximizingPlayerType) {

    if (gameState.getGameStage() == GameStage.ENDED) {
      return evaluateGameEnd(gameState, maximizingPlayerType);
    }
    evaluateUnitsCost(gameState);

    double sum = sumUnitsCosts(maximizingPlayerType);
    return normalize(sum);
  }

  private double normalize(double sum) {
    double maxPossibleValue = maxUnitCost > minUnitCost ? 6 * maxUnitCost : 6 * minUnitCost;
    double minPossibleValue = maxUnitCost > minUnitCost ? -6 * maxUnitCost : -6 * minUnitCost;
    return (sum - minPossibleValue) / (maxPossibleValue - minPossibleValue) * 2 - 1;
  }

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

  private void evaluateBaseHpCost(int col, int row, Board board) {
    Unit unit = board.getUnit(col, row);
    if (unit.isAlive()) {
      unitsCosts[col][row] = (double) unit.getCurrentHp() / unit.getMaxHp();
    } else {
      unitsCosts[col][row] = 0;
    }
  }

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

  private boolean isLongRangeUnit(Unit unit) {
    return unit.getUnitType() != UnitType.KNIGHT;
  }

  private void evaluateIsGeneral(int col, int row, Board board) {
    Unit unit = board.getUnit(col, row);
    if (unit.isGeneral()) {
      unitsCosts[col][row] *= isGeneralBonus;
    }
  }

  private double evaluateGameEnd(GameState gameState, PlayerType maximizingPlayer) {
    if (gameState.getWinner() == maximizingPlayer) {
      return 1;
    } else if (gameState.getWinner() == null) {
      return 0;
    } else {
      return -1;
    }
  }
}
