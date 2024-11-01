package com.ap.neighborrentapplication.domain;

public class Device {

    private String id;
    private String name;
    private String description;
    private String picUrl;
    private String price;
    private String status;
    private String city;
    private boolean isFavorite;

    public Device(String id, String name, String description, String picUrl, String price, String status, String city, boolean isFavorite) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.picUrl = picUrl;
        this.price = price;
        this.status = status;
        this.city = city;
        this.isFavorite = isFavorite;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
