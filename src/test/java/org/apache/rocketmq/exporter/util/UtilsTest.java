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
