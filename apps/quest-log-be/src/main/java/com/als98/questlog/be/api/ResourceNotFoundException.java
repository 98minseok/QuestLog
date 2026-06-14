package com.als98.questlog.be.api;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, long id) {
        super(resource + " " + id + " was not found");
    }
}
