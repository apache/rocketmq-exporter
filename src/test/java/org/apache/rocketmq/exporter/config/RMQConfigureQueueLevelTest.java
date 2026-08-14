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
package org.apache.rocketmq.exporter.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RMQConfigureQueueLevelTest {

    @Test
    public void testDisabledByDefault() {
        RMQConfigure configure = new RMQConfigure();
        Assertions.assertFalse(configure.isQueueLevelTopic("any-topic"));
    }

    @Test
    public void testBlankKeepsDisabled() {
        RMQConfigure configure = new RMQConfigure();
        configure.setQueueLevelTopics("   ");
        Assertions.assertFalse(configure.isQueueLevelTopic("any-topic"));
    }

    @Test
    public void testWhitelistIsExactMatchAndTrimmed() {
        RMQConfigure configure = new RMQConfigure();
        configure.setQueueLevelTopics(" topic-a , topic-b ,, ");

        Assertions.assertTrue(configure.isQueueLevelTopic("topic-a"));
        Assertions.assertTrue(configure.isQueueLevelTopic("topic-b"));
        // prefix/suffix must not leak in, otherwise the series budget silently blows up
        Assertions.assertFalse(configure.isQueueLevelTopic("topic-a-extra"));
        Assertions.assertFalse(configure.isQueueLevelTopic("prefix-topic-a"));
        Assertions.assertFalse(configure.isQueueLevelTopic("topic-c"));
        Assertions.assertFalse(configure.isQueueLevelTopic(null));
    }

    @Test
    public void testWildcardEnablesEveryTopic() {
        RMQConfigure configure = new RMQConfigure();
        configure.setQueueLevelTopics("topic-a,*");
        Assertions.assertTrue(configure.isQueueLevelTopic("topic-a"));
        Assertions.assertTrue(configure.isQueueLevelTopic("whatever"));
    }

    @Test
    public void testResetBackToDisabled() {
        RMQConfigure configure = new RMQConfigure();
        configure.setQueueLevelTopics("*");
        Assertions.assertTrue(configure.isQueueLevelTopic("topic-a"));

        configure.setQueueLevelTopics("");
        Assertions.assertFalse(configure.isQueueLevelTopic("topic-a"));
    }
}
