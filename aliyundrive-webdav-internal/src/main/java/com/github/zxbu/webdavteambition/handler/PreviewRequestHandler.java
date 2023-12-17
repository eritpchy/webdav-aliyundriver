package com.github.zxbu.webdavteambition.handler;

import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.store.AliyunDriveFileSystemStore;
import net.sf.webdav.ITransaction;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.bean.AliyunDriveRequest;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.util.JsonUtils;

import java.io.IOException;

public class PreviewRequestHandler implements IGetRequestHandler {
    @Override
    public boolean handle(AliyunDriveFileSystemStore store, ITransaction transaction, String path) throws IOException {
        String res = getVideoPreviewPlayInfo(store, transaction, path);
        if (res == null) {
            transaction.getResponse().setStatus(JapHttpResponse.SC_NOT_FOUND);
            return false;
        }
        transaction.getResponse().getOutputStream().write(res.getBytes());
        return true;
    }

    public String getVideoPreviewPlayInfo(AliyunDriveFileSystemStore store, ITransaction transaction, String path) {
        AliyunDriveClientService service = store.getAliyunDriveClientService();
        AliyunDriveFileInfo tFile = service.getTFileByPath(path);
        if (tFile == null) {
            return null;
        }
        AliyunDriveRequest.VideoPreviewPlayInfo query = new AliyunDriveRequest.VideoPreviewPlayInfo(tFile.getDriveId(), tFile.getFileId());
        AliyunDriveResponse.VideoPreviewPlayInfo response = service.getAliyunDrive().videoPreviewPlayInfo(query).execute();
        transaction.getResponse().setContentType("application/json; charset=UTF-8");
        if (response.isError()) {
            return JsonUtils.toJson(response);
        }
        return JsonUtils.toJson(response);
    }
}
