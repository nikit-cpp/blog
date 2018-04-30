package com.github.nkonev.blog.utils;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {
    public static String nullToEmpty(String s){
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    public static String getQuery(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return "";
        } else {
            return "?" + request.getQueryString();
        }
    }

    public static String getPath(HttpServletRequest request) {
        String s = request.getRequestURI();
        if (s == null){
            return "";
        }
        if (s.endsWith("/")){
            return s.substring(0, s.length()-1);
        }
        return s;
    }
}
