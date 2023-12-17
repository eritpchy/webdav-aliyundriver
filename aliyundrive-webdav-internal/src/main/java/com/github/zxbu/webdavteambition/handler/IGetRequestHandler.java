package com.github.zxbu.webdavteambition.handler;

import com.github.zxbu.webdavteambition.store.AliyunDriveFileSystemStore;
import net.sf.webdav.ITransaction;

import java.io.IOException;

public interface IGetRequestHandler {
    boolean handle(AliyunDriveFileSystemStore store, ITransaction transaction, String path) throws IOException;
}
