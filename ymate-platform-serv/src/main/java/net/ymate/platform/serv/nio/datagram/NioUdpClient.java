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
package net.ymate.platform.serv.nio.datagram;

import net.ymate.platform.core.util.RuntimeUtils;
import net.ymate.platform.serv.*;
import net.ymate.platform.serv.nio.INioCodec;
import net.ymate.platform.serv.nio.INioSession;
import net.ymate.platform.serv.nio.support.NioEventGroup;
import net.ymate.platform.serv.nio.support.NioEventProcessor;
import net.ymate.platform.serv.nio.support.NioSession;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * @author 刘镇 (suninformation@163.com) on 15/11/17 下午3:04
 * @version 1.0
 */
public class NioUdpClient extends AbstractService implements IClient<NioUdpListener, INioCodec> {

    private static final Log _LOG = LogFactory.getLog(NioUdpClient.class);

    protected IClientCfg __clientCfg;

    protected NioUdpEventGroup __eventGroup;

    protected NioUdpListener __listener;

    protected INioCodec __codec;

    @Override
    public void init(IClientCfg clientCfg,
                     NioUdpListener listener,
                     INioCodec codec,
                     IReconnectService reconnectService,
                     IHeartbeatService heartbeatService) {
        __clientCfg = clientCfg;
        //
        __listener = listener;
        __codec = codec;
        __codec.init(__clientCfg.getCharset());
        //
        __doSetHeartbeatService(heartbeatService);
    }

    @Override
    public void connect() throws IOException {
        if (__eventGroup != null && __eventGroup.session() != null) {
            if (__eventGroup.session().isConnected() || __eventGroup.session().isNew()) {
                return;
            }
        }
        __eventGroup = new NioUdpEventGroup(__clientCfg, __listener, __codec);
        __eventGroup.start();
        //
        __doStartHeartbeatService();
    }

    @Override
    public void reconnect() throws IOException {
        // Don't need to reconnect
    }

    @Override
    public boolean isConnected() {
        return __eventGroup != null && __eventGroup.session() != null && __eventGroup.session().isConnected();
    }

    @Override
    public IClientCfg clientCfg() {
        return __clientCfg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NioUdpListener> T listener() {
        return (T) __listener;
    }

    @Override
    public void send(Object message) throws IOException {
        __eventGroup.session().send(message);
    }

    @Override
    public void close() throws IOException {
        __doStopHeartbeatService();
        //
        __eventGroup.close();
    }

    public class NioUdpEventGroup extends NioEventGroup<NioUdpListener> {

        public NioUdpEventGroup(IClientCfg cfg, NioUdpListener listener, INioCodec codec) throws IOException {
            super(cfg, listener, codec);
        }

        @Override
        protected INioSession __doSessionCreate(IClientCfg cfg) throws IOException {
            DatagramChannel _channel = DatagramChannel.open();
            _channel.configureBlocking(false);
            _channel.socket().connect(new InetSocketAddress(cfg.getRemoteHost(), cfg.getPort()));
            __channel = _channel;
            INioSession _session = new NioSession<NioUdpListener>(this, __channel) {
                @Override
                protected int __doChannelRead(ByteBuffer buffer) throws IOException {
                    SocketAddress _address = ((DatagramChannel) __channel).receive(buffer);
                    if (_address != null) {
                        return __buffer.remaining();
                    }
                    return 0;
                }

                @Override
                protected int __doChannelWrite(ByteBuffer buffer) throws IOException {
                    SocketAddress _address = ((DatagramChannel) __channel).socket().getRemoteSocketAddress();
                    if (_address != null) {
                        return ((DatagramChannel) __channel).send(buffer, _address);
                    }
                    buffer.reset();
                    return 0;
                }
            };
            _session.attr(SocketAddress.class.getName(), _channel.socket().getRemoteSocketAddress());
            return _session;
        }

        @Override
        public void start() throws IOException {
            _LOG.info("UdpClient [" + __eventGroup.name() + "] connecting to " + __clientCfg.getRemoteHost() + ":" + __clientCfg.getPort());
            super.start();
        }

        @Override
        protected String __doBuildProcessorName() {
            return StringUtils.capitalize(name()).concat("UdpClient-NioEventProcessor");
        }

        @Override
        protected void __doInitProcessors() throws IOException {
            __processors = new NioEventProcessor[]{
                    new NioEventProcessor<NioUdpListener>(this, __doBuildProcessorName()) {
                        @Override
                        protected void __doExceptionEvent(SelectionKey key, final Throwable e) {
                            final INioSession _session = (INioSession) key.attachment();
                            __eventGroup.executorService().submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        __eventGroup.listener().onExceptionCaught(e, _session);
                                    } catch (IOException ex) {
                                        _LOG.error(e.getMessage(), RuntimeUtils.unwrapThrow(ex));
                                    }
                                }
                            });
                        }
                    }
            };
            __processors[0].start();
        }

        @Override
        protected void __doRegisterEvent() throws IOException {
            processor().registerEvent(__channel, SelectionKey.OP_READ, session());
        }
    }
}
