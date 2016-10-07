package org.hoshi.spark.ping;

import java.util.Optional;

import static spark.Spark.get;
import static spark.Spark.port;

/**
 * @author robosoul
 */
public class App {
    public static void main(final String[] args) {
        int port = getHerokuAssignedPort();
        System.out.println("Port: " + port);

        port(port);
        get("/api/v1/ping", (request, response) -> "frame-pong");
    }

    static int getHerokuAssignedPort() {
        return Optional
                .ofNullable(new ProcessBuilder().environment().get("PORT"))
                .filter(p -> !p.isEmpty())
                .map(Integer::parseInt)
                .orElse(4567);
    }
}
