**Starting**

Starting 3 processes on 192.168.1.30 at 17:30

``rm log.txt ; for run in {1..3} ; do java -jar target/hazelcast-1.0-SNAPSHOT.jar 192.168.1.30 $run 17 30 >> log.txt & ; done``

**Killing processes**

``ps -ef | grep "java -jar" | awk '{print $2}' | xargs kill``