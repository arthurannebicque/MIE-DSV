/*
 * Copyright 2015, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.Scanner;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class HelloWorldServer {
  private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

  private Server server;

  private GreeterImpl greeterImpl;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 50051;
    greeterImpl = new GreeterImpl();
    server = ServerBuilder.forPort(port)
        .addService(greeterImpl)
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        HelloWorldServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  private class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    public String before:
    public String after:

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void sayHelloAgain(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello again " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void InsertBefore(InsertBeforeRequest req, StreamObserver<Empty> responseObserver) {
      InsertBeforeReply reply = InsertBeforeReply.newBuilder().setBefore(this.before).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void InsertAfter(InsertAfterRequest req, StreamObserver<Empty> responseObserver) {
    }

  }

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public HelloWorldServer(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build());
  }

  public void changeIP(String host, String port)
    {

    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build());
    }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  HelloWorldServer(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /** Say hello to server. */
  public void greet(String name) {
    logger.info("Will try to greet " + name + " ...");
    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.getMessage());
    try {
      response = blockingStub.sayHelloAgain(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.getMessage());
    }

  public static HelloWorldServer previousNode;
  public static HelloWorldServer nextNode;

  public static void main(String[] args) throws Exception {
    HelloWorldServer self = new HelloWorldServer("localhost", 50051);
    self.start();
    //server.blockUntilShutdown();
    Scanner scanner = new Scanner(System.in);
    String message = null;
    while (true) {
      message = scanner.nextLine();
      if (message.indexOf("/connect ") == 0) {
        String ip = message.substring(8).trim();
        self.greeterImpl.nextIP = ip;
        self.shutdown();
        self.changeIP(ip, 50051);
        self.start();

        InsertBeforeRequest request = InsertBeforeRequest.newBuilder();
        request.setNewBefore(ip);
        request.build();

        InsertBeforeReply reply = blockingStub.InsertBefore(request);
        greeterImpl.before = reply.getBefore();

        InsertAfterRequest request = InsertAfterRequest.newBuilder();
        request.setNewAfter(ip);
        request.build();
      }
      /* Access a service running on the local machine on port 50051 */
      String user = ip;
      self.greet(message);
    }
  }
}
