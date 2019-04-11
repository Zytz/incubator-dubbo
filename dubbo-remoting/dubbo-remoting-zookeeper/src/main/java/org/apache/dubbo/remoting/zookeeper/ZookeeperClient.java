/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.URL;

import java.util.List;

public interface ZookeeperClient {
    /**
     * 创建监听器
     * @param path 节点的路径
     * @param ephemeral  是否为临时节点
     */
    void create(String path, boolean ephemeral);

    void delete(String path);

    /**
     * 获取节点的子节点
     * @param path
     * @return
     */
    List<String> getChildren(String path);

    /**
     * 添加
     * @param path 节点路径
     * @param listener 监听器
     * @return 子节点列表
     */
    List<String> addChildListener(String path, ChildListener listener);

    /**
     * 移除节点的监听器
     * @param path 父节点
     * @param listener 移除的特定监听器
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     * 添加节点的状态监听器
     * @param listener
     */
    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    /**
     * 是否连接
     * @return
     */
    boolean isConnected();

    void close();

    /**
     * 获取到注册中心
     * @return
     */
    URL getUrl();

    void create(String path, String content, boolean ephemeral);

    String getContent(String path);

}
