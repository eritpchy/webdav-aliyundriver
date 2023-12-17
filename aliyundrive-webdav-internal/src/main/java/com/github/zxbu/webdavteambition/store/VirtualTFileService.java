package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.bean.AFileReqInfo;
import net.sf.webdav.util.DateTimeUtils;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟文件（用于上传时，列表展示）
 */
public class VirtualTFileService {

    private static class Holder {
        private static VirtualTFileService sVirtualTFileService = new VirtualTFileService();
    }

    public static VirtualTFileService getInstance() {
        return Holder.sVirtualTFileService;
    }

    private final Map<AFileReqInfo, Map<String, AliyunDriveFileInfo>> virtualTFileMap = new ConcurrentHashMap<>();

    /**
     * 创建文件
     */
    public void createVirtualFile(AFileReqInfo info, AliyunDriveResponse.FileCreateInfo fileCreateInfo, long modifyTimeSec) {
        Map<String, AliyunDriveFileInfo> tFileMap = virtualTFileMap.get(info);
        if (tFileMap == null) {
            tFileMap = new ConcurrentHashMap<>();
            virtualTFileMap.put(info, tFileMap);
        }
        AliyunDriveFileInfo tFile = convert(fileCreateInfo);
        if (modifyTimeSec != -1) {
            tFile.setLocalModifiedAt(DateTimeUtils.convertLocalDateToGMT(modifyTimeSec * 1000));
        }
        tFileMap.put(fileCreateInfo.getFileId(), tFile);
    }

    public AliyunDriveFileInfo get(AFileReqInfo info, String fileId) {
        Map<String, AliyunDriveFileInfo> tFileMap = virtualTFileMap.get(info);
        if (tFileMap == null) {
            return null;
        }
        return tFileMap.get(fileId);
    }

    public void updateLength(AFileReqInfo info, String fileId, long length) {
        AliyunDriveFileInfo tFile = get(info, fileId);
        if (tFile == null) {
            return;
        }
        tFile.setSize(tFile.getSize() + length);
    }

    public void remove(AFileReqInfo parentInfo, String fileId) {
        Map<String, AliyunDriveFileInfo> tFileMap = virtualTFileMap.get(parentInfo);
        if (tFileMap == null) {
            return;
        }
        tFileMap.remove(fileId);
    }

    public Collection<AliyunDriveFileInfo> list(AFileReqInfo info) {
        Map<String, AliyunDriveFileInfo> tFileMap = virtualTFileMap.get(info);
        if (tFileMap == null) {
            return Collections.emptyList();
        }
        return tFileMap.values();
    }

    private AliyunDriveFileInfo convert(AliyunDriveResponse.FileCreateInfo fileCreateInfo) {

        AliyunDriveFileInfo tFile = new AliyunDriveFileInfo();
        tFile.setDriveId(fileCreateInfo.getDriveId());
        tFile.setFileId(fileCreateInfo.getFileId());
        tFile.setParentFileId(fileCreateInfo.getParentFileId());
        tFile.setName(fileCreateInfo.getFileName());
        tFile.setType(AliyunDriveEnum.Type.File);
        tFile.setSize(0L);
        Date currentDateGMT = DateTimeUtils.getCurrentDateGMT();
        tFile.setCreatedAt(currentDateGMT);
        tFile.setUpdatedAt(currentDateGMT);
        return tFile;
    }
}
