package integration.localnet;

import bootstrap.Bootstrap;

/**
 * The main runner class for orchestrating the building of the HTTP Server, Prometheus, and Grafana
 * components of the TestNet. This class also utilizes the Java Docker API in order to containerize
 * the three components and build up the necessary supporting infrastructure.
 */
public class Cmd {
  private static final short NODE_COUNT = 5;

  /**
   * The main function.
   *
   * @param args standard java parameters.
   */
  public static void main(String[] args) {
    Bootstrap bootstrap = new Bootstrap(NODE_COUNT);
    LocalTestNet testNet = new LocalTestNet(bootstrap.build());
    try {
      testNet.runLocalTestNet();
    } catch (IllegalStateException e) {
      throw new RuntimeException("could not initialize and run local net", e);
    }
  }
}