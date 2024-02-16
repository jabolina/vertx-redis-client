package io.vertx.redis.harness;

import java.util.function.Supplier;

import org.junit.rules.TestRule;
import org.testcontainers.containers.GenericContainer;

public interface RespServerRule extends TestRule, Supplier<GenericContainer<?>> {

  default String getHost() {
    return get().getHost();
  }

  default Integer getFirstMappedPort() {
    return get().getFirstMappedPort();
  }
}
