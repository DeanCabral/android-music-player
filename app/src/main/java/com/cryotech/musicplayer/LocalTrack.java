package com.cryotech.musicplayer;

public class LocalTrack{

    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private String duration;
    private int songs;

    public LocalTrack(long id, String title, String artist, String album, String path, String duration, int songs) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
        this.songs = songs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getSongs() {
        return songs;
    }

    public void setSongs(int songs) {
        this.songs = songs;
    }

}
