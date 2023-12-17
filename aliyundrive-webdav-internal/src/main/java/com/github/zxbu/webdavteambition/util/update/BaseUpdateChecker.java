package com.github.zxbu.webdavteambition.util.update;

import com.github.zxbu.webdavteambition.bean.UpdateInfo;
import com.github.zxbu.webdavteambition.inf.IUpdateCheck;
import com.github.zxbu.webdavteambition.inf.UpdateResultListener;

/**
 * Created by Jason on 2017/9/9.
 */

public abstract class BaseUpdateChecker implements IUpdateCheck, UpdateResultListener {

    private UpdateResultListener mResultListener;

    public BaseUpdateChecker(UpdateResultListener listener) {
        mResultListener = listener;
    }

    @Override
    public void onNoUpdate() {
        UpdateResultListener listener = mResultListener;
        if (listener == null) {
            return;
        }
        listener.onNoUpdate();
    }

    @Override
    public void onNetErr(Exception exception) {
        UpdateResultListener listener = mResultListener;
        if (listener == null) {
            return;
        }
        listener.onNetErr(exception);
    }

    @Override
    public void onHasUpdate(UpdateInfo updateInfo) {
        UpdateResultListener listener = mResultListener;
        if (listener == null) {
            return;
        }
        listener.onHasUpdate(updateInfo);
    }
}