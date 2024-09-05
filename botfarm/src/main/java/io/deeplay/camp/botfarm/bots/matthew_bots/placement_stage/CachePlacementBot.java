package io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.entities.Unit;
import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Бот для этапа placement. Для генерации расстановки использует заранее подготовленные файлы. */
public class CachePlacementBot extends PlacementBot {
  private static final Logger logger = LoggerFactory.getLogger(CachePlacementBot.class);

  /** Список лучших расстановок для первого игрока. */
  private final List<Unit[][]> bestFirstPlayerPlacements;

  /** Мапа, где ключ - расстановка первого игрока, а значение - расстановка второго игрока. */
  private final Map<Unit[][], Unit[][]> counterPlacements;

  /**
   * Стэк с уже сгенерированной расстановкой. Необходим, так как взаимодействие с игрой происходит
   * через ивенты.
   */
  private Stack<PlaceUnitEvent> placeUnitEvents;

  /** Конструктор бота, загружающий кэш. */
  public CachePlacementBot() {
    bestFirstPlayerPlacements = loadBestFirstPlayerPlacements();
    counterPlacements = loadCounterPlacements();
    placeUnitEvents = new Stack<>();
  }

  /**
   * Метод загружает данные из csv файла с расстановками и контррасстановками в хэш-мапу.
   *
   * @return мапу с расстановками и контррасстановками.
   */
  private Map<Unit[][], Unit[][]> loadCounterPlacements() {
    Map<Unit[][], Unit[][]> counterPlacements = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    try (BufferedReader br =
        new BufferedReader(
            new FileReader("botfarm/src/main/resources/matthews_bots/counter-placements.csv"))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length == 2) {
          Unit[][] placement = objectMapper.readValue(parts[0], Unit[][].class);
          Unit[][] counterPlacement = objectMapper.readValue(parts[1], Unit[][].class);
          counterPlacements.put(placement, counterPlacement);
        }
      }
    } catch (IOException e) {
      logger.error("Ошибка в загрузке контррастановок!");
    }
    return counterPlacements;
  }

  /**
   * Метод загружает данные из json файла с лучшими расстановками первого игрока в список.
   *
   * @return список с расстановками.
   */
  private List<Unit[][]> loadBestFirstPlayerPlacements() {
    List<Unit[][]> bestPlacements = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();
    try (BufferedReader br =
        new BufferedReader(
            new FileReader("botfarm/src/main/resources/top-first-placements.json"))) {
      String line;
      while ((line = br.readLine()) != null) {
        Unit[][] placement = objectMapper.readValue(line, Unit[][].class);
        bestPlacements.add(placement);
      }
    } catch (IOException e) {
      logger.error("Ошибка в загрузке расстановок для первого игрока.");
    }
    return bestPlacements;
  }

  /**
   * Метод генерации хода этапа Placement.
   *
   * @param gameState Игровое состояние.
   * @return ход.
   */
  @Override
  public PlaceUnitEvent generatePlaceUnitEvent(GameState gameState) {
    GameState gameStateCopy = gameState.getCopy();
    if (placeUnitEvents.isEmpty()) {
      if (gameStateCopy.getCurrentPlayer() == PlayerType.FIRST_PLAYER) {
        Unit[][] placement =
            bestFirstPlayerPlacements.get(new Random().nextInt(bestFirstPlayerPlacements.size()));
        placeUnitEvents = generateEventsFromPlacements(placement, PlayerType.FIRST_PLAYER);
      } else {
        Unit[][] enemyPlacement = new Unit[Board.COLUMNS][Board.ROWS];
        Board board = gameStateCopy.getBoard();
        for (int col = 0; col < Board.COLUMNS; col++) {
          for (int row = 0; row < Board.ROWS / 2; row++) {
            enemyPlacement[col][row] = board.getUnit(col, row);
          }
        }
        if (counterPlacements.containsKey(enemyPlacement)) {
          placeUnitEvents =
              generateEventsFromPlacements(
                  counterPlacements.get(enemyPlacement), PlayerType.FIRST_PLAYER);
        } else {
          int randomInd = new Random().nextInt(bestFirstPlayerPlacements.size());
          System.out.println(randomInd);
          Unit[][] placement = invertPlacement(bestFirstPlayerPlacements.get(randomInd));
          placeUnitEvents = generateEventsFromPlacements(placement, PlayerType.SECOND_PLAYER);
        }
      }
    }
    return placeUnitEvents.pop();
  }

  /**
   * Метод, инвертирующий расстановку первого игрока в расстановку для второго игрока.
   *
   * @param placement расстановка.
   * @return инвертированная расстановка.
   */
  private Unit[][] invertPlacement(Unit[][] placement) {
    Unit[][] temp = new Unit[Board.COLUMNS][Board.ROWS];
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS / 2; row++) {
        temp[col][row] = placement[Board.COLUMNS - col - 1][Board.ROWS / 2 - row - 1];
      }
    }
    return temp;
  }

  /**
   * Метод заполняющий стэк ивентов по двумерному массиву расстановки.
   *
   * @param placement расстановка.
   * @param playerType игрок, чья эта расстановка.
   * @return стэк ивентов.
   */
  private Stack<PlaceUnitEvent> generateEventsFromPlacements(
      Unit[][] placement, PlayerType playerType) {
    Stack<PlaceUnitEvent> events = new Stack<>();
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS / 2; row++) {
        int eventRow = PlayerType.FIRST_PLAYER == playerType ? row : row + Board.ROWS / 2;
        events.add(
            new PlaceUnitEvent(
                col,
                eventRow,
                placement[col][row].getCopy(),
                playerType,
                col != 0 || row != 0,
                placement[col][row].isGeneral()));
      }
    }
    return events;
  }
}
