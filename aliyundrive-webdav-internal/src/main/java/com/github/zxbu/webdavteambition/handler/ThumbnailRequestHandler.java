package com.github.zxbu.webdavteambition.handler;

import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.bean.AFileReqInfo;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.store.AliyunDriveFileSystemStore;
import com.google.common.net.HttpHeaders;
import net.sf.webdav.ITransaction;
import net.sf.webdav.exceptions.WebdavException;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThumbnailRequestHandler implements IGetRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailRequestHandler.class);

    @Override
    public boolean handle(AliyunDriveFileSystemStore store, ITransaction transaction, String path) throws IOException {
        InputStream is = getResourceThumbnailContent(store, transaction, path);
        if (is == null) {
            transaction.getResponse().setStatus(JapHttpResponse.SC_NOT_FOUND);
            return false;
        }
        IOUtils.copy(is, transaction.getResponse().getOutputStream());
        return true;
    }


    public InputStream getResourceThumbnailContent(AliyunDriveFileSystemStore store,ITransaction transaction, String path) {
        AliyunDriveClientService service = store.getAliyunDriveClientService();
        AliyunDriveFileInfo tFile = service.getTFileByPath(path);
        if (tFile == null) {
            return null;
        }
        String thumbnail = tFile.getThumbnail();
        if (StringUtils.isEmpty(thumbnail)) {
            return null;
        }
        try {
            JapHttpResponse response = transaction.getResponse();
            Response downResponse = service.getAliyunDrive().download(thumbnail, null, null).execute();
            if (downResponse.code() == 403) { //expire
                service.clearCache(AFileReqInfo.fromParent(tFile));
                service.clearCache(AFileReqInfo.from(tFile));
                tFile = service.getTFileByPath(path);
                thumbnail = tFile.getThumbnail();
                downResponse = service.getAliyunDrive().download(thumbnail, null, null).execute();
            }
            LOGGER.debug("{} code = {}", thumbnail, downResponse.code());
            for (String name : downResponse.headers().names()) {
                //Fix Winscp Invalid Content-Length in response
                if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                    continue;
                }
                LOGGER.debug("{} downResponse: {} = {}", thumbnail, name, downResponse.header(name));
                response.addHeader(name, downResponse.header(name));
            }
            ResponseBody body = downResponse.body();
            if (body == null) {
                response.setContentLengthLong(0L);
                response.setStatus(downResponse.code());
                return new ByteArrayInputStream(new byte[0]);
            }
            response.setContentLengthLong(body.contentLength());
            response.setStatus(downResponse.code());
            return body.byteStream();
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }
}
