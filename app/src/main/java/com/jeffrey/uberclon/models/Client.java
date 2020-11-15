package com.jeffrey.uberclon.models;

public class Client {
    String id;
    String name;
    String email;
    String image;

    public Client() {

    }

    public Client(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Client(String id, String name, String email, String image) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
