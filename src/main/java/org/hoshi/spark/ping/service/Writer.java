package org.hoshi.spark.ping.service;

import com.google.common.base.Preconditions;
import org.hoshi.spark.ping.model.Action;
import org.hoshi.spark.ping.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author robosoul
 */
public class Writer {
    public static final Logger logger = LoggerFactory.getLogger(Writer.class);

    private final ScheduledExecutorService executor;
    private final File file;
    private final BlockingQueue<Action> queue;

    public Writer(final File file, final BlockingQueue<Action> queue) {
        Preconditions.checkNotNull(file, "file must not be null");
        Preconditions.checkArgument(file.exists(), "file must exist");
        Preconditions.checkArgument(file.canRead(), "file must be readable");
        Preconditions.checkArgument(file.canWrite(), "file must be writable");
        this.file = file;

        Preconditions.checkNotNull(queue, "queue must not be null");
        this.queue = queue;

        this.executor = Executors.newScheduledThreadPool(
                1,
                r -> {
                    final Thread thread = new Thread(r);
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        logger.error("UncaughtException", e);
                    });
                    return thread;
                });
    }

    public void start() {
        executor.scheduleAtFixedRate(new WriterRunnable(file, queue), 0, 10, TimeUnit.SECONDS);
    }

    public void shutdown() {
        logger.info("Shutting down executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                logger.warn("Failed finishing procession all requests in {} {}!", 1, TimeUnit.MICROSECONDS);
                logger.warn("Shutting down now!");
                executor.shutdownNow();

                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    logger.error("Executor did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            logger.error("Failed processing all request.", ex);
        }
        logger.info("Done!");
    }

    private static class WriterRunnable implements Runnable {
        private final File file;
        private final BlockingQueue<Action> queue;

        WriterRunnable(final File file, final BlockingQueue<Action> queue) {
            this.file = file;
            this.queue = queue;
        }

        @Override
        public void run() {
            logger.info("Draining the queue...");
            final Collection<Action> actions = new ArrayList<>();
            queue.drainTo(actions);
            logger.info("Got {} results", actions.size());

            if (actions.isEmpty()) {
                logger.info("Ignoring.");
                return;
            }

            Map<String, User> users;
            logger.info("Reading users from '{}'...", file.getName());
            try (final FileInputStream in = new FileInputStream(file)) {
                users =
                        User.fromJson(in)
                                .stream()
                                .collect(Collectors.toMap
                                        (User::getUsername,
                                         Function.identity()));
            } catch (FileNotFoundException ex) {
                logger.error("File '{}' doesn't exist!", file.getName(), ex);
                // this should never happen
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                logger.error("Failed reading from '{}'!", file.getName(), ex);
                // empty file
                users = null;
            }

            if (users == null) {
                // first time (file might empty)
                users = new HashMap<>();
            }

            for (Action action : actions) {
                if (action.getActionType() == Action.ActionType.DELETE) {
                    users.remove(action.getContent());
                } else if (action.getActionType() == Action.ActionType.UPSERT) {
                    try {
                        final User user = User.fromJson(action.getContent());
                        users.put(user.getUsername(), user);
                    } catch (IOException e) {
                        // this should never happen
                        logger.error("Failed reading user!");
                    }
                }

                action.markDone();
            }

            logger.info("Writing '{}' users to '{}'...", users.size(), file.getName());
            try (final FileOutputStream out = new FileOutputStream(file)) {
                User.toJson(out, users.values());
            } catch (FileNotFoundException ex) {
                logger.error("File '{}' doesn't exist!", file.getName(), ex);
                // this should never happen
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                logger.error("Failed writing to '{}'!", file.getName(), ex);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        final File file = new File("xxx.json");

        try (FileInputStream in = new FileInputStream(file)) {
            Collection<User> users = User.fromJson(in);
        } catch (IOException ex) {
            logger.error("Failed reading from '{}'!", file.getName(), ex);
        }


//        final File file = new File("test.json");
//        final BlockingQueue<Action> users = new ArrayBlockingQueue<>(10000);
//
//        users.add(new Action("zivko", Action.ActionType.DELETE));
//
//        final Writer writer = new Writer(file, users);
//        writer.start();
//
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                logger.info("Shutting down Writer...");
//                writer.shutdown();
//                logger.info("Done.");
//            }
//        });
    }
}
