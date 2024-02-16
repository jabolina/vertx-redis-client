package io.vertx.test.redis;

import io.vertx.core.Context;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.client.*;
import io.vertx.redis.harness.RespServerRule;
import io.vertx.redis.harness.RespServerRuleBuilder;

import org.junit.*;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;

@RunWith(VertxUnitRunner.class)
public class RedisReconnectTest {

  @Rule
  public final RunTestOnContext rule = new RunTestOnContext();

  @ClassRule
  public static final RespServerRule container = RespServerRuleBuilder.builder()
    .withContainerImage("redis:6.0.6")
    .withExposedPorts(6379)
    .readEnvironment()
    .build();

  private Redis client;

  // this connection will mutate during tests with re-connect
  private final int RETRIES = 10;
  private RedisConnection redis;

  @Before
  public void before(TestContext should) {
    final Async before = should.async();

    Context context = rule.vertx().getOrCreateContext();
    client = Redis.createClient(rule.vertx(), new RedisOptions().setConnectionString("redis://" + container.getHost() + ":" + container.getFirstMappedPort()));
    client.connect().onComplete(onConnect -> {
      should.assertTrue(onConnect.succeeded());
      should.assertEquals(context, rule.vertx().getOrCreateContext());
      redis = onConnect.result();
      before.complete();
    });
  }

  @After
  public void after() {
    client.close();
  }

  @Test
  public void testConnection(TestContext should) {
    final Async test = should.async();

    redis
      .exceptionHandler(should::fail)
      .endHandler(end -> {
        // the connection was closed, will reconnect
        reconnect(0);
      })
      .send(Request.cmd(Command.CLIENT).arg("LIST"))
      .onFailure(should::fail)
      .onSuccess(list -> {
        String res = list.toString();
        // this is a hack
        String id = res.substring(3, res.indexOf(' '));

        // kill the connection
        final RedisConnection orig = redis;

        redis
          .send(Request.cmd(Command.CLIENT).arg("KILL").arg("SKIPME").arg("no").arg("ID").arg(id))
          .onFailure(should::fail)
          .onSuccess(kill -> {
            should.assertEquals(1, kill.toInteger());
            // wait until the connection is updated
            rule.vertx()
              .setPeriodic(500, t -> {
                if (orig != redis) {
                  // when the 2 references change
                  // it means the connection has been replaced
                  test.complete();
                }
              });
          });

      });
  }

  private void reconnect(int retry) {
    if (RETRIES >= 0 && retry < RETRIES) {
      // retry with backoff
      long backoff = (long) (Math.pow(2, Math.min(retry, RETRIES)) * RETRIES);

      rule
        .vertx()
        .setTimer(backoff, timer -> client.connect().onComplete(onReconnect -> {
          if (onReconnect.failed()) {
            reconnect(retry + 1);
          } else {
            redis = onReconnect.result();
          }
        }));
    }
  }
}
