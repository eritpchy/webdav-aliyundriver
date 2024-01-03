package net.xdow.aliyundrive.webapi.bean;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AliyunDriveWebResponse {

    @Data
    public static class ShareTokenInfo extends AliyunDriveWebShareRequestInfo {
    }

    @Data
    public static class ShareSaveInfo extends AliyunDriveResponse.GenericMessageInfo {
        private String driveId;
        private String fileId;
    }

    @Data
    public static class DirectTransferSaveInfo extends AliyunDriveResponse.GenericMessageInfo {
        private String toDriveId;
        private String toParentFileId;
        private String toParentFileName;
        private List<DirectTransferSaveItemInfo> items = new ArrayList<>();

        public String getFileId() {
            if (!items.isEmpty()) {
                DirectTransferSaveItemInfo item = items.get(0);
                return item.getFileId();
            }
            return null;
        }

        public String getDriveId() {
            if (!items.isEmpty()) {
                DirectTransferSaveItemInfo item = items.get(0);
                return item.getDriveId();
            }
            return toDriveId;
        }
    }

    @Data
    public static class DirectTransferGetFileInfo extends AliyunDriveResponse.GenericMessageInfo {
        private String expiration;
        private String name;
        private Map<String, String> creator;
        private String shareId;
        @NonNull
        private List<AliyunDriveFileInfo> previewFileList = new ArrayList<>();
    }

    public static class UserSpaceInfo extends AliyunDriveResponse.UserSpaceInfo {
        private long driveUsedSize;
        private long driveTotalSize;
        @Getter
        private long defaultDriveUsedSize;
        @Getter
        private long albumDriveUsedSize;
        @Getter
        private long shareAlbumDriveUsedSize;
        @Getter
        private long noteDriveUsedSize;
        @Getter
        private long sboxDriveUsedSize;

        public long getTotalSize() {
            return this.driveTotalSize;
        }

        public long getUsedSize() {
            return this.driveUsedSize;
        }
    }
}
