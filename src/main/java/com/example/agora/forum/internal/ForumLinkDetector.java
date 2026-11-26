package com.example.agora.forum.internal;

import java.util.regex.Pattern;

final class ForumLinkDetector {

    private static final Pattern LINK_PATTERN =
            Pattern.compile("(?i)(https?://|www\\.)\\S+");

    private ForumLinkDetector() {
    }

    static boolean containsLink(String text) {
        return LINK_PATTERN.matcher(text).find();
    }
}