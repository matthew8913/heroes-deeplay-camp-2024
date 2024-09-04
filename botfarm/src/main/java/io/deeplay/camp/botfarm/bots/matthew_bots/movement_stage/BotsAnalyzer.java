package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.CachePlacementBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.RandomPlacementBot;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameStage;
import io.deeplay.camp.game.mechanics.GameState;
import io.deeplay.camp.game.mechanics.PlayerType;
import lombok.SneakyThrows;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BotsAnalyzer {
    final String path = "botfarm/src/main/resources";
    File moveTimeData;
    File winPercentsWithRandomPlacement;
    File winPercentsWithCachePlacement;

    public BotsAnalyzer() {
        this.moveTimeData = new File(path, "moveTimeData.txt");
        this.winPercentsWithRandomPlacement = new File(path, "winPercentsWithRandomPlacement.txt");
        this.winPercentsWithCachePlacement = new File(path, "winPercentsWithCachePlacement.txt");
    }

    public static void main(String[] args) throws IOException {
        BotsAnalyzer botsAnalyzer = new BotsAnalyzer();

        List<MovementBot> bots = new ArrayList<>();
        bots.add(new MonteCarloMinimaxBot(0));
        bots.add(new MinimaxBot(0));
        bots.add(new AlphaBetaMinimaxBot(0));
        bots.add(new MultiThreadMinimaxBot(0));
        bots.add(new ExpectimaxBot(0));
        bots.add(new MultiThreadExpectimaxBot(0));
        bots.add(new MinimaxWithSimpleClusterizationBot(0, 5));
        //botsAnalyzer.analyzeMoveTimes(bots);
//        bots.removeFirst();
//        botsAnalyzer.analyzeRandomFightsWithRandomPlacement(bots, 1000);
//        botsAnalyzer.analyzeRandomFightsWithoutRandomPlacement(bots, 1000);

//        botsAnalyzer.plotWinPercentages(botsAnalyzer.winPercentsWithRandomPlacement);
//        botsAnalyzer.plotWinPercentages(botsAnalyzer.winPercentsWithCachePlacement);
//
//        botsAnalyzer.plotMoveTimes(botsAnalyzer.moveTimeData);
    }

    private void plotWinPercentages(File file) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries currentSeries = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches(".*Bot")) {
                try {
                    currentSeries = dataset.getSeries(line);
                } catch (Exception e) {
                    currentSeries = new XYSeries(line);
                    dataset.addSeries(currentSeries);
                }

            } else if (line.matches("\\d+")) {
                int depth = Integer.parseInt(line);
                double winPercentage = Double.parseDouble(lines.get(i + 1));
                currentSeries.add(depth, winPercentage);
            }
        }

        org.jfree.chart.JFreeChart chart = ChartFactory.createXYLineChart(
                "Win Percentages by Depth",
                "Depth",
                "Win Percentage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setLowerBound(0.7);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f)); // Устанавливаем толщину линии
        }
        plot.setRenderer(renderer);

        ChartFrame frame = new ChartFrame("Win Percentages", chart);
        frame.pack();
        frame.setVisible(true);
    }

    private void plotMoveTimes(File file) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries currentSeries = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches(".*Bot")) {
                currentSeries = new XYSeries(line);
                dataset.addSeries(currentSeries);
            } else if (line.matches("\\d+")) {
                int depth = Integer.parseInt(line);
                long time = Long.parseLong(lines.get(i + 1));
                currentSeries.add(depth, time);
                i++;
            }
        }

        org.jfree.chart.JFreeChart chart = ChartFactory.createXYLineChart(
                "Move Times by Depth",
                "Depth",
                "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f)); // Устанавливаем толщину линии
        }
        plot.setRenderer(renderer);
        ChartFrame frame = new ChartFrame("Move Times", chart);
        frame.pack();
        frame.setVisible(true);
    }

    private void analyzeMoveTimes(List<MovementBot> bots) throws IOException {
        try (var moveTimeWriter = new FileWriter(moveTimeData)) {
            GameState gameState = new GameState();
            gameState.setDefaultPlacementWithoutMage();
            for (MovementBot bot : bots) {
                moveTimeWriter.write(bot.getClass().getSimpleName() + "\n");
                for (int i = 1; i < 10; i++) {
                    bot.setMaxDepth(i);
                    GameState copy = gameState.getCopy();
                    long startTime = System.currentTimeMillis();
                    bot.generateMakeMoveEvent(copy);
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    if (elapsedTime > 30000) {
                        moveTimeWriter.write(bot.getMaxDepth() + "\n");
                        moveTimeWriter.write(elapsedTime + "\n");
                        //System.out.println("Time exceeded 30 seconds, skipping to next bot.\n");
                        break;
                    }
                    moveTimeWriter.write(bot.getMaxDepth() + "\n");
                    moveTimeWriter.write(elapsedTime + "\n");
                }
            }
        }
    }

    @SneakyThrows
    private void analyzeRandomFightsWithRandomPlacement(List<MovementBot> bots, int gamesCount) {
        RandomMovementBot randomBot = new RandomMovementBot();
        try (var writer = new FileWriter(winPercentsWithRandomPlacement)) {
            for (MovementBot bot : bots) {
                for (int k = 1; k < 6; k++) {
                    bot.setMaxDepth(k);
                    int randomWinCount = 0;
                    int botWinCount = 0;
                    int draw = 0;
                    for (int i = 0; i < gamesCount; i++) {
                        GameState gameState = new GameState();
                        RandomPlacementBot randomPlacementBot = new RandomPlacementBot();
                        for (int j = 0; j < 12; j++) {
                            gameState.makePlacement(randomPlacementBot.generatePlaceUnitEvent(gameState));
                            if (j == 5 || j == 11) {
                                gameState.changeCurrentPlayer();
                            }
                        }
                        while (gameState.getGameStage() != GameStage.ENDED) {
                            MakeMoveEvent move;
                            if (gameState.getCurrentPlayer() == PlayerType.FIRST_PLAYER) {
                                move = bot.generateMakeMoveEvent(gameState);
                            } else {
                                move = randomBot.generateMakeMoveEvent(gameState);
                            }
                            if (move != null) {
                                gameState.makeMove(move);
                            } else {
                                gameState.changeCurrentPlayer();
                            }
                        }
                        if (gameState.getWinner() == null) {
                            draw++;
                        } else if (gameState.getWinner() == PlayerType.FIRST_PLAYER) {
                            botWinCount++;

                        } else if (gameState.getWinner() == PlayerType.SECOND_PLAYER) {
                            randomWinCount++;
                        }
                    }
                    writer.write(bot.getClass().getSimpleName() + "\n");
                    writer.write(bot.getMaxDepth() + "\n");
                    writer.write((double) botWinCount / (botWinCount + randomWinCount + draw) + "\n");
                }
            }
        }
    }

    @SneakyThrows
    private void analyzeRandomFightsWithoutRandomPlacement(List<MovementBot> bots, int gamesCount) {
        RandomMovementBot randomBot = new RandomMovementBot();
        try (var writer = new FileWriter(winPercentsWithCachePlacement)) {
            for (MovementBot bot : bots) {
                for (int k = 1; k < 6; k++) {
                    bot.setMaxDepth(k);
                    int randomWinCount = 0;
                    int botWinCount = 0;
                    int draw = 0;
                    for (int i = 0; i < gamesCount; i++) {
                        GameState gameState = new GameState();
                        RandomPlacementBot randomPlacementBot = new RandomPlacementBot();
                        CachePlacementBot cachePlacementBot = new CachePlacementBot();
                        for (int j = 0; j < 6; j++) {
                            gameState.makePlacement(cachePlacementBot.generatePlaceUnitEvent(gameState));
                        }
                        gameState.changeCurrentPlayer();
                        for (int j = 0; j < 6; j++) {
                            gameState.makePlacement(randomPlacementBot.generatePlaceUnitEvent(gameState));
                        }
                        while (gameState.getGameStage() != GameStage.ENDED) {
                            MakeMoveEvent move;
                            if (gameState.getCurrentPlayer() == PlayerType.FIRST_PLAYER) {
                                move = bot.generateMakeMoveEvent(gameState);
                            } else {
                                move = randomBot.generateMakeMoveEvent(gameState);
                            }
                            if (move != null) {
                                gameState.makeMove(move);
                            } else {
                                gameState.changeCurrentPlayer();
                            }
                        }
                        if (gameState.getWinner() == null) {
                            draw++;
                        } else if (gameState.getWinner() == PlayerType.FIRST_PLAYER) {
                            botWinCount++;

                        } else if (gameState.getWinner() == PlayerType.SECOND_PLAYER) {
                            randomWinCount++;
                        }
                    }
                    writer.write(bot.getClass().getSimpleName() + "\n");
                    writer.write(bot.getMaxDepth() + "\n");
                    writer.write((double) botWinCount / (botWinCount + randomWinCount + draw) + "\n");
                }
            }
        }
    }
}