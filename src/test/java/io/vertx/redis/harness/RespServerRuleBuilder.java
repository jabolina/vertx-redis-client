package io.vertx.redis.harness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.testcontainers.containers.GenericContainer;

public final class RespServerRuleBuilder {

  public static final String VERTX_BASE_TEST_PROPERTY = "io.vertx.test";
  public static final String CONTAINER_BUILD_OVERRIDE_PROPERTY = VERTX_BASE_TEST_PROPERTY + ".container.override";
  public static final String CONTAINER_IMAGE_PROPERTY = VERTX_BASE_TEST_PROPERTY + ".container.image";
  public static final String CONTAINER_EXPOSE_PORTS_PROPERTY = VERTX_BASE_TEST_PROPERTY + ".container.ports";
  public static final String CONTAINER_ENVIRONMENT_ARGUMENT_BASE = VERTX_BASE_TEST_PROPERTY + ".container.param.";

  private final List<Integer> ports = new ArrayList<>(1);
  private final Map<String, String> parameters = new HashMap<>();
  private String image;
  private RespServerRule hijack = null;

  public static RespServerRuleBuilder builder() {
    return new RespServerRuleBuilder();
  }

  public RespServerRuleBuilder withExposedPorts(Integer ... ports) {
     this.ports.addAll(Arrays.asList(ports));
    return this;
  }

  public RespServerRuleBuilder withContainerImage(String image) {
    this.image = image;
    return this;
  }

  public RespServerRuleBuilder readEnvironment() {
    Properties properties = System.getProperties();
    Object o = properties.get(CONTAINER_BUILD_OVERRIDE_PROPERTY);
    if (o instanceof RespServerRule) {
      hijack = (RespServerRule) o;
    }

    Integer[] ports = Arrays.stream(System.getProperty(CONTAINER_EXPOSE_PORTS_PROPERTY, "").split(","))
      .filter(s -> !s.isEmpty())
      .mapToInt(Integer::parseInt)
      .boxed()
      .toArray(Integer[]::new);

    if (ports.length > 0) {
      this.ports.clear();
      withExposedPorts(ports);
    }

    String image = System.getProperty(CONTAINER_IMAGE_PROPERTY, "");
    if (!image.isEmpty()) withContainerImage(image);

    Map<String, String> env = new HashMap<>();
    properties.forEach((k, v) -> {
      if (k instanceof String && v instanceof String) {
        String key = (String) k;
        if (key.startsWith(CONTAINER_ENVIRONMENT_ARGUMENT_BASE)) {
          String replaced = key.replace(CONTAINER_ENVIRONMENT_ARGUMENT_BASE, "");
          env.put(replaced, (String) v);
        }
      }
    });

    parameters.putAll(env);
    return this;
  }

  public RespServerRule build() {
    if (hijack != null) {
      return hijack;
    }

    return new RespServerRuleImpl(image)
      .withEnv(parameters)
      .withExposedPorts(ports.toArray(new Integer[0]));
  }

  private static class RespServerRuleImpl extends GenericContainer<RespServerRuleImpl> implements RespServerRule {

    public RespServerRuleImpl(String image) {
      super(image);
    }

    @Override
    public Integer getFirstMappedPort() {
      return super.getFirstMappedPort();
    }

    @Override
    public String getHost() {
      return super.getHost();
    }

    @Override
    public GenericContainer<?> get() {
      return this;
    }
  }
}
