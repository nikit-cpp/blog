package com.github.nkonev.blog.dto;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * Created by nik on 22.06.17.
 * Contains public information
 */
public class UserAccountDTO implements Serializable {
    private static final long serialVersionUID = -5796134399691582320L;

    private Long id;

    @NotEmpty
    private String login;

    private String avatar;

    private String facebookId;

    public UserAccountDTO(Long id, String login, String avatar, String facebookId) {
        this.id = id;
        this.login = login;
        this.avatar = avatar;
        this.facebookId = facebookId;
    }


    public UserAccountDTO() { }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }
}
