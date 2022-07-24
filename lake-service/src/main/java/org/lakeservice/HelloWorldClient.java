package org.lakeservice;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.lakeservice.conf.LakeServiceConfigure;
import org.metastore.common.Configure;
import org.metastore.proxy.proto.GreeterGrpc;
import org.metastore.proxy.proto.HelloReply;
import org.metastore.proxy.proto.HelloRequest;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HelloWorldClient {

    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private static final Jedis serviceCenter;
    private static Configure configure = new LakeServiceConfigure();

    static {
        JedisClientConfig serviceCenterConfigure = DefaultJedisClientConfig.builder()
                .password(configure.get("service.center.password"))
                .database(1)
                .build();
        serviceCenter = new Jedis(configure.get("service.center.ip"), Integer.parseInt(configure.get("service.center.port")), serviceCenterConfigure);
    }

    public HelloWorldClient(Channel channel) {
        blockingStub = GreeterGrpc.newBlockingStub(channel);

    }

    public void greet(String name) {
        log.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.withDeadlineAfter(4, TimeUnit.SECONDS).sayHello(request);
        } catch (StatusRuntimeException e) {
            log.info("RPC failed: {}", e.getStatus());
            return;
        }
        log.info("Greeting: " + response.getMessage());
    }

    public static void main(String[] args) throws Exception {
        Map<String, Integer> cnt = new HashMap<>();
        for (int idx = 0; idx < 100; ++idx) {
            String user = "world";
            String target = serviceCenter.randomKey();
            log.info("Service: " + target);
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder.forTarget(target)
                        .usePlaintext()
                        .disableRetry()
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                HelloWorldClient client = new HelloWorldClient(channel);
                client.greet(user);
            } finally {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
            cnt.put(target, cnt.getOrDefault(target, 0) + 1);
        }
        for (String target : cnt.keySet()) {
            log.info(target + ": " + cnt.get(target));
        }
    }
}