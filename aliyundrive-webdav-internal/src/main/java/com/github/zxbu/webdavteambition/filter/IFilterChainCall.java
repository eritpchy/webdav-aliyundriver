package com.github.zxbu.webdavteambition.filter;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;

import java.io.IOException;

public interface IFilterChainCall {
    void doFilter (JapHttpRequest request, JapHttpResponse response ) throws IOException;
}
