package net.xdow.aliyundrive.bean;

import lombok.NonNull;

import java.util.Date;

public class AliyunDriveBackupRootFileInfo extends AliyunDriveFileInfo {

    public AliyunDriveBackupRootFileInfo(@NonNull String driveId) {
        this.setDriveId(driveId);
    }

    @Override
    public String getName() {
        return "备份盘";
    }

    @Override
    public String getFileId() {
        return "root";
    }

    @Override
    public String getParentFileId() {
        return getFileId();
    }

    @Override
    public Date getCreatedAt() {
        return new Date();
    }

    @Override
    public Date getUpdatedAt() {
        return new Date();
    }

    @Override
    public AliyunDriveEnum.Type getType() {
        return AliyunDriveEnum.Type.Folder;
    }

}
