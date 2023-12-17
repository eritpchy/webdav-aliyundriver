package com.github.zxbu.webdavteambition.util.update;


import com.github.zxbu.webdavteambition.bean.GithubAssetsInfo;
import com.github.zxbu.webdavteambition.bean.GithubLatestInfo;
import com.github.zxbu.webdavteambition.bean.UpdateInfo;
import com.github.zxbu.webdavteambition.inf.UpdateResultListener;
import com.github.zxbu.webdavteambition.util.StringUtils;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jason on 2017/9/9.
 */

public class GithubUpdateChecker extends BaseUpdateChecker {

    public static final long DEFAULT_TIMEOUT_MS = 60_000L; //60s
    public static final long DEFAULT_TIMEOUT_MS_WHEN_WAIT = 3_000L; //3s

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubUpdateChecker.class);
    private final String mLocalVersion;
    private final String mUpdateUrl;

    private Callback callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            onNetErr(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response != null && response.isSuccessful()) {
                String replay = response.body().string();
                response.close();
                try {
                    GithubLatestInfo info = new Gson().fromJson(replay, GithubLatestInfo.class);
                    if (!info.isDataComplete()) {
                        onNetErr(new IllegalArgumentException("data not complete!"));
                        return;
                    }
                    if (StringUtils.isAppNewVersion(mLocalVersion, info.version)) {
                        LOGGER.debug("info", info);
                        String content = info.content;
                        GithubAssetsInfo assetsInfo = info.getDownloadAssetsInfo();
                        UpdateInfo updateInfo = new UpdateInfo(info.version, content,
                                info.contentUrl, assetsInfo.url, assetsInfo.name, assetsInfo.size);
                        onHasUpdate(updateInfo);
                    } else {
                        onNoUpdate();
                    }
                    return;
                } catch (Exception e) {
                    LOGGER.debug("doUpdateCheck", e);
                }
            }
            onNetErr(new IOException("response not successful. code: " + response.code()));
        }
    };

    public GithubUpdateChecker(String localVersion, String updateUrl, UpdateResultListener listener) {
        super(listener);
        this.mLocalVersion = localVersion;
        this.mUpdateUrl = updateUrl;
    }

    @Override
    public void doUpdateCheck(boolean wait, long timeoutMS) {
        Request request = new Request.Builder()
                .url(this.mUpdateUrl)
                .build();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .callTimeout(timeoutMS, TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutMS, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMS, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMS, TimeUnit.MILLISECONDS);
        Call call = builder.build().newCall(request);
        if (!wait) {
            call.enqueue(callback);
            return;
        }
        try {
            Response res = call.execute();
            callback.onResponse(call, res);
        } catch (IOException e) {
            callback.onFailure(call, e);
        }
    }
}
