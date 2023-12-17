package net.xdow.aliyundrive.bean;

import java.util.Date;

public class AliyunDriveVirtualRootFileInfo extends AliyunDriveFileInfo {

    public static final AliyunDriveVirtualRootFileInfo INSTANCE = new AliyunDriveVirtualRootFileInfo();

    @Override
    public String getName() {
        return "/";
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
    public String getDriveId() {
        return "aliyun://";
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

    public static boolean isRoot(String driveId) {
        return AliyunDriveVirtualRootFileInfo.INSTANCE.getDriveId().equals(driveId);
    }
}
