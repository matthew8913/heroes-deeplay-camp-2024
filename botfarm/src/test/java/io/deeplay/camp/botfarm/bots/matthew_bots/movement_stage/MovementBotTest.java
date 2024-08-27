package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.game.entities.Archer;
import io.deeplay.camp.game.entities.Healer;
import io.deeplay.camp.game.entities.Knight;
import io.deeplay.camp.game.entities.Position;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.PlayerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovementBotTest {

  @Test
  void removeSimilarMovesTest() {
    List<MakeMoveEvent> similarMoves = new ArrayList<>();
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 0), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 1), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
    MovementBot bot = new MinimaxMovementBot(1);
    bot.removeUnnecessaryMoves(similarMoves);
    assertEquals(1, similarMoves.size());
  }

  @Test
  void removeNotSimilarMovesTest() {
    List<MakeMoveEvent> similarMoves = new ArrayList<>();
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 0), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 1), new Position(1, 1), new Knight(PlayerType.FIRST_PLAYER)));
    MovementBot bot = new MinimaxMovementBot(1);
    bot.removeUnnecessaryMoves(similarMoves);
    assertEquals(2, similarMoves.size());
  }

  @Test
  void removeUnnecessaryMovesTest() {
    List<MakeMoveEvent> similarMoves = new ArrayList<>();
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 0), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 1), new Position(2, 1), new Knight(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 0), new Position(2, 1), new Archer(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 1), new Position(2, 1), new Archer(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 0), new Position(2, 1), new Healer(PlayerType.FIRST_PLAYER)));
    similarMoves.add(
        new MakeMoveEvent(
            new Position(0, 1), new Position(2, 1), new Healer(PlayerType.FIRST_PLAYER)));
    MovementBot bot = new MinimaxMovementBot(1);
    bot.removeUnnecessaryMoves(similarMoves);
    assertEquals(3, similarMoves.size());
  }
}
