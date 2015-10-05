package com.javijuol.signalmap.content.bean;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class NetworkSignal {

    private long id;
    private double lat, lng;
    private int type;
    private long createdAt, strength;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public long getStrength() {
        return strength;
    }

    public void setStrength(long strength) {
        this.strength = strength;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long created_at) {
        this.createdAt = created_at;
    }
}
