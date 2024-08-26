package io.deeplay.camp.botfarm.bots.matthew_bots.evaluate;

import io.deeplay.camp.game.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Класс, содержащий ивент и его оценку. Используется в алгоритмах ботов. */
@AllArgsConstructor
@Getter
@Setter
public class EventScore {
  private Event event;
  private double score;
}
