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
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractRegistryService
 * 
 * @author william.liangf
 */
public abstract class AbstractRegistryService implements RegistryService {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // Map<serviceName, Map<url, queryString>>
    /**
     * 已注册的服务
     */
    private final ConcurrentMap<String, List<URL>> registered = new ConcurrentHashMap<String, List<URL>>();

    // Map<serviceName, queryString>
    /**
     * 已订阅的服务
     */
    private final ConcurrentMap<String, Map<String, String>> subscribed = new ConcurrentHashMap<String, Map<String, String>>();

    // Map<serviceName, Map<url, queryString>>
    /**
     * 已通知的服务
     */
    private final ConcurrentMap<String, List<URL>> notified = new ConcurrentHashMap<String, List<URL>>();
    
    // Map<serviceName, List<notificationListener>>
    /**
     * 已订阅服务的监听器列表（观察者）
     */
    private final ConcurrentMap<String, List<NotifyListener>> notifyListeners = new ConcurrentHashMap<String, List<NotifyListener>>();
    
    public void register(URL url) {
        if (logger.isInfoEnabled()) {
            logger.info("Register service: " + url.getServiceKey() + ",url:" + url);
        }
        register(url.getServiceKey(), url);// 实际注册服务的逻辑
    }

    public void unregister(URL url) {
        if (logger.isInfoEnabled()) {
            logger.info("Unregister service: " + url.getServiceKey() + ",url:" + url);
        }
        unregister(url.getServiceKey(), url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        subscribe(url.getServiceKey(), url.getParameters(), listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        unsubscribe(url.getServiceKey(), url.getParameters(), listener);
    }

    public List<URL> lookup(URL url) {
        return getRegistered(url.getServiceKey());
    }

    public void register(String service, URL url) {
        // 参数校验
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        // 先从已注册的服务缓存中查找，找不到则把服务放进缓存列表
        List<URL> urls = registered.get(service);
        if (urls == null) {
            registered.putIfAbsent(service, new CopyOnWriteArrayList<URL>());
            urls = registered.get(service);
        }
        // 将服务提供者的url放进提供者列表
        if (! urls.contains(url)) {
            urls.add(url);
        }
    }
    
    public void unregister(String service, URL url) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        List<URL> urls = registered.get(service);
        if (urls != null) {
            urls.remove(url);
        }
    }
    
    public void subscribe(String service, Map<String, String> parameters, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.put(service, parameters); 
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners == null) {
            notifyListeners.putIfAbsent(service, new CopyOnWriteArrayList<NotifyListener>());
            listeners = notifyListeners.get(service);
        }
        listeners.add(listener);
    }

    public void unsubscribe(String service, Map<String, String> parameters, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.remove(service);
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    private void doNotify(String service, List<URL> urls) {
        notified.put(service, urls);
        List<NotifyListener> listeners = notifyListeners.get(service);// 获取该服务的所有观察者
        if (listeners != null) {
            for (NotifyListener listener : listeners) {
                try {
                    notify(service, urls, listener);
                } catch (Throwable t) {
                    logger.error("Failed to notify registry event, service: " + service + ", urls: " +  urls + ", cause: " + t.getMessage(), t);
                }
            }
        }
    }
    
    protected void notify(String service, List<URL> urls, NotifyListener listener) {
        listener.notify(urls);
    }
    
    protected final void forbid(String service) {
        doNotify(service, new ArrayList<URL>(0));
    }

    protected final void notify(String service, List<URL> urls) {
        if (service == null || service.length() == 0
                || urls == null || urls.size() == 0) {
            return;
        }
        doNotify(service, urls);
    }
    
    public Map<String, List<URL>> getRegistered() {
        return Collections.unmodifiableMap(registered);
    }
    
    public List<URL> getRegistered(String service) {
        return Collections.unmodifiableList(registered.get(service));
    }
    
    public Map<String, Map<String, String>> getSubscribed() {
        return Collections.unmodifiableMap(subscribed);
    }
    
    public Map<String, String> getSubscribed(String service) {
        return subscribed.get(service);
    }
    
    public Map<String, List<URL>> getNotified() {
        return Collections.unmodifiableMap(notified);
    }
    
    public List<URL> getNotified(String service) {
        return Collections.unmodifiableList(notified.get(service));
    }
    
    public Map<String, List<NotifyListener>> getListeners() {
        return Collections.unmodifiableMap(notifyListeners);
    }

}