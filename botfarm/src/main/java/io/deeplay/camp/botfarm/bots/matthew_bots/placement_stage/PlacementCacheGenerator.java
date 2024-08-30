package io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.deeplay.camp.botfarm.bots.RandomBot;
import io.deeplay.camp.game.entities.Board;
import io.deeplay.camp.game.entities.Knight;
import io.deeplay.camp.game.entities.Unit;
import io.deeplay.camp.game.entities.UnitType;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.events.PlaceUnitEvent;
import io.deeplay.camp.game.exceptions.GameException;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Класс, генерирующий кэш для этапа игры Placement. */
public class PlacementCacheGenerator {
  private static final Logger logger = LoggerFactory.getLogger(PlacementCacheGenerator.class);

  /** МНожество всех возможных расстановок. */
  final Set<String> possiblePlacements;

  /** Маппер для сериализации/десериализации. */
  private final ObjectMapper objectMapper;

  /** Файл с расстановками первого игрока. */
  private final File firstPlacementsFile;

  /** Файл с контррасстановками для расстановок из первого файла. */
  private final File secondPlacementsFile;

  /** Директория ресурсов. */
  private final File resourcesDirectory;

  /** Конструктор, загружающий файлы. */
  public PlacementCacheGenerator() {
    resourcesDirectory = new File("botfarm/src/main/resources");
    if (!resourcesDirectory.exists()) {
      resourcesDirectory.mkdir();
    }
    objectMapper = new ObjectMapper();
    possiblePlacements = new HashSet<>();
    firstPlacementsFile = new File(resourcesDirectory, "first-placements.json");
    secondPlacementsFile = new File(resourcesDirectory, "second-placements.json");
  }

  public static void main(String[] args) {
    try {
      PlacementCacheGenerator analyzer = new PlacementCacheGenerator();
      long startTime = System.currentTimeMillis();

      GameState gameState = new GameState();
      gameState.makePlacement(
          new PlaceUnitEvent(
              0, 1, new Knight(PlayerType.FIRST_PLAYER), PlayerType.FIRST_PLAYER, true, false));
      gameState.makePlacement(
          new PlaceUnitEvent(
              1, 1, new Knight(PlayerType.FIRST_PLAYER), PlayerType.FIRST_PLAYER, true, false));

      analyzer.generatePossiblePlacements(gameState.getCopy());
      long endTime = System.currentTimeMillis();
      System.out.println("Number of first: " + analyzer.possiblePlacements.size());
      System.out.println("Execution time: " + (endTime - startTime) + " ms");

      analyzer.writePlacementsToFile(analyzer.firstPlacementsFile, analyzer.possiblePlacements);
      analyzer.generateCounterPlacementsFile(analyzer.firstPlacementsFile, 0);

      analyzer.generateBestFirstPlacements();
      analyzer.generateCSVFromJsonFiles();

    } catch (GameException e) {
      logger.error("Не удалась расстановка первых юнитов.");
    } catch (IOException e) {
      logger.error("Проблемы ввода вывода.");
    }
  }

  /** Метод генерации файла с самыми эффективными расстановками. */
  private void generateBestFirstPlacements() {
    Map<String, Integer> frequencyMap = new HashMap<>();

    try (BufferedReader br = new BufferedReader(new FileReader(secondPlacementsFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        frequencyMap.put(line, frequencyMap.getOrDefault(line, 0) + 1);
      }
    } catch (IOException e) {
      logger.error("Ошибка чтения файла {}", secondPlacementsFile);
    }

    PriorityQueue<Map.Entry<String, Integer>> pq =
        new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

    for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
      pq.offer(entry);
      if (pq.size() > 3) {
        pq.poll();
      }
    }

    String topFirstPlacementsFilename = "top-first-placements.json";
    File topPlacementsFile = new File(resourcesDirectory, topFirstPlacementsFilename);
    try (FileWriter fileWriter = new FileWriter(topPlacementsFile)) {
      while (!pq.isEmpty()) {
        Map.Entry<String, Integer> entry = pq.poll();
        fileWriter.write(entry.getKey() + "\n");
      }
    } catch (IOException e) {
      logger.error(
          "Ошибка записи в файл {}", resourcesDirectory + "/" + topFirstPlacementsFilename);
    }
  }

  /**
   * Метод, запускающй рекурсию для генерации всех возможных расстановок.
   *
   * @param gameState игровое состояние.
   */
  public void generatePossiblePlacements(GameState gameState) {
    generatePossiblePlacementsRecursive(gameState.getCopy());
  }

  /**
   * Рекурсивный метод генерации всех вохможных расстановок, отталкивающийся от игрового состояния.
   *
   * @param gameState игровое состояние.
   */
  private void generatePossiblePlacementsRecursive(GameState gameState) {
    try {
      List<PlaceUnitEvent> possiblePlaces = gameState.getPossiblePlaces();
      if (possiblePlaces.isEmpty()) {
        Unit[][] placement = extractFirstPlayerPlacement(gameState.getBoard());
        String placementJson = objectMapper.writeValueAsString(placement);
        if (placement[2][1].getUnitType() == UnitType.KNIGHT) {
          possiblePlacements.add(placementJson);
        }
        return;
      }

      for (PlaceUnitEvent placeUnitEvent : possiblePlaces) {
        GameState gameStateCopy = gameState.getCopy();
        gameStateCopy.makePlacement(placeUnitEvent);
        generatePossiblePlacementsRecursive(gameStateCopy);
      }
    } catch (JsonProcessingException e) {
      logger.error("Расстановка не ссериализовалась!");
    } catch (GameException e) {
      logger.error("При построении дерева ход не применился к состоянию!");
    }
  }

  /**
   * Метод, достающий двумерный массив юнитов из доски, как расстановку первого игрока.
   *
   * @param board доска, из которой берется расстановка.
   * @return двумерный массив юнитов.
   */
  private Unit[][] extractFirstPlayerPlacement(Board board) {
    Unit[][] placement = new Unit[Board.COLUMNS][Board.ROWS / 2];
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS / 2; row++) {
        placement[col][row] = board.getUnit(col, row);
      }
    }
    return placement;
  }

  /**
   * Метод, записывающий множество расстановокв в файл.
   *
   * @param file файл для записи.
   * @param placements множество расстановок.
   * @throws IOException если происходит ошибка записи.
   */
  private void writePlacementsToFile(File file, Set<String> placements) throws IOException {
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      for (String placement : placements) {
        fileWriter.write(placement + "\n");
      }
    }
  }

  /**
   * Метод генерации файла контррасстановок(расстановок второго игрока).
   *
   * @param firstPlayerPlacements файл с расстановками первого игрока, для которого ищутся
   *     контррасстановки..
   * @param startLine строка, с которой начинает читать входной файл.
   * @throws IOException если произошла ошибка записи.
   */
  private void generateCounterPlacementsFile(File firstPlayerPlacements, int startLine)
      throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(firstPlayerPlacements))) {
      String line;
      int currentLine = 0;
      int count = 0;

      // Пропускаем строки до начальной
      while ((line = br.readLine()) != null) {
        currentLine++;
        if (currentLine < startLine) {
          continue;
        }

        Unit[][] placement = objectMapper.readValue(line, Unit[][].class);
        String counterPlacement = generateCounterPlacement(placement);
        count++;
        logger.info("Контррасстановка №" + count + startLine + " найдена.");
        writePlacementsToFile(secondPlacementsFile, Set.of(counterPlacement));
      }
    }
  }

  /**
   * Метод генерации контррасстановки для конкретной расстановки первого игрока
   *
   * @param firstPlayerPlacement расстановка первого игрока.
   * @return строку с контррасстановкой.
   * @throws IOException если произошла ошибка чтения/записи.
   */
  private String generateCounterPlacement(Unit[][] firstPlayerPlacement) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(firstPlacementsFile))) {
      String line;
      double bestPercent = 0;
      String bestCounterPlacement = "";
      while ((line = br.readLine()) != null) {
        Unit[][] counterPlacement = objectMapper.readValue(line, Unit[][].class);
        setPlayerTypeForPlacement(counterPlacement, PlayerType.SECOND_PLAYER);
        double wins = simulateBattles(firstPlayerPlacement, counterPlacement);
        if (wins >= bestPercent) {
          bestPercent = wins;
          bestCounterPlacement = objectMapper.writeValueAsString(counterPlacement);
        }
      }
      return bestCounterPlacement;
    }
  }

  /**
   * Метод для установки playerType у кадого юнита расстановки.
   *
   * @param placement расстановка.
   * @param playerType игрок.
   */
  private void setPlayerTypeForPlacement(Unit[][] placement, PlayerType playerType) {
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS / 2; row++) {
        placement[col][row].setPlayerType(playerType);
      }
    }
  }

  /**
   * Метод, симулируюший определенное количество рандомных игр.
   *
   * @param firstPlayerPlacement расстановка первого игрока.
   * @param secondPlayerPlacement расстановка второго игрока.
   * @return вероятность выигрыша второго игрока.
   */
  private double simulateBattles(Unit[][] firstPlayerPlacement, Unit[][] secondPlayerPlacement) {
    Board board = createBoard(firstPlayerPlacement, secondPlayerPlacement);
    GameState gameState = createGameState(board);
    RandomBot randomBot = new RandomBot();

    int numGames = 5000;
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    try {
      Future<Integer>[] results = new Future[numGames];
      for (int i = 0; i < numGames; i++) {
        results[i] =
            executorService.submit(new BattleSimulationTask(gameState.getCopy(), randomBot));
      }

      int firstWins = 0;
      int secondWins = 0;
      int draw = 0;
      for (Future<Integer> result : results) {
        int outcome = result.get();
        if (outcome == 1) {
          firstWins++;
        } else if (outcome == 2) {
          secondWins++;
        } else {
          draw++;
        }
      }

      return (double) secondWins / (double) (firstWins + secondWins + draw);
    } catch (ExecutionException | InterruptedException e) {
      logger.error("При симуляции рандомных игр возникли проблемы с многопоточностью.");
      return 0;
    } finally {
      executorService.shutdown();
    }
  }

  /**
   * Метод, создающий доску по двум расстановкам.
   *
   * @param placement расстановка первого игрока.
   * @param counterPlacement расстановка второго игрока.
   * @return доску.
   */
  private Board createBoard(Unit[][] placement, Unit[][] counterPlacement) {
    Board board = new Board();
    for (int col = 0; col < Board.COLUMNS; col++) {
      for (int row = 0; row < Board.ROWS / 2; row++) {
        board.setUnit(col, row, placement[col][row]);
        board.setUnit(Board.COLUMNS - col - 1, Board.ROWS - row - 1, counterPlacement[col][row]);
      }
    }
    return board;
  }

  /**
   * Метод, создающий игровое состояние для симуляции по борде.
   *
   * @param board доска.
   * @return игровое состояние.
   */
  private GameState createGameState(Board board) {
    GameState gameState = new GameState();
    gameState.setBoard(board);
    gameState.getArmyFirst().fillArmy(board);
    gameState.getArmySecond().fillArmy(board);
    gameState.setCurrentPlayer(PlayerType.FIRST_PLAYER);
    gameState.setGameStage(GameStage.MOVEMENT_STAGE);
    return gameState;
  }

  /** Метод, создающий csv файл с расстановками и контррасстановками. */
  public void generateCSVFromJsonFiles() {
    try (BufferedReader firstReader = new BufferedReader(new FileReader(firstPlacementsFile));
        BufferedReader secondReader = new BufferedReader(new FileReader(secondPlacementsFile));
        FileWriter csvWriter =
            new FileWriter(new File(resourcesDirectory, "counter-placements.csv"))) {

      String firstLine;
      String secondLine;
      while ((firstLine = firstReader.readLine()) != null
          && (secondLine = secondReader.readLine()) != null) {
        csvWriter.append(firstLine).append(",").append(secondLine).append("\n");
      }
    } catch (IOException e) {
      logger.error("Ошибка при создании csv файла расстановок!");
    }
  }

  /** Класс таски для рандомной игры. */
  private static class BattleSimulationTask implements Callable<Integer> {
    private final GameState gameState;
    private final RandomBot randomBot;

    public BattleSimulationTask(GameState gameState, RandomBot randomBot) {
      this.gameState = gameState;
      this.randomBot = randomBot;
    }

    /**
     * Метод, запускающий рандомную игру.
     *
     * @return 1,2 - выигрыш первого/второго игрока. 0 - ничья.
     */
    @Override
    public Integer call() {
      try {
        while (gameState.getGameStage() != GameStage.ENDED) {
          MakeMoveEvent moveEvent = randomBot.generateMakeMoveEvent(gameState);
          if (moveEvent == null) {
            gameState.changeCurrentPlayer();
            continue;
          }
          gameState.makeMove(moveEvent);
        }
        return gameState.getWinner() == PlayerType.FIRST_PLAYER
            ? 1
            : gameState.getWinner() == PlayerType.SECOND_PLAYER ? 2 : 0;
      } catch (GameException e) {
        logger.error("В симуляции рандомной игры возник некорректный ход!");
        return 0;
      }
    }
  }
}
