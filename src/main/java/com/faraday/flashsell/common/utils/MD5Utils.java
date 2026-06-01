package com.faraday.flashsell.common.utils;

import org.springframework.util.DigestUtils;

public class MD5Utils {

    public static String md5(String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}
