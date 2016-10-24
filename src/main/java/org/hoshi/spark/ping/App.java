package org.hoshi.spark.ping;

import org.hoshi.spark.ping.service.UserServiceImpl;
import org.hoshi.spark.ping.service.UserWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * @author robosoul
 */
public class App {
    public static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(final String[] args) throws Exception {
        int port = getHerokuAssignedPort();

        final File file = new File("test.json");
        if (!file.exists()) {
            logger.error("File '{}' doesn't exist. Exiting...", file.getName());
            return;
        }

        if (!file.canRead()) {
            logger.error("File '{}' must be readable. Exiting...", file.getName());
            return;
        }

        if (!file.canWrite()) {
            logger.error("File '{}' must be writable. Exiting...", file.getName());
            return;
        }

        final UserWebService service =
                new UserWebService(port, new UserServiceImpl(file));

        logger.info("Starting service on {}...", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping application...");
            service.stop();
            logger.info("Stopped.");
        }));

        service.start();
    }

    static int getHerokuAssignedPort() {
        return Optional
                .ofNullable(new ProcessBuilder().environment().get("PORT"))
                .filter(p -> !p.isEmpty())
                .map(Integer::parseInt)
                .orElse(4567);
    }
}
