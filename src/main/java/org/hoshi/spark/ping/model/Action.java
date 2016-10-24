/**
 * Copyright Vast 2016. All Rights Reserved.
 *
 * http://www.vast.com
 */
package org.hoshi.spark.ping.model;

import com.google.common.base.Preconditions;

/**
 * @author Luka Obradovic (luka@vast.com)
 */
public class Action {
    public enum ActionType { UPSERT, DELETE }

    private final String content;
    private final ActionType actionType;
    private boolean done;

    public Action(final String content, final ActionType actionType) {
        Preconditions.checkNotNull(content, "content must not be null");
        Preconditions.checkArgument(!content.isEmpty(), "content must not be empty");
        this.content = content;
        this.actionType = actionType;
    }

    public String getContent() {
        return content;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public boolean isDone() {
        return done;
    }

    public void markDone() {
        done = true;
    }
}
