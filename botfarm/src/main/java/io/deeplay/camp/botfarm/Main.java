package io.deeplay.camp.botfarm;


import io.deeplay.camp.botfarm.bots.RandomBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.MatthewsBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage.AlphaBetaMinimaxBot;
import io.deeplay.camp.botfarm.bots.matthew_bots.placement_stage.CachePlacementBot;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

  static String path = "botfarm/src/main/";

  public static void main(String[] args) throws IOException {

    deleteFilesForPathByPrefix(path, "resultgame");


    for(int i = 0; i<1;i++){
      BotFight botFight = new BotFight(new MatthewsBot(new CachePlacementBot(), new AlphaBetaMinimaxBot(4)), new RandomBot(), 1000, true);

    }

  }

  public static boolean deleteFilesForPathByPrefix(final String path, final String prefix) {
    boolean success = true;
    try (DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(Paths.get(path), prefix + "*")) {
      for (final Path newDirectoryStreamItem : newDirectoryStream) {
        Files.delete(newDirectoryStreamItem);
      }
    } catch (final Exception e) {
      success = false;
      e.printStackTrace();
    }
    return success;
  }
}
