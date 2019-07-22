# RocketMQ-Exporter Use example #

## 1 Start up NameServer and Broker ##
In order to use the RocketMQ Exporter, firstly make sure that the RocketMQ service is properly downloaded and started. Users can refer to the quick start of the RocketMQ master station for operation. Make sure the NameServer and Broker have started correctly.

## 2 Compile RocketMQ-Exporter ##
Users currently need to download the git source code and then compile it

```
git clone https://github.com/apache/rocketmq-exporter
cd rocketmq-exporter
mvn clean install
```

## 3 Configuration and startup ##
RocketMQ-Exporter has the following running options

operations | default value | meaning 
---|---|---
rocketmq.config.namesrvAddr | 127.0.0.1:9876 | MQ cluster nameSrv address 
rocketmq.config.webTelemetryPath | /metrics | metric collection path 
server.port | 5557 | HTTP service exposed port 

The above running options can be changed either in the configuration file after downloading the code or via the command line. The compiled jar package is called rocketmq-exporter-0.0.1-SNAPSHOT.jar, which can be run as follows.

```
java -jar rocketmq-exporter-0.0.1-SNAPSHOT.jar [--rocketmq.config.namesrvAddr="127.0.0.1:9876" ...]
```

## 4 Install Prometheus ##
Firstly go to Prometheus official download address: https://prometheus.io/download/ to download the Prometheus installation package, currently using linux installation as an example, the selected installation package is Prometheus-2.7.0-rc.1.linux-amd64.tar.gz, the Prometheus process can be started after the following steps.

```
tar -xzf prometheus-2.7.0-rc.1.linux-amd64.tar.gz
cd prometheus-2.7.0-rc.1.linux-amd64/
./prometheus --config.file=prometheus.yml --web.listen-address=:5555
```

The default listening port number of Prometheus is 9090. In order not  conflicts with other processes on the system, we reset the listening port number to 5555 in the startup parameters. Then go to website http://<server IP address>:5555 through  browser and users can verify whether the Prometheus has been successfully installed. Since the RocketMQ-Exporter process has been started, the data of RocketMQ-Exporter can be retrieved by Prometheus at this time. At this time, users only need to change the Prometheus configuration file to set the collection target to the url exposed by the RocketMQ Exporter. After changing the configuration file, restart the service.

## 5 Creating Grafana dashboard for RocketMQ ##

Prometheus' own metric display platform is not as good as Grafana. In order to  better show RocketMQ's metrics, Grafana can be used to show the metrics that Prometheus gets. Firstly go to the official website https://grafana.com/grafana/download to download installation file. Here is a  an example for binary file installation.

```
wget https://dl.grafana.com/oss/release/grafana-6.2.5.linux-amd64.tar.gz 
tar -zxvf grafana-6.2.5.linux-amd64.tar.gz
cd grafana-5.4.3/
```
Similarly, in order not to conflict with the ports of other processes, users can modify the listening port in the defaults.ini file in the conf directory. Currently, the listening port of the Grafana is changed to 55555, and then use the following command to start up.

```
./bin/grafana-server web
```

Then, by accessing http://<server IP address>:55555 through the browser, users can verify whether the Grafana has been successfully installed. The system default username and password are admin/admin. The first time users log in to the system, users will be asked to change the password. In addition, users need to set Grafana's data source to Prometheus. For the convenience of users, RocketMQ's dashboard configuration file has been uploaded to Grafana's official website  https://grafana.com/dashboards/10477/revisions. Users only need to download the configuration file and creating the RocketMQ dashboard by importing the configuration file into the Grafana.

