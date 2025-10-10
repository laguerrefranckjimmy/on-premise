package com.example;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
public class VertxMain extends AbstractVerticle {
  public void start() {
    Router router = Router.router(vertx);
    router.get("/stream").handler(rc -> {
      rc.response().putHeader("content-type","text/event-stream");
      rc.response().end("data: hello from vertx\n\n");
    });
    vertx.createHttpServer().requestHandler(router).listen(8080);
  }
  public static void main(String[] args){
    Vertx.vertx().deployVerticle(new VertxMain());
  }
}
