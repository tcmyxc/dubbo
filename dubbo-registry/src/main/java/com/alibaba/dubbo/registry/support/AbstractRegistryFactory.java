/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RegistryLocators. (API, Static, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.registry.RegistryFactory
 * @author william.liangf
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    // 日志输出
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    // 注册中心获取过程锁
    protected static final ReentrantLock LOCK = new ReentrantLock();
    
    // 注册中心集合 Map<RegistryAddress, Registry>
    protected static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<String, Registry>();

    public Registry getRegistry(URL url) {
        // 锁定注册中心获取过程，保证注册中心单一实例
        LOCK.lock();
        try {
            // 1、先从缓存中查找
            Registry registry = REGISTRIES.get(getCacheKey(url));
            // 2、如果缓存中有，直接返回即可
            if (registry != null) {
                return registry;
            }
            // 3、如果缓存中没有，则创建一个注册中心
            registry = createRegistry(url);
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            // 4、新建注册中心之后，放进缓存
            REGISTRIES.put(getCacheKey(url), registry);
            return registry;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }
    
    protected abstract Registry createRegistry(URL url);

    /**
     * 获取所有注册中心
     * 
     * @return 所有注册中心
     */
    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }
    
    /**
     * 关闭所有已创建注册中心
     */
    public static void destroyAll() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Close all registries " + getRegistries());
        }
        // 锁定注册中心关闭过程
        LOCK.lock();
        try {
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            REGISTRIES.clear();
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    /**
     * 获取缓存key
     */
    protected static String getCacheKey(URL url){
        return url.getProtocol() + "://" + url.getUsername() + ":" + url.getPassword() + "@" + url.getAddress();
    }
    
}