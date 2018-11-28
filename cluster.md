# How to roll a Spark Standalone cluster on GCP 
## Create disk image
1. Create new instance on GCP - https://console.cloud.google.com/compute/instances
1vCPU and 3.75 GB memory - micro does not work.
2. Initialize gcloud settings 
Run `gcloud init` and follow prompts for the project
3. SSH - copy SSH command from GCP
Ex. `gcloud compute --project "cpsc490-222104" ssh --zone "us-east1-b" "slave-1"`
4. Install Git on the GCP instance
`sudo apt update`
`sudo apt install git`
6. `git clone <your-version-of-spark>`
7. Install Java8 (https://tecadmin.net/install-java-8-on-debian/)
Note: the OpenJDK version does not work with Spark - you have to install the Oracle version
* `sudo apt-get install dirmngr`
* `echo deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main > /etc/apt/sources.list.d/java-8-debian.list`
* `echo deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main >> /etc/apt/sources.list.d/java-8-debian.list`
* `sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886`
* `sudo apt-get update`
* `sudo apt-get install oracle-java8-installer`
* Verify `java -version` says `java version ...` 
8. Install Scala
`sudo apt-get install scala`
9. Set Maven to use more memory
`export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m"`
10. Build Spark
`./build/mvn -DskipTests clean package`

## View Cluster UI
10. Create firewall for UI
`gcloud compute firewall-rules create spark-port --allow tcp:8080`
11. Run Google Chrome in a new terminal (leave this running)
`/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --proxy-server="socks5://localhost:8080" --user-data-dir=/tmp/gcp-instance`
12. Create an SSH Tunnel in a new terminal (leave this running)
`gcloud compute --project "cpsc490-222104" ssh --zone "us-east1-b" "slave-1" -- -D 8080 -N`
13. Open new terminal - SSH into your instance
`gcloud compute --project "cpsc490-222104" ssh --zone "us-east1-b" "slave-1"`
14. Start the master/UI
`./sbin/start-master.sh`
15. Go to `localhost:8080` on your local machine in the Chrome window that was opened

## Create slaves for your cluster
1. Click `Snapshots` in the Compute Engine Dashboard
2. Create Snapshot > Source Disk = instance where you installed everything
3. Create new instance with the Boot Disk as your Snapshot

## Submit Jobs to your cluster
1. `./sbin/start-slave.sh spark://IP:PORT` on your slave, where IP is the External IP of the master from the GCP Dashboard.
* You need at least 2 workers if you want to submit a job (one for the driver), so if you only have one slave then start a slave on the master node as well
`./bin/spark-submit --class org.apache.spark.examples.SparkPi --master spark://<Cluster IP>:7077 --deploy-mode cluster examples/target/scala-2.11/jars/spark-examples_2.11-3.0.0-SNAPSHOT.jar`

# How to Benchmark using spark-bench
## Benchmarking
1. `wget https://github.com/CODAIT/spark-bench/releases/download/v99/spark-bench_2.3.0_0.4.0-RELEASE_99.tgz` in master 
2. `tar -xvzf spark-bench_2.3.0_0.4.0-RELEASE_99.tgz`
3. `export SPARK_HOME=/home/anushreeagrawal/spark`
4. `export SPARK_MASTER_HOST=spark://instance-2.us-east1-b.c.cpsc490-222104.internal:7077`
5. `./bin/spark-bench.sh ../spark/benchmark_confs/<CONF_NAME>`
Notes: Minimium memory requirements is probably around 265.4 MB, memory cannot include decimals in the conf file, benchmark-output file needs extension.



