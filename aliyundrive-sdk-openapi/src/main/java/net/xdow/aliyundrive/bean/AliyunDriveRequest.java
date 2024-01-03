package net.xdow.aliyundrive.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;
import java.util.List;

public class AliyunDriveRequest {
    @Data
    public static class AccessTokenInfo {
        private String clientId;
        private String clientSecret;
        private AliyunDriveEnum.GrantType grantType;
        private String code;
        private String refreshToken;
    }

    @Data
    public static class FileListInfo {
        @NonNull
        private String driveId;
        private int limit = 100;
        private String marker;
        private AliyunDriveEnum.OrderBy orderBy;
        private AliyunDriveEnum.OrderDirection orderDirection;
        @NonNull
        private String parentFileId;
        @SerializedName("category")
        private String categories;
        @SerializedName("video_thumbnail_time")
        private Integer videoThumbnailTimeMS;
        private Integer videoThumbnailWidth;
        private Integer imageThumbnailWidth;
        private String fields = "*";

    }

    @Data
    public static class FileGetInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        private String category;
        @SerializedName("video_thumbnail_time")
        private Integer videoThumbnailTimeMS;
        private Integer videoThumbnailWidth;
        private Integer imageThumbnailWidth;
        private String videoThumbnailProcess;
    }

    @Data
    public static class FileGetByPathInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String filePath;
    }

    @Data
    public static class FileBatchGetInfo {

        @NonNull
        private List<FileInfo> fileList;

        @Data
        public static class FileInfo {
            @NonNull
            private String driveId;
            @NonNull
            private String fileId;
        }
    }

    @Data
    public static class FileGetDownloadUrlInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        private int expireSec = 900;
    }

    @Data
    public static class FileCreateInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String parentFileId;
        @NonNull
        private String name;
        @NonNull
        private AliyunDriveEnum.Type type;
        @NonNull
        private AliyunDriveEnum.CheckNameMode checkNameMode;
        private List<AliyunDriveFilePartInfo> partInfoList;
        private Boolean parallelUpload;
        private String preHash;
        private long size;
        private String contentHash;
        private String contentHashName;
        private String proofCode;
        private Date localCreatedAt;
        private Date localModifiedAt;

        @Data
        public static class StreamInfo {
            private String content_hash;
            private String content_hash_name;
            private String proof_version;
            private String proof_code;
            private String content_md5;
            private String pre_hash;
            private String size;
            private List<AliyunDriveFilePartInfo> partInfoList;
        }
    }

    @Data
    public static class FileGetUploadUrlInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String uploadId;
        @NonNull
        private List<AliyunDriveFilePartInfo> partInfoList;
    }

    @Data
    public static class FileListUploadPartsInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String uploadId;
        private List<AliyunDriveFilePartInfo> partInfoList;
    }

    @Data
    public static class FileUploadCompleteInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String uploadId;
    }

    @Data
    public static class FileRenameInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String name;
        @NonNull
        //for force deleted
        private transient String parentFileId;
    }

    @Data
    public static class FileMoveInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String toDriveId;
        @NonNull
        private String fileId;
        @NonNull
        private String toParentFileId;
        private AliyunDriveEnum.CheckNameMode checkNameMode;
        private String newName;
    }

    @Data
    public static class FileCopyInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String toDriveId;
        @NonNull
        private String toParentFileId;
        private boolean autoRename;
    }

    @Data
    public static class FileMoveToTrashInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
    }

    @Data
    public static class FileRestoreFromTrashInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
    }

    @Data
    public static class FileDeleteInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
    }

    @Data
    public static class VideoPreviewPlayInfo {
        @NonNull
        private String driveId;
        @NonNull
        private String fileId;
        @NonNull
        private String category = "live_transcoding";
        private boolean getSubtitleInfo = true;
        private String templateId;
        /**
         * 单位秒，最长4小时，默认15分钟。
         */
        private long urlExpireSec = 60 * 60 * 4;
        private boolean onlyVip;
        private boolean withPlayCursor;
    }

    @Data
    public static class QrCodeGenerateInfo {
        @NonNull
        private String clientId;
        @NonNull
        private String clientSecret;
        @NonNull
        private String[] scopes;
        private int width = 430;
        private int height = 430;
    }
}
