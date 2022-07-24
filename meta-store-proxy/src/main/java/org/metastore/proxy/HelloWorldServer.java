package org.metastore.proxy;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.metastore.common.Configure;
import org.metastore.proxy.conf.ProxyConfigure;
import org.metastore.proxy.proto.GreeterGrpc;
import org.metastore.proxy.proto.HelloReply;
import org.metastore.proxy.proto.HelloRequest;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HelloWorldServer {

    private Server server;
    private static final Configure configure;

    static {
        configure = new ProxyConfigure();
    }

    private Jedis metastore;
    private Jedis serviceCenter;
    private int port;
    private String serviceName;
    private ScheduledExecutorService executor;
    private String localhost;

    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        log.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server since JVM is shutting down");
            try {
                this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            log.info("*** Server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
        if (executor != null) {
            executor.shutdown();
        }
    }


    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void register() throws InterruptedException {
        executor.scheduleWithFixedDelay(() -> {
            try {
                serviceCenter.setex(localhost + ":" + port, 3, Configure.SERVICE_LIST + "." + localhost + "." + serviceName);
                log.info("Register service " + serviceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void init() throws UnknownHostException {
        localhost = InetAddress.getLocalHost().getHostAddress();
        serviceName = configure.get("service.name");
        port = Integer.parseInt(configure.get("service.port"));

        JedisClientConfig metaStoreConfigure = DefaultJedisClientConfig.builder().password("root").build();
        metastore = new Jedis(configure.get("metastore.ip"), Integer.parseInt(configure.get("metastore.port")), metaStoreConfigure);

        JedisClientConfig serviceCenterConfigure = DefaultJedisClientConfig.builder().database(1).password(configure.get("service.center.password")).build();
        serviceCenter = new Jedis(configure.get("service.center.ip"), Integer.parseInt(configure.get("service.center.port")), serviceCenterConfigure);

        executor = Executors.newScheduledThreadPool(1);
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.init();
        server.start();
        server.register();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}