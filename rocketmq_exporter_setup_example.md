##                            RocketMQ-Exporter使用示例

1 启动NameServer和Broker

要验证RocketMQ的Spring-Boot客户端，首先要确保RocketMQ服务正确的下载并启动。可以参考RocketMQ主站的快速开始来进行操作。确保启动NameServer和Broker已经正确启动。

2 编译RocketMQ-Exporter
用户当前使用，需要自行下载git源码编译

```
git clone https://github.com/apache/rocketmq-exporter
cd rocketmq-exporter
mvn clean install
```
3 配置和运行
RocketMQ-Exporter 有如下的运行选项
选项 | 默认值 | 含义
---|---|---
rocketmq.config.namesrvAddr | 127.0.0.1:9876 | MQ集群的nameSrv地址
rocketmq.config.webTelemetryPath | /metrics | 指标搜集路径
server.port | 5557 | HTTP服务暴露端口
以上的运行选项既可以在下载代码后在配置文件中更改，也可以通过命令行来设置。
编译出来的jar包就叫rocketmq-exporter-0.0.1-SNAPSHOT.jar，可以通过如下的方式来运行。

```
java -jar rocketmq-exporter-0.0.1-SNAPSHOT.jar [--rocketmq.config.namesrvAddr="127.0.0.1:9876" ...]
```

4 安装Prometheus
首先到Prometheus官方下载地址:[https://prometheus.io/download/](https://prometheus.io/download/)去下载Prometheus安装包，当前以linux的安装为例，选择的安装包为
prometheus-2.7.0-rc.1.linux-amd64.tar.gz，经过如下的操作步骤就可以启动prometheus进程。

```
tar -xzf prometheus-2.7.0-rc.1.linux-amd64.tar.gz
cd prometheus-2.7.0-rc.1.linux-amd64/
./prometheus --config.file=prometheus.yml --web.listen-address=:5555
```
prometheus 默认监听端口号为9090，为了不与系统上的其它进程监听端口冲突，我们在启动参数里面重新设置了监听端口号为5555。然后通过浏览器访问http://<服务器IP地址>:5555,就可以验证prometheus是否已成功安装。由于RocketMQ-Exporter进程已启动，这个时候可以通过prometheus来抓取RocketMQ-Exporter的数据，这个时候只需要更改prometheus启动的配置文件即可。
整体配置文件如下：

```
# my global config
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
更改配置文件后，重启即可。

5 Grafana dashboard for RocketMQ

Prometheus自身的指标展示平台没有当前流行的展示平台Grafana好， 为了更好的展示RocketMQ的指标，可以使用Grafana来展示Prometheus获取的指标。首先到官网去下载[https://grafana.com/grafana/download](https://grafana.com/grafana/download), 这里仍以二进制文件安装为例进行介绍。

```
wget https://dl.grafana.com/oss/release/grafana-6.2.5.linux-amd64.tar.gz 
tar -zxvf grafana-6.2.5.linux-amd64.tar.gz
cd grafana-5.4.3/
```
同样为了不与其它进程的端口冲突，可以修改conf目录下的defaults.ini文件的监听端口，当前将这个grafana的监听端口改为55555，然后使用如下的命令启动即可

```
./bin/grafana-server web
```
然后通过浏览器访问http://<服务器IP地址>:55555,就可以验证grafana是否已成功安装。系统默认用户名和密码为admin/admin，第一次登陆系统会要求修改密码，修改密码后登陆。为了便于用户使用，当前已将RocketMQ的dashboard配置文件上传到Grafana的官网，地址为[https://grafana.com/dashboards/10477/revisions](https://grafana.com/dashboards/10477/revisions)去下载dashboard配置文件，用户只需要到下载配置文件，并将配置文件导入dashboard即可创建RocketMQ的dashboard




















