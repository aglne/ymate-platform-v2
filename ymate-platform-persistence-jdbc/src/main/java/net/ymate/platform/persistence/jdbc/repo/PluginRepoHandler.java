/*
 * Copyright 2007-2018 the original author or authors.
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
package net.ymate.platform.persistence.jdbc.repo;

import net.ymate.platform.core.YMP;
import net.ymate.platform.persistence.jdbc.JDBC;
import net.ymate.platform.persistence.jdbc.repo.annotation.Repository;
import net.ymate.platform.plugin.PluginClassLoader;
import net.ymate.platform.plugin.annotation.Handler;

/**
 * @author 刘镇 (suninformation@163.com) on 2018-12-12 19:27
 * @version 1.0
 */
@Handler(Repository.class)
public class PluginRepoHandler extends RepoHandler {

    public PluginRepoHandler(YMP owner) throws Exception {
        super(owner.getModule(JDBC.class));
    }

    @Override
    public Object handle(Class<?> targetClass) throws Exception {
        if (targetClass.getClassLoader() instanceof PluginClassLoader) {
            return super.handle(targetClass);
        }
        return null;
    }
}
