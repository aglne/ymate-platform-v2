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
package net.ymate.platform.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * 插件类加载器
 *
 * @author 刘镇 (suninformation@163.com) on 2010-1-10 下午03:01:53
 * @version 1.0
 */
public class PluginClassLoader extends URLClassLoader {

    private String __pluginHome;

    public PluginClassLoader(String pluginHome, URL[] urls) {
        super(urls);
        this.__pluginHome = pluginHome;
    }

    public PluginClassLoader(String pluginHome, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.__pluginHome = pluginHome;
    }

    public PluginClassLoader(String pluginHome, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.__pluginHome = pluginHome;
    }

    public String getPluginHome() {
        return __pluginHome;
    }
}
