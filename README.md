# Apache RocketMQ Exporter for Prometheus.
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://api.travis-ci.com/apache/rocketmq-exporter.svg?branch=master)](https://travis-ci.com/github/apache/rocketmq-exporter)
[![codecov](https://codecov.io/gh/apache/rocketmq-exporter/branch/master/graph/badge.svg?token=jS7OXwJW5Q)](https://codecov.io/gh/apache/rocketmq-exporter)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/rocketmq-exporter.svg)](http://isitmaintained.com/project/apache/rocketmq-exporter "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/rocketmq-exporter.svg)](http://isitmaintained.com/project/apache/rocketmq-exporter "Percentage of issues still open")
![Twitter Follow](https://img.shields.io/twitter/follow/ApacheRocketMQ?style=social)


Table of Contents
-----------------
-	[Compatibility](#compatibility)
-   [Configuration](#configuration)
-   [Build](#build)
	-   [Build Binary](#build-binary)
	-   [Build Docker Image](#build-docker-image)
-   [Run](#run)
	-   [Run Binary](#run-binary)
	-   [Run Docker Image](#run-docker-image)
-   [Metrics](#metrics)
	-   [Broker](#broker)
	-   [Producer](#producer)
	-   [Consumer Groups](#consumer-groups)
	-   [Consumer](#consumer)
-   [Grafana Dashboard](#grafana-dashboard)
-   [Quick Start](#quick-start)

Compatibility
-------------

Support [Apache RocketMQ](https://rocketmq.apache.org) version 4.3.2 (and later).


Configuration
---

This image is configurable using different properties, see ``application.properties`` for a configuration example.

| name                           | Default            | Description                                        |
| -----------------------------------|--------------------|----------------------------------------------------|
| `rocketmq.config.namesrvAddr`      |  127.0.0.1:9876 |name server address  for  broker cluster            |
| `rocketmq.config.webTelemetryPath` | /metrics           |Path under which to expose metrics                  |
| `server.port`                      | 5557               |Address to listen on for web interface and telemetry|
| `rocketmq.config.rocketmqVersion`  | V4_3_2             |rocketmq broker version                             |


Build
-------

### Build Binary

```shell
mvn clean install
```

### Build Docker Image

```shell
mvn package -Dmaven.test.skip=true docker:build
```

Run
---

### Run Binary

```shell
java -jar target/rocketmq-exporter-0.0.2-SNAPSHOT.jar
```

### Run Docker Image

```
docker container run -itd --rm  -p 5557:5557  docker.io/rocketmq-exporter
```


Metrics
-------

Documents about exposed Prometheus metrics.

### Broker 

**Metrics details**

| Name         | Exposed information                                  |
| ------------ | ---------------------------------------------------- |
| `rocketmq_broker_tps` | Broker produces the number of messages per second |
| `rocketmq_broker_qps` | Broker consumes messages per second |

**Metrics output example**

```txt
# HELP rocketmq_broker_tps BrokerPutNums
# TYPE rocketmq_broker_tps gauge
rocketmq_broker_tps{cluster="MQCluster",broker="broker-a",} 7.0
rocketmq_broker_tps{cluster="MQCluster",broker="broker-b",} 7.0
# HELP rocketmq_broker_qps BrokerGetNums
# TYPE rocketmq_broker_qps gauge
rocketmq_broker_qps{cluster="MQCluster",broker="broker-a",} 8.0
rocketmq_broker_qps{cluster="MQCluster",broker="broker-b",} 8.0
```

### Producer

**Metrics details**

| Name                | Exposed information                                |
| ------------------- | -------------------------------------------------- |
| `rocketmq_producer_tps`      | The number of messages produced per second per topic |
| `rocketmq_producer_message_size` | The size of a message produced per second by a topic (in bytes) |
| `rocketmq_producer_offset`   | The progress of a topic's production message |

**Metrics output example**

```txt
# HELP rocketmq_producer_tps TopicPutNums
# TYPE rocketmq_producer_tps gauge
rocketmq_producer_tps{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 7.0
rocketmq_producer_tps{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 7.0
# HELP rocketmq_producer_message_size TopicPutMessageSize
# TYPE rocketmq_producer_message_size gauge
rocketmq_producer_message_size{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 1642.0
rocketmq_producer_message_size{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 1638.0
# HELP rocketmq_producer_offset TopicOffset
# TYPE rocketmq_producer_offset counter
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="TBW102",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",} 1878633.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",} 3843787.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_20190304",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="BenchmarkTest",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_20190305",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="MQCluster",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",} 2798195.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="BenchmarkTest",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",} 1459666.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="MQCluster",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="SELF_TEST_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="OFFSET_MOVED_EVENT",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="broker-b",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="broker-a",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="SELF_TEST_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="RMQ_SYS_TRANS_HALF_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_20190305",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="OFFSET_MOVED_EVENT",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="RMQ_SYS_TRANS_HALF_TOPIC",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-b",topic="TBW102",} 0.0
rocketmq_producer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_20190304",} 0.0

```

### Consumer Groups

**Metrics details**

| Name                                                         | Exposed information                                          |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| `rocketmq_consumer_tps`                                      | The number of messages consumed per second by a consumer group |
| `rocketmq_consumer_message_size`                             | The size of the message consumed by the consumer group per second (in bytes) |
| `rocketmq_consumer_offset`                                   | Progress of consumption message for a consumer group         |
| `rocketmq_group_get_latency`                                 | Consumer latency on some topic for one queue                 |
| `rocketmq_group_get_latency_by_storetime `                   | Consumption delay time of a consumer group                   |
| `rocketmq_message_accumulation`| How far Consumer offset lag behind |

**Metrics output example**

```txt
# HELP rocketmq_consumer_tps GroupGetNums
# TYPE rocketmq_consumer_tps gauge
rocketmq_consumer_tps{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 7.0
rocketmq_consumer_tps{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 7.0
# HELP rocketmq_consumer_message_size GroupGetMessageSize
# TYPE rocketmq_consumer_message_size gauge
rocketmq_consumer_message_size{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1638.0
rocketmq_consumer_message_size{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1642.0
# HELP rocketmq_consumer_offset GroupOffset
# TYPE rocketmq_consumer_offset counter
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 1462030.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 3843787.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 2800569.0
rocketmq_consumer_offset{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 1878633.0
# HELP rocketmq_group_get_latency GroupGetLatency
# TYPE rocketmq_group_get_latency gauge
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="0",} 0.05
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="1",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="7",} 0.05
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="6",} 0.01
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="3",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="7",} 0.03
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="4",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="5",} 0.03
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="6",} 0.01
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="2",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="3",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="0",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="4",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="1",} 0.03
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="5",} 0.0
rocketmq_group_get_latency{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="2",} 0.0
# HELP rocketmq_group_get_latency_by_storetime GroupGetLatencyByStoreTime
# TYPE rocketmq_group_get_latency_by_storetime gauge
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 3215.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-a",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 0.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-a",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",} 3232.0
rocketmq_group_get_latency_by_storetime{cluster="MQCluster",broker="broker-b",topic="DEV_TID_tfq",group="DEV_CID_cfq",} 0.0
```

### Consumer

**Metrics details**

| Name                                     | Exposed information                                |
| ---------------------------------------- | -------------------------------------------------- |
| `rocketmq_client_consume_fail_msg_count` | The number of messages consumed fail in one hour   |
| `rocketmq_client_consume_fail_msg_tps`   | The number of messages consumed fail per second    |
| `rocketmq_client_consume_ok_msg_tps`     | The number of messages consumed success per second |
| `rocketmq_client_consume_rt`             | The average time of consuming every message        |
| `rocketmq_client_consumer_pull_rt `      | The average time of pulling every message          |
| `rocketmq_client_consumer_pull_tps`      | The number of messages pulled by client per second |

**Metrics output example**

```
# HELP rocketmq_client_consume_fail_msg_count consumerClientFailedMsgCounts
# TYPE rocketmq_client_consume_fail_msg_count gauge
rocketmq_client_consume_fail_msg_count{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 0.0
rocketmq_client_consume_fail_msg_count{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
# HELP rocketmq_client_consume_fail_msg_tps consumerClientFailedTPS
# TYPE rocketmq_client_consume_fail_msg_tps gauge
rocketmq_client_consume_fail_msg_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 0.0
rocketmq_client_consume_fail_msg_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
# HELP rocketmq_client_consume_ok_msg_tps consumerClientOKTPS
# TYPE rocketmq_client_consume_ok_msg_tps gauge
rocketmq_client_consume_ok_msg_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 9.833333333333334
rocketmq_client_consume_ok_msg_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
# HELP rocketmq_client_consume_rt consumerClientRT
# TYPE rocketmq_client_consume_rt gauge
rocketmq_client_consume_rt{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 0.0576271186440678
rocketmq_client_consume_rt{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
# HELP rocketmq_client_consumer_pull_rt consumerClientPullRT
# TYPE rocketmq_client_consumer_pull_rt gauge
rocketmq_client_consumer_pull_rt{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 402.6186440677966
rocketmq_client_consumer_pull_rt{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
# HELP rocketmq_client_consumer_pull_tps consumerClientPullTPS
# TYPE rocketmq_client_consumer_pull_tps gauge
rocketmq_client_consumer_pull_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="topic_one",} 9.833333333333334
rocketmq_client_consumer_pull_tps{clientAddr="172.16.116.51:52178",clientId="10.0.4.0@120367",group="consumer_one",topic="%RETRY%consumer_one",} 0.0
```

Grafana Dashboard
-------

Grafana Dashboard ID: 10477, name: RocketMQ Exporter Overview.
For details of the dashboard please see [RocketMQ Exporter Overview](https://grafana.com/dashboards/10477).

Quick Start
-------------
This [guide](./rocketmq_exporter_quickstart.md) will tell you how to build and run rocketmq exporter with Prometheus and Grafana Integration from scratch.
