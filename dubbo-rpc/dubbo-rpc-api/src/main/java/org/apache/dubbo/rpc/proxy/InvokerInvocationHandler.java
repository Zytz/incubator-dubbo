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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;
import static org.apache.dubbo.rpc.Constants.FUTURE_RETURNTYPE_KEY;

/**
 * InvokerHandler
 */
public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);
    private final Invoker<?> invoker;

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //获取方法名
        String methodName = method.getName();
        //获取方法中的参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        //定义的方法名为Object时,直接反射调用，不需要RPC
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        /**
         * tostiong ,equals , hashcode,同时不需要RPC
         */
        //如果方法名为toString ,且参数为空
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return invoker.toString();
        }
        //方法为hashcode时
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return invoker.equals(args[0]);
        }
        //进行RPC调用
        return invoker.invoke(createInvocation(method, args)).recreate();
    }

    private RpcInvocation createInvocation(Method method, Object[] args) {

        RpcInvocation invocation = new RpcInvocation(method, args);
        //如果方法中含有回调，java 8的结果，
        if (RpcUtils.hasFutureReturnType(method)) {
            invocation.setAttachment(FUTURE_RETURNTYPE_KEY, "true");
            invocation.setAttachment(ASYNC_KEY, "true");
        }
        return invocation;
    }

}
