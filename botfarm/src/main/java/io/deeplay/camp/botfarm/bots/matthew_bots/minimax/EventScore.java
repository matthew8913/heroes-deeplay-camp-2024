package io.deeplay.camp.botfarm.bots.matthew_bots.minimax;

import io.deeplay.camp.game.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EventScore {
  private Event event;
  private double score;
}
