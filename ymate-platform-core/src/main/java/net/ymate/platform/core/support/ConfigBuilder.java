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
package net.ymate.platform.core.support;

import net.ymate.platform.core.IConfig;
import net.ymate.platform.core.beans.IBeanLoader;
import net.ymate.platform.core.beans.intercept.InterceptSettings;
import net.ymate.platform.core.beans.proxy.IProxyFactory;
import net.ymate.platform.core.beans.proxy.impl.DefaultProxyFactory;
import net.ymate.platform.core.event.Events;
import net.ymate.platform.core.event.IEventConfig;
import net.ymate.platform.core.event.IEventProvider;
import net.ymate.platform.core.event.impl.DefaultEventConfig;
import net.ymate.platform.core.i18n.II18NEventHandler;
import net.ymate.platform.core.lang.BlurObject;
import net.ymate.platform.core.support.impl.DefaultPasswordProcessor;
import net.ymate.platform.core.util.ClassUtils;
import net.ymate.platform.core.util.RuntimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author 刘镇 (suninformation@163.com) on 15/12/18 下午2:19
 * @version 1.0
 */
public final class ConfigBuilder {

    private boolean __isDevelopMode;

    private IConfig.Environment __runEnv;

    private final List<String> __packageNames;

    private final List<String> __excludedPackages;

    private final List<String> __excludedFiles;

    private final List<String> __excludedModules;

    private Locale __locale;

    private IBeanLoader __beanLoader;

    private IProxyFactory __proxyFactory;

    private II18NEventHandler __i18nEventHandler;

    private Class<? extends IPasswordProcessor> __defaultPasswordClass;

    private final Map<String, String> __paramsMap;

    private final Map<String, String> __eventConfigs;

    private final IModuleCfgProcessor __processor;

    private boolean __interceptSettingsEnabled;

    private final InterceptSettings __interceptSettings;

    private static List<String> __doParserArrayStr(Properties properties, String key) {
        String[] _strArr = StringUtils.split(properties.getProperty(key), "|");
        if (_strArr != null) {
            return new ArrayList<String>(Arrays.asList(_strArr));
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static ConfigBuilder create(final Properties properties) {
        //
        IModuleCfgProcessor _processor = ModuleCfgProcessBuilder.create(properties).build();
        //
        Class<? extends IPasswordProcessor> _passProcessor;
        try {
            String _passClassName = properties.getProperty("ymp.default_password_class");
            if (StringUtils.isNotBlank(_passClassName)) {
                _passProcessor = (Class<? extends IPasswordProcessor>) ClassUtils.loadClass(_passClassName, ConfigBuilder.class);
            } else {
                _passProcessor = DefaultPasswordProcessor.class;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), RuntimeUtils.unwrapThrow(e));
        }
        //
        ConfigBuilder _builder = ConfigBuilder.create(_processor)
                .developMode(BlurObject.bind(properties.getProperty("ymp.dev_mode")).toBooleanValue())
                .packageNames(__doParserArrayStr(properties, "ymp.autoscan_packages"))
                .excludedPackages(__doParserArrayStr(properties, "ymp.excluded_packages"))
                .excludedFiles(__doParserArrayStr(properties, "ymp.excluded_files"))
                .excludedModules(__doParserArrayStr(properties, "ymp.excluded_modules"))
                .locale(StringUtils.trimToNull(properties.getProperty("ymp.i18n_default_locale")))
                .beanLoader(ClassUtils.impl(properties.getProperty("ymp.bean_loader_class"), IBeanLoader.class, ConfigBuilder.class))
                .proxyFactory(ClassUtils.impl(properties.getProperty("ymp.proxy_factory_class"), IProxyFactory.class, ConfigBuilder.class))
                .i18nEventHandler(ClassUtils.impl(properties.getProperty("ymp.i18n_event_handler_class"), II18NEventHandler.class, ConfigBuilder.class))
                .defaultPasswordProcessor(_passProcessor);
        //
        try {
            IConfig.Environment _runEnv = IConfig.Environment.valueOf(StringUtils.defaultIfBlank(properties.getProperty(IConfig.SYSTEM_RUN_ENV), IConfig.Environment.UNKNOWN.name()).toUpperCase());
            _builder.runEnv(_runEnv);
        } catch (IllegalArgumentException e) {
            _builder.runEnv(IConfig.Environment.UNKNOWN);
        }
        // 提取模块配置
        String _prefix = "ymp.params.";
        for (Object _key : properties.keySet()) {
            if (StringUtils.startsWith((String) _key, _prefix)) {
                String _cfgKey = StringUtils.substring((String) _key, _prefix.length());
                String _cfgValue = properties.getProperty((String) _key);
                _builder.param(_cfgKey, _cfgValue);
            }
        }
        //
        _prefix = "ymp.event.";
        for (Object _key : properties.keySet()) {
            if (StringUtils.startsWith((String) _key, _prefix)) {
                String _cfgKey = StringUtils.substring((String) _key, _prefix.length());
                String _cfgValue = properties.getProperty((String) _key);
                _builder.__eventConfigs.put(_cfgKey, _cfgValue);
            }
        }
        //
        _builder.__interceptSettingsEnabled = new BlurObject(properties.getProperty("ymp.intercept_settings_enabled")).toBooleanValue();
        //
        if (_builder.__interceptSettingsEnabled) {
            _prefix = "ymp.intercept.globals.";
            for (Object _key : properties.keySet()) {
                if (StringUtils.startsWith((String) _key, _prefix)) {
                    String _cfgKey = StringUtils.substring((String) _key, _prefix.length());
                    String _cfgValue = properties.getProperty((String) _key);
                    if (StringUtils.equalsIgnoreCase(_cfgValue, "disabled")) {
                        _builder.__interceptSettings.registerInterceptGlobal(_cfgKey);
                    }
                }
            }
            //
            _prefix = "ymp.intercept.settings.";
            for (Object _key : properties.keySet()) {
                if (StringUtils.startsWith((String) _key, _prefix)) {
                    String _cfgKey = StringUtils.substring((String) _key, _prefix.length());
                    String _cfgValue = properties.getProperty((String) _key);
                    if (StringUtils.isNotBlank(_cfgValue)) {
                        _builder.__interceptSettings.registerInterceptSetting(_cfgKey, _cfgValue);
                    }
                }
            }
            //
            _prefix = "ymp.intercept.packages.";
            for (Object _key : properties.keySet()) {
                if (StringUtils.startsWith((String) _key, _prefix)) {
                    String _cfgKey = StringUtils.substring((String) _key, _prefix.length());
                    String _cfgValue = properties.getProperty((String) _key);
                    if (StringUtils.isNotBlank(_cfgValue)) {
                        _builder.__interceptSettings.registerInterceptPackage(_cfgKey, _cfgValue);
                    }
                }
            }
        }
        //
        return _builder;
    }

    private static InputStream __doLoadResourceStream(String prefix) {
        prefix = "ymp-conf" + StringUtils.trimToEmpty(prefix);
        ClassLoader _classLoader = ConfigBuilder.class.getClassLoader();
        InputStream _in = _classLoader.getResourceAsStream(prefix + ".properties");
        if (_in == null) {
            if (RuntimeUtils.isWindows()) {
                _in = _classLoader.getResourceAsStream(prefix + "_WIN.properties");
            } else if (RuntimeUtils.isUnixOrLinux()) {
                _in = _classLoader.getResourceAsStream(prefix + "_UNIX.properties");
            }
        }
        return _in;
    }

    public static ConfigBuilder system() {
        final Properties __props = new Properties();
        boolean _devFlag = false;
        boolean _testFlag = false;
        InputStream _in = null;
        try {
            String _env = System.getProperty("ymp.run_env");
            if (StringUtils.isNotBlank(_env)) {
                if (StringUtils.equalsIgnoreCase(_env, "dev")) {
                    _in = __doLoadResourceStream("_DEV");
                    _devFlag = _in != null;
                } else if (StringUtils.equalsIgnoreCase(_env, "test")) {
                    _in = __doLoadResourceStream("_TEST");
                    _testFlag = _in != null;
                }
            }
            if (_in == null) {
                _in = __doLoadResourceStream("_DEV");
                _devFlag = _in != null;
                if (_in == null) {
                    _in = __doLoadResourceStream(null);
                }
            }
            if (_in != null) {
                __props.load(_in);
                if (_devFlag) {
                    __props.setProperty("ymp.dev_mode", "true");
                    __props.setProperty("ymp.run_env", "dev");
                } else if (_testFlag) {
                    __props.setProperty("ymp.run_env", "test");
                } else {
                    __props.setProperty("ymp.run_env", "product");
                }
            }
            return create(__props);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }

    public static ConfigBuilder create() {
        return new ConfigBuilder(IModuleCfgProcessor.EMPTY);
    }

    public static ConfigBuilder create(IModuleCfgProcessor processor) {
        return new ConfigBuilder(processor);
    }

    private ConfigBuilder(IModuleCfgProcessor processor) {
        __packageNames = new ArrayList<String>();
        __excludedPackages = new ArrayList<String>();
        __excludedFiles = new ArrayList<String>();
        __excludedModules = new ArrayList<String>();
        __paramsMap = new HashMap<String, String>();
        __eventConfigs = new HashMap<String, String>();
        //
        __processor = processor;
        //
        __interceptSettings = new InterceptSettings();
    }

    public ConfigBuilder developMode(boolean isDevelopMode) {
        __isDevelopMode = isDevelopMode;
        return this;
    }

    public ConfigBuilder runEnv(IConfig.Environment runMode) {
        __runEnv = runMode;
        return this;
    }

    public ConfigBuilder packageNames(Collection<String> packageNames) {
        __packageNames.addAll(packageNames);
        return this;
    }

    public ConfigBuilder packageName(String packageName) {
        __packageNames.add(packageName);
        return this;
    }

    public ConfigBuilder excludedPackages(Collection<String> excludedPackages) {
        __excludedPackages.addAll(excludedPackages);
        return this;
    }

    public ConfigBuilder excludedPackages(String excludedPackage) {
        __excludedPackages.add(excludedPackage);
        return this;
    }

    public ConfigBuilder excludedFiles(Collection<String> excludedFiles) {
        __excludedFiles.addAll(excludedFiles);
        return this;
    }

    public ConfigBuilder excludedFiles(String excludedFile) {
        __excludedFiles.add(excludedFile);
        return this;
    }

    public ConfigBuilder excludedModules(Collection<String> excludeModules) {
        __excludedModules.addAll(excludeModules);
        return this;
    }

    public ConfigBuilder excludedModule(String excludeModule) {
        __excludedModules.add(excludeModule);
        return this;
    }

    public ConfigBuilder locale(Locale locale) {
        __locale = locale;
        return this;
    }

    public ConfigBuilder locale(String locale) {
        __locale = LocaleUtils.toLocale(locale);
        return this;
    }

    public ConfigBuilder beanLoader(IBeanLoader beanLoader) {
        __beanLoader = beanLoader;
        return this;
    }

    public ConfigBuilder proxyFactory(IProxyFactory proxyFactory) {
        __proxyFactory = proxyFactory;
        return this;
    }

    public ConfigBuilder i18nEventHandler(II18NEventHandler i18NEventHandler) {
        __i18nEventHandler = i18NEventHandler;
        return this;
    }

    public ConfigBuilder defaultPasswordProcessor(Class<? extends IPasswordProcessor> passwordClass) {
        __defaultPasswordClass = passwordClass;
        return this;
    }

    public ConfigBuilder params(Map<String, String> params) {
        __paramsMap.putAll(params);
        return this;
    }

    public ConfigBuilder param(String paramName, String paramValue) {
        __paramsMap.put(paramName, paramValue);
        return this;
    }

    public ConfigBuilder eventMode(boolean async) {
        __eventConfigs.put(IEventConfig.DEFAULT_MODE, async ? Events.MODE.ASYNC.name() : Events.MODE.NORMAL.name());
        return this;
    }

    public ConfigBuilder eventProviderClass(Class<? extends IEventProvider> providerClass) {
        __eventConfigs.put(IEventConfig.PROVIDER_CLASS, providerClass.getName());
        return this;
    }

    public ConfigBuilder eventThreadPoolSize(int threadPoolSize) {
        __eventConfigs.put(IEventConfig.THREAD_POOL_SIZE, String.valueOf(threadPoolSize));
        return this;
    }

    public ConfigBuilder eventThreadMaxPoolSize(int threadMaxPoolSize) {
        __eventConfigs.put(IEventConfig.THREAD_MAX_POOL_SIZE, String.valueOf(threadMaxPoolSize));
        return this;
    }

    public ConfigBuilder eventThreadWorkQueueSize(int threadWorkQueueSize) {
        __eventConfigs.put(IEventConfig.THREAD_QUEUE_SIZE, String.valueOf(threadWorkQueueSize));
        return this;
    }

    public IConfig build() {
        final IEventConfig __eventCfg = new DefaultEventConfig(__eventConfigs);
        if (__proxyFactory == null) {
            __proxyFactory = new DefaultProxyFactory();
        }
        return new IConfig() {

            @Override
            public boolean isDevelopMode() {
                return __isDevelopMode;
            }

            @Override
            public boolean isTestEnv() {
                return Environment.TEST.equals(__runEnv);
            }

            @Override
            public boolean isDevEnv() {
                return Environment.DEV.equals(__runEnv);
            }

            @Override
            public boolean isProductEnv() {
                return Environment.PRODUCT.equals(__runEnv);
            }

            @Override
            public Environment getRunEnv() {
                return __runEnv;
            }

            @Override
            public List<String> getAutoscanPackages() {
                return Collections.unmodifiableList(__packageNames);
            }

            @Override
            public List<String> getExcludedPackages() {
                return Collections.unmodifiableList(__excludedPackages);
            }

            @Override
            public List<String> getExcludedFiles() {
                return Collections.unmodifiableList(__excludedFiles);
            }

            @Override
            public List<String> getExcludedModules() {
                return Collections.unmodifiableList(__excludedModules);
            }

            @Override
            public Locale getDefaultLocale() {
                return __locale != null ? __locale : Locale.getDefault();
            }

            @Override
            public IBeanLoader getBeanLoader() {
                return __beanLoader;
            }

            @Override
            public IProxyFactory getProxyFactory() {
                return __proxyFactory;
            }

            @Override
            public II18NEventHandler getI18NEventHandler() {
                return __i18nEventHandler;
            }

            @Override
            public Class<? extends IPasswordProcessor> getDefaultPasswordClass() {
                return __defaultPasswordClass;
            }

            @Override
            public Map<String, String> getParams() {
                return Collections.unmodifiableMap(__paramsMap);
            }

            @Override
            public String getParam(String name) {
                if (StringUtils.isBlank(name)) {
                    return null;
                }
                return __paramsMap.get(name);
            }

            @Override
            public String getParam(String name, String defaultValue) {
                if (StringUtils.isBlank(name)) {
                    return defaultValue;
                }
                return StringUtils.defaultIfBlank(__paramsMap.get(name), defaultValue);
            }

            @Override
            public Map<String, String> getModuleConfigs(String moduleName) {
                return Collections.unmodifiableMap(__processor.getModuleCfg(moduleName));
            }

            @Override
            public IEventConfig getEventConfigs() {
                return __eventCfg;
            }

            @Override
            public boolean isInterceptSettingsEnabled() {
                return __interceptSettingsEnabled;
            }

            @Override
            public InterceptSettings getInterceptSettings() {
                return __interceptSettings;
            }
        };
    }
}
