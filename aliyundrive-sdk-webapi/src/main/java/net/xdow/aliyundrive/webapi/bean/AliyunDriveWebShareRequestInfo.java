package net.xdow.aliyundrive.webapi.bean;

import lombok.Data;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.util.StringUtils;

@Data
public class AliyunDriveWebShareRequestInfo extends AliyunDriveResponse.GenericMessageInfo {
    private String shareToken;
    private String expiration;
    private String expireTime;
    private int expiresIn = 7200;

    private transient boolean skipShareToken = false;

    public String getExpireTime() {
        if (StringUtils.isEmpty(expireTime)) {
            return expiration;
        }
        return expireTime;
    }
}
