package com.github.zxbu.webdavteambition;

import com.github.zxbu.webdavteambition.bean.*;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.filter.AliyunDriveLoginFilter;
import com.github.zxbu.webdavteambition.filter.ErrorFilter;
import com.github.zxbu.webdavteambition.servlet.StartupServlet;
import com.github.zxbu.webdavteambition.store.AliyunDriveFileSystemStore;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import net.sf.webdav.StoredObject;
import net.xdow.aliyundrive.bean.*;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.webapi.bean.AliyunDriveWebRequest;
import net.xdow.aliyundrive.webapi.bean.AliyunDriveWebResponse;
import net.xdow.aliyundrive.webapi.bean.AliyunDriveWebShareRequestInfo;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import net.xdow.webdav.WebdavServlet;
import org.apache.catalina.filters.CorsFilter;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@EnableScheduling
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
@RegisterReflectionForBinding({
        PathInfo.class,
        AliyunDriveProperties.class,
        AliyunDriveProperties.Driver.class,
        AliyunDriveProperties.Auth.class,
        AliyunDriveProperties.Session.class,
        AliyunDriveEnum.GrantType.class,
        AliyunDriveEnum.OrderDirection.class,
        AliyunDriveEnum.Category.class,
        AliyunDriveEnum.Type.class,
        AliyunDriveEnum.CheckNameMode.class,
        AliyunDriveEnum.OrderBy.class,
        AliyunDriveFileInfo.class,
        AliyunDriveFilePartInfo.class,
        AliyunDriveRequest.AccessTokenInfo.class,
        AliyunDriveRequest.FileListInfo.class,
        AliyunDriveRequest.FileGetInfo.class,
        AliyunDriveRequest.FileBatchGetInfo.class,
        AliyunDriveRequest.FileBatchGetInfo.FileInfo.class,
        AliyunDriveRequest.FileGetDownloadUrlInfo.class,
        AliyunDriveRequest.FileCreateInfo.class,
        AliyunDriveRequest.FileCreateInfo.StreamInfo.class,
        AliyunDriveRequest.FileGetUploadUrlInfo.class,
        AliyunDriveRequest.FileListUploadPartsInfo.class,
        AliyunDriveRequest.FileUploadCompleteInfo.class,
        AliyunDriveRequest.FileRenameInfo.class,
        AliyunDriveRequest.FileMoveInfo.class,
        AliyunDriveRequest.FileCopyInfo.class,
        AliyunDriveRequest.FileMoveToTrashInfo.class,
        AliyunDriveRequest.FileDeleteInfo.class,
        AliyunDriveResponse.AccessTokenInfo.class,
        AliyunDriveResponse.UserSpaceInfo.class,
        AliyunDriveResponse.UserSpaceInfo.Data.class,
        AliyunDriveResponse.UserDriveInfo.class,
        AliyunDriveResponse.FileListInfo.class,
        AliyunDriveResponse.FileGetInfo.class,
        AliyunDriveResponse.FileBatchGetInfo.class,
        AliyunDriveResponse.FileGetDownloadUrlInfo.class,
        AliyunDriveResponse.FileCreateInfo.class,
        AliyunDriveResponse.FileGetUploadUrlInfo.class,
        AliyunDriveResponse.FileListUploadPartsInfo.class,
        AliyunDriveResponse.FileUploadCompleteInfo.class,
        AliyunDriveResponse.FileRenameInfo.class,
        AliyunDriveResponse.FileMoveInfo.class,
        AliyunDriveResponse.FileCopyInfo.class,
        AliyunDriveResponse.FileMoveToTrashInfo.class,
        AliyunDriveResponse.FileDeleteInfo.class,
        AliyunDriveResponse.GenericMessageInfo.class,
        AliyunDriveResponse.QrCodeGenerateInfo.class,
        AliyunDriveResponse.QrCodeQueryStatusInfo.class,
        AliyunDriveUploadedPartInfo.class,
        AliyunDriveMediaMetaData.class,
        AliyunDriveWebResponse.ShareTokenInfo.class,
        AliyunDriveWebResponse.ShareSaveInfo.class,
        AliyunDriveWebResponse.UserSpaceInfo.class,
        AliyunDriveWebResponse.ShareSaveInfo.class,
        AliyunDriveWebShareRequestInfo.class,
        AliyunDriveWebRequest.ShareTokenInfo.class,
        AliyunDriveWebRequest.ShareListInfo.class,
        AliyunDriveWebRequest.ShareGetFileInfo.class,
        AliyunDriveWebRequest.ShareSaveInfo.class,
        AliyunDriveOpenApiImplV1.class,
        AliyunDriveWebApiImplV1.class,
        AliyunDriveFileSystemStore.class,
        StoredObject.class,
        CorsFilter.class,
        FrontendVersionInfo.class,
        AFileReqInfo.class,
        AliyunDriveBackupRootFileInfo.class,
        AliyunDriveLiveTranscodingSubtitleTaskInfo.class,
        AliyunDriveLiveTranscodingTaskInfo.class,
        AliyunDriveResourceRootFileInfo.class,
        AliyunDriveVideoPreviewPlayInfo.class,
        AliyunDriveVirtualRootFileInfo.class,
        FrontendVersionInfo.class,
        GithubAssetsInfo.class,
        GithubLatestInfo.class,
})
public class WebdavApplication {
    public static void main(String[] args) {
         SpringApplication.run(WebdavApplication.class, args);
    }

    @Autowired
    private AliyunDriveClientServiceHolder mAliyunDriveClientServiceHolder;

    @Bean
    public ServletRegistrationBean<WebdavServlet> myServlet(){
        WebdavServlet webdavServlet = new WebdavServlet(this.mAliyunDriveClientServiceHolder.getAliyunDriveClientService());
        ServletRegistrationBean<WebdavServlet> servletRegistrationBean = new ServletRegistrationBean<>(webdavServlet, "/dav/*");
        Map<String, String> inits = new LinkedHashMap<>();
        inits.put("ResourceHandlerImplementation", AliyunDriveFileSystemStore.class.getName());
        // inits.put("ResourceHandlerImplementation", LocalFileSystemStore.class.getName());
        inits.put("rootpath", "./");
        inits.put("storeDebug", "1");
        servletRegistrationBean.setInitParameters(inits);
        return servletRegistrationBean;
    }

    @Bean
    public ServletRegistrationBean<StartupServlet> startupServlet(){
        StartupServlet startupServlet = new StartupServlet(this.mAliyunDriveClientServiceHolder.getAliyunDriveClientService());
        ServletRegistrationBean<StartupServlet> servletRegistrationBean = new ServletRegistrationBean<>(startupServlet, false);

        servletRegistrationBean.setLoadOnStartup(1);
        return servletRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean disableSpringBootErrorFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setUrlPatterns(Collections.singleton("/dav/*"));
        filterRegistrationBean.setFilter(new ErrorFilter());
        filterRegistrationBean.setEnabled(true);
        return filterRegistrationBean;
    }
    @Bean
    public FilterRegistrationBean aliyunDriveLoginFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new AliyunDriveLoginFilter());
        filterRegistrationBean.setEnabled(true);
        return filterRegistrationBean;
    }
}
