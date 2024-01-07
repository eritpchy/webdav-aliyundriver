package com.github.zxbu.webdavteambition.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.util.UUID;

@Slf4j
@ConfigurationProperties("aliyundrive")
public class AliyunDrivePropertiesSpring extends AliyunDriveProperties implements InitializingBean  {

    @Override
    public void afterPropertiesSet() throws Exception {
        String refreshToken = this.getRefreshToken();
        Auth auth = this.getAuth();
        String workDir = ensureWorkDir(getWorkDir());
        AliyunDriveProperties other = load(workDir);
        other.setWorkDir(workDir);
        other.setDriver(getDriver());
        other.setDownloadProxyMode(getDownloadProxyMode());
        other.setShareExpireSec(getShareExpireSec());
        BeanUtils.copyProperties(other, this);
        this.setAuth(auth);
        this.setAuthorization(null);
        if (StringUtils.isEmpty(this.getDeviceId())) {
            this.setDeviceId(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        }
        if (StringUtils.isEmpty(this.getRefreshToken())) {
            this.setRefreshToken(refreshToken);
        } else {
            this.setRefreshTokenNext(refreshToken);
        }
        if (StringUtils.isEmpty(this.getAuth().getUserName())) {
            this.getAuth().setUserName("admin");
        }
        if (StringUtils.isEmpty(this.getAuth().getPassword())) {
            this.getAuth().setPassword("admin");
        }
        if (StringUtils.isEmpty(this.getShareToken())) {
            this.setShareToken(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        }
        save();
    }

    private String ensureWorkDir(String workPath) {
        File workDir = new File(workPath);
        if (!workDir.exists()) {
            try {
                workDir.mkdirs();
            } catch (Exception e) {
                log.error("ensureWorkDir, create dir failed", e);
            }
        }
        if (workDir.exists() && workDir.isDirectory()) {
            return workDir.getAbsolutePath() + File.separator;
        }
        workDir = new File(System.getProperty("user.home"), ".aliyundrive");
        if (!workDir.exists()) {
            try {
                workDir.mkdirs();
            } catch (Exception e) {
                log.error("ensureWorkDir, create dir failed", e);
            }
        }
        if (workDir.exists() && workDir.isDirectory()) {
            String newWorkPath = workDir.getAbsolutePath() + File.separator;
            log.warn("Default workDir({}) is not writeable, change to {} instead",
                    new File(workPath).getAbsoluteFile() + File.separator, newWorkPath);
            return newWorkPath;
        }
        return workPath;
    }
}
