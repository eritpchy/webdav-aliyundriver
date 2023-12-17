package net.xdow.aliyundrive.filter;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.fujieid.jap.http.adapter.javax.JavaxRequestAdapter;
import com.fujieid.jap.http.adapter.javax.JavaxResponseAdapter;
import com.github.zxbu.webdavteambition.filter.IFilter;
import com.github.zxbu.webdavteambition.filter.IFilterChainCall;
import com.github.zxbu.webdavteambition.filter.impl.AliyunDriveLoginFilterImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AliyunDriveLoginFilter implements Filter {

    private final IFilter IMPL = new AliyunDriveLoginFilterImpl();
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        IMPL.doHttpFilter(new JavaxRequestAdapter(request), new JavaxResponseAdapter(response), new IFilterChainCall() {
            @Override
            public void doFilter(JapHttpRequest _request, JapHttpResponse _response) throws IOException {
                try {
                    chain.doFilter(request, response);
                } catch (ServletException e) {
                    throw new IOException(e);
                }
            }
        });
    }

    @Override
    public void destroy() {

    }
}
