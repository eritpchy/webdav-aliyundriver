package com.github.zxbu.webdavteambition.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties("aliyundrive")
public class AliyunDrivePropertiesSpring extends AliyunDriveProperties implements InitializingBean  {

    @Override
    public void afterPropertiesSet() throws Exception {
        String refreshToken = this.getRefreshToken();
        Auth auth = this.getAuth();
        AliyunDriveProperties other = load(getWorkDir());
        other.setWorkDir(getWorkDir());
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
}
