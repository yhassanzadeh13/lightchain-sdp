package metrics.integration;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import integration.localnet.ContainerLogger;
import metrics.collectors.MetricServer;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

/**
 * Creates a metrics collection network that is composed of a grafana and a prometheus containers.
 * The grafana container is exposed at localhost:3000.
 * The prometheus container is exposed at localhost:9090.
 */
public class MetricsTestNet {
  private static final String LIGHTCHAIN_PREFIX = "lightchain_";
  protected static final String NETWORK_NAME = LIGHTCHAIN_PREFIX + "network";
  // common
  private static final String MAIN_TAG = "main";
  private static final String USER_DIR = "user.dir";
  private static final String NETWORK_DRIVER_NAME = "bridge";
  // Prometheus
  private static final int PROMETHEUS_PORT = 9090;
  private static final String PROMETHEUS_CONTAINER_NAME = LIGHTCHAIN_PREFIX + "prometheus";
  private static final String PROMETHEUS_YAML_PATH = "prometheus/prometheus.yml";
  private static final String PROMETHEUS_IMAGE = "prom/prometheus";
  private static final String PROMETHEUS_VOLUME_NAME = LIGHTCHAIN_PREFIX + "prometheus_volume";
  private static final String PROMETHEUS_VOLUME_BINDING_VOLUME =
      PROMETHEUS_VOLUME_NAME + ":/prometheus";
  private static final String PROMETHEUS_CONFIG_PATH = "prometheus";
  private static final String PROMETHEUS_TARGETS_FILE = PROMETHEUS_CONFIG_PATH + "/targets.json";
  private static final String PROMETHEUS_MAIN_CMD = "prom/prometheus:main";
  private static final String PROMETHEUS_VOLUME_BINDING_ETC =
      "/prometheus" + ":" + "/etc/prometheus";
  // Grafana
  private static final int GRAFANA_PORT = 3000;
  private static final String GRAFANA_CONTAINER_NAME = LIGHTCHAIN_PREFIX + "grafana";
  private static final String GRAFANA_VOLUME_NAME = LIGHTCHAIN_PREFIX + "grafana_volume";
  private static final String GRAFANA_VOLUME_BINDING = GRAFANA_VOLUME_NAME + ":/var/lib/grafana";
  private static final String GRAFANA_IMAGE = "grafana/grafana";
  private static final String GRAFANA_MAIN_CMD = "grafana/grafana:main";
  private static final String GRAFANA_NO_SIGN_UP = "GF_USERS_ALLOW_SIGN_UP=false";
  private static final String GRAFANA_ADMIN_USER_NAME =
      "GF_SECURITY_ADMIN_USER=${ADMIN_USER" + ":-admin}";
  private static final String GRAFANA_ADMIN_PASSWORD =
      "GF_SECURITY_ADMIN_PASSWORD=$" + "{ADMIN_PASSWORD:-admin}";
  private static final String GRAFANA_DASHBOARD_BINDING =
      "/grafana/provisioning/dashboards:/etc/grafana/provisioning" + "/dashboards";
  private static final String GRAFANA_DATA_SOURCE_BINDING =
      "/grafana/provisioning/datasources:/etc/grafana" + "/provisioning/datasources";
  protected final DockerClient dockerClient;
  protected final ContainerLogger containerLogger;
  private final Logger logger = LightchainLogger.getLogger(MetricsTestNet.class.getCanonicalName());

  /**
   * Prometheus targets to be scraped by prometheus. The targets are expected to be in the format
   * of host:port.
   */
  private final List<String> prometheusTargets;

  /**
   * Default constructor.
   */
  public MetricsTestNet(List<String> targets) {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    DockerHttpClient client = new ApacheDockerHttpClient.Builder().dockerHost(
            config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build();

    this.prometheusTargets = new ArrayList<>(targets);
    this.dockerClient = DockerClientImpl.getInstance(config, client);
    this.containerLogger = new ContainerLogger(dockerClient);
  }

  /**
   * Creates and runs a prometheus and grafana containers that are interconnected.
   *
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  public void runMetricsTestNet() throws IllegalStateException {
    try {
      PrometheusTargetWriter.writeTargetsToFile(prometheusTargets, PROMETHEUS_TARGETS_FILE);
    } catch (IOException e) {
      throw new IllegalStateException("failed to write prometheus targets to file", e);
    }

    // Volume check + create if absent
    this.createVolumesIfNotExist(PROMETHEUS_VOLUME_NAME);
    this.createVolumesIfNotExist(GRAFANA_VOLUME_NAME);

    // Network
    logger.info("creating docker network");
    this.createNetworkIfNotExist();
    logger.info("created docker network");

    // Prometheus
    logger.info("creating prometheus container");
    CreateContainerResponse prometheusContainer = createPrometheusContainer();
    dockerClient.startContainerCmd(prometheusContainer.getId()).exec();
    logger.info("created prometheus container");

    // Grafana
    logger.info("creating grafana container");
    CreateContainerResponse grafanaContainer = this.createGrafanaContainer();
    dockerClient.startContainerCmd(grafanaContainer.getId()).exec();
    logger.info("created grafana container");

    this.logger.info("prometheus is running at localhost:{}", PROMETHEUS_PORT);
    this.logger.info("grafana is running at localhost:{}", GRAFANA_PORT);
  }

  /**
   * Checks for existence of given volume name in the client, and creates one with the
   * given name if volume name does not exist.
   *
   * @param volumeName volume name to create.
   */
  protected void createVolumesIfNotExist(String volumeName) {
    ListVolumesResponse volumesResponse = this.dockerClient.listVolumesCmd().exec();
    List<InspectVolumeResponse> volumes = volumesResponse.getVolumes();

    for (InspectVolumeResponse v : volumes) {
      if (v.getName().equals(volumeName)) {
        // volume exists
        return;
      }
    }

    // volume name does not exist, create one.
    this.dockerClient.createVolumeCmd().withName(volumeName).exec();
  }

  /**
   * Checks for existence of the given network in the client, and creates one with the given name
   * if the network does not exist.
   */
  private void createNetworkIfNotExist() {
    List<Network> networks = this.dockerClient.listNetworksCmd().exec();

    for (Network n : networks) {
      if (n.getName().equals(NETWORK_NAME)) {
        // network exists
        return;
      }
    }

    // network does not exist, create one/
    dockerClient.createNetworkCmd().withName(NETWORK_NAME).withDriver(NETWORK_DRIVER_NAME).exec();
  }

  /**
   * Creates and returns a Grafana container.
   *
   * @return create container response for grafana.
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  private CreateContainerResponse createGrafanaContainer() throws IllegalStateException {
    try {
      this.dockerClient.pullImageCmd(GRAFANA_IMAGE)
          .withTag(MAIN_TAG)
          .exec(new PullImageResultCallback())
          .awaitCompletion(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new IllegalStateException("could not run grafana container" + ex);
    }

    Ports grafanaPortBindings = new Ports();
    grafanaPortBindings.bind(ExposedPort.tcp(GRAFANA_PORT), Ports.Binding.bindPort(GRAFANA_PORT));

    List<Bind> grafBinds = new ArrayList<Bind>();
    grafBinds.add(Bind.parse(GRAFANA_VOLUME_BINDING));
    grafBinds.add(Bind.parse(System.getProperty(USER_DIR) + GRAFANA_DASHBOARD_BINDING));
    grafBinds.add(Bind.parse(System.getProperty(USER_DIR) + GRAFANA_DATA_SOURCE_BINDING));

    HostConfig hostConfig = new HostConfig().withBinds(grafBinds)
        .withNetworkMode(NETWORK_NAME)
        .withPortBindings(grafanaPortBindings);

    try {
      return this.dockerClient.createContainerCmd(GRAFANA_MAIN_CMD)
          .withName(GRAFANA_CONTAINER_NAME)
          .withTty(true)
          .withEnv(GRAFANA_ADMIN_USER_NAME)
          .withEnv(GRAFANA_ADMIN_PASSWORD)
          .withEnv(GRAFANA_NO_SIGN_UP)
          .withHostConfig(hostConfig)
          .exec();
    } catch (ConflictException ex) {
      // Get the existing container's info instead of throwing an exception
      List<Container> containers = this.dockerClient.listContainersCmd()
          .withShowAll(true)
          .withNameFilter(List.of(GRAFANA_CONTAINER_NAME))
          .exec();

      if (containers.size() > 0) {
        Container container = containers.get(0);
        CreateContainerResponse response = new CreateContainerResponse();
        response.setId(container.getId());
        return response;

      } else {
        throw new IllegalStateException("unable to retrieve existing grafana container: " + ex);
      }
    }
  }

  /**
   * Creates and returns a Prometheus container.
   *
   * @return create container response for prometheus.
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  private CreateContainerResponse createPrometheusContainer() throws IllegalStateException {
    try {
      this.dockerClient.pullImageCmd(PROMETHEUS_IMAGE)
          .withTag(MAIN_TAG)
          .exec(new PullImageResultCallback())
          .awaitCompletion(180, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new IllegalStateException("could not run prometheus container" + ex);
    }

    Ports promPortBindings = new Ports();
    promPortBindings.bind(ExposedPort.tcp(PROMETHEUS_PORT),
        Ports.Binding.bindPort(PROMETHEUS_PORT));

    List<Bind> promBinds = new ArrayList<Bind>();
    promBinds.add(Bind.parse(System.getProperty(USER_DIR) + PROMETHEUS_VOLUME_BINDING_ETC));
    promBinds.add(Bind.parse(PROMETHEUS_VOLUME_BINDING_VOLUME));

    HostConfig hostConfig = new HostConfig().withBinds(promBinds)
        .withNetworkMode(NETWORK_NAME)
        .withPortBindings(promPortBindings);

    try {
      return this.dockerClient.createContainerCmd(PROMETHEUS_MAIN_CMD)
          .withName(PROMETHEUS_CONTAINER_NAME)
          .withTty(true)
          .withHostConfig(hostConfig)
          .exec();
    } catch (ConflictException ex) {
      // Get the existing container's info instead of throwing an exception
      List<Container> containers = this.dockerClient.listContainersCmd()
          .withShowAll(true)
          .withNameFilter(List.of(PROMETHEUS_CONTAINER_NAME))
          .exec();

      if (containers.size() > 0) {
        Container container = containers.get(0);
        CreateContainerResponse response = new CreateContainerResponse();
        response.setId(container.getId());
        return response;
      } else {
        throw new IllegalStateException("unable to retrieve existing prometheus container: " + ex);
      }
    }
  }

  /**
   * Reads the IP address of the local machine and overrides the prometheus configuration file.
   *
   * @throws IllegalStateException when the local address cannot be obtained or the prometheus
   *                               configuration cannot be read or written.
   */
  public void overridePrometheusMetricServerAddress() throws IllegalStateException {
    // Obtain the local address
    String localAddress = null;
    try {
      localAddress = Inet4Address.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      throw new IllegalStateException("could not get local address: " + e);
    }

    // Change the prometheus configuration
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    File prometheusConfig = new File(PROMETHEUS_YAML_PATH);
    Map<String, Object> config = null;
    try {
      config = objectMapper.readValue(prometheusConfig, new TypeReference<Map<String, Object>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("could not read prometheus config: " + e);
    }

    List<Object> scrapeConfigs = (List<Object>) config.get("scrape_configs");
    Map<String, Object> simulatorJob = (Map<String, Object>) scrapeConfigs.get(0);
    List<Object> staticConfigs = (List<Object>) simulatorJob.get("static_configs");
    Map<String, Object> targets = (Map<String, Object>) staticConfigs.get(0);
    targets.put("targets", List.of(localAddress + ":" + MetricServer.SERVER_PORT));

    // write again on the file
    try {
      objectMapper.writeValue(new File(PROMETHEUS_YAML_PATH), config);
    } catch (IOException e) {
      throw new IllegalStateException("could not write prometheus config: " + e);
    }
  }
}
