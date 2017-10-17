package com.github.nkonev.pages.object;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.github.nkonev.integration.AbstractItTestRunner.CLICKABLE;

/**
 * Created by nik on 15.07.17.
 */
public class UserNav {
    private static final int USER_NAV_TIMEOUT = 20000;
    public static void open() {
        $(".user-profile-nav .multiselect")
                .waitUntil(Condition.enabled, USER_NAV_TIMEOUT)
                .waitUntil(Condition.visible, USER_NAV_TIMEOUT)
                .click();
    }

    public static void exit() {
        $(byText("exit"))
                .waitUntil(Condition.enabled, USER_NAV_TIMEOUT)
                .waitUntil(Condition.visible, USER_NAV_TIMEOUT)
                .click();
    }

    public static void profile() {
        $(byText("profile")).shouldBe(CLICKABLE).click();
    }

    public static String getAvatarUrl(){
        return $(".user-profile-nav .avatar").getAttribute("src");
    }
}
