package com.github.zxbu.webdavteambition.filter;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;

import java.io.IOException;

public interface IFilter {
    void doHttpFilter(JapHttpRequest request, JapHttpResponse response, IFilterChainCall chain) throws IOException;
}
