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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_WARMUP;
import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_WEIGHT;
import static org.apache.dubbo.rpc.cluster.Constants.REMOTE_TIMESTAMP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WARMUP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;

/**
 * AbstractLoadBalance
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    /**
     * Calculate the weight according to the uptime proportion of warmup time
     * the new weight will be within 1(inclusive) to weight(inclusive)
     *
     * @param uptime the uptime in milliseconds
     * @param warmup the warmup time in milliseconds
     * @param weight the weight of an invoker
     * @return weight which takes warmup into account
     * <p>
     * Warmup指的是当一个provider启动的时候，请求量是逐渐从零开始累加的，而不是一次行过来。
     * 在每个provider启动的一个时期内，根据启动的时间与provider的权重计算一个新的权重，consumer根据这个新的权重去选择一个provider。
     */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        // 计算权重，下面代码逻辑上形似于 (uptime / warmup) * weight。
        // 随着服务运行时间 uptime 增大，权重计算值 ww 会慢慢接近配置值 weight
        //设计思路，就是让刚刚启动不久的服务，处以一个低效率工作的阶段；然后随着时间升级，不断加大其分配的效率
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        //如果只有一个，直接返回
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        //调用各子类的计算方式
        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);


    /**
     * Get the weight of the invoker's invocation which takes warmup time into account
     * if the uptime is within the warmup time, the weight will be reduce proportionally
     *
     * @param invoker    the invoker
     * @param invocation the invocation of this invoker
     * @return weight
     */
    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 从 url 中获取权重 weight 配置值
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), WEIGHT_KEY, DEFAULT_WEIGHT);
        if (weight > 0) {
            //注意这里考虑的是provider启动的时间 remote_timeStamp_key
            long timestamp = invoker.getUrl().getParameter(REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                //获取服务预热时间，默认为10分钟
                int warmup = invoker.getUrl().getParameter(WARMUP_KEY, DEFAULT_WARMUP);
                if (uptime > 0 && uptime < warmup) {
                    //启动时间小于预热时间，则进行weight计算
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight >= 0 ? weight : 0;
    }

}
