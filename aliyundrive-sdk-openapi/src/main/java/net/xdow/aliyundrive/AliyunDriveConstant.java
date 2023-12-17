package net.xdow.aliyundrive;

public class AliyunDriveConstant {

    public static final int MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC = 115200; //32h
    public static final int MAX_FILE_CREATE_PART_INFO_LIST_SIZE = 10000;
    public static final String REFERER = "https://www.aliyundrive.com/";
    public static final String API_HOST = "https://openapi.aliyundrive.com";
    public static final String API_ACCESS_TOKEN = API_HOST + "/oauth/access_token";
    public static final String API_QRCODE_GENERATE = API_HOST + "/oauth/authorize/qrcode";
    public static final String API_QRCODE_IMAGE = API_HOST + "/oauth/qrcode/%s";
    public static final String API_QRCODE_QUERY_STATUS = API_HOST + "/oauth/qrcode/%s/status";
    public static final String API_USER_INFO = API_HOST + "/oauth/users/info";
    public static final String API_USER_GET_SPACE_INFO = API_HOST + "/adrive/v1.0/user/getSpaceInfo";
    public static final String API_USER_GET_DRIVE_INFO = API_HOST + "/adrive/v1.0/user/getDriveInfo";
    public static final String API_USER_GET_VIP_INFO = API_HOST + "/adrive/v1.0/user/getVipInfo";
    public static final String API_FILE_LIST = API_HOST + "/adrive/v1.0/openFile/list";
    public static final String API_FILE_GET = API_HOST + "/adrive/v1.0/openFile/get";
    public static final String API_FILE_GET_BY_PATH = API_HOST + "/adrive/v1.0/openFile/get_by_path";
    public static final String API_FILE_BATCH_GET = API_HOST + "/adrive/v1.0/openFile/batch/get";
    public static final String API_FILE_GET_DOWNLOAD_URL = API_HOST + "/adrive/v1.0/openFile/getDownloadUrl";
    public static final String API_FILE_CREATE = API_HOST + "/adrive/v1.0/openFile/create";
    public static final String API_FILE_GET_UPLOAD_URL = API_HOST + "/adrive/v1.0/openFile/getUploadUrl";
    public static final String API_FILE_LIST_UPLOADED_PARTS = API_HOST + "/adrive/v1.0/openFile/listUploadedParts";
    public static final String API_FILE_UPLOAD_COMPLETE = API_HOST + "/adrive/v1.0/openFile/complete";
    public static final String API_FILE_RENAME = API_HOST + "/adrive/v1.0/openFile/update";
    public static final String API_FILE_MOVE = API_HOST + "/adrive/v1.0/openFile/move";
    public static final String API_FILE_COPY = API_HOST + "/adrive/v1.0/openFile/copy";
    public static final String API_FILE_MOVE_TO_TRASH = API_HOST + "/adrive/v1.0/openFile/recyclebin/trash";
    public static final String API_FILE_DELETE = API_HOST + "/adrive/v1.0/openFile/delete";
    public static final String API_VIDEO_PREVIEW_PLAY_INFO = API_HOST + "/adrive/v1.0/openFile/getVideoPreviewPlayInfo";
}
