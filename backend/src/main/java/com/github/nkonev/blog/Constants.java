package com.github.nkonev.blog;


/**
 * Created by nik on 23.05.17.
 */
public class Constants {

    public static class Uls {
        public static final String ROOT = "/";
        public static final String API = "/api";
        public static final String ADMIN = "/admin";
        public static final String POST = "/post";
        public static final String PROFILE = "/profile";
        public static final String MY = "/my";
        public static final String POST_ID = "/{"+ PathVariables.POST_ID+"}";
        public static final String COMMENT_ID = "/{"+PathVariables.COMMENT_ID+"}";
        public static final String COMMENT = "/comment";
        public static final String REGISTER = "/register";
        public static final String CONFIRM = "/confirm"; // html for handle link from email
        public static final String UUID = "uuid";
        public static final String RESEND_CONFIRMATION_EMAIL = "/resend-confirmation-email";
        public static final String PASSWORD_RESET = "/password-reset"; // html for handle link from email
        public static final String USER = "/user";
        public static final String LOCK = "/lock";
        public static final String USER_ID = "/{"+PathVariables.USER_ID+"}";
        public static final String REQUEST_PASSWORD_RESET = "/request-password-reset";
        public static final String PASSWORD_RESET_SET_NEW = "/password-reset-set-new";
        public static final String SESSIONS = "/sessions";
        public static final String CONFIG = "/config";
    }

    public static class PathVariables {
        public static final String POST_ID = "postId";
        public static final String COMMENT_ID = "commentId";
        public static final String USER_ID = "postId";
    }

    public static class Schemas {
        public static final String AUTH = "auth";
        public static final String POSTS = "posts";
    }
}
