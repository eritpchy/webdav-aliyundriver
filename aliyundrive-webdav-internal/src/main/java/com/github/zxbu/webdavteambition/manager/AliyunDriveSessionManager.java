package com.github.zxbu.webdavteambition.manager;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.inf.IBackgroundTask;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliyunDriveSessionManager implements IBackgroundTask {

    public static final int SIGN_EXPIRED_TIME_SEC = 60 * 5; //5min

    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveSessionManager.class);

    private final AliyunDriveWebApiImplV1 mAliyunDrive;
    private final AliyunDriveProperties mAliyunDriveProperties;
    private ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);

    public AliyunDriveSessionManager(AliyunDriveWebApiImplV1 aliyunDrive, AliyunDriveProperties aliYunDriveProperties) {
        this.mAliyunDrive = aliyunDrive;
        this.mAliyunDriveProperties = aliYunDriveProperties;
        makeKeyPairIfNeeded();
    }

    public void makeKeyPairIfNeeded() {
        AliyunDriveProperties.Session session = this.mAliyunDriveProperties.getSession();
        if (!session.isEmpty()) {
            return;
        }
        makeKeyPair();
    }

    public void makeKeyPair() {
        AliyunDriveProperties.Session session = this.mAliyunDriveProperties.getSession();
        BigInteger privateKeyInt = new BigInteger(256, new SecureRandom());
        BigInteger publicKeyInt = Sign.publicKeyFromPrivate(privateKeyInt);
        session.setPrivateKey(privateKeyInt.toString(16));
        session.setPublicKey(publicKeyInt.toString(16));
        makeSignature(0);
        this.mAliyunDriveProperties.save();
    }

    private void makeSignature(int nonce) {
        AliyunDriveProperties aliYunDriveProperties = this.mAliyunDriveProperties;
        AliyunDriveProperties.Session session = aliYunDriveProperties.getSession();
        byte[] dataBytes = (aliYunDriveProperties.getAppId() + ":" + aliYunDriveProperties.getDeviceId() + ":"
                + aliYunDriveProperties.getUserId() + ":" + nonce).getBytes(StandardCharsets.UTF_8);
        byte[] dataHash = Hash.sha256(dataBytes);
        ECKeyPair keyPair = new ECKeyPair(new BigInteger(session.getPrivateKey(), 16),
                new BigInteger(session.getPublicKey(), 16));
        Sign.SignatureData signatureInfo = Sign.signMessage(dataHash, keyPair, false);
        session.setSignature(Hex.toHexString(signatureInfo.getR()) + Hex.toHexString(signatureInfo.getS()));
        session.setNonce(nonce);
        session.setExpireTimeSec(System.currentTimeMillis() / 1000 + SIGN_EXPIRED_TIME_SEC);
    }

    public void updateSession() {
        AliyunDriveProperties.Session session = this.mAliyunDriveProperties.getSession();
        String OS_NAME = System.getProperty("os.name");
        Map<String, String> args = new HashMap<>();
        args.put("deviceName", "Webdav");
        if (OS_NAME.contains("Windows")) {
            args.put("modelName", OS_NAME);
        } else {
            args.put("modelName", OS_NAME + " " + System.getProperty("os.version"));
        }
        args.put("pubKey", "04" + session.getPublicKey());

        String createSessionResult = "";
        if (session.getNonce() >= 1073741823) {
            session.setNonce(0);
        }
        makeSignature(session.getNonce());
        if (session.getNonce() == 0) {
            createSessionResult = this.mAliyunDrive.post("https://api.aliyundrive.com/users/v1/users/device/create_session", args);
        } else {
            createSessionResult = this.mAliyunDrive.post("https://api.aliyundrive.com/users/v1/users/device/renew_session", args);
        }

        if (createSessionResult != null) {
            if (createSessionResult.contains("DeviceSessionSignatureInvalid")
                    || createSessionResult.contains("UserDeviceOffline")) {
                makeKeyPair();
                createSessionResult = this.mAliyunDrive.post("https://api.aliyundrive.com/users/v1/users/device/create_session", args);
            }
        }
        if (createSessionResult.contains("\"result\":false")) {
            LOGGER.error("登录设备过多, 请进入\"登录设备管理\", 退出一些设备, 如设备被意外退出登录, 请手动删除配置文件后重启程序。");
            session.setNonce(0);
        }
        if (createSessionResult.contains("\"result\":true")) {
            session.setNonce(session.getNonce() + 1);
        }
        this.mAliyunDriveProperties.save();
    }

    public void start() {
        updateSession();
        mTaskPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                AliyunDriveProperties.Session session = AliyunDriveSessionManager.this.mAliyunDriveProperties.getSession();
                if (!session.isExpired()) {
                    return;
                }
                AliyunDriveSessionManager.this.updateSession();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        mTaskPool.shutdownNow();
    }
}
