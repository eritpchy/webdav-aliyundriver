package com.github.zxbu.webdavteambition.task;

import com.github.zxbu.webdavteambition.bean.FrontendVersionInfo;
import com.github.zxbu.webdavteambition.inf.IBackgroundTask;
import com.github.zxbu.webdavteambition.util.DecompressUtils;
import com.github.zxbu.webdavteambition.util.FileUtils;
import com.github.zxbu.webdavteambition.util.update.FrontendUpdateFactory;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FrontendUpdateTask implements IBackgroundTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendUpdateTask.class);

    private final File mTargetDir;
    private final Runnable mOnFrontendUpdatedRunnable;
    private final FrontendUpdateFactory mFrontendUpdateFactory;

    private final ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);

    public FrontendUpdateTask(File targetDir, Runnable onUpdatedRunnable) {
        this.mTargetDir = targetDir;
        this.mOnFrontendUpdatedRunnable = onUpdatedRunnable;
        this.mFrontendUpdateFactory = new FrontendUpdateFactory(targetDir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        extractIfNeeded();
    }

    @Override
    public void start() {
        mTaskPool.execute(new Runnable() {
            @Override
            public void run() {
                mFrontendUpdateFactory.doUpdateCheck(false, mOnFrontendUpdatedRunnable);
            }
        });
    }

    @Override
    public void stop() {
        mTaskPool.shutdownNow();
    }

    private void extractIfNeeded() {
        String localVersion = mFrontendUpdateFactory.readFrontendVersionFromLocal();
        String jarVersion = readFrontendVersionFromJar();
        if (StringUtils.isEmpty(localVersion)
                || com.github.zxbu.webdavteambition.util.StringUtils.isAppNewVersion(localVersion, jarVersion))  {
            extract();
        }
    }

    private void extract() {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream("com/github/zxbu/webdavteambition/frontend.tar.xz");
            DecompressUtils.unxz(inputStream, mTargetDir);
        } catch (Exception e) {
            LOGGER.error("Unexpected error on extract", e);
        } finally {
            FileUtils.closeCloseable(inputStream);
        }
    }

    private String readFrontendVersionFromJar() {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream("com/github/zxbu/webdavteambition/frontend.version.json");
            String content = FileUtils.convertStreamToString(inputStream);
            FrontendVersionInfo frontendVersionInfo = JsonUtils.fromJson(content, FrontendVersionInfo.class);
            return frontendVersionInfo.version;
        } catch (Exception e) {
            LOGGER.error("Unexpected error on readFrontendVersionFromJar", e);
            throw new RuntimeException(e);
        } finally {
            FileUtils.closeCloseable(inputStream);
        }
    }

}