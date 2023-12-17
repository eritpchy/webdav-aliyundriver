package net.xdow.aliyundrive.impl;

import net.xdow.aliyundrive.AliyunDriveConstant;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.IAliyunDriveAuthorizer;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;
import net.xdow.aliyundrive.bean.AliyunDriveFilePartInfo;
import net.xdow.aliyundrive.bean.AliyunDriveRequest;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import net.xdow.aliyundrive.net.interceptor.AccessTokenInvalidInterceptor;
import net.xdow.aliyundrive.net.interceptor.AliyunDriveAuthenticateInterceptor;
import net.xdow.aliyundrive.net.interceptor.XHttpLoggingInterceptor;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.util.StringUtils;
import okhttp3.*;
import okhttp3.internal.Util;
import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AliyunDriveOpenApiImplV1 implements IAliyunDrive, AliyunDriveAuthenticateInterceptor.IAccessTokenInfoGetter {

    private OkHttpClient mOkHttpClient;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private AliyunDriveResponse.AccessTokenInfo mAccessTokenInfo;
    private IAliyunDriveAuthorizer mAliyunDriveAuthorizer;
    private AccessTokenInvalidInterceptor mAccessTokenInvalidInterceptor = new AccessTokenInvalidInterceptor();

    public AliyunDriveOpenApiImplV1() {
        initOkHttp();
    }

    private void initOkHttp() {
        XHttpLoggingInterceptor loggingInterceptor = new XHttpLoggingInterceptor();
        AliyunDriveAuthenticateInterceptor authenticateInterceptor = new AliyunDriveAuthenticateInterceptor(this);
        this.mOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)//response ↑
                .addInterceptor(authenticateInterceptor) //response ↑
                .addInterceptor(this.mAccessTokenInvalidInterceptor) //response ↑
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Response response = chain.proceed(AliyunDriveOpenApiImplV1.this.buildCommonRequestHeader(request));
                        try {
                            int code = response.code();
                            if (code == 401 || code == 400) {
                                ResponseBody body = response.peekBody(40960);
                                try {
                                    String res = body.string();
                                    if (res.contains("AccessTokenInvalid") || res.contains("TokenExpired")) {
                                        AliyunDriveOpenApiImplV1.this.requestNewAccessToken();
                                        return chain.proceed(AliyunDriveOpenApiImplV1.this.buildCommonRequestHeader(request));
                                    }
                                } finally {
                                    Util.closeQuietly(body);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                        return response;
                    }
                }) //response ↑
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        List<InetAddress> list = new ArrayList<>();
                        UnknownHostException unknownHostException = null;
                        try {
                            list.addAll(Dns.SYSTEM.lookup(hostname));
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                        }
                        try {
                            list.addAll(Arrays.asList(Address.getAllByName(hostname)));
                        } catch (UnknownHostException e) {
                            if (unknownHostException == null) {
                                unknownHostException = e;
                            }
                        }
                        if (list.size() <= 0 && unknownHostException != null) {
                            throw unknownHostException;
                        }
                        return list;
                    }
                })
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build();
    }


    public void setAccessTokenInfo(AliyunDriveResponse.AccessTokenInfo info) {
        this.mAccessTokenInfo = info;
    }

    @Override
    public void setAuthorizer(IAliyunDriveAuthorizer authorizer) {
        this.mAliyunDriveAuthorizer = authorizer;
    }

    @Override
    public void setAccessTokenInvalidListener(Runnable listener) {
        this.mAccessTokenInvalidInterceptor.setAccessTokenInvalidListener(listener);
    }

    private void requestNewAccessToken() {
        IAliyunDriveAuthorizer authorizer = this.mAliyunDriveAuthorizer;
        if (authorizer == null) {
            return;
        }
        try {
            AliyunDriveResponse.AccessTokenInfo newAccessTokenInfo = authorizer.acquireNewAccessToken(this.mAccessTokenInfo);
            if (newAccessTokenInfo != null) {
                this.mAccessTokenInfo = newAccessTokenInfo;
            }
        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    private Request buildCommonRequestHeader(Request request) {
        Request.Builder builder = request.newBuilder();
        Map<String, String> map = getCommonHeaders();
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            builder.removeHeader(key);
            builder.addHeader(key, value);
        }
        return builder.build();
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> getAccessToken(String url) {
        return postApiRequest(url, AliyunDriveResponse.AccessTokenInfo.class, FLAG_API_ANONYMOUS_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.QrCodeGenerateInfo> qrCodeGenerate(String url) {
        return postApiRequest(url, AliyunDriveResponse.QrCodeGenerateInfo.class, FLAG_API_ANONYMOUS_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.QrCodeQueryStatusInfo> qrCodeQueryStatus(String sid) {
        String url = String.format(Locale.getDefault(), AliyunDriveConstant.API_QRCODE_QUERY_STATUS, sid);
        return getApiRequest(url, AliyunDriveResponse.QrCodeQueryStatusInfo.class, FLAG_API_ANONYMOUS_CALL);
    }

    @Override
    public String qrCodeImageUrl(String sid) {
        return String.format(Locale.getDefault(), AliyunDriveConstant.API_QRCODE_IMAGE, sid);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> getAccessToken(AliyunDriveRequest.AccessTokenInfo query) {
        if (query.getGrantType() == AliyunDriveEnum.GrantType.RefreshToken) {
            String[] refreshTokenParts = query.getRefreshToken().split("\\.");
            if (refreshTokenParts.length < 3) {
                AliyunDriveResponse.AccessTokenInfo res = new AliyunDriveResponse.AccessTokenInfo();
                res.setCode("JWTDecodeException");
                res.setMessage("The token was expected to have 3 parts, but got " + refreshTokenParts.length + ".");
                return new AliyunDriveCall<>(res);
            }
        }
        return postApiRequest(AliyunDriveConstant.API_ACCESS_TOKEN, query,
                AliyunDriveResponse.AccessTokenInfo.class, FLAG_API_ANONYMOUS_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.QrCodeGenerateInfo> qrCodeGenerate(AliyunDriveRequest.QrCodeGenerateInfo query) {
        return postApiRequest(AliyunDriveConstant.API_QRCODE_GENERATE, query,
                AliyunDriveResponse.QrCodeGenerateInfo.class, FLAG_API_ANONYMOUS_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileListInfo> fileList(AliyunDriveRequest.FileListInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_LIST, query,
                AliyunDriveResponse.FileListInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.UserSpaceInfo> getUserSpaceInfo() {
        return postApiRequest(AliyunDriveConstant.API_USER_GET_SPACE_INFO,
                AliyunDriveResponse.UserSpaceInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.UserDriveInfo> getUserDriveInfo() {
        return postApiRequest(AliyunDriveConstant.API_USER_GET_DRIVE_INFO,
                AliyunDriveResponse.UserDriveInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileGetInfo> fileGet(AliyunDriveRequest.FileGetInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_GET, query,
                AliyunDriveResponse.FileGetInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileGetInfo> fileGetByPath(AliyunDriveRequest.FileGetByPathInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_GET_BY_PATH, query,
                AliyunDriveResponse.FileGetInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileBatchGetInfo> fileBatchGet(AliyunDriveRequest.FileBatchGetInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_BATCH_GET, query,
                AliyunDriveResponse.FileBatchGetInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileGetDownloadUrlInfo> fileGetDownloadUrl(AliyunDriveRequest.FileGetDownloadUrlInfo query) {
        int expireSec = query.getExpireSec();
        if (expireSec > 115200) {
            throw new IllegalArgumentException("Error: expire_sec argument must less then 115200s, got: " + expireSec);
        }
        return postApiRequest(AliyunDriveConstant.API_FILE_GET_DOWNLOAD_URL, query,
                AliyunDriveResponse.FileGetDownloadUrlInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileCreateInfo> fileCreate(AliyunDriveRequest.FileCreateInfo query) {
        List<AliyunDriveFilePartInfo> partInfoList = query.getPartInfoList();
        if (partInfoList != null) {
            int partInfoListSize = partInfoList.size();
            if (partInfoListSize > AliyunDriveConstant.MAX_FILE_CREATE_PART_INFO_LIST_SIZE) {
                throw new IllegalArgumentException("Error: max part_info_list size must < "
                        + AliyunDriveConstant.MAX_FILE_CREATE_PART_INFO_LIST_SIZE + ", got: " + partInfoListSize);
            }
        }
        return postApiRequest(AliyunDriveConstant.API_FILE_CREATE, query,
                AliyunDriveResponse.FileCreateInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileGetUploadUrlInfo> fileGetUploadUrl(AliyunDriveRequest.FileGetUploadUrlInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_GET_UPLOAD_URL, query,
                AliyunDriveResponse.FileGetUploadUrlInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileListUploadPartsInfo> fileListUploadedParts(AliyunDriveRequest.FileListUploadPartsInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_LIST_UPLOADED_PARTS, query,
                AliyunDriveResponse.FileListUploadPartsInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileUploadCompleteInfo> fileUploadComplete(AliyunDriveRequest.FileUploadCompleteInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_UPLOAD_COMPLETE, query,
                AliyunDriveResponse.FileUploadCompleteInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileRenameInfo> fileRename(AliyunDriveRequest.FileRenameInfo query) {
        //拒绝重名
        AliyunDriveRequest.FileCreateInfo createQuery = new AliyunDriveRequest.FileCreateInfo(
                query.getDriveId(), query.getParentFileId(), query.getName(), AliyunDriveEnum.Type.File, AliyunDriveEnum.CheckNameMode.Refuse
        );
        AliyunDriveResponse.FileCreateInfo createRes = this.fileCreate(createQuery).execute();
        String createdFileId = createRes.getFileId();
        if (!StringUtils.isEmpty(createdFileId)) {
            AliyunDriveRequest.FileDeleteInfo deleteQuery = new AliyunDriveRequest.FileDeleteInfo(
                    query.getDriveId(), createdFileId
            );
            this.fileDelete(deleteQuery).execute();
        }
        return postApiRequest(AliyunDriveConstant.API_FILE_RENAME, query,
                AliyunDriveResponse.FileRenameInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileMoveInfo> fileMove(AliyunDriveRequest.FileMoveInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_MOVE, query,
                AliyunDriveResponse.FileMoveInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileCopyInfo> fileCopy(AliyunDriveRequest.FileCopyInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_COPY, query,
                AliyunDriveResponse.FileCopyInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileMoveToTrashInfo> fileMoveToTrash(AliyunDriveRequest.FileMoveToTrashInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_MOVE_TO_TRASH, query,
                AliyunDriveResponse.FileMoveToTrashInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.FileDeleteInfo> fileDelete(AliyunDriveRequest.FileDeleteInfo query) {
        return postApiRequest(AliyunDriveConstant.API_FILE_DELETE, query,
                AliyunDriveResponse.FileDeleteInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public AliyunDriveCall<AliyunDriveResponse.VideoPreviewPlayInfo> videoPreviewPlayInfo(AliyunDriveRequest.VideoPreviewPlayInfo query) {
         return postApiRequest(AliyunDriveConstant.API_VIDEO_PREVIEW_PLAY_INFO, query,
                 AliyunDriveResponse.VideoPreviewPlayInfo.class, FLAG_API_AUTHENTICATION_CALL);
    }

    @Override
    public Call upload(String url, byte[] bytes, final int offset, final int byteCount) {
        Request request = new Request.Builder()
                .addHeader(AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_NAME, AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_VALUE)
                .addHeader(XHttpLoggingInterceptor.SKIP_HEADER_NAME, XHttpLoggingInterceptor.SKIP_HEADER_VALUE)
                .put(RequestBody.create(MediaType.parse(""), bytes, offset, byteCount))
                .url(url).build();
        return this.mOkHttpClient.newCall(request);
    }

    @Override
    public Call download(String url, String range, String ifRange) {
        Request.Builder builder = new Request.Builder();
        builder.addHeader(XHttpLoggingInterceptor.SKIP_HEADER_NAME, XHttpLoggingInterceptor.SKIP_HEADER_VALUE);
        builder.addHeader(AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_NAME,
                AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_VALUE);

        if (range != null) {
            builder.header("range", range);
        }
        if (ifRange != null) {
            builder.header("if-range", ifRange);
        }

        Request request = builder.url(url).build();
        return this.mOkHttpClient.newCall(request);
    }

    @Override
    public Map<String, String> getCommonHeaders() {
        Map<String, String> map = new HashMap<>();
        AliyunDriveResponse.AccessTokenInfo info = this.mAccessTokenInfo;
        if (info != null) {
            map.put("authorization", info.getTokenType() + " " + info.getAccessToken());
        }
        map.put("referer", AliyunDriveConstant.REFERER);
        return map;
    }

    private <T extends AliyunDriveResponse.GenericMessageInfo> AliyunDriveCall<T> getApiRequest(
            String url, Class<T> classOfT, int flags) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return new AliyunDriveCall<>(this.mOkHttpClient.newCall(request), classOfT);
    }

    private <T extends AliyunDriveResponse.GenericMessageInfo> AliyunDriveCall<T> postApiRequest(
            String url, Class<T> classOfT, int flags) {
        return postApiRequest(url, null, classOfT, flags);
    }

    private <T extends AliyunDriveResponse.GenericMessageInfo> AliyunDriveCall<T> postApiRequest(
            String url, Object object, Class<T> classOfT, int flags) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (object == null) {
            builder.post(RequestBody.create(JSON, "{}"));
        } else {
            builder.post(RequestBody.create(JSON, JsonUtils.toJson(object)));
        }
        if ((FLAG_API_AUTHENTICATION_CALL & flags) != 0) {
            builder.addHeader(AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_NAME,
                    AliyunDriveAuthenticateInterceptor.HEADER_AUTHENTICATE_VALUE);
        }
        return new AliyunDriveCall<>(this.mOkHttpClient.newCall(builder.build()), classOfT);
    }

    @Override
    public AliyunDriveResponse.AccessTokenInfo getAccessTokenInfo() {
        return this.mAccessTokenInfo;
    }
}
