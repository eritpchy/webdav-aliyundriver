package net.xdow.aliyundrive;

import net.xdow.aliyundrive.bean.*;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import net.xdow.aliyundrive.util.Categories;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.util.StringUtils;
import net.xdow.aliyundrive.webapi.AliyunDriveWebConstant;
import net.xdow.aliyundrive.webapi.bean.AliyunDriveWebRequest;
import net.xdow.aliyundrive.webapi.bean.AliyunDriveWebResponse;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import okhttp3.Call;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AliyunDriveTests {
    private static final String ACCESS_TOKEN_CODE = "";
    private static final boolean TEST_GET_ACCESS_TOKEN_ASYNC = Boolean.parseBoolean("false");
    private static String mOpenApiClientId = "";
    private static String mOpenApiClientSecret = "";

    private static final String[] ALIYUN_DRIVER_SCOPES = new String[]{"file:all:write", "file:all:read", "user:base", "user:phone"};
    private AliyunDriveOpenApiImplV1 mAliyunDriveClient = new AliyunDriveOpenApiImplV1();

    private static String mOpenApiAccessTokenJson = "{}";
    private AliyunDriveResponse.AccessTokenInfo mAccessTokenInfo;
    private String mDefaultDriveId;

    private AliyunDriveResponse.FileListInfo mRootFileListInfo;
    private String mFileUploadId;
    private String mFileUploadFileId;
    private List<AliyunDriveFilePartInfo> mFilePartInfoList;

    private String mQrCodeSid;
    private Properties mLocalProperties = new Properties();
    private String mWebApiRefreshToken;
    private String mWebApiAccessToken;
    private String mWebApiSignature;
    private String mWebApiDeviceId;
    private String mTestShareId;
    private String mTestSharePassword;
    private AliyunDriveWebApiImplV1 webApi = new AliyunDriveWebApiImplV1();



    public AliyunDriveTests() throws Exception {
        this.mAliyunDriveClient.setAccessTokenInfo(this.mAccessTokenInfo);
        InputStream input = new FileInputStream("local.properties");
        this.mLocalProperties.load(input);
        this.mOpenApiClientId = this.mLocalProperties.getProperty("client_id");
        this.mOpenApiClientSecret = this.mLocalProperties.getProperty("client_secret");
        this.mOpenApiAccessTokenJson = this.mLocalProperties.getProperty("access_token_info");
        this.mAccessTokenInfo = JsonUtils.fromJson(mOpenApiAccessTokenJson, AliyunDriveResponse.AccessTokenInfo.class);
        this.mWebApiRefreshToken = this.mLocalProperties.getProperty("web_api_refresh_token");
        this.mWebApiAccessToken = this.mLocalProperties.getProperty("web_api_access_token");
        this.mWebApiSignature = this.mLocalProperties.getProperty("web_api_signature");
        this.mWebApiDeviceId = this.mLocalProperties.getProperty("web_api_device_id");
        this.mTestShareId = this.mLocalProperties.getProperty("test_share_id");
        this.mTestSharePassword = this.mLocalProperties.getProperty("test_share_password");
        this.mDefaultDriveId = this.mLocalProperties.getProperty("default_drive_id");
        input.close();
        webApiAuthorize();
    }

    @Test
    @Order(1)
    public void testHelloWorld() {
        assertEquals(2, 1 + 1, "message");
    }

    @Test
    @Order(2)
    public void testGson() throws Exception {
        AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
        query.setClientId(mOpenApiClientId);
        query.setClientSecret(mOpenApiClientSecret);
        query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
        query.setRefreshToken(this.mAccessTokenInfo.getRefreshToken());
        AliyunDriveResponse.AccessTokenInfo res = this.mAliyunDriveClient.getAccessToken(query).execute();
        assertTrue(!res.isError());
        this.mAccessTokenInfo = res;
        this.mLocalProperties.setProperty("access_token_info", JsonUtils.toJson(res));
        FileOutputStream outputStream = new FileOutputStream("local.properties");
        this.mLocalProperties.store(outputStream, null);
        outputStream.close();
        new FileInputStream("local.properties");
        String json = JsonUtils.toJson(query);
        System.out.println("AliyunDriveAccessTokenRequest: " + json);
        assertTrue(json.contains("client_id"));
        this.mAliyunDriveClient.setAccessTokenInfo(res);
    }

    @Test
    @Order(3)
    public void testAccessToken() {
        if (TEST_GET_ACCESS_TOKEN_ASYNC) {
            return;
        }
        if (ACCESS_TOKEN_CODE.isEmpty()) {
            return;
        }

        AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
        query.setClientId(mOpenApiClientId);
        query.setClientSecret(mOpenApiClientSecret);
        query.setGrantType(AliyunDriveEnum.GrantType.AuthorizationCode);
        query.setCode(ACCESS_TOKEN_CODE);
        AliyunDriveResponse.AccessTokenInfo res = this.mAliyunDriveClient.getAccessToken(query).execute();
        System.out.println("AccessTokenInfo: " + JsonUtils.toJson(res));
        assertTrue(!res.getAccessToken().isEmpty());
        this.mAliyunDriveClient.setAccessTokenInfo(res);
    }

    @Test
    @Order(4)
    public void testAccessTokenAsync() throws InterruptedException {
        if (!TEST_GET_ACCESS_TOKEN_ASYNC) {
            return;
        }
        if (ACCESS_TOKEN_CODE.isEmpty()) {
            return;
        }
        final CountDownLatch lock = new CountDownLatch(1);
        AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
        query.setClientId(mOpenApiClientId);
        query.setClientSecret(mOpenApiClientSecret);
        query.setGrantType(AliyunDriveEnum.GrantType.AuthorizationCode);
        query.setCode(ACCESS_TOKEN_CODE);
        this.mAliyunDriveClient.getAccessToken(query).enqueue(new AliyunDriveCall.Callback<AliyunDriveResponse.AccessTokenInfo>() {
            @Override
            public void onResponse(Call call, Response response, AliyunDriveResponse.AccessTokenInfo res) {
                System.out.println("AccessTokenInfo: " + JsonUtils.toJson(res));
                AliyunDriveTests.this.mAliyunDriveClient.setAccessTokenInfo(res);
                assertTrue(!res.getAccessToken().isEmpty());
                lock.countDown();
            }

            @Override
            public void onFailure(Call call, Throwable t, AliyunDriveResponse.AccessTokenInfo res) {
                System.out.println("AccessTokenInfo: " + JsonUtils.toJson(res));
                fail();
                lock.countDown();
            }
        });
        lock.await();
    }

    @Test
    @Order(5)
    public void testGetUserSpaceInfo() {
        AliyunDriveResponse.UserSpaceInfo userSpaceInfo = this.mAliyunDriveClient.getUserSpaceInfo().execute();
        System.out.println("userSpaceInfo: " + JsonUtils.toJsonPretty(userSpaceInfo));
    }

    @Test
    @Order(6)
    public void testGetUserDriveInfo() {
        AliyunDriveResponse.UserDriveInfo userDriveInfo = this.mAliyunDriveClient.getUserDriveInfo().execute();
        System.out.println("userDriveInfo: " + JsonUtils.toJsonPretty(userDriveInfo));
        assertTrue(!userDriveInfo.getDefaultDriveId().isEmpty());
        this.mDefaultDriveId = userDriveInfo.getDefaultDriveId();
    }

    @Test
    @Order(7)
    public void testFileList() {
        AliyunDriveRequest.FileListInfo fileListInfo = new AliyunDriveRequest.FileListInfo(this.mDefaultDriveId, "root");
        fileListInfo.setParentFileId("root");
        fileListInfo.setCategories(Categories.toString(AliyunDriveEnum.Category.Video, AliyunDriveEnum.Category.Doc, AliyunDriveEnum.Category.Image));
        String categories = fileListInfo.getCategories();
        System.out.println("category: " + categories);
        assertEquals("video,doc,image", categories);
        AliyunDriveResponse.FileListInfo res = this.mAliyunDriveClient.fileList(fileListInfo).execute();
        System.out.println("fileList: " + JsonUtils.toJsonPretty(res));
        assertTrue(res.getItems().size() > 0);
        this.mRootFileListInfo = res;
    }

    private List<AliyunDriveFileInfo> shareListFromApi(AliyunDriveWebApiImplV1 webApi, String shareId, String shareToken,
                                                       String parentFileId,
                                                       String marker, List<AliyunDriveFileInfo> all) {
        AliyunDriveWebRequest.ShareListInfo query = new AliyunDriveWebRequest.ShareListInfo(
                shareId, parentFileId
        );
        query.setMarker(marker);
        query.setLimit(200);
        query.setOrderBy(AliyunDriveEnum.OrderBy.UpdatedAt);
        query.setOrderDirection(AliyunDriveEnum.OrderDirection.Desc);
        query.setShareToken(shareToken);
        AliyunDriveResponse.FileListInfo res = webApi.shareList(query).execute();
        all.addAll(res.getItems());
        String nextMarker = res.getNextMarker();
        if (StringUtils.isEmpty(nextMarker)) {
            return all;
        }
        return shareListFromApi(webApi, shareId, shareToken, parentFileId, nextMarker, all);
    }

    private AliyunDriveFileInfo findFirstShareFileByCategory(AliyunDriveWebApiImplV1 webApi,
                                                             String shareId, String shareToken,
                                                             AliyunDriveEnum.Category category,
                                                             String parentFileId) {
        List<AliyunDriveFileInfo> files = new ArrayList<>();
        this.shareListFromApi(webApi, shareId, shareToken, parentFileId, null, files);
        for (AliyunDriveFileInfo f : files) {
            if (!f.isFile()) {
                continue;
            }
            System.out.println(JsonUtils.toJsonPretty(f));
            if (f.getCategory() == category) {
                return f;
            }
        }
        for (AliyunDriveFileInfo f : files) {
            if (!f.isDirectory()) {
                continue;
            }
            AliyunDriveFileInfo childFile = findFirstShareFileByCategory(webApi, shareId, shareToken, category, f.getFileId());
            if (childFile != null) {
                return childFile;
            }
        }
        return null;

    }

    @Test
    @Order(8)
    public void testFileGet() {
        List<AliyunDriveFileInfo> list = this.mRootFileListInfo.getItems();
        AliyunDriveFileInfo fileInfo = null;
        for (AliyunDriveFileInfo info : list) {
            if (info.getType() == AliyunDriveEnum.Type.File) {
                fileInfo = info;
                break;
            }
        }
        AliyunDriveRequest.FileGetInfo query = new AliyunDriveRequest.FileGetInfo(fileInfo.getDriveId(), fileInfo.getFileId());
        AliyunDriveResponse.FileGetInfo res = this.mAliyunDriveClient.fileGet(query).execute();
        System.out.println("fileGet: " + JsonUtils.toJsonPretty(res));
        assertTrue(!res.getDriveId().isEmpty());
        assertTrue(!res.getFileId().isEmpty());
        assertTrue(!res.getName().isEmpty());
        if (res.getType() == AliyunDriveEnum.Type.File) {
            assertTrue(res.getSize() >= 0);
        }
        assertTrue(!res.getFileExtension().isEmpty());
        assertTrue(!res.getContentHash().isEmpty());
        assertTrue(res.getCategory() != null);
        assertTrue(res.getType() == AliyunDriveEnum.Type.File || res.getType() == AliyunDriveEnum.Type.Folder);
        assertNotNull(res.getCreatedAt());
        assertNotNull(res.getUpdatedAt());
    }

    @Test
    @Order(9)
    public void testFileCreate() {
        AliyunDriveRequest.FileCreateInfo query = new AliyunDriveRequest.FileCreateInfo(
                this.mDefaultDriveId, "root", "testFileCreate1",
                AliyunDriveEnum.Type.File, AliyunDriveEnum.CheckNameMode.Ignore
        );
        AliyunDriveResponse.FileCreateInfo res = this.mAliyunDriveClient.fileCreate(query).execute();
        System.out.println("FileCreateInfo: " + JsonUtils.toJsonPretty(res));
        assertTrue(!res.getDriveId().isEmpty());
        assertTrue(!res.getFileId().isEmpty());
        assertTrue(!res.getParentFileId().isEmpty());
        assertTrue(!res.getUploadId().isEmpty());
        assertEquals("testFileCreate1", res.getFileName());
        assertTrue(!res.getPartInfoList().isEmpty());
        this.mFileUploadId = res.getUploadId();
        this.mFileUploadFileId = res.getFileId();
        this.mFilePartInfoList = res.getPartInfoList();
    }

    @Test
    @Order(10)
    public void testGetFileUploadUrl() {
        AliyunDriveRequest.FileGetUploadUrlInfo query = new AliyunDriveRequest.FileGetUploadUrlInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, this.mFileUploadId, this.mFilePartInfoList
        );
        AliyunDriveResponse.FileGetUploadUrlInfo res = this.mAliyunDriveClient.fileGetUploadUrl(query).execute();
        System.out.println("FileGetUploadUrlInfo: " + JsonUtils.toJsonPretty(res));
        assertTrue(!res.getDriveId().isEmpty());
        assertTrue(!res.getFileId().isEmpty());
        assertTrue(!res.getUploadId().isEmpty());
        assertTrue(!res.getPartInfoList().isEmpty());
        assertTrue(!res.getPartInfoList().get(0).getUploadUrl().isEmpty());
    }

    @Test
    @Order(11)
    public void testListUploadedParts() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        AliyunDriveRequest.FileListUploadPartsInfo query = new AliyunDriveRequest.FileListUploadPartsInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, this.mFileUploadId
        );
        AliyunDriveResponse.FileListUploadPartsInfo res = this.mAliyunDriveClient.fileListUploadedParts(query).execute();
        System.out.println("FileListUploadPartsInfo: " + JsonUtils.toJsonPretty(res));
        assertEquals(this.mFileUploadId, res.getUploadId());
    }


    @Test
    @Order(12)
    public void testFileUploadComplete() {
        AliyunDriveRequest.FileUploadCompleteInfo query = new AliyunDriveRequest.FileUploadCompleteInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, this.mFileUploadId
        );
        AliyunDriveResponse.FileUploadCompleteInfo res = this.mAliyunDriveClient.fileUploadComplete(query).execute();
        System.out.println("FileUploadCompleteInfo: " + JsonUtils.toJsonPretty(res));

        assertEquals(this.mDefaultDriveId, res.getDriveId());
        assertEquals(this.mFileUploadFileId, res.getFileId());
        assertEquals("testFileCreate1", res.getName());
        assertTrue(res.getSize() >= 0);
        assertTrue(!res.getContentHash().isEmpty());
        assertTrue(res.getType() == AliyunDriveEnum.Type.File);
        assertNotNull(res.getCreatedAt());
        assertNotNull(res.getUpdatedAt());
    }

    @Test
    @Order(13)
    public void testFileRename() {
        AliyunDriveRequest.FileRenameInfo query = new AliyunDriveRequest.FileRenameInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, "testFileCreate2", "root"
        );
        AliyunDriveResponse.FileRenameInfo res = this.mAliyunDriveClient.fileRename(query).execute();
        System.out.println("FileRenameInfo: " + JsonUtils.toJsonPretty(res));
        assertEquals(this.mDefaultDriveId, res.getDriveId());
        assertEquals(this.mFileUploadFileId, res.getFileId());
        assertEquals("testFileCreate2", res.getName());
        assertTrue(!res.getContentHash().isEmpty());
        assertTrue(res.getType() == AliyunDriveEnum.Type.File);
        query = new AliyunDriveRequest.FileRenameInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, "testFileCreate1", "root"
        );
        res = this.mAliyunDriveClient.fileRename(query).execute();
        System.out.println("FileRenameInfo: " + JsonUtils.toJsonPretty(res));
        assertEquals(this.mDefaultDriveId, res.getDriveId());
        assertEquals(this.mFileUploadFileId, res.getFileId());
        assertTrue(!res.getContentHash().isEmpty());
        assertTrue(res.getType() == AliyunDriveEnum.Type.File);
        assertEquals("testFileCreate1", res.getName());
    }

    @Test
    @Order(14)
    public void testFileMove() {
        AliyunDriveRequest.FileMoveInfo query = new AliyunDriveRequest.FileMoveInfo(
                this.mDefaultDriveId, this.mDefaultDriveId, this.mFileUploadFileId, "root"
        );
        AliyunDriveResponse.FileMoveInfo res = this.mAliyunDriveClient.fileMove(query).execute();
        System.out.println("FileMoveInfo: " + JsonUtils.toJsonPretty(res));
    }

    @Test
    @Order(14)
    public void testFileCopy() {
        AliyunDriveRequest.FileCopyInfo query = new AliyunDriveRequest.FileCopyInfo(
                this.mDefaultDriveId, this.mFileUploadFileId, this.mDefaultDriveId, "root"
        );
        query.setAutoRename(true);
        AliyunDriveResponse.FileCopyInfo res = this.mAliyunDriveClient.fileCopy(query).execute();
        System.out.println("FileCopyInfo: " + JsonUtils.toJsonPretty(res));
    }

    @Test
    @Order(15)
    public void testFileMoveToTrash() {
        AliyunDriveRequest.FileMoveToTrashInfo query = new AliyunDriveRequest.FileMoveToTrashInfo(
                this.mDefaultDriveId, this.mFileUploadFileId
        );
        AliyunDriveResponse.FileMoveToTrashInfo res = this.mAliyunDriveClient.fileMoveToTrash(query).execute();
        System.out.println("FileMoveToTrashInfo: " + JsonUtils.toJsonPretty(res));
    }

    @Test
    @Order(16)
    public void testFileDelete() {
        AliyunDriveRequest.FileDeleteInfo query = new AliyunDriveRequest.FileDeleteInfo(
                this.mDefaultDriveId, this.mFileUploadFileId
        );
        AliyunDriveResponse.FileDeleteInfo res = this.mAliyunDriveClient.fileDelete(query).execute();
        System.out.println("FileDeleteInfo: " + JsonUtils.toJsonPretty(res));
    }

    @Test
    @Order(17)
    public void testQrCodeGenerate() {
        AliyunDriveRequest.QrCodeGenerateInfo query = new AliyunDriveRequest.QrCodeGenerateInfo(
                mOpenApiClientId, mOpenApiClientSecret, ALIYUN_DRIVER_SCOPES
        );
        AliyunDriveResponse.QrCodeGenerateInfo res = this.mAliyunDriveClient.qrCodeGenerate(query).execute();
        System.out.println("QrCodeGenerateInfo: " + JsonUtils.toJsonPretty(res));
        assertTrue(!res.getQrCodeUrl().isEmpty());
        assertTrue(!res.getSid().isEmpty());
        this.mQrCodeSid = res.getSid();
    }

    @Test
    @Order(18)
    public void testQrCodeQueryStatus() {
        AliyunDriveResponse.QrCodeQueryStatusInfo res = this.mAliyunDriveClient.qrCodeQueryStatus(this.mQrCodeSid).execute();
        System.out.println("QrCodeQueryStatusInfo: " + JsonUtils.toJsonPretty(res));
        assertTrue(res.getStatus() == AliyunDriveEnum.QrCodeState.WaitLogin);
    }


    @Test
    @Order(19)
    public void testFileListShared() {
        AliyunDriveRequest.FileListInfo fileListInfo = new AliyunDriveRequest.FileListInfo(this.mDefaultDriveId, "root");
        fileListInfo.setDriveId("8cto5ne9wdU");
        fileListInfo.setParentFileId("root");
        fileListInfo.setCategories(Categories.toString(AliyunDriveEnum.Category.Video, AliyunDriveEnum.Category.Doc, AliyunDriveEnum.Category.Image));
        String categories = fileListInfo.getCategories();
        System.out.println("categories: " + categories);
        assertEquals("video,doc,image", categories);
        AliyunDriveResponse.FileListInfo res = this.mAliyunDriveClient.fileList(fileListInfo).execute();
        System.out.println("fileList Share: " + JsonUtils.toJsonPretty(res));
        assertTrue(res.getItems().size() > 0);
        this.mRootFileListInfo = res;

        String shareId = this.mTestShareId;
        String sharePassword = this.mTestSharePassword;

        AliyunDriveResponse.UserDriveInfo userDriveInfoRes = webApi.getUserDriveInfo().execute();
        assertTrue(!userDriveInfoRes.isError());
        assertTrue(!userDriveInfoRes.getDefaultDriveId().isEmpty());


        AliyunDriveWebResponse.ShareTokenInfo shareTokenRes = webApi.shareToken(new AliyunDriveWebRequest.ShareTokenInfo(shareId, sharePassword)).execute();
        System.out.println("ShareTokenInfo: " + JsonUtils.toJsonPretty(shareTokenRes));
        assertTrue(!shareTokenRes.isError());
        assertTrue(!shareTokenRes.getShareToken().isEmpty());
        AliyunDriveWebRequest.ShareListInfo shareListQuery = new AliyunDriveWebRequest.ShareListInfo(
                shareId, "root"
        );
        shareListQuery.setShareToken(shareTokenRes.getShareToken());
        AliyunDriveResponse.FileListInfo shareListRes = webApi.shareList(shareListQuery).execute();
        System.out.println("shareListRes: " + JsonUtils.toJsonPretty(shareListRes));
        assertTrue(!shareListRes.isError());
        assertTrue(shareListRes.getItems().size() > 0);

        AliyunDriveFileInfo videoFile = findFirstShareFileByCategory(webApi, shareId,
                shareTokenRes.getShareToken(), AliyunDriveEnum.Category.Video, "root");
        System.out.println("findFirstShareVideoFile: " + JsonUtils.toJsonPretty(videoFile));
        assertNotNull(videoFile);

        AliyunDriveWebRequest.ShareSaveInfo shareSaveQuery = new AliyunDriveWebRequest.ShareSaveInfo(
                shareId, videoFile.getFileId(), userDriveInfoRes.getDefaultDriveId(), "root"
        );
        shareSaveQuery.setShareToken(shareTokenRes.getShareToken());
        AliyunDriveWebResponse.ShareSaveInfo shareSaveRes = webApi.shareSave(shareSaveQuery).execute();
        System.out.println("ShareSaveInfo: " + JsonUtils.toJsonPretty(shareSaveRes));
        assertTrue(!shareSaveRes.isError());
        assertTrue(!shareSaveRes.getFileId().isEmpty());
    }

    @Test
    @Order(20)
    public void testFileDirectTransfer() {
        String shareId = "kA0c1ihtZdYQOXnxm1KE";
        String sharePassword = "";
        AliyunDriveWebResponse.ShareTokenInfo shareTokenRes = webApi.directTransferToken(new AliyunDriveWebRequest.ShareTokenInfo(shareId, sharePassword)).execute();
        System.out.println("ShareTokenInfo: " + JsonUtils.toJsonPretty(shareTokenRes));
        assertTrue(!shareTokenRes.isError());
        assertTrue(!shareTokenRes.getShareToken().isEmpty());

        AliyunDriveWebRequest.DirectTransferSaveInfo testFileDirectTransferSaveQuery = new AliyunDriveWebRequest.DirectTransferSaveInfo(shareId);
        testFileDirectTransferSaveQuery.setShareToken(shareTokenRes.getShareToken());
        AliyunDriveWebResponse.DirectTransferSaveInfo testFileDirectTransferSaveRes = webApi.directTransferSave(testFileDirectTransferSaveQuery).execute();
        System.out.println("testFileDirectTransferSaveRes: " + JsonUtils.toJsonPretty(testFileDirectTransferSaveRes));
        assertTrue(!testFileDirectTransferSaveRes.isError());
        assertTrue(!testFileDirectTransferSaveRes.getFileId().isEmpty());

        AliyunDriveWebResponse.DirectTransferGetFileInfo directTransferGetFileRes = webApi.directTransferGetFile(new AliyunDriveWebRequest.DirectTransferGetFileInfo(shareId)).execute();
        System.out.println("directTransferGetFileRes: " + JsonUtils.toJsonPretty(directTransferGetFileRes));

    }

    @Test
    @Order(21)
    public void testFileRestoreFromTrash() {
        AliyunDriveResponse.GenericMessageInfo res = webApi.fileRestoreFromTrash(
                new AliyunDriveRequest.FileRestoreFromTrashInfo(mDefaultDriveId, "659516f22b76ccc4dd0044c689cd95671fe102a2")).execute();
        System.out.println("fileRestoreFromTrash: " + JsonUtils.toJsonPretty(res));
        assertFalse(res.isError());
    }

    private void webApiAuthorize() {
        AliyunDriveResponse.AccessTokenInfo token = new AliyunDriveResponse.AccessTokenInfo();
        token.setRefreshToken(this.mWebApiRefreshToken);
        token.setAccessToken(this.mWebApiAccessToken);
        token.setTokenType("Bearer");
        webApi.setAccessTokenInfo(token);
        webApi.setAuthorizer(new IAliyunDriveAuthorizer() {

            @Override
            public AliyunDriveResponse.AccessTokenInfo acquireNewAccessToken(AliyunDriveResponse.AccessTokenInfo oldAccessTokenInfo) {
                return token;
            }

            @Override
            public <T> T onAuthorizerEvent(String eventId, Object data, Class<T> resultCls) {
                switch (eventId) {
                    case AliyunDriveWebConstant.Event.ACQUIRE_DEVICE_ID: {
                        return (T) AliyunDriveTests.this.mWebApiDeviceId;
                    }
                    case AliyunDriveWebConstant.Event.ACQUIRE_SESSION_SIGNATURE: {
                        return (T) AliyunDriveTests.this.mWebApiSignature;
                    }
                }
                return null;
            }
        });
    }
}
