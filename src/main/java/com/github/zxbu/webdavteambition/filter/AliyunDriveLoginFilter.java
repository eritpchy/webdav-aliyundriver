package com.github.zxbu.webdavteambition.filter;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.fujieid.jap.http.adapter.jakarta.JakartaRequestAdapter;
import com.fujieid.jap.http.adapter.jakarta.JakartaResponseAdapter;
import com.github.zxbu.webdavteambition.filter.impl.AliyunDriveLoginFilterImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AliyunDriveLoginFilter implements Filter {

    private final IFilter IMPL = new AliyunDriveLoginFilterImpl();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
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
        IMPL.doHttpFilter(new JakartaRequestAdapter(request), new JakartaResponseAdapter(response), new IFilterChainCall() {
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
        Filter.super.destroy();
    }
}
