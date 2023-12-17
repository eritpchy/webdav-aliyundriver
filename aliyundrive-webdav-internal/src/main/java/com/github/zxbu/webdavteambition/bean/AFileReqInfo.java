package com.github.zxbu.webdavteambition.bean;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode
public class AFileReqInfo {
    @NonNull
    private String driveId;
    @NonNull
    private String fileId;


    public static AFileReqInfo from(@NonNull AliyunDriveResponse.FileCreateInfo fileCreateInfo){
        return new AFileReqInfo(fileCreateInfo.getDriveId(), fileCreateInfo.getFileId());
    }

    public static AFileReqInfo from(@NonNull AliyunDriveFileInfo aliyunDriveFileInfo){
        return new AFileReqInfo(aliyunDriveFileInfo.getDriveId(), aliyunDriveFileInfo.getFileId());
    }

    public static AFileReqInfo fromParent(@NonNull AliyunDriveFileInfo aliyunDriveFileInfo){
        return new AFileReqInfo(aliyunDriveFileInfo.getDriveId(), aliyunDriveFileInfo.getParentFileId());
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(driveId) && StringUtils.isNotEmpty(fileId);
    }
}
