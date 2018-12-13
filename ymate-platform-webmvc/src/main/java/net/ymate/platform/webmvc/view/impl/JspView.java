/*
 * Copyright 2007-2017 the original author or authors.
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
package net.ymate.platform.webmvc.view.impl;

import net.ymate.platform.webmvc.IWebMvc;
import net.ymate.platform.webmvc.context.WebContext;
import net.ymate.platform.webmvc.util.WebUtils;
import net.ymate.platform.webmvc.view.AbstractView;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.util.Map;

/**
 * JSP视图
 *
 * @author 刘镇 (suninformation@163.com) on 2011-7-24 下午06:49:28
 * @version 1.0
 */
public class JspView extends AbstractView {

    protected String __path;

    public static JspView bind() {
        return new JspView();
    }

    public static JspView bind(String path) {
        return new JspView(path);
    }

    public static JspView bind(IWebMvc owner) {
        return new JspView(owner);
    }

    public static JspView bind(IWebMvc owner, String path) {
        return new JspView(owner, path);
    }

    public JspView(IWebMvc owner) {
        __doViewInit(owner);
    }

    /**
     * 构造器
     *
     * @param owner 所属MVC框架管理器
     * @param path  JSP文件路径
     */
    public JspView(IWebMvc owner, String path) {
        __doViewInit(owner);
        __path = path;
    }

    public JspView() {
        __doViewInit(WebContext.getContext().getOwner());
    }

    public JspView(String path) {
        this(WebContext.getContext().getOwner(), path);
    }

    protected void __doProcessPath() {
        if (StringUtils.isNotBlank(__contentType)) {
            WebContext.getResponse().setContentType(__contentType);
        }
        for (Map.Entry<String, Object> _entry : __attributes.entrySet()) {
            WebContext.getRequest().setAttribute(_entry.getKey(), _entry.getValue());
        }
        if (StringUtils.isBlank(__path)) {
            String _mapping = WebContext.getRequestContext().getRequestMapping();
            if (_mapping.charAt(0) == '/') {
                _mapping = _mapping.substring(1);
            }
            if (_mapping.endsWith("/")) {
                _mapping = _mapping.substring(0, _mapping.length() - 1);
            }
            __path = __baseViewPath + _mapping + ".jsp";
        } else {
            if (!__path.startsWith("/")) {
                __path = __baseViewPath + __path;
            }
            if (!__path.contains("?") && !__path.endsWith(".jsp")) {
                __path += ".jsp";
            }
        }
    }

    @Override
    protected void __doRenderView() throws Exception {
        __doProcessPath();
        HttpServletRequest _request = WebContext.getRequest();
        _request.getRequestDispatcher(__path).forward(_request, WebContext.getResponse());
    }

    @Override
    public void render(final OutputStream output) throws Exception {
        __doProcessPath();
        //
        WebUtils.includeJSP(WebContext.getRequest(), WebContext.getResponse(), __path, WebContext.getResponse().getCharacterEncoding(), output);
    }
}
