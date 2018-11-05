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
package net.ymate.platform.serv.impl;

import net.ymate.platform.core.util.DateTimeUtils;
import net.ymate.platform.core.util.RuntimeUtils;
import net.ymate.platform.serv.IClient;
import net.ymate.platform.serv.IHeartbeatService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * @author 刘镇 (suninformation@163.com) on 15/11/19 下午3:07
 * @version 1.0
 */
public class DefaultHeartbeatService extends Thread implements IHeartbeatService {

    private static final Log _LOG = LogFactory.getLog(DefaultReconnectService.class);

    private IClient __client;

    private boolean __inited;
    private boolean __flag;

    private long __heartbeatInterval = 5;

    private String __heartbeatMessage;

    @Override
    public void init(IClient client) {
        __client = client;
        __inited = true;
    }

    @Override
    public boolean isInited() {
        return __inited;
    }

    @Override
    public void start() {
        if (__inited && !__flag) {
            __flag = true;
            setName("HeartbeatService-" + __client.listener().getClass().getSimpleName());
            if (__client.clientCfg().getHeartbeatInterval() > 0) {
                __heartbeatInterval = __client.clientCfg().getHeartbeatInterval();
            }
            __heartbeatMessage = StringUtils.defaultIfBlank(__client.clientCfg().getParam("heartbeat_message"), "0");
            super.start();
        }
    }

    @Override
    public void run() {
        if (__inited) {
            long _millis = __heartbeatInterval * DateTimeUtils.SECOND;
            while (__flag) {
                try {
                    if (__client.isConnected()) {
                        __client.send(__heartbeatMessage);
                    }
                    sleep(_millis);
                } catch (Exception e) {
                    if (__flag) {
                        _LOG.error(e.getMessage(), RuntimeUtils.unwrapThrow(e));
                    } else {
                        _LOG.debug(e.getMessage(), RuntimeUtils.unwrapThrow(e));
                    }
                }
            }
        }
    }

    @Override
    public boolean isStarted() {
        return __flag;
    }

    @Override
    public void interrupt() {
        if (__inited && __flag) {
            try {
                __flag = false;
                join();
            } catch (InterruptedException e) {
                _LOG.debug(e.getMessage(), RuntimeUtils.unwrapThrow(e));
            }
            super.interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        interrupt();
    }
}
