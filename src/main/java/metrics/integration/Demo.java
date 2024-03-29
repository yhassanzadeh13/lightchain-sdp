package metrics.integration;

import java.util.List;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;

/**
 * Demonstrative class to set up a Prometheus server and create LightChain Counter and Gauge instances.
 */
public class Demo {
  private static final List<String> DEFAULT_TARGETS = List.of("host.docker.internal:" + MetricServer.SERVER_PORT);
  private static LightChainCollector collector;
  private static Counter finalizedBlockCount;
  private static Gauge currentBlockCount;

  /**
   * main function.
   *
   * @param args standard Java args
   */
  public static void main(String[] args) {
    MetricsTestNet testNet = new MetricsTestNet(DEFAULT_TARGETS);
    MetricServer server = new MetricServer();

    try {
      testNet.runMetricsTestNet();
    } catch (IllegalStateException e) {
      System.err.println("could not run metrics testnet" + e);
      System.exit(1);
    }

    // Metric Server Initiation
    try {

      collector = new LightChainCollector();

      finalizedBlockCount = collector.counter().register("finalized_block_count",
          "consensus", "proposal", "Finalized block count");

      currentBlockCount = collector.gauge().register("current_block_count",
          "consensus", "proposal", "Finalized block count");

    } catch (IllegalArgumentException ex) {
      System.err.println("Could not initialize the metrics with the provided arguments" + ex);
      System.exit(1);
    }

    try {
      server.start();
    } catch (IllegalStateException ex) {
      System.err.println("Could not start the Metric Server");
      System.exit(1);
    }

    while (true) {
      try {
        Thread.sleep(1000);
        finalizedBlockCount.inc(1);
        currentBlockCount.inc(1);
        System.out.println("Finalized block count: " + finalizedBlockCount.get());
        System.out.println("Current block count: " + currentBlockCount.get());
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }
    }

    try {
      server.terminate();
    } catch (IllegalStateException ex) {
      System.err.println("Could not stop the Metric Server");
      System.exit(1);
    }

  }
}
