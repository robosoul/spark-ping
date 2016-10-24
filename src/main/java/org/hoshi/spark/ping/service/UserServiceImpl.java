package org.hoshi.spark.ping.service;

import com.google.common.base.Preconditions;
import org.hoshi.spark.ping.model.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author robosoul
 */
public class UserServiceImpl implements UserService {
    public static final Logger logger =
            LoggerFactory.getLogger(UserServiceImpl.class);

    // file where users are stores
    private final File usersFile;

    // queue holding new incoming request
    private final BlockingQueue<Action> usersQueue;

    // responsible for updating users file data
    private final Writer writer;

    public UserServiceImpl(final File usersFile) {
        Preconditions.checkNotNull(usersFile, "file must not be null");
        Preconditions.checkArgument(usersFile.exists(), "file must exist");
        Preconditions.checkArgument(usersFile.canRead(), "file must be readable");
        Preconditions.checkArgument(usersFile.canWrite(), "file must be writable");

        this.usersFile = usersFile;
        this.usersQueue = new LinkedBlockingQueue<>(10000);
        this.writer = new Writer(usersFile, usersQueue);
    }

    @Override
    public void setup() {
        logger.info("Setting up...");
        writer.start();
        logger.info("Done setting up.");
    }

    @Override
    public Action upsert(final String content) {
        logger.info("Upserting '{}'...", content);
        final Action action = new Action(content, Action.ActionType.UPSERT);
        usersQueue.add(action);
        return action;
    }

    @Override
    public Action delete(final String username) {
        logger.info("Deleting '{}'...", username);

        final Action action = new Action(username, Action.ActionType.DELETE);
        usersQueue.add(action);
        return action;
    }

    @Override
    public String read() {
        logger.info("Reading '{}'...");
        final String path = usersFile.getAbsolutePath();
        logger.info("Done reading.");
        return path;
    }

    @Override
    public void cleanup() {
        logger.info("Cleaning up...");
        writer.shutdown();
        logger.info("Done cleaning up");
    }
}
