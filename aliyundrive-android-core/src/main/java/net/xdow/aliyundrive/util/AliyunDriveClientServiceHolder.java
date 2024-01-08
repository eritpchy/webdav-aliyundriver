package net.xdow.aliyundrive.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.R;
import net.xdow.aliyundrive.event.AliyunDriveAccessTokenInvalidEvent;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class AliyunDriveClientServiceHolder {
    private static AliyunDriveClientService sAliyunDriveClientService;
    public static AliyunDriveClientService getInstance() {
        if (sAliyunDriveClientService == null) {
            synchronized (AliyunDriveClientServiceHolder.class) {
                if (sAliyunDriveClientService == null) {
                    ContextHandler.Context webContext = WebAppContext.getCurrentContext();
                    Context context = (Context) webContext.getAttribute("org.mortbay.ijetty.context");
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String refreshToken = sharedPreferences.getString(context.getString(R.string.config_refresh_token),"");
                    String deviceId = sharedPreferences.getString(context.getString(R.string.config_device_id),"");
                    String shareToken = sharedPreferences.getString(context.getString(R.string.config_share_token),"");
                    String downloadProxyMode = sharedPreferences.getString(context.getString(R.string.config_download_proxy_mode),"Auto");
                    String workDir = ensureWorkDir(context).getAbsolutePath() + File.separator;
                    final AliyunDriveProperties properties = AliyunDriveProperties.load(workDir);
                    properties.setAuthorization(null);
                    if (!refreshToken.equals(properties.getRefreshTokenNext())) {
                        properties.setRefreshToken(refreshToken);
                    }
                    properties.setRefreshTokenNext(refreshToken);
                    properties.setDeviceId(deviceId);
                    properties.setShareToken(shareToken);
                    properties.setDownloadProxyMode(AliyunDriveProperties.DownloadProxyMode.valueOf(downloadProxyMode));
                    properties.setWorkDir(workDir);
                    properties.save();
                    boolean useAliyunDriveOpenApi = Boolean.parseBoolean(String.valueOf(webContext.getAttribute(context.getString(R.string.config_use_aliyun_drive_openapi))));
                    if (useAliyunDriveOpenApi) {
                        sAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveOpenApiImplV1.class, properties);
                    } else {
                        sAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveWebApiImplV1.class, properties);
                    }
                    sAliyunDriveClientService.setAccessTokenInvalidListener(() -> EventBus.getDefault().post(new AliyunDriveAccessTokenInvalidEvent()));
                    sAliyunDriveClientService.setOnAccountChangedListener(() -> {
                        String newRefreshToken = properties.getRefreshToken();
                        sharedPreferences.edit()
                                .putString(context.getString(R.string.config_refresh_token), newRefreshToken)
                                .apply();
                    });
                }
            }
        }
        return sAliyunDriveClientService;
    }

    private static File ensureWorkDir(Context context) {
        List<Callable<File>> workDirCallList =  new ArrayList<>();
        workDirCallList.add(context::getFilesDir);
        workDirCallList.add(() -> context.getExternalFilesDir(""));
        workDirCallList.add(() -> new File("/data/user_de/0/" + context.getPackageName() + "/files"));
        for (Callable<File> caller: workDirCallList) {
            try {
                File dir = caller.call();
                if (mkdirs(dir)) {
                    return dir;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return context.getFilesDir();
    }

    private static boolean mkdirs(File dir) {
        if (!dir.exists()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dir.exists() && dir.isDirectory();
    }

    public static void onShutdown() {
        synchronized (AliyunDriveClientServiceHolder.class) {
            sAliyunDriveClientService = null;
        }
    }
}
