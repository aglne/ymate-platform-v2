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
package net.ymate.platform.persistence.redis.impl;

import net.ymate.platform.core.IConfig;
import net.ymate.platform.persistence.redis.IRedisDataSourceAdapter;
import net.ymate.platform.persistence.redis.IRedisModuleCfg;
import net.ymate.platform.persistence.redis.RedisDataSourceCfgMeta;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 刘镇 (suninformation@163.com) on 15/12/2 上午2:31
 * @version 1.0
 */
public class RedisDataSourceAdapter implements IRedisDataSourceAdapter {

    private RedisDataSourceCfgMeta __cfgMeta;

    private Pool __pool;

    private JedisCluster __cluster;

    private boolean __isCluster;

    @Override
    public void initialize(RedisDataSourceCfgMeta cfgMeta) throws Exception {
        __cfgMeta = cfgMeta;
        //
        switch (cfgMeta.getConnectionType()) {
            case SHARD:
                if (!cfgMeta.getServers().isEmpty()) {
                    List<JedisShardInfo> _shards = new ArrayList<JedisShardInfo>();
                    for (IRedisModuleCfg.ServerMeta _server : cfgMeta.getServers().values()) {
                        _shards.add(new JedisShardInfo(_server.getHost(), _server.getName(), _server.getPort(), _server.getTimeout(), _server.getWeight()));
                    }
                    __pool = new ShardedJedisPool(cfgMeta.getPoolConfig(), _shards);
                }
                break;
            case SENTINEL:
                if (!cfgMeta.getServers().isEmpty()) {
                    Set<String> _sentinel = new HashSet<String>();
                    for (IRedisModuleCfg.ServerMeta _server : cfgMeta.getServers().values()) {
                        _sentinel.add(_server.getHost() + ":" + _server.getPort());
                    }
                    IRedisModuleCfg.ServerMeta _server = cfgMeta.getMasterServerMeta();
                    __pool = new JedisSentinelPool(_server.getName(),
                            _sentinel, cfgMeta.getPoolConfig(),
                            _server.getTimeout(), _server.getPassword(), _server.getDatabase(), _server.getClientName());
                }
                break;
            case CLUSTER:
                Set<HostAndPort> _cluster = new HashSet<HostAndPort>();
                for (IRedisModuleCfg.ServerMeta _server : cfgMeta.getServers().values()) {
                    _cluster.add(new HostAndPort(_server.getHost(), _server.getPort()));
                }
                IRedisModuleCfg.ServerMeta _server = cfgMeta.getMasterServerMeta();
                __cluster = new JedisCluster(_cluster, _server.getTimeout(), _server.getSocketTimeout(), _server.getMaxAttempts(), _server.getPassword(), cfgMeta.getPoolConfig());
                __isCluster = true;
                break;
            default:
                if (cfgMeta.getServers().isEmpty()) {
                    __pool = new JedisPool(cfgMeta.getPoolConfig(), "localhost");
                } else {
                    IRedisModuleCfg.ServerMeta _defaultServer = cfgMeta.getServers().get(IConfig.DEFAULT_STR);
                    __pool = new JedisPool(cfgMeta.getPoolConfig(),
                            _defaultServer.getHost(),
                            _defaultServer.getPort(),
                            _defaultServer.getTimeout(),
                            _defaultServer.getPassword(),
                            _defaultServer.getDatabase(),
                            _defaultServer.getClientName());
                }
        }
    }

    @Override
    public JedisCommands getCommands() {
        if (__isCluster) {
            return __cluster;
        }
        return (JedisCommands) __pool.getResource();
    }

    @Override
    public RedisDataSourceCfgMeta getDataSourceCfgMeta() {
        return __cfgMeta;
    }

    @Override
    public void destroy() {
        __pool.destroy();
    }
}
