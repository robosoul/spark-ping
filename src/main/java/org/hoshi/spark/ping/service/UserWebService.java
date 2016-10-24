package org.hoshi.spark.ping.service;

import org.hoshi.spark.ping.model.Action;
import org.hoshi.spark.ping.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.utils.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.nio.channels.FileLockInterruptionException;

import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * @author robosoul
 */
public class UserWebService {
    public static final Logger logger =
            LoggerFactory.getLogger(UserWebService.class);

    private final int port;
    private final UserService userService;

    public UserWebService(final int port, final UserService userService) {
        this.port = port;
        this.userService = userService;
    }

    public void start() {
        logger.info("Starting UserWebService on port: {}.", port);
        userService.setup();

        port(port);
        get("/api/v1/users/read", new ReadRoute(userService));
        post("/api/v1/users/add", "application/json", new AddUserRoute(userService));
        post("/api/v1/users/delete", "text/plain", new DeleteRoute(userService));
    }

    public void stop() {
        userService.cleanup();
        Spark.stop();
    }

    private static abstract class UserRoute implements Route {
        private final UserService userService;

        UserRoute(final UserService userService) {
            this.userService = userService;
        }

        public UserService getUserService() {
            return userService;
        }

    }

    public static class DeleteRoute extends UserRoute {
        DeleteRoute(final UserService userService) {
            super(userService);
        }

        @Override
        public Object handle(final Request request, final Response response)
        throws Exception {
            final String body = request.body();
            if (body == null || body.isEmpty()) {
                logger.error("Empty request body!");
                halt(HttpServletResponse.SC_BAD_REQUEST);
            }

            while (!getUserService().delete(body).isDone()) {
                // empty
            }

            response.status(HttpServletResponse.SC_ACCEPTED);
            return "";
        }
    }

    public static class AddUserRoute extends UserRoute {
        AddUserRoute(final UserService userService) {
            super(userService);
        }

        @Override
        public Object handle(final Request request, final Response response)
        throws Exception {
            final String body = request.body();
            if (body == null || body.isEmpty()) {
                logger.error("Empty request body!");
                halt(HttpServletResponse.SC_BAD_REQUEST);
            }

            try {
                // validating request body
                User.fromJson(body);

                final Action action = getUserService().upsert(body);
                while (!action.isDone()) {
                    logger.info("Waiting...");
                    Thread.sleep(5000);
                }

                logger.info("Upsert done!");
                response.status(HttpServletResponse.SC_CREATED);
            } catch (Exception ex) {
                logger.error("Failed reading request body: '{}'", body, ex);
                halt(HttpServletResponse.SC_BAD_REQUEST);
            }

            return "";
        }
    }

    public static class ReadRoute extends UserRoute {
        ReadRoute(final UserService userService) {
            super(userService);
        }

        @Override
        public Object handle(final Request request, final Response response)
        throws Exception {
            try (final FileInputStream in = new FileInputStream(getUserService().read())) {
                response.raw().getOutputStream().write(IOUtils.toByteArray(in));
            } catch (FileLockInterruptionException ex) {
                logger.error("File not found: '{}'.", getUserService().read(), ex);
                halt(HttpServletResponse.SC_NOT_FOUND);
            } catch (Exception ex) {
                logger.error("Failed reading from '{}'.", getUserService().read(), ex);
                halt(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            return "";
        }
    }
}