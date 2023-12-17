package com.github.zxbu.webdavteambition.servlet.impl;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.inf.IBackgroundTask;
import com.github.zxbu.webdavteambition.inf.IStartupServlet;
import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.task.AliyunDriveCronTask;
import com.github.zxbu.webdavteambition.task.FrontendUpdateTask;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StartupServletImpl implements IStartupServlet {

    private AliyunDriveClientService mAliyunDriveClientService;
    private Runnable mOnFrontendUpdatedRunnable;
    private Map<Class<? extends IBackgroundTask>, IBackgroundTask> mBackgroundTaskMap = new ConcurrentHashMap<>();

    @Override
    public void init(Object... args) {
        this.mAliyunDriveClientService = (AliyunDriveClientService) args[0];
        this.mOnFrontendUpdatedRunnable = (Runnable) args[1];
        loadAliyunDriveCronTask();
        loadAliYunSessionManager();
        loadFrontendUpdateTask();
    }

    @Override
    public void destroy() {
        stopAllTask();
    }

    private void loadAliyunDriveCronTask() {
        restartTask(AliyunDriveCronTask.class, new AliyunDriveCronTask(this.mAliyunDriveClientService));
    }

    private void loadAliYunSessionManager(){
        AliyunDriveClientService service = this.mAliyunDriveClientService;
        IAliyunDrive aliyunDrive = service.getAliyunDrive();
        if (aliyunDrive instanceof AliyunDriveWebApiImplV1) {
        } else {
            return;
        }
        restartTask(AliyunDriveSessionManager.class, new AliyunDriveSessionManager((AliyunDriveWebApiImplV1) service.getAliyunDrive(), service.getProperties()));
    }

    private void loadFrontendUpdateTask() {
        AliyunDriveClientService service = this.mAliyunDriveClientService;
        AliyunDriveProperties properties = service.getProperties();
        File frontendDir = properties.getFrontendDir();
        restartTask(FrontendUpdateTask.class, new FrontendUpdateTask(frontendDir, mOnFrontendUpdatedRunnable));
    }

    private synchronized void addTask(Class<? extends IBackgroundTask> clazz, IBackgroundTask task) {
        mBackgroundTaskMap.put(clazz, task);
    }

    private synchronized boolean restartTask(Class<? extends IBackgroundTask> clazz, IBackgroundTask task) {
        stopTask(clazz);
        addTask(clazz, task);
        return startTask(clazz);
    }

    private synchronized boolean startTask(Class<? extends IBackgroundTask> clazz) {
        IBackgroundTask task = getTaskByClass(clazz);
        if (task == null) {
            return false;
        }
        task.start();
        return true;
    }

    private synchronized boolean stopTask(Class<? extends IBackgroundTask> clazz) {
        IBackgroundTask task = getTaskByClass(clazz);
        if (task == null) {
            return false;
        }
        task.stop();
        mBackgroundTaskMap.remove(clazz);
        return true;
    }

    private synchronized void stopAllTask() {
        Iterator<Map.Entry<Class<? extends IBackgroundTask>, IBackgroundTask>> it = mBackgroundTaskMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Class<? extends IBackgroundTask>, IBackgroundTask> entry = it.next();
            IBackgroundTask task = entry.getValue();
            task.stop();
            it.remove();
        }
    }

    public <T extends IBackgroundTask> T getTaskByClass(Class<T> clazz) {
        return (T)mBackgroundTaskMap.get(clazz);
    }
}
