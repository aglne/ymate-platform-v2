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

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 刘镇 (suninformation@163.com) on 2017/12/11 上午3:29
 * @version 1.0
 */
public class DefaultThreadFactory implements ThreadFactory {

    private static final AtomicInteger __poolNumber = new AtomicInteger(1);

    private final ThreadGroup __group;

    private final AtomicInteger __threadNumber = new AtomicInteger(1);

    private final String __namePrefix;

    private boolean __daemon;

    private int __priority = Thread.NORM_PRIORITY;

    private Thread.UncaughtExceptionHandler __uncaughtExceptionHandler;

    public DefaultThreadFactory() {
        this("ymp-pool-");
    }

    public DefaultThreadFactory(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            throw new NullArgumentException("prefix");
        }
        SecurityManager _securityManager = System.getSecurityManager();
        __group = (_securityManager != null) ? _securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        __namePrefix = prefix + __poolNumber.getAndIncrement() + "-thread-";
    }

    public DefaultThreadFactory daemon(boolean daemon) {
        __daemon = daemon;
        return this;
    }

    public DefaultThreadFactory priority(int priority) {
        __priority = priority;
        return this;
    }

    public DefaultThreadFactory uncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        __uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread _thread = new Thread(__group, r, __namePrefix + __threadNumber.getAndIncrement(), 0);
        if (__daemon) {
            _thread.setDaemon(true);
        }
        if (__priority > 0) {
            _thread.setPriority(__priority);
        }
        if (__uncaughtExceptionHandler != null) {
            _thread.setUncaughtExceptionHandler(__uncaughtExceptionHandler);
        }
        return _thread;
    }
}
