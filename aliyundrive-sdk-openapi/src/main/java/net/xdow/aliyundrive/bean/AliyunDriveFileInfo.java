package net.xdow.aliyundrive.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

@Data
public class AliyunDriveFileInfo extends AliyunDriveResponse.GenericMessageInfo {
    private String driveId;
    private String fileId;
    private String parentFileId;
    private String name;
    private Long size;
    //fileList Api默认不取回
    private String fileExtension;
    private String contentHash;
    //fileList Api默认不取回
    private AliyunDriveEnum.Category category;
    private AliyunDriveEnum.Type type;
    //fileList Api默认不取回
    private String thumbnail;
    //fileList Api默认不取回
    private String url;
    //fileList Api默认不取回
    @Deprecated
    private String downloadUrl;
    private Date createdAt;
    private Date updatedAt;
    private Date localCreatedAt;
    private Date localModifiedAt;
    private String mimeType;

    /**
     * WebApi Only
     */
    @SerializedName("video_media_metadata")
    private AliyunDriveMediaMetaData videoMediaMetaData;


    public boolean isDirectory() {
        return type == AliyunDriveEnum.Type.Folder;
    }

    public boolean isFile() {
        return type == AliyunDriveEnum.Type.File;
    }

    public String getMimeType() {
        if ("ts".equalsIgnoreCase(fileExtension)) {
            return "video/mp2t";
        }
        return mimeType;
    }
}
