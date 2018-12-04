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
package net.ymate.platform.core.event;

/**
 * 事件配置接口
 *
 * @author 刘镇 (suninformation@163.com) on 15/5/16 上午2:42
 * @version 1.0
 */
public interface IEventConfig {

    String PROVIDER_CLASS = "provider_class";

    String DEFAULT_MODE = "default_mode";

    String THREAD_POOL_SIZE = "thread_pool_size";

    String THREAD_MAX_POOL_SIZE = "thread_max_pool_size";

    String THREAD_QUEUE_SIZE = "thread_queue_size";

    /**
     * @return 返回事件管理提供者接口实现，默认为net.ymate.platform.core.event.impl.DefaultEventProvider
     */
    IEventProvider getEventProvider();

    /**
     * @return 返回默认事件触发模式，取值范围：NORMAL-同步执行，ASYNC-异步执行，默认为ASYNC
     */
    Events.MODE getDefaultMode();

    /**
     * @return 返回初始化线程池大小，默认为 Runtime.getRuntime().availableProcessors()
     */
    int getThreadPoolSize();

    /**
     * @return 返回最大线程池大小，默认为 200
     */
    int getThreadMaxPoolSize();

    /**
     * @return 返回线程队列大小，默认为 1024
     */
    int getThreadQueueSize();
}
