package net.xdow.aliyundrive.webapi;

public class AliyunDriveWebConstant {

    public static final int MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC = 115200; //32h
    public static final int MAX_FILE_CREATE_PART_INFO_LIST_SIZE = 10000;
    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36";
    public static final String REFERER = "https://www.aliyundrive.com/";
    public static final String API_HOST = "https://api.aliyundrive.com";
    public static final String API_HOST_USER = "https://user.aliyundrive.com/v2";

    public static final String API_TOKEN_REFRESH = "https://api.aliyundrive.com/token/refresh";
    public static final String API_ACCESS_TOKEN = API_TOKEN_REFRESH;
    public static final String API_USER_INFO = API_HOST + "/v2/user/get";
    public static final String API_USER_GET_SPACE_INFO = "https://api.aliyundrive.com/adrive/v1/user/driveCapacityDetails";
    public static final String API_USER_GET_DRIVE_INFO = API_HOST_USER + "/user/get";
    public static final String API_USER_GET_VIP_INFO = API_HOST + "/v2";
    public static final String API_FILE_LIST = API_HOST + "/v2/file/list";
    public static final String API_FILE_GET = API_HOST + "/v2/file/get";
    public static final String API_FILE_BATCH_GET = API_HOST + "/v2";
    public static final String API_FILE_GET_DOWNLOAD_URL = API_HOST + "/v2/file/get_download_url";
    public static final String API_FILE_CREATE = API_HOST + "/v2/file/create_with_proof";
    public static final String API_FILE_GET_UPLOAD_URL = API_HOST + "/v2/file/get_upload_url";
    public static final String API_FILE_LIST_UPLOADED_PARTS = API_HOST + "/v2";
    public static final String API_FILE_UPLOAD_COMPLETE = API_HOST + "/v2/file/complete";
    public static final String API_FILE_RENAME = API_HOST + "/v2/file/update";
    public static final String API_FILE_MOVE = API_HOST + "/v2/file/move";
    public static final String API_FILE_COPY = API_HOST + "/v2/file/copy";
    public static final String API_FILE_MOVE_TO_TRASH = API_HOST + "/v2/recyclebin/trash";
    public static final String API_FILE_RESTORE_FROM_TRASH = API_HOST + "/v2/recyclebin/restore";
    public static final String API_FILE_DELETE = API_HOST + "/v2/file/delete";

    public static final String API_SHARE_TOKEN = API_HOST + "/v2/share_link/get_share_token";

    public static final String API_SHARE_LIST = "https://api.aliyundrive.com/adrive/v2/file/list_by_share";

    public static final String API_SHARE_SAVE = API_FILE_COPY;
    public static final String API_SHARE_GET_FILE = "https://api.aliyundrive.com/adrive/v2/file/get_by_share";

    public static final String API_DIRECT_TRANSFER_TOKEN = API_HOST + "/adrive/v1/share/getShareToken";
    public static final String API_DIRECT_TRANSFER_SAVE = API_HOST + "/adrive/v1/share/saveFile";
    public static final String API_DIRECT_TRANSFER_GET_FILE = API_HOST + "/adrive/v1/share/getByAnonymous";

    public static class Event {
        public static final String DEVICE_SESSION_SIGNATURE_INVALID = "Web.DeviceSessionSignatureInvalid";
        public static final String USER_DEVICE_OFFLINE = "Web.UserDeviceOffline";
        public static final String REFRESH_TOKEN_INVALID = "Web.RefreshTokenInvalid";
        public static final String ACQUIRE_DEVICE_ID = "Web.AcquireDeviceId";
        public static final String ACQUIRE_SESSION_SIGNATURE = "Web.AcquireSessionSignature";
    }
}
