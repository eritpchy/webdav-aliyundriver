package com.github.zxbu.webdavteambition.task;

import com.github.zxbu.webdavteambition.bean.AFileReqInfo;
import com.github.zxbu.webdavteambition.inf.IBackgroundTask;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliyunDriveCronTask implements IBackgroundTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveCronTask.class);

    private final AliyunDriveClientService mAliyunDriveClientService;

    private ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);


    public AliyunDriveCronTask(AliyunDriveClientService service) {
        mAliyunDriveClientService = service;
    }

    /**
     * 每隔30-60分钟请求一下接口，保证token不过期
     */
    public void refreshToken() {
        try {
            LOGGER.info("定时刷新 Refresh Token ↓↓↓↓↓");
            AliyunDriveFileInfo root = mAliyunDriveClientService.getTFileByPath("/");
            AFileReqInfo rootInfo = AFileReqInfo.from(root);
            mAliyunDriveClientService.getTFileListCached(rootInfo);
        } catch (Throwable e) {
            if (e.getCause() instanceof NotAuthenticatedException) {
                LOGGER.error("阿里云盘登录已失效, 请重新登录!");
                return;
            }
            LOGGER.error("", e);
        } finally {
            LOGGER.info("定时刷新 Refresh Token ↑↑↑↑↑");
        }
    }

    public void start() {
        mTaskPool.schedule(new Runnable() {
            @Override
            public void run() {
                refreshToken();
                mTaskPool.schedule(this, getRandomNumber(30, 60), TimeUnit.MINUTES);
            }
        }, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        mTaskPool.shutdownNow();
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
