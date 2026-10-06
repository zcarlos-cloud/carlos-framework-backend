package com.carlos.auth.app.pojo.dto;


import lombok.Data;

import java.io.Serializable;

@Data
public class Oauth2ClientSettings implements Serializable {

    /** 默认true */
    private Boolean requireProofKey = false;
    /** 默认true */
    private Boolean requireAuthorizationConsent = false;

    private String jwkSetUrl;

}
