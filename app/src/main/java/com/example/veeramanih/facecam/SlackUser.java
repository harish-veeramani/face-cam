package com.example.veeramanih.facecam;

class SlackUser {
    private String name;
    private String title;
    private String email;
    private String phoneNumber;
    private String instagram;
    private String vimeo;
    private String github;

    public SlackUser(String name, String title, String email, String phoneNumber, String instagram, String vimeo, String github) {
        this.name = name;
        this.title = title;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.instagram = instagram;
        this.vimeo = vimeo;
        this.github = github;
    }

    public SlackUser() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
