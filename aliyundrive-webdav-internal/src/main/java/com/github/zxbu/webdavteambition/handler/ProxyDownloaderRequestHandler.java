package com.github.zxbu.webdavteambition.handler;

import com.github.zxbu.webdavteambition.store.AliyunDriveFileSystemStore;
import net.sf.webdav.ITransaction;
import net.sf.webdav.methods.DoGet;

import java.io.IOException;

public class ProxyDownloaderRequestHandler implements IGetRequestHandler {
    @Override
    public boolean handle(AliyunDriveFileSystemStore store, ITransaction transaction, String path) throws IOException {
        DoGet.copyInputStream(store.getResourceContent(transaction, path), transaction.getResponse().getOutputStream());
        return true;
    }

}
