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
package org.apache.rocketmq.exporter.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testMachineReadableByteCount() {
        double unitSize = 1024;
        Assertions.assertThat(Utils.machineReadableByteCount("100 B")).isEqualTo(100);
        Assertions.assertThat(Utils.machineReadableByteCount("1 KiB")).isEqualTo(Math.pow(unitSize, 1));
        Assertions.assertThat(Utils.machineReadableByteCount("1 MiB")).isEqualTo(Math.pow(unitSize, 2));
        Assertions.assertThat(Utils.machineReadableByteCount("1 GiB")).isEqualTo(Math.pow(unitSize, 3));
        Assertions.assertThat(Utils.machineReadableByteCount("1 TiB")).isEqualTo(Math.pow(unitSize, 4));
        Assertions.assertThat(Utils.machineReadableByteCount("1 PiB")).isEqualTo(Math.pow(unitSize, 5));
        Assertions.assertThat(Utils.machineReadableByteCount("1 EiB")).isEqualTo(Math.pow(unitSize, 6));
    }
}
