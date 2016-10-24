
package org.hoshi.spark.ping.service;

import org.hoshi.spark.ping.model.Action;

/**
 * @author robosoul
 */
public interface UserService {
    void setup();

    Action upsert(String content);

    Action delete(String username);

    String read();

    void cleanup();
}
