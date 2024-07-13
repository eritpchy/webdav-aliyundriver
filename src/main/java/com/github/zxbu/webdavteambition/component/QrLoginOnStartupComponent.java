package com.github.zxbu.webdavteambition.component;

import com.github.zxbu.webdavteambition.bean.AFileReqInfo;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import it.auties.qr.QrTerminal;
import lombok.extern.slf4j.Slf4j;
import net.xdow.aliyundrive.AliyunDriveConstant;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import net.xdow.aliyundrive.util.JsonUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

@Slf4j
@Component
public class QrLoginOnStartupComponent implements ApplicationListener<WebServerInitializedEvent> {

    private final static int QRCODE_CHECK_RETRY_COUNT = 60; //最大3分钟有效期
    private final static long QRCODE_CHECK_DELAY_MILLISECONDS = 2500L;

    @Autowired
    private TaskScheduler mTaskScheduler;

    @Autowired
    private AliyunDriveClientServiceHolder mAliyunDriveClientServiceHolder;

    private boolean mReLoginShowed = false;

    private AliyunDriveResponse.QrCodeGenerateInfo mQrCodeGenerateDataInfo = null;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        AliyunDriveClientService service = mAliyunDriveClientServiceHolder.getAliyunDriveClientService();
        if (service.getAliyunDrive() instanceof AliyunDriveOpenApiImplV1) {

        } else {
            // Not Supported
            return;
        }
        mTaskScheduler.schedule(() -> {
            try {
                AliyunDriveFileInfo root = service.getTFileByPath("/");
                AFileReqInfo rootInfo = AFileReqInfo.from(root);
                service.getTFileListCached(rootInfo);
            } catch (Throwable e) {
                if (e.getCause() instanceof NotAuthenticatedException) {
                    if (!mReLoginShowed) {
                        mReLoginShowed = true;
                        doReLogin();
                    }
                    return;
                }
            }
        }, Instant.now().plusSeconds(6));
    }

    private void doReLogin() {
        IAliyunDrive aliyunDrive = mAliyunDriveClientServiceHolder.getAliyunDriveClientService().getAliyunDrive();
        if (aliyunDrive instanceof AliyunDriveOpenApiImplV1 openApiDrive) {

        } else {
            throw new IllegalStateException("doReLogin not supported " + aliyunDrive);
        }
        openApiDrive.qrCodeGenerate(mAliyunDriveClientServiceHolder.getAliyunDriveClientService().getProperties().getAliyunQrGenerateUrl()).enqueue(new AliyunDriveCall.Callback<AliyunDriveResponse.QrCodeGenerateInfo>() {
            @Override
            public void onResponse(Call call, Response response, AliyunDriveResponse.QrCodeGenerateInfo res) {
                if (res.isError()) {
                    log.error("Error on qrCodeGenerate onResponse: {}", JsonUtils.toJson(res));
                    return;
                }
                mQrCodeGenerateDataInfo = res;
//                processQrCodeUrl(res.getQrCodeUrl());
                processQrCodeSid(res.getSid());
            }

            @Override
            public void onFailure(Call call, Throwable t, AliyunDriveResponse.QrCodeGenerateInfo res) {
                log.error("Error on qrCodeGenerate onFailure: {}", JsonUtils.toJson(res), t);
                log.error("", t);
            }
        });

    }

    private void processQrCodeSid(String sid) {
        try {
            String url = String.format(Locale.getDefault(), AliyunDriveConstant.API_QRCODE_AUTHORIZE, sid);
            BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 33, 33);
            QrTerminal.print(bitMatrix, true);

            log.info("请使用阿里云盘APP扫码登录, 或进入管理页面登录.");
            log.info("Please scan the QR code using the Aliyun Drive app, or log in through the management page.");
            query(QRCODE_CHECK_RETRY_COUNT);
        } catch (Throwable t) {
            log.error("Error on qrCodeGenerate onResponse", t);
        }
    }

    // https://github.com/oracle/graal/issues/4124
    private void processQrCodeUrl(String qrCodeUrl) {

        OkHttpClient client = new OkHttpClient();

        // Create a request object
        Request request = new Request.Builder()
                .url(qrCodeUrl)
                .build();
        try {
            // Execute the request and get the response
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Read the image data
            byte[] imageData = response.body().bytes();

            // Convert byte array to BufferedImage
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));

            // Convert BufferedImage to BitMatrix
            BitMatrix bitMatrix = convertToBitMatrix(bufferedImage);

            // Print BitMatrix
            QrTerminal.print(bitMatrix, true);

            log.info("请使用阿里云盘APP扫码登录, 或进入管理页面登录.");
            log.info("Please scan the QR code using the Aliyun Drive app, or log in through the management page.");
            query(QRCODE_CHECK_RETRY_COUNT);
        } catch (Throwable t) {
            log.error("Error on qrCodeGenerate onResponse", t);
        }
    }

    private static BitMatrix convertToBitMatrix(BufferedImage bufferedImage) throws NotFoundException, WriterException {
        BufferedImageLuminanceSource luminanceSource = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
        String text = new MultiFormatReader().decode(binaryBitmap).getText();
        return new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 33, 33);
    }
    private void query(int countDown) {
        if (countDown <= 0) {
            log.info("二维码已过期, 如需二维码登录, 请重启应用程序.");
            log.info("The QR code has expired. Please restart the application if you need to log in with a QR code.");
            return;
        }
        if (mQrCodeGenerateDataInfo != null) {
            AliyunDriveCall.Callback<AliyunDriveResponse.QrCodeQueryStatusInfo> callback = new AliyunDriveCall.Callback<AliyunDriveResponse.QrCodeQueryStatusInfo>() {
                @Override
                public void onResponse(Call call, okhttp3.Response response, AliyunDriveResponse.QrCodeQueryStatusInfo res) {
                    if (res.isError()) {
                        onErrorCallback(String.format("%s (%s)", res.getMessage(), res.getCode()));
                        return;
                    }
                    switch (res.getStatus()) {
                        case WaitLogin:
                            log.debug("请使用阿里云网盘APP扫码.");
                            log.debug("Please use the Aliyun Drive app to scan the QR code.");
                            break;
                        case ScanSuccess:
                            log.info("扫描成功！请在手机上根据提示确认登录.");
                            log.info("Scan successful! Please confirm login on your mobile device as prompted.");
                            break;
                        case LoginSuccess:
                            log.info("已确认, 认证中...");
                            log.info("Confirmed, authenticating...");
                            if (res.getAuthCode().isEmpty()) {
                                onErrorCallback("authCode is empty");
                                return;
                            }
                            mQrCodeGenerateDataInfo = null;
                            AliyunDriveProperties properties = mAliyunDriveClientServiceHolder.getAliyunDriveClientService().getProperties();
                            String url = String.format(Locale.getDefault(), properties.getAliyunAccessTokenUrl(), res.getAuthCode(), "");
                            mAliyunDriveClientServiceHolder.getAliyunDriveClientService().getAliyunDrive().getAccessToken(url).enqueue(new AliyunDriveCall.Callback<AliyunDriveResponse.AccessTokenInfo>() {
                                @Override
                                public void onResponse(Call call, okhttp3.Response response, AliyunDriveResponse.AccessTokenInfo res) {
                                    if (res.isError()) {
                                        onErrorCallback(String.format("%s (%s)", res.getMessage(), res.getCode()));
                                        return;
                                    }
                                    if (mAliyunDriveClientServiceHolder.getAliyunDriveClientService().login(res.getRefreshToken())) {
                                        log.info("登录成功.");
                                        log.info("Login successful.");
                                    }
                                }

                                @Override
                                public void onFailure(Call call, Throwable t, AliyunDriveResponse.AccessTokenInfo res) {
                                    onErrorCallback(String.format("%s (%s)", res.getMessage(), res.getCode()));
                                }
                            });
                            return;
                        case QrCodeExpired:
                            log.info("二维码已经过期.");
                            log.info("The QR code has expired.");
                            return;
                        default:
                            log.info("未知错误: 可能二维码已经过期.");
                            log.info("Unknown error: The QR code may have expired.");
                            break;
                    }
                    mTaskScheduler.schedule(() ->  query(countDown - 1), Instant.now().plusMillis(QRCODE_CHECK_DELAY_MILLISECONDS));
                }

                @Override
                public void onFailure(Call call, Throwable t, AliyunDriveResponse.QrCodeQueryStatusInfo res) {
                    onErrorCallback(String.format("%s (%s)", res.getMessage(), res.getCode()));
                }
            };
            mAliyunDriveClientServiceHolder.getAliyunDriveClientService().getAliyunDrive().qrCodeQueryStatus(mQrCodeGenerateDataInfo.getSid()).enqueue(callback);
        } else {
            mTaskScheduler.schedule(() ->  query(countDown - 1), Instant.now().plusMillis(QRCODE_CHECK_DELAY_MILLISECONDS));
        }
    }

    private void onErrorCallback(String response) {
        log.error(response);
    }
}
