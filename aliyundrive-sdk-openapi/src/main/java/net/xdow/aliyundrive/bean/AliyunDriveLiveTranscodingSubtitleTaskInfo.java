package net.xdow.aliyundrive.bean;

import lombok.Data;

@Data
public class AliyunDriveLiveTranscodingSubtitleTaskInfo {
    private String language;
    /**
     * 状态。 枚举值如下：
     * ● finished, 索引完成，可以获取到url
     * ● running, 正在索引，请稍等片刻重试
     * ● failed, 转码失败，请检查是否媒体文件，如果有疑问请联系客服
     */
    private String status;
    private String url;
}
