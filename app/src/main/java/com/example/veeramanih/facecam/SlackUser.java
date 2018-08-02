package com.example.veeramanih.facecam;

import android.widget.TextView;

class SlackUser {
    private String name;
    private String title;
    private String email;
    private String phoneNumber;
    private String instagram;
    private String vimeo;
    private String twitter;
    private String github;

    public SlackUser(String name, String title, String email, String phoneNumber, String instagram, String vimeo, String github, String twitter) {
        this.name = name;
        this.title = title;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.instagram = instagram;
        this.vimeo = vimeo;
        this.github = github;
        this.twitter = twitter;
    }

    public SlackUser() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "?????";
            return;
        }
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) {
            this.title = "No title";
            return;
        }
        this.title = title;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null) {
            this.email = "Email not found";
            return;
        }
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getVimeo() {
        return vimeo;
    }

    public void setVimeo(String vimeo) {
        this.vimeo = vimeo;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name).append("\nTitle: ").append(title).append("\nEmail: ").append(email).append("\nPhone: ").append(phoneNumber);
        return sb.toString();
    }
}
