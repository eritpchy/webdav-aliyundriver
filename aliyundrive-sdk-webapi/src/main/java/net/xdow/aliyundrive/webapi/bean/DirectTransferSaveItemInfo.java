package net.xdow.aliyundrive.webapi.bean;

import lombok.Data;

@Data
public class DirectTransferSaveItemInfo {

    private String id;
    private int status;
    private Body body;

    public String getFileId() {
        if (body == null) {
            return null;
        }
        return body.getFileId();
    }

    public String getDriveId() {
        if (body == null) {
            return null;
        }
        return body.getDriveId();
    }

    @Data
    public static class Body {
        private String domainId;
        private String driveId;
        private String fileId;
    }
}
