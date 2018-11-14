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
package net.ymate.platform.core.beans.impl;

import net.ymate.platform.core.YMP;
import net.ymate.platform.core.beans.*;
import net.ymate.platform.core.beans.annotation.By;
import net.ymate.platform.core.beans.annotation.CleanProxy;
import net.ymate.platform.core.beans.annotation.Inject;
import net.ymate.platform.core.beans.annotation.Proxy;
import net.ymate.platform.core.beans.proxy.IProxy;
import net.ymate.platform.core.beans.proxy.IProxyFactory;
import net.ymate.platform.core.beans.proxy.IProxyFilter;
import net.ymate.platform.core.util.ClassUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 默认对象工厂接口实现
 *
 * @author 刘镇 (suninformation@163.com) on 15-3-5 下午2:56
 * @version 1.0
 */
public class DefaultBeanFactory implements IBeanFactory {

    private static final Log _LOG = LogFactory.getLog(DefaultBeanFactory.class);

    private final YMP __owner;

    private IBeanFactory __parentFactory;

    private List<String> __packageNames;

    private List<String> __excludedPackages;

    private List<Class<?>> __excludedClassSet;

    private List<String> __excludedFileSet;

    private Map<Class<? extends Annotation>, IBeanHandler> __beanHandlerMap;

    private Map<Class<? extends Annotation>, IBeanInjector> __beanInjectorMap;

    /**
     * 对象类型 -> 对象实例
     */
    private Map<Class<?>, BeanMeta> __beanInstancesMap;

    /**
     * 接口类型 -> 对象类型
     */
    private Map<Class<?>, Class<?>> __beanInterfacesMap;

    private IBeanLoader __beanLoader;

    private IProxyFactory __proxyFactory;

    public DefaultBeanFactory(YMP owner) {
        this.__owner = owner;
        this.__packageNames = new ArrayList<String>();
        this.__excludedPackages = new ArrayList<String>();
        this.__excludedClassSet = new ArrayList<Class<?>>();
        this.__beanHandlerMap = new HashMap<Class<? extends Annotation>, IBeanHandler>();
        this.__beanInjectorMap = new HashMap<Class<? extends Annotation>, IBeanInjector>();
        this.__beanInstancesMap = new HashMap<Class<?>, BeanMeta>();
        this.__beanInterfacesMap = new HashMap<Class<?>, Class<?>>();
    }

    public DefaultBeanFactory(YMP owner, IBeanFactory parent) {
        this(owner);
        this.__parentFactory = parent;
    }

    @Override
    public void registerHandler(Class<? extends Annotation> annoClass, IBeanHandler handler) {
        if (!__beanHandlerMap.containsKey(annoClass)) {
            __beanHandlerMap.put(annoClass, handler);
        } else {
            _LOG.warn("Handler class [" + annoClass.getSimpleName() + "] duplicate registration is not allowed");
        }
    }

    @Override
    public void registerHandler(Class<? extends Annotation> annoClass) {
        registerHandler(annoClass, IBeanHandler.DEFAULT_HANDLER);
    }

    @Override
    public IBeanHandler getBeanHandler(Class<? extends Annotation> annoClass) {
        return __beanHandlerMap.get(annoClass);
    }

    @Override
    public void registerInjector(Class<? extends Annotation> annoClass, IBeanInjector injector) {
        if (!__beanInjectorMap.containsKey(annoClass)) {
            __beanInjectorMap.put(annoClass, injector);
        } else {
            _LOG.warn("Injector class [" + annoClass.getSimpleName() + "] duplicate registration is not allowed");
        }
    }

    private void __doParsePackagePath(List<String> targetList, String packageName) {
        boolean _flag = false;
        do {
            if (!targetList.contains(packageName)) {
                for (int _idx = 0; _idx < targetList.size(); _idx++) {
                    if (packageName.startsWith(targetList.get(_idx))) {
                        _flag = true;
                    } else if (targetList.get(_idx).startsWith(packageName)) {
                        targetList.remove(_idx);
                        targetList.add(packageName);
                        _flag = true;
                    }
                }
                if (!_flag) {
                    targetList.add(packageName);
                    _flag = true;
                }
            }
        } while (!_flag);
    }

    @Override
    public void registerPackage(String packageName) {
        __doParsePackagePath(this.__packageNames, packageName);
    }

    @Override
    public List<String> getPackageNames() {
        return __packageNames;
    }

    @Override
    public void registerExcludedPackage(String packageName) {
        __doParsePackagePath(this.__excludedPackages, packageName);
    }

    @Override
    public List<String> getExcludedPackageNames() {
        return __excludedPackages;
    }

    @Override
    public void registerExcludedClass(Class<?> excludedClass) {
        if (excludedClass.isInterface()) {
            this.__excludedClassSet.add(excludedClass);
        }
    }

    @Override
    public List<String> getExcludedFiles() {
        return __excludedFileSet == null ? Collections.<String>emptyList() : __excludedFileSet;
    }

    @Override
    public void setExcludedFiles(List<String> excludedFiles) {
        __excludedFileSet = excludedFiles;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        T _obj = null;
        if (clazz != null && !clazz.isAnnotation()) {
            BeanMeta _beanMeta;
            if (clazz.isInterface()) {
                Class<?> _targetClass = this.__beanInterfacesMap.get(clazz);
                _beanMeta = this.__beanInstancesMap.get(_targetClass);
            } else {
                _beanMeta = this.__beanInstancesMap.get(clazz);
            }
            if (_beanMeta != null) {
                if (!_beanMeta.isSingleton()) {
                    try {
                        _obj = (T) _beanMeta.getBeanClass().newInstance();
                        if (__proxyFactory != null) {
                            _obj = (T) __wrapProxy(__proxyFactory, _obj);
                        }
                        __initBeanIoC(_beanMeta.getBeanClass(), _obj);
                    } catch (Exception e) {
                        _LOG.warn("", e);
                    }
                } else {
                    _obj = (T) _beanMeta.getBeanObject();
                }
            }
            if (_obj == null && this.__parentFactory != null) {
                _obj = this.__parentFactory.getBean(clazz);
            }
        }
        return _obj;
    }

    @Override
    public Map<Class<?>, BeanMeta> getBeans() {
        return Collections.unmodifiableMap(this.__beanInstancesMap);
    }

    @Override
    public void registerBean(BeanMeta beanMeta) {
        // 注解、枚举和接口类型采用不同方式处理
        if (beanMeta.getBeanClass().isInterface()) {
            if (beanMeta.getBeanObject() != null) {
                __beanInstancesMap.put(beanMeta.getBeanClass(), beanMeta);
                __addClassInterfaces(beanMeta);
            }
        } else if (!beanMeta.getBeanClass().isAnnotation() && !beanMeta.getBeanClass().isEnum()) {
            __addClass(beanMeta);
        }
    }

    @Override
    public void registerBean(Class<?> clazz) {
        registerBean(BeanMeta.create(clazz));
    }

    @Override
    public void registerBean(Class<?> clazz, Object object) {
        registerBean(BeanMeta.create(object, clazz));
    }

    protected void __addClass(BeanMeta beanMeta) {
        __beanInstancesMap.put(beanMeta.getBeanClass(), beanMeta);
        //
        __addClassInterfaces(beanMeta);
    }

    private void __addClassInterfaces(BeanMeta beanMeta) {
        if (!beanMeta.isSkipInterface()) {
            for (Class<?> _interface : beanMeta.getBeanInterfaces(__excludedClassSet)) {
                __beanInterfacesMap.put(_interface, beanMeta.getBeanClass());
            }
        } else if (beanMeta.isSkipInterface() && beanMeta.getBeanClass().isInterface() && !__excludedClassSet.contains(beanMeta.getBeanClass())) {
            __beanInterfacesMap.put(beanMeta.getBeanClass(), beanMeta.getBeanClass());
        }
    }

    @Override
    public void init() throws Exception {
        if (this.__beanLoader == null) {
            if (this.__parentFactory != null) {
                this.__beanLoader = this.__parentFactory.getLoader();
            }
            if (this.__beanLoader == null) {
                this.__beanLoader = new DefaultBeanLoader();
            }
        }
        __beanLoader.load(this);
    }

    @Override
    public YMP getOwner() {
        return __owner;
    }

    @Override
    public void destroy() throws Exception {
        this.__parentFactory = null;
        this.__packageNames = null;
        this.__excludedPackages = null;
        this.__excludedClassSet = null;
        this.__beanHandlerMap = null;
        this.__beanInstancesMap = null;
        this.__beanInterfacesMap = null;
        this.__beanLoader = null;
    }

    @Override
    public IBeanFactory getParent() {
        return __parentFactory;
    }

    @Override
    public void setParent(IBeanFactory parent) {
        this.__parentFactory = parent;
    }

    @Override
    public IBeanLoader getLoader() {
        return this.__beanLoader;
    }

    @Override
    public void setLoader(IBeanLoader loader) {
        this.__beanLoader = loader;
    }

    @Override
    public void initProxy(IProxyFactory proxyFactory) throws Exception {
        if (proxyFactory == null) {
            throw new NullArgumentException("proxyFactory");
        }
        __proxyFactory = proxyFactory;
        for (Map.Entry<Class<?>, BeanMeta> _entry : this.getBeans().entrySet()) {
            if (!_entry.getKey().isInterface() && _entry.getValue().isSingleton()) {
                _entry.getValue().setBeanObject(__wrapProxy(proxyFactory, _entry.getValue().getBeanObject()));
            }
        }
    }

    private Object __wrapProxy(IProxyFactory proxyFactory, Object targetObject) {
        final Class<?> _targetClass = targetObject.getClass();
        //
        List<IProxy> _targetProxies = proxyFactory.getProxies(new IProxyFilter() {

            private boolean __doCheckAnnotation(Proxy targetProxyAnno) {
                // 若设置了自定义注解类型，则判断targetClass是否匹配，否则返回true
                if (targetProxyAnno.annotation().length > 0) {
                    for (Class<? extends Annotation> _annoClass : targetProxyAnno.annotation()) {
                        if (_targetClass.isAnnotationPresent(_annoClass)) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }

            @Override
            public boolean filter(IProxy targetProxy) {
                CleanProxy _cleanProxy = _targetClass.getAnnotation(CleanProxy.class);
                if (_cleanProxy != null) {
                    if (_cleanProxy.value().length > 0) {
                        for (Class<? extends IProxy> _proxyClass : _cleanProxy.value()) {
                            if (_proxyClass.equals(targetProxy.getClass())) {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                }
                Proxy _targetProxyAnno = targetProxy.getClass().getAnnotation(Proxy.class);
                // 若已设置作用包路径
                if (StringUtils.isNotBlank(_targetProxyAnno.packageScope())) {
                    // 若当前类对象所在包路径匹配
                    if (!StringUtils.startsWith(_targetClass.getPackage().getName(), _targetProxyAnno.packageScope())) {
                        return false;
                    }
                }
                return __doCheckAnnotation(_targetProxyAnno);
            }
        });
        if (!_targetProxies.isEmpty()) {
            // 由于创建代理是通过接口重新实例化对象并覆盖原对象，所以需要复制原有对象成员（暂时先这样吧，还没想到好的处理办法）
            Object _proxyObject = proxyFactory.createProxy(_targetClass, _targetProxies);
            if (_proxyObject != null) {
                return ClassUtils.wrapper(targetObject).duplicate(_proxyObject);
            }
        }
        return targetObject;
    }

    @Override
    public void initIoC() throws Exception {
        for (Map.Entry<Class<?>, BeanMeta> _bean : this.getBeans().entrySet()) {
            if (!_bean.getKey().isInterface() && _bean.getValue().isSingleton()) {
                __initBeanIoC(_bean.getKey(), _bean.getValue().getBeanObject());
            }
        }
    }

    /**
     * 对目标类进行IoC注入
     *
     * @param targetClass  目标类型对象(不允许是代理对象)
     * @param targetObject 目标类型对象实例
     * @throws Exception 可能产生的异常
     */
    private void __initBeanIoC(Class<?> targetClass, Object targetObject) throws Exception {
        Field[] _fields = targetClass.getDeclaredFields();
        if (_fields != null && _fields.length > 0) {
            for (Field _field : _fields) {
                Object _injectObj = null;
                if (_field.isAnnotationPresent(Inject.class)) {
                    if (_field.isAnnotationPresent(By.class)) {
                        By _injectBy = _field.getAnnotation(By.class);
                        _injectObj = this.getBean(_injectBy.value());
                    } else {
                        _injectObj = this.getBean(_field.getType());
                    }
                }
                _injectObj = __tryInjector(targetClass, _field, _injectObj);
                if (_injectObj != null) {
                    _field.setAccessible(true);
                    _field.set(targetObject, _injectObj);
                }
            }
        }
        if (targetObject instanceof IBeanInitializer) {
            ((IBeanInitializer) targetObject).afterInitialized();
        }
    }

    private Object __tryInjector(Class<?> targetClass, Field field, Object originInject) {
        if (!__beanInjectorMap.isEmpty()) {
            for (Map.Entry<Class<? extends Annotation>, IBeanInjector> _entry : __beanInjectorMap.entrySet()) {
                Annotation _annotation = field.getAnnotation(_entry.getKey());
                if (_annotation != null) {
                    return _entry.getValue().inject(this, _annotation, targetClass, field, originInject);
                }
            }
        }
        return originInject;
    }
}
