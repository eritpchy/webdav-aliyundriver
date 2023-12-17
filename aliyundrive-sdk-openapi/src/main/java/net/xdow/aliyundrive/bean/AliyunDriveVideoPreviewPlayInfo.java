package net.xdow.aliyundrive.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AliyunDriveVideoPreviewPlayInfo {
    private String category;
    private AliyunDriveMediaMetaData meta;
    private List<AliyunDriveLiveTranscodingTaskInfo> liveTranscodingTaskList = new ArrayList<>();
    private List<AliyunDriveLiveTranscodingSubtitleTaskInfo> liveTranscodingSubtitleTaskList = new ArrayList<>();
}
