package net.sf.webdav.util;

import net.sf.webdav.fromcatalina.URLEncoder;

public class EscaperUtils {

    private static URLEncoder URL_ENCODER = new URLEncoder();

    static {
        URL_ENCODER.addSafeCharacter('-');
        URL_ENCODER.addSafeCharacter('_');
        URL_ENCODER.addSafeCharacter('.');
        URL_ENCODER.addSafeCharacter('/');
    }

    public static String escapePath(String s) {
        return URL_ENCODER.encode(s)
                .replace("&", "%26");
    }
}
