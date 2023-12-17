package net.xdow.aliyundrive.filter;

import com.fujieid.jap.http.adapter.javax.JavaxRequestAdapter;
import com.fujieid.jap.http.adapter.javax.JavaxResponseAdapter;
import com.github.zxbu.webdavteambition.filter.IErrorFilter;
import com.github.zxbu.webdavteambition.filter.IErrorFilterCall;
import com.github.zxbu.webdavteambition.filter.impl.ErrorFilterImpl;
import net.xdow.aliyundrive.filter.impl.ErrorWrapperResponseJavaxImpl;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ErrorFilter implements Filter, IErrorFilterCall {


    private final IErrorFilter mErrorFilter = new ErrorFilterImpl(this);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse && request instanceof HttpServletRequest) {
        } else {
            return;
        }
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        JavaxRequestAdapter req = new JavaxRequestAdapter(httpServletRequest);
        JavaxResponseAdapter res = new JavaxResponseAdapter(httpServletResponse);
        ErrorWrapperResponseJavaxImpl wrapperResponse = new ErrorWrapperResponseJavaxImpl(httpServletResponse);
        filterChain.doFilter(httpServletRequest, wrapperResponse);
        this.mErrorFilter.doFilterCall(req, res, wrapperResponse);
    }

    @Override
    public void destroy() {

    }

    public String readErrorPage() {
        try {
            InputStream inputStream = WebAppContext.getCurrentContext().getResourceAsStream("/WEB-INF/lib/error.xml");
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error in reading error.xml");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

}
