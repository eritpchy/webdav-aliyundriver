package net.sf.webdav.util;

import net.sf.webdav.fromcatalina.URLEncoder;

public class EscaperUtils {

    private static URLEncoder URL_Encoder = new URLEncoder();
    public static String escapePath(String s) {
        return URL_Encoder.encode(s).replace("&", "%26");
    }
}
