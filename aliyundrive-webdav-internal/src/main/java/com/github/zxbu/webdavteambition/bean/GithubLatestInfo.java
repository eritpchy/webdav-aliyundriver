package com.github.zxbu.webdavteambition.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.xdow.aliyundrive.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Jason on 2017/9/10.
 */

public class GithubLatestInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubLatestInfo.class);

    @SerializedName("html_url")
    public String contentUrl;

    @SerializedName("name")
    public String version;

    @SerializedName("body")
    public String content;

    @SerializedName("created_at")
    public Date date;


    public List<GithubAssetsInfo> assets = new ArrayList<>();

    public GithubAssetsInfo getDownloadAssetsInfo() {
        LOGGER.debug("assets", new Gson().toJson(assets));
        if (assets.size() == 0) {
            return null;
        }
        for (GithubAssetsInfo asset : assets) {
            if (asset == null || StringUtils.isEmpty(asset.name) || StringUtils.isEmpty(asset.url)) {
                continue;
            }

            if (asset.name.toLowerCase().matches(".+\\.tar\\.xz$")) {
                return asset;
            }
        }
        return null;
    }

    public boolean isDataComplete() {
        GithubAssetsInfo downloadAssetsInfo = getDownloadAssetsInfo();
        if (downloadAssetsInfo == null) {
            LOGGER.debug("downloadAssetsInfo == null");
            return false;
        }
        if (StringUtils.isEmpty(downloadAssetsInfo.url)) {
            LOGGER.debug("url is empty");
            return false;
        }
        if (StringUtils.isEmpty(version)) {
            LOGGER.debug("version is empty");
            return false;
        }
        if (StringUtils.isEmpty(contentUrl)) {
            LOGGER.debug("contentUrl is empty");
            return false;
        }
        if (StringUtils.isEmpty(content)) {
            LOGGER.debug("content is empty");
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
