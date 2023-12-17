package net.xdow.aliyundrive.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.xdow.aliyundrive.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AliyunDriveResponse {

    @Data
    public static class AccessTokenInfo extends GenericMessageInfo {
        private String tokenType;
        private String accessToken;
        private String refreshToken;
        private String expiresIn;
        //WebApi
        private String userId;
    }

    public static class UserSpaceInfo extends GenericMessageInfo {
        @SerializedName("personal_space_info")
        private UserSpaceInfo.Data data;

        public long getTotalSize() {
            return this.data.getTotalSize();
        }

        public long getUsedSize() {
            return this.data.getUsedSize();
        }

        @lombok.Data
        public static class Data {
            private long usedSize;
            private long totalSize;
        }
    }

    @Data
    public static class UserDriveInfo extends GenericMessageInfo {
        private String avatar;
        private String email;
        private String phone;
        private String role;
        private String status;
        private String description;
        private List<Object> punishments;
        private long punishFlagEnum;
        private String userId;
        private String domainId;
        private String userName;
        private String nickName;
        private String defaultDriveId;
        private String resourceDriveId;
        private String backupDriveId;
        private long createdAt;
        private long updatedAt;
        private Map userData;
        private long punishFlag;
    }

    @Data
    public static class FileListInfo extends GenericMessageInfo {
        private List<AliyunDriveFileInfo> items = new ArrayList<>();
        private String nextMarker;
    }

    @Data
    public static class FileGetInfo extends AliyunDriveFileInfo {
    }

    @Data
    public static class FileBatchGetInfo extends FileListInfo {
    }

    @Data
    public static class FileGetDownloadUrlInfo extends FileListInfo {
        private String url;
        private Date expiration;
        private String method;
    }

    @Data
    public static class FileCreateInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String status;
        private String parentFileId;
        private String uploadId;
        private String fileName;
        private boolean available;
        private boolean exist;
        private boolean rapidUpload;
        private List<AliyunDriveFilePartInfo> partInfoList;
    }

    @Data
    public static class FileGetUploadUrlInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String uploadId;
        private Date createdAt;
        private List<AliyunDriveFilePartInfo> partInfoList;
    }

    @Data
    public static class FileListUploadPartsInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String uploadId;
        @SerializedName("parallelUpload")
        private String parallelUpload;
        private AliyunDriveUploadedPartInfo[] uploadedParts;
        private String nextPartNumberMarker;
    }

    @Data
    public static class FileUploadCompleteInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String name;
        private long size;
        private String fileExtension;
        private String contentHash;
        private AliyunDriveEnum.Category category;
        private AliyunDriveEnum.Type type;
        private String thumbnail;
        private String url;
        private String downloadUrl;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    public static class FileRenameInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String name;
        private long size;
        private String fileExtension;
        private String contentHash;
        private AliyunDriveEnum.Category category;
        private AliyunDriveEnum.Type type;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    public static class FileMoveInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String asyncTaskId;
        private boolean exist;
    }

    @Data
    public static class FileCopyInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String asyncTaskId;
    }

    @Data
    public static class FileMoveToTrashInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String asyncTaskId;
    }

    @Data
    public static class FileDeleteInfo extends GenericMessageInfo {
        private String driveId;
        private String fileId;
        private String asyncTaskId;
    }

    @Data
    public static class VideoPreviewPlayInfo extends GenericMessageInfo {
        private String domainId;
        private String driveId;
        private String fileId;
        private AliyunDriveVideoPreviewPlayInfo videoPreviewPlayInfo;
        private String playCursor;
    }

    @Data
    public static class QrCodeGenerateInfo extends GenericMessageInfo {
        @SerializedName("qrCodeUrl")
        private String qrCodeUrl;
        private String sid;
    }

    @Data
    public static class QrCodeQueryStatusInfo extends GenericMessageInfo {
        private AliyunDriveEnum.QrCodeState status;
        @SerializedName("authCode")
        private String authCode;
    }

    @Getter
    @Setter
    public static class GenericMessageInfo {
        private String code;
        private String message;

        public boolean isError() {
            return !StringUtils.isEmpty(this.code);
        }
    }
}