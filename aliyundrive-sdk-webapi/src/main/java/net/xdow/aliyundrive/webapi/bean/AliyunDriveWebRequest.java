package net.xdow.aliyundrive.webapi.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NonNull;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;

public class AliyunDriveWebRequest {

    @Data
    public static class ShareTokenInfo {
        @NonNull
        private String shareId;
        @NonNull
        @SerializedName("share_pwd")
        private String sharePassword;
    }

    @Data
    public static class ShareListInfo extends AliyunDriveWebShareRequestInfo {
        @NonNull
        private String shareId;
        private int limit = 100;
        private String marker;
        private AliyunDriveEnum.OrderBy orderBy;
        private AliyunDriveEnum.OrderDirection orderDirection;
        @NonNull
        private String parentFileId;
        private AliyunDriveEnum.Category category;
        @SerializedName("video_thumbnail_time")
        private Integer videoThumbnailTimeMS;
        private Integer videoThumbnailWidth;
        private Integer imageThumbnailWidth;
        private String fields = "*";
    }

    @Data
    public static class ShareGetFileInfo extends AliyunDriveWebShareRequestInfo {
        @NonNull
        private String shareId;
        @NonNull
        private String fileId;
        private String category;
        @SerializedName("video_thumbnail_time")
        private Long videoThumbnailTimeMS;
        private Integer videoThumbnailWidth;
        private Integer imageThumbnailWidth;
        private String videoThumbnailProcess = "video/snapshot,t_1000,f_jpg,ar_auto,w_300";

        public void setThumbnailTimeMs(long time_ms) {
            setVideoThumbnailProcess("video/snapshot,t_" + time_ms + ",f_jpg,ar_auto,w_300");
        }
    }

    @Data
    public static class DirectTransferGetFileInfo extends AliyunDriveWebShareRequestInfo {
        @NonNull
        private String shareId;
        private String fileId;
        private String category;
        @SerializedName("video_thumbnail_time")
        private Long videoThumbnailTimeMS;
        private Integer videoThumbnailWidth;
        private Integer imageThumbnailWidth;
        private String videoThumbnailProcess = "video/snapshot,t_1000,f_jpg,ar_auto,w_300";

        public void setThumbnailTimeMs(long time_ms) {
            setVideoThumbnailProcess("video/snapshot,t_" + time_ms + ",f_jpg,ar_auto,w_300");
        }
    }

    @Data
    public static class ShareSaveInfo extends AliyunDriveWebShareRequestInfo {
        @NonNull
        private String shareId;
        @NonNull
        private String fileId;
        @NonNull
        private String toDriveId;
        @NonNull
        private String toParentFileId;
        private boolean autoRename = true;
    }

    @Data
    public static class DirectTransferSaveInfo extends AliyunDriveWebShareRequestInfo {
        @NonNull
        private String shareId;
    }
}
