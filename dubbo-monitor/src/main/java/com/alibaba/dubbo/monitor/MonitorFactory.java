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
package com.alibaba.dubbo.monitor;

import com.alibaba.dubbo.common.Adaptive;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;

/**
 * MonitorFactory. (SPI, Singleton, ThreadSafe)
 * 
 * @author william.liangf
 */
@Extension("dubbo")
public interface MonitorFactory {
    
    /**
     * Create monitor.
     * 
     * @param url
     * @return
     */
    @Adaptive("protocol")// 运行时，根据 URL 里面的实际参数 "protocol" 决定实现，如果没有，则使用默认的 dubbo
    Monitor getMonitor(URL url);

}