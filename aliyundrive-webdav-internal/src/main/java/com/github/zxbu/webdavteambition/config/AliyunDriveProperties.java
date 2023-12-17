package com.github.zxbu.webdavteambition.config;

import lombok.Data;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.util.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Data
public class AliyunDriveProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveProperties.class);
    private static final String META_FILE_NAME = "meta.json";
    private String url = "https://api.aliyundrive.com/v2";
    private transient String authorization = "";
    private String refreshToken;
    private String refreshTokenNext;
    private transient String workDir = "./conf/";

    private String driveId;
    private String userId;
    private String deviceId;
    private String appId = "5dde4e1bdf9e4966b387ba58f4b3fdc3";
    private Session session = new Session();
    private String shareToken;
    private long shareExpireSec = 60 * 60 * 24;

    private transient String clientId;

    private transient String authorizationCode;
    private transient String aliyunAccessTokenUrl = "https://adrive.xdow.net/oauth/access_token?code=%s&refresh_token=%s&ver=2.0.0";
    private transient String aliyunAuthorizeUrl = "https://adrive.xdow.net/oauth/authorize?redirect_uri=%s";

    private transient Auth auth = new Auth();
    private transient Driver driver = Driver.OpenApi;
    private transient DownloadProxyMode downloadProxyMode = DownloadProxyMode.Auto;

    public enum Driver {
        OpenApi, WebApi
    }

    public enum DownloadProxyMode {
        Auto, Direct, Proxy
    }

    public void save() {
        String json = JsonUtils.toJsonPretty(this);
        File metaFile = new File(workDir, getMetaFileName());
        if (!metaFile.exists()) {
            metaFile.getParentFile().mkdirs();
        }
        try {
            FileUtils.write(metaFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Error: write meta file failed", e);
        }
    }

    public void save(AliyunDriveResponse.AccessTokenInfo accessTokenInfo) {
        this.setAuthorization(accessTokenInfo.getAccessToken());
        this.setRefreshToken(accessTokenInfo.getRefreshToken());
        this.setUserId(accessTokenInfo.getUserId());
        this.save();
    }

    public static AliyunDriveProperties load(String workDir) {
        return load(workDir, META_FILE_NAME);
    }

    public static AliyunDriveProperties load(String workDir, String metaFileName) {
        File metaFile = new File(workDir, metaFileName);
        if (!metaFile.exists()) {
            return new AliyunDriveProperties();
        }
        try {
            String json = FileUtils.readFileToString(metaFile, StandardCharsets.UTF_8);
            return JsonUtils.fromJson(json, AliyunDriveProperties.class);
        } catch (IOException e) {
            LOGGER.error("Error: read meta file failed", e);
        }
        return new AliyunDriveProperties();
    }


    public String getMetaFileName() {
        return META_FILE_NAME;
    }

    public File getFrontendDir() {
        return new File(getWorkDir(), "web");
    }

    @Data
    public static class Session {
        private String privateKey;
        private String publicKey;
        private String signature;
        private long expireTimeSec = 0;
        private int nonce = 0;

        public boolean isEmpty() {
            return StringUtils.isEmpty(privateKey) || StringUtils.isEmpty(publicKey);
        }

        public boolean isExpired() {
            return expireTimeSec < System.currentTimeMillis() / 1000;
        }
    }

    @Data
    public static class Auth {
        private Boolean enable = true;
        private String userName;
        private String password;
    }
}
