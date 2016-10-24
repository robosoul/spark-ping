/**
 * Copyright Vast 2016. All Rights Reserved.
 *
 * http://www.vast.com
 */
package org.hoshi.spark.ping.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author robosoul
 */
public class User {
    public static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("username")
    private final String username;

    @JsonProperty("password")
    private final String password;

    @JsonProperty("read_permission")
    private final boolean canRead;

    @JsonProperty("write_permission")
    private final boolean canWrite;

    @JsonCreator
    public User(
            @JsonProperty("username") final String username,
            @JsonProperty("password") final String password,
            @JsonProperty("read_permission") final boolean canRead,
            @JsonProperty("write_permission") final boolean canWrite) {
        this.username = username;
        this.password = password;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }

    @Override
    public String toString() {
        try {
            return DEFAULT_OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static User fromJson(final String json) throws IOException {
        return DEFAULT_OBJECT_MAPPER.readValue(json, User.class);
    }

    public static Collection<User> fromJson(final InputStream json)
    throws IOException {
        return DEFAULT_OBJECT_MAPPER.readValue(json, new TypeReference<Collection<User>>() {});
    }

    public static void toJson(final OutputStream out, Collection<User> users)
    throws IOException {
        DEFAULT_OBJECT_MAPPER.writeValue(out, users);
    }
}