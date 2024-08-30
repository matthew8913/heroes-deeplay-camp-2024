package io.deeplay.camp.botfarm.bots.matthew_bots.movement_stage;

import io.deeplay.camp.botfarm.bots.matthew_bots.TreeAnalyzer;
import io.deeplay.camp.game.events.MakeMoveEvent;
import io.deeplay.camp.game.mechanics.GameState;
import lombok.Getter;
import lombok.Setter;

/** Абстрактный класс бота для игрового состояния movement. */
@Setter
@Getter
public abstract class MovementBot {
  TreeAnalyzer treeAnalyzer;
  public MovementBot(TreeAnalyzer treeAnalyzer) {
    this.treeAnalyzer = treeAnalyzer;
  }
  public abstract MakeMoveEvent generateMakeMoveEvent(GameState gameState);


}

