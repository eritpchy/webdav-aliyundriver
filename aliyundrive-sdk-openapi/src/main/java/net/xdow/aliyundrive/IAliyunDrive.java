package net.xdow.aliyundrive;

import net.xdow.aliyundrive.bean.AliyunDriveRequest;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import okhttp3.Call;

import java.util.Map;

public interface IAliyunDrive {

    int FLAG_API_ANONYMOUS_CALL = 1 << 0;
    int FLAG_API_AUTHENTICATION_CALL = 1 << 1;
    AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> getAccessToken(AliyunDriveRequest.AccessTokenInfo query);
    AliyunDriveCall<AliyunDriveResponse.QrCodeGenerateInfo> qrCodeGenerate(AliyunDriveRequest.QrCodeGenerateInfo query);

    /**
     * Get access token from custom server
     * ex: https://server.com/oauth/access_token?code=123456&refresh_token=45689
     * @param url
     * @return
     */
    AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> getAccessToken(String url);
    AliyunDriveCall<AliyunDriveResponse.QrCodeGenerateInfo> qrCodeGenerate(String url);
    AliyunDriveCall<AliyunDriveResponse.QrCodeQueryStatusInfo> qrCodeQueryStatus(String sid);
    String qrCodeImageUrl(String sid);
    AliyunDriveCall<AliyunDriveResponse.FileListInfo> fileList(AliyunDriveRequest.FileListInfo query);
    AliyunDriveCall<AliyunDriveResponse.UserSpaceInfo> getUserSpaceInfo();
    AliyunDriveCall<AliyunDriveResponse.UserDriveInfo> getUserDriveInfo();
    AliyunDriveCall<AliyunDriveResponse.FileGetInfo> fileGet(AliyunDriveRequest.FileGetInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileGetInfo> fileGetByPath(AliyunDriveRequest.FileGetByPathInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileBatchGetInfo> fileBatchGet(AliyunDriveRequest.FileBatchGetInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileGetDownloadUrlInfo> fileGetDownloadUrl(AliyunDriveRequest.FileGetDownloadUrlInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileCreateInfo> fileCreate(AliyunDriveRequest.FileCreateInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileGetUploadUrlInfo> fileGetUploadUrl(AliyunDriveRequest.FileGetUploadUrlInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileListUploadPartsInfo> fileListUploadedParts(AliyunDriveRequest.FileListUploadPartsInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileUploadCompleteInfo> fileUploadComplete(AliyunDriveRequest.FileUploadCompleteInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileRenameInfo> fileRename(AliyunDriveRequest.FileRenameInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileMoveInfo> fileMove(AliyunDriveRequest.FileMoveInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileCopyInfo> fileCopy(AliyunDriveRequest.FileCopyInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileMoveToTrashInfo> fileMoveToTrash(AliyunDriveRequest.FileMoveToTrashInfo query);
    AliyunDriveCall<AliyunDriveResponse.FileDeleteInfo> fileDelete(AliyunDriveRequest.FileDeleteInfo query);
    AliyunDriveCall<AliyunDriveResponse.VideoPreviewPlayInfo> videoPreviewPlayInfo(AliyunDriveRequest.VideoPreviewPlayInfo query);
    Call upload(String url, byte[] bytes, final int offset, final int byteCount);
    Call download(String url, String range, String ifRange);
    Map<String, String> getCommonHeaders();
    void setAccessTokenInfo(AliyunDriveResponse.AccessTokenInfo info);
    void setAuthorizer(IAliyunDriveAuthorizer listener);

    void setAccessTokenInvalidListener(Runnable listener);
}
