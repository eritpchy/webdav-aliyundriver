package com.github.zxbu.webdavteambition.servlet;

import com.github.zxbu.webdavteambition.servlet.impl.StartupServletImpl;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

public class StartupServlet extends GenericServlet {

    private final StartupServletImpl IMPL = new StartupServletImpl();
    private final AliyunDriveClientService mAliyunDriveClientService;

    public StartupServlet(AliyunDriveClientService aliyunDriveClientService) {
        mAliyunDriveClientService = aliyunDriveClientService;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Runnable mOnFrontendUpdatedRunnable = () -> {};
            IMPL.init(this.mAliyunDriveClientService, mOnFrontendUpdatedRunnable);
        } finally {
            getServletContext().setAttribute(StartupServletImpl.class.getName(), IMPL);
            getServletContext().setAttribute(AliyunDriveClientService.class.getName(), mAliyunDriveClientService);
        }

    }

    @Override
    public void destroy() {
        try {
            IMPL.destroy();
        } finally {
            getServletContext().setAttribute(StartupServletImpl.class.getName(), null);
            getServletContext().setAttribute(AliyunDriveClientService.class.getName(), null);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

    }
}
