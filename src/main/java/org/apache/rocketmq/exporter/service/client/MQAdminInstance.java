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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.exporter.service.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.exporter.config.RMQConfigure;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExtImpl;
import org.joor.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import static org.apache.rocketmq.common.MixAll.TOOLS_CONSUMER_GROUP;

@Service
public class MQAdminInstance {
    private final static Logger log = LoggerFactory.getLogger(MQAdminInstance.class);
    @Autowired
    private RMQConfigure configure;
    private RPCHook aclHook;

    private MQAdminInstance(RMQConfigure configure) {
        this.configure = configure;
        aclHook = getAclRPCHook();
    }

    private RPCHook getAclRPCHook() {
        if (configure.enableACL()) {
            if (StringUtils.isAllBlank(configure.getAccessKey())) {
                throw new RuntimeException("acl config error: accessKey is empty");
            }
            if (StringUtils.isAllBlank(configure.getSecretKey())) {
                throw new RuntimeException("acl config error: secretKey is empty");
            }
            return new AclClientRPCHook(new SessionCredentials(configure.getAccessKey(), configure.getSecretKey()));
        }
        return null;
    }

    @Bean(destroyMethod = "shutdown", name = "defaultMQAdminExt")
    private DefaultMQAdminExt buildDefaultMQAdminExt() throws Exception {
        String namesrvAddress = configure.getNamesrvAddr();
        if (StringUtils.isBlank(namesrvAddress)) {
            log.error("Build DefaultMQAdminExt error, namesrv is null");
            throw new Exception("Build DefaultMQAdminExt error, namesrv is null", null);
        }
        DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt(this.aclHook,5000L);
        defaultMQAdminExt.setInstanceName("admin-" + System.currentTimeMillis());
        defaultMQAdminExt.setNamesrvAddr(namesrvAddress);
        try {
            defaultMQAdminExt.start();
        } catch (MQClientException ex) {
            log.error(String.format("init default admin error, namesrv=%s", System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY)), ex);
        }
        return defaultMQAdminExt;
    }

    @Bean(destroyMethod = "shutdown")
    private DefaultMQPullConsumer buildPullConsumer() throws Exception {
        String namesrvAddress = configure.getNamesrvAddr();
        if (StringUtils.isBlank(namesrvAddress)) {
            log.error("init default pull consumer error, namesrv is null");
            throw new Exception("init default pull consumer error, namesrv is null", null);
        }
        DefaultMQPullConsumer pullConsumer = new DefaultMQPullConsumer(TOOLS_CONSUMER_GROUP, this.aclHook);
        pullConsumer.setInstanceName("consumer-" + System.currentTimeMillis());
        pullConsumer.setNamesrvAddr(namesrvAddress);
        try {
            pullConsumer.start();
            pullConsumer.getDefaultMQPullConsumerImpl().getPullAPIWrapper().setConnectBrokerByUser(true);
        } catch (MQClientException ex) {
            log.error(String.format("init default pull consumer error, namesrv=%s", System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY)), ex);
        }
        return pullConsumer;
    }

    @Bean(destroyMethod = "shutdown")
    private MQClientInstance buildInstance(@Qualifier("defaultMQAdminExt") DefaultMQAdminExt defaultMQAdminExt) {
        DefaultMQAdminExtImpl defaultMQAdminExtImpl = Reflect.on(defaultMQAdminExt).get("defaultMQAdminExtImpl");
        return Reflect.on(defaultMQAdminExtImpl).get("mqClientInstance");
    }

    @Bean
    private RemotingClient client(MQClientInstance instance) {
        MQClientAPIImpl mQClientAPIImpl = Reflect.on(instance).get("mQClientAPIImpl");
        return Reflect.on(mQClientAPIImpl).get("remotingClient");
    }
}
