package com.omnisys.pricechallenge;

public class User {

    public final long id;
    public final String username;
    public final long createdAt;

    public User(long id, String username, long createdAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
    }
}