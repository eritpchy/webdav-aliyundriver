package net.xdow.aliyundrive.servlet;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import net.xdow.aliyundrive.util.AliyunDriveClientServiceHolder;
import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.*;

public class FrontendServlet extends DefaultServlet {

    Map<String, String> mInitParameterMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        AliyunDriveProperties properties = AliyunDriveClientServiceHolder.getInstance().getProperties();
        mInitParameterMap.put("resourceBase", properties.getFrontendDir().getAbsolutePath());
        mInitParameterMap.put("dirAllowed", "true");
        mInitParameterMap.put("pathInfoOnly", "true");
        super.init(config);
    }

    @Override
    public String getInitParameter(String name) {
        String val = mInitParameterMap.get(name);
        if (val != null) {
            return val;
        }
        return super.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        Collection<String> names = new ArrayList<>();
        names.addAll(mInitParameterMap.keySet());
        Enumeration<String> origNames = super.getInitParameterNames();
        while (origNames.hasMoreElements()) {
            names.add(origNames.nextElement());
        }
        return Collections.enumeration(names);
    }
}
