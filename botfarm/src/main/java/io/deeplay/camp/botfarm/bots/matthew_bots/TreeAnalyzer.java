package io.deeplay.camp.botfarm.bots.matthew_bots;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

/** Класс, анализирующий деревья ботов. */
@Getter
public class TreeAnalyzer {
  /** Количество вершин */
  private final AtomicInteger nodesCount = new AtomicInteger(0);

  /** Время начала генерации хода. */
  private long moveStartTime = 0L;

  /** Время окончания генерации хода. */
  private long moveEndTime = 0L;

  /** Метод засекает начало выполнения хода. */
  public void startMoveStopWatch() {
    moveStartTime = System.currentTimeMillis();
  }

  /** Метод фиксирует окончание генерации хода. */
  public void endMoveStopWatch() {
    moveEndTime = System.currentTimeMillis();
  }

  /**
   * Метод возвращающий разницу между moveEndTime и moveStartTime.
   *
   * @return время хода в наносекундах.
   */
  public long getMoveTime() {
    return moveEndTime - moveStartTime;
  }

  /** Метод, инкрементирующий количество вершин. */
  public void incrementNodesCount() {
    nodesCount.incrementAndGet();
  }

  public void printStatistics() {
    System.out.println("Tree Analysis Statistics:");
    System.out.println("------------------------");
    System.out.println("Nodes Visited: " + nodesCount);
    System.out.println("Move Time: " + getMoveTime() + " ms");
    System.out.println("------------------------");
  }
}
