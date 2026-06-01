package com.faraday.flashsell.model.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private String nickname;

    private String phone;

    public LoginVO(String token, String nickname, String phone) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
    }
}
