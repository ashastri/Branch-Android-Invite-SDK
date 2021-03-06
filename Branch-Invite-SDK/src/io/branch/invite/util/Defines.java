package io.branch.invite.util;

/**
 * <p>
 * Defines all Json keys associated with branch invite parameters.
 * </p>
 */
public enum Defines {

    INVITE_USER_ID("BRANCH_INVITE_USER_ID_KEY"),
    INVITE_USER_FULLNAME("BRANCH_INVITE_USER_FULLNAME_KEY"),
    INVITE_USER_SHORT_NAME("BRANCH_INVITE_USER_SHORT_NAME_KEY"),
    INVITE_USER_IMAGE_URL("BRANCH_INVITE_USER_IMAGE_URL_KEY"),
    FULL_NAME_SUB("$FULL_NAME"),
    SHORT_NAME_SUB("$SHORT_NAME");

    private String key = "";

    Defines(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
