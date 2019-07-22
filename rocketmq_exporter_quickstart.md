# RocketMQ Exporter Quick Start #

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

The default listening port number of Prometheus is 9090. In order not  conflict with other processes on the system, we reset the listening port number to 5555 in the startup parameters. Then go to website http:// sever ip:5555 through  browser and users can verify whether the Prometheus has been successfully installed. Since the RocketMQ-Exporter process has been started, the data of RocketMQ-Exporter can be retrieved by Prometheus at this time. At this time, users only need to change the Prometheus configuration file to set the collection target to the URL address exposed by the RocketMQ Exporter. After changing the configuration file, restart the service. The content of prometheus.yml will be like as follows:

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



## Create Grafana dashboard for RocketMQ

Prometheus' own metric display platform is not as good as Grafana. In order to  better show RocketMQ's metrics, Grafana can be used to show the metrics that Prometheus gets. Download and install it as the following.

```
wget https://dl.grafana.com/oss/release/grafana-6.2.5.linux-amd64.tar.gz 
tar -zxvf grafana-6.2.5.linux-amd64.tar.gz
cd grafana-5.4.3/
```
Similarly, in order not to conflict with the ports of other processes, users can modify the listening port in the defaults.ini file in the conf directory. Currently, the listening port of the Grafana is changed to 55555, and then use the following command to start up.

```
./bin/grafana-server web
```

Then, by accessing http:// server ip:55555 through the browser, users can verify whether the Grafana has been successfully installed. The system default username and password are admin/admin. The first time users log in to the system, users will be asked to change the password. In addition, users need to set Grafana's data source to Prometheus. If user have started up Prometheus like above, now the data source address will be  http:// server ip:5555. For the convenience of users, RocketMQ's dashboard configuration file has been uploaded to Grafana's official website  https://grafana.com/dashboards/10477/revisions. Users only need to download the configuration file and creating the RocketMQ dashboard by importing the configuration file into the Grafana.

## Configure alarms in Prometheus

If users want to configure alarms,there are two things users should do. 
Firstly, modify the Prometheus configuration file prometheus.yml and add the following configuration: 

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


