package net.xdow.aliyundrive.servlet;

import com.github.zxbu.webdavteambition.servlet.impl.StartupServletImpl;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.event.OnFrontendUpdatedEvent;
import net.xdow.aliyundrive.util.AliyunDriveClientServiceHolder;
import org.greenrobot.eventbus.EventBus;

import javax.servlet.*;
import java.io.IOException;

public class StartupServlet extends GenericServlet {

    private final StartupServletImpl IMPL = new StartupServletImpl();

    @Override
    public void init() throws ServletException {
        super.init();
        AliyunDriveClientService service = AliyunDriveClientServiceHolder.getInstance();
        try {
            Runnable mOnFrontendUpdatedRunnable = () -> EventBus.getDefault().post(new OnFrontendUpdatedEvent());
            IMPL.init(service, mOnFrontendUpdatedRunnable);
        } finally {
            ServletContext servletContext = getServletContext();
            servletContext.setAttribute(StartupServletImpl.class.getName(), IMPL);
            servletContext.setAttribute(AliyunDriveClientService.class.getName(), service);
        }
    }

    @Override
    public void destroy() {
        try {
            IMPL.destroy();
        } finally {
            ServletContext servletContext = getServletContext();
            servletContext.setAttribute(StartupServletImpl.class.getName(), null);
            servletContext.setAttribute(AliyunDriveClientService.class.getName(), null);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

    }
}
