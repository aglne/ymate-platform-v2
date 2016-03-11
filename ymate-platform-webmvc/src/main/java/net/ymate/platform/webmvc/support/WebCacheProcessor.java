/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.webmvc.support;

import net.ymate.platform.cache.CacheElement;
import net.ymate.platform.cache.Caches;
import net.ymate.platform.cache.ICaches;
import net.ymate.platform.webmvc.IRequestContext;
import net.ymate.platform.webmvc.IWebCacheProcessor;
import net.ymate.platform.webmvc.IWebMvc;
import net.ymate.platform.webmvc.annotation.ResponseCache;
import net.ymate.platform.webmvc.context.WebContext;
import net.ymate.platform.webmvc.view.IView;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author 刘镇 (suninformation@163.com) on 16/2/1 上午3:11
 * @version 1.0
 */
public class WebCacheProcessor implements IWebCacheProcessor {

    private static ConcurrentHashMap<String, ReentrantLock> __LOCK_MAP = new ConcurrentHashMap<String, ReentrantLock>();

    public boolean processResponseCache(IWebMvc owner, ResponseCache responseCache, IRequestContext requestContext, IView resultView) throws Exception {
        HttpServletRequest _request = WebContext.getRequest();
        GenericResponseWrapper _response = (GenericResponseWrapper) WebContext.getResponse();
        // 仅缓存处理状态为200响应
        if (_response.getStatus() == HttpServletResponse.SC_OK) {
            String _cacheKey = __doBuildCacheKey(_request, responseCache.key());
            ReentrantLock _locker = __LOCK_MAP.get(_cacheKey);
            if (_locker == null) {
                _locker = new ReentrantLock();
                ReentrantLock _previous = __LOCK_MAP.putIfAbsent(_cacheKey, _locker);
                if (_previous != null) {
                    _locker = _previous;
                }
            }
            _locker.lock();
            try {
                ICaches _caches = Caches.get(owner.getOwner());
                CacheElement _element = (CacheElement) _caches.get(responseCache.cacheName(), _cacheKey);
                // 若缓存内容不存在或已过期
                if (_element == null || _element.isExpired()) {
                    // 重新生成缓存内容
                    OutputStream _output = new ByteArrayOutputStream();
                    resultView.render(_output);
                    _element = new CacheElement(_output.toString());
                    // 计算超时时间
                    int _timeout = responseCache.timeout() > 0 ? responseCache.timeout() : _caches.getModuleCfg().getDefaultCacheTimeout();
                    if (_timeout > 0) {
                        _element.setTimeout(_timeout);
                    }
                    // 存入缓存
                    _caches.put(responseCache.cacheName(), _cacheKey, _element);
                }
                // 输出内容
                _response.getWriter().write((String) _element.getObject());
                //
                return true;
            } finally {
                _locker.unlock();
            }
        }
        return false;
    }

    protected String __doBuildCacheKey(HttpServletRequest request, String keyPrefix) {
        // 计算缓存KEY值
        StringBuilder _keyBuilder = new StringBuilder()
                .append(ResponseCache.class.getName());
        if (StringUtils.isNotBlank(keyPrefix)) {
            _keyBuilder.append(":").append(keyPrefix);
        } else {
            _keyBuilder
                    .append(":").append(request.getMethod())
                    .append(":").append(request.getRequestURI())
                    .append(":").append(request.getQueryString());
        }
        return DigestUtils.md5Hex(_keyBuilder.toString());
    }
}
