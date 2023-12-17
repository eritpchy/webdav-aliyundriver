package com.github.zxbu.webdavteambition.util.update;

import com.github.zxbu.webdavteambition.bean.FrontendVersionInfo;
import com.github.zxbu.webdavteambition.bean.UpdateInfo;
import com.github.zxbu.webdavteambition.inf.UpdateResultListener;
import com.github.zxbu.webdavteambition.util.DecompressUtils;
import com.github.zxbu.webdavteambition.util.FileUtils;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.util.StringUtils;
import okhttp3.*;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FrontendUpdateFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendUpdateFactory.class);

    private static final String URL_FRONTEND_UPDATE = "https://xbimg.xdow.net/upload/aliyundrive-web/dist/latest";

    public static final long DEFAULT_TIMEOUT_MS_WHEN_DOWNLOAD_WAIT = 12_000L; //3s

    private File mTargetDir;

    public FrontendUpdateFactory(File targetDir) {
        this.mTargetDir = targetDir;
    }

    public void doUpdateCheck(final boolean isWait, final Runnable onFrontendUpdatedRunnable) {
        try {
            String localVersion = readFrontendVersionFromLocal();
            if (StringUtils.isEmpty(localVersion)) {
                localVersion = "0.0.0";
            }
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            new GithubUpdateChecker(localVersion, URL_FRONTEND_UPDATE, new UpdateResultListener() {
                @Override
                public void onNoUpdate() {
                    LOGGER.debug("onNoUpdate");
                    countDownLatch.countDown();
                }

                @Override
                public void onNetErr(Exception exception) {
                    LOGGER.debug("onNetErr", exception);
                    countDownLatch.countDown();
                }

                @Override
                public void onHasUpdate(UpdateInfo updateInfo) {
                    LOGGER.debug("onHasUpdate: {}", updateInfo);
                    downloadUpdate(updateInfo, onFrontendUpdatedRunnable, isWait);
                    countDownLatch.countDown();
                }
            }).doUpdateCheck(isWait, isWait ? GithubUpdateChecker.DEFAULT_TIMEOUT_MS_WHEN_WAIT : GithubUpdateChecker.DEFAULT_TIMEOUT_MS);
            if (isWait) {
                countDownLatch.await();
            }
        } catch (Exception e) {
            LOGGER.error("doUpdateCheck", e);
        }
    }


    private void downloadUpdate(UpdateInfo updateInfo, final Runnable onFrontendUpdatedRunnable, boolean isWait) {
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LOGGER.error("downloadUpdate", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }
                InputStream inputStream = null;
                try {
                    inputStream = body.byteStream();
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    DecompressUtils.unxz(new ByteArrayInputStream(bytes), mTargetDir);
                    if (onFrontendUpdatedRunnable != null) {
                        onFrontendUpdatedRunnable.run();
                    }
                } catch (Exception e) {
                    LOGGER.error("downloadUpdate", e);
                } finally {
                    FileUtils.closeCloseable(inputStream);
                }
            }
        };

        long timeoutMS = isWait ? GithubUpdateChecker.DEFAULT_TIMEOUT_MS_WHEN_WAIT : GithubUpdateChecker.DEFAULT_TIMEOUT_MS;
        Request request = new Request.Builder()
                .url(updateInfo.url)
                .build();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .callTimeout(timeoutMS, TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutMS, TimeUnit.MILLISECONDS)
                .readTimeout(isWait ? DEFAULT_TIMEOUT_MS_WHEN_DOWNLOAD_WAIT : GithubUpdateChecker.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(isWait ? DEFAULT_TIMEOUT_MS_WHEN_DOWNLOAD_WAIT : GithubUpdateChecker.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Call call = builder.build().newCall(request);
        if (!isWait) {
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


    public String readFrontendVersionFromLocal() {
        File versionJsonFile = new File(mTargetDir, "version.json");
        if (!versionJsonFile.exists()) {
            return null;
        }

        try {
            String content = FileUtils.getContent(versionJsonFile);
            if (StringUtils.isEmpty(content)) {
                return null;
            }
            FrontendVersionInfo frontendVersionInfo = JsonUtils.fromJson(content, FrontendVersionInfo.class);
            return frontendVersionInfo.version;
        } catch (Exception e) {
            LOGGER.error("Unexpected error on readFrontendVersionFromLocal", e);
        }
        return null;
    }
    
}
