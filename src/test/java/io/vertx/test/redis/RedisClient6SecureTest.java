package io.vertx.test.redis;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.harness.RespServerRule;
import io.vertx.redis.harness.RespServerRuleBuilder;

import org.junit.*;
import org.junit.runner.RunWith;

import java.util.UUID;

import static io.vertx.redis.client.Command.CONFIG;
import static io.vertx.redis.client.Command.GET;
import static io.vertx.redis.client.Request.cmd;

@RunWith(VertxUnitRunner.class)
public class RedisClient6SecureTest {

  @Rule
  public final RunTestOnContext rule = new RunTestOnContext();

  @ClassRule
  public static final RespServerRule redis = RespServerRuleBuilder.builder()
    .withContainerImage("redis:6.2.1")
    .withExposedPorts(6379)
    .readEnvironment()
    .build();

  private Redis client;

  @Before
  public void before(TestContext should) {
    final Async before = should.async();

    Redis setupClient = Redis.createClient(
      rule.vertx(),
      new RedisOptions().setConnectionString("redis://" + redis.getHost() + ":" + redis.getFirstMappedPort()));

    setupClient
      .send(cmd(CONFIG).arg("SET").arg("requirepass").arg("foobar")).onComplete(onConfigSet -> {
        should.assertTrue(onConfigSet.succeeded());
        // disconnect this client and create a new one
        setupClient.close();

        client = Redis.createClient(
          rule.vertx(),
          new RedisOptions().setConnectionString("redis://:foobar@" + redis.getHost() + ":" + redis.getFirstMappedPort()));

        before.complete();
      });
  }

  @After
  public void after() {
    client.close();
  }

  private static String makeKey() {
    return UUID.randomUUID().toString();
  }

  @Test(timeout = 10_000L)
  public void testBasicInterop(TestContext should) {
    final Async test = should.async();
    final String nonexisting = makeKey();

    client.send(cmd(GET).arg(nonexisting)).onComplete(reply0 -> {
      should.assertTrue(reply0.succeeded());
      should.assertNull(reply0.result());
      test.complete();
    });
  }
}
