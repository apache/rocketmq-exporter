# Quick Start #

## Start up NameServer and Broker
To use RocketMQ Exporter, first make sure the RocketMQ is downloaded and started correctly. Users can refer to [quick start](http://rocketmq.apache.org/docs/quick-start/) ensure that the service starts properly.

## Build and Run RocketMQ Exporter


## Install Prometheus
Download [Prometheus installation package](https://prometheus.io/download/) and install it.

```
tar -xzf prometheus-2.7.0-rc.1.linux-amd64.tar.gz
cd prometheus-2.7.0-rc.1.linux-amd64/
./prometheus --config.file=prometheus.yml --web.listen-address=:5555
```

The content of prometheus.yml is as follows.

```
# Global config
global:
   scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
   evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
   # scrape_timeout is set to the global default (10s).
 
 
 # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
 rule_files:
   # - "first_rules.yml"
   # - "second_rules.yml"
   

 scrape_configs:
   - job_name: 'prometheus'
     static_configs:
     - targets: ['localhost:5555']
   
   
   - job_name: 'exporter'
     static_configs:
     - targets: ['localhost:5557']
```



## Integration with Grafana Dashboard

Download Grafana installation package and install it.


```
wget https://dl.grafana.com/oss/release/grafana-6.2.5.linux-amd64.tar.gz 
tar -zxvf grafana-6.2.5.linux-amd64.tar.gz
cd grafana-5.4.3/
```
The user can modify the listener port in the default.ini file in the conf directory. Currently, the Grafana listener port changes to 55555 and starts with the following command.

```
./bin/grafana-server web
```

The user can verify that Grafana has been successfully installed by visiting http:// server IP :55555 in a browser. The default system username and password is admin/admin. The first time the user logs into the system, the system will ask the user to change the password. In addition, the user needs to set the data source of Grafana as Prometheus. If Prometheus was launched by the user as above, the data source address will now be http:// server IP :5555. For the convenience of users, RocketMQ dashboard configuration file has been uploaded to the Grafana's official website https://grafana.com/dashboards/10477/revisions. The user simply downloads the configuration file and imports the Grafana.

## Configure alarms in Prometheus

The user wants to configure monitoring alerts, there are two things the user should do.
First, modify the Prometheus configuration file. And add the following configuration.

```
rule_files:
  - /etc/prometheus/rules/*.rules
```

Secondly, create an alert file in the directory /etc/prometheus/rules/. The content of rules file will be like as follows. For more details, please refer to the file [example.rules](./example.rules)

```
groups:
- name: GaleraAlerts
  rules:
  - alert: RocketMQClusterProduceHigh
    expr: sum(rocketmq_producer_tps) by (cluster) >= 10
    for: 3m
    labels:
      severity: warning
    annotations:
      description: '{{$labels.cluster}} Sending tps too high.'
      summary: cluster send tps too high
  - alert: RocketMQClusterProduceLow
    expr: sum(rocketmq_producer_tps) by (cluster) < 1
    for: 3m
    labels:
      severity: warning
    annotations:
      description: '{{$labels.cluster}} Sending tps too low.'
      summary: cluster send tps too low
```


