package com.github.zxbu.webdavteambition.inf;

import com.github.zxbu.webdavteambition.bean.UpdateInfo;

/**
 * Created by Jason on 2017/9/9.
 */

public interface UpdateResultListener {

    void onNoUpdate();
    void onNetErr(Exception exception);
    void onHasUpdate(UpdateInfo updateInfo);
}
