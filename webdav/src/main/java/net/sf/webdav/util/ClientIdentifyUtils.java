package net.sf.webdav.util;

public class ClientIdentifyUtils {

    public static boolean isWinSCP(String userAgent) {
        return String.valueOf(userAgent).contains("WinSCP");
    }

    public static boolean isWinSCP5AndBelow(String userAgent) {
        return String.valueOf(userAgent).matches("WinSCP/[1-5]\\..+");
    }
    public static boolean isMicrosoftExplorer(String userAgent) {
        return String.valueOf(userAgent).contains("Microsoft-WebDAV");
    }
    public static boolean isOSXFinder(String userAgent) {
        userAgent = String.valueOf(userAgent);
        return userAgent.contains("WebDAVFS") && !isTransmit(userAgent);
    }

    public static boolean isTransmit(String userAgent) {
        userAgent = String.valueOf(userAgent);
        return userAgent.contains("Transmit");
    }

    public static boolean isRclone(String userAgent) {
        userAgent = String.valueOf(userAgent);
        return userAgent.contains("rclone");
    }

    /**
     * 群晖 Cloud Sync 不提供UserAgent
     * @param userAgent
     * @return
     */
    public static boolean isSynoCloudSync(String userAgent) {
        return userAgent == null;
    }

    public static boolean isKodi19AndBelow(String userAgent) {
        return String.valueOf(userAgent).matches("Kodi/1*[0-9]\\..+");
    }

    public static boolean isKodi(String userAgent) {
        return String.valueOf(userAgent).matches("Kodi/.+");
    }

    public static boolean isAppleCoreMedia(String userAgent) {
        return String.valueOf(userAgent).matches("AppleCoreMedia/.+");
    }

    public static boolean isVLC(String userAgent) {
        return String.valueOf(userAgent).matches("VLC/.+");
    }

    /**
     *
     * @param referer
     * @return true Proxy mode, false Direct mode
     */
    public static boolean checkAliyunDriveRefererForProxyMode(String referer) {
        if (referer == null) {
            return false;
        }
        referer = referer.trim();
        if (referer.isEmpty()) {
            return false;
        }
        if (referer.toLowerCase().matches("https*://.*aliyundrive.com.*")) {
            return false;
        }
        return true;
    }
}
