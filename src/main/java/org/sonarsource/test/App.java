package org.sonarsource.test;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.*;

import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.group.DistributedGroup;
import io.atomix.group.LocalMember;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
*/
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Hello world!
 */
public class App {
  /**
   * @param args :
   *             * IP
   *             * Port increment
   *             * Hour
   *             * Minute
   * @throws InterruptedException
   * @throws SocketException
   */
  public static void main(String[] args) throws InterruptedException, SocketException {
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    final String ip = args[0];
    final int port = Integer.parseInt(args[1]);
    ZonedDateTime launchTime = ZonedDateTime.now(Clock.system(ZoneId.of("Europe/Paris")))
        .withHour(Integer.parseInt(args[2]))
        .withMinute(Integer.parseInt(args[3]))
        .withSecond(0);

    executorService.schedule(
        () -> testHazelcast(ip, port),
        Duration.between(ZonedDateTime.now(), launchTime).toNanos(),
        TimeUnit.NANOSECONDS
    );
  }

  static private void testHazelcast(String ip, int port) {
    Config cfg = new Config();
    cfg.setInstanceName("sonarqube")
        .getNetworkConfig()
        .setPort(5900 + port)
        .setInterfaces(new InterfacesConfig().addInterface(ip).setEnabled(true))
        .getJoin()
        .setTcpIpConfig(
            new TcpIpConfig()
                .addMember("192.168.1.245")
                .addMember("192.168.1.30")
                .setEnabled(true)
        );
    /*cfg.setProperty("hazelcast.initial.wait.seconds", "10");
    cfg.setProperty("hazelcast.max.join.seconds", "10");
    cfg.setProperty("hazelcast.max.no.master.confirmation.seconds", "10");
    cfg.setProperty("hazelcast.max.wait.seconds.before.join", "10");
    cfg.setProperty("hazelcast.io.input.thread.count", "10");
    cfg.setProperty("hazelcast.io.output.thread.count", "10");
    cfg.setProperty("hazelcast.io.thread.count", "20");
    cfg.setProperty("hazelcast.member.list.publish.interval.seconds", "10");
    cfg.setProperty("hazelcast.phone.home.enabled", "false");
    cfg.setProperty("hazelcast.wait.seconds.before.join", "10"); */
    cfg.setProperty("hazelcast.socket.bind.any", "false");
    cfg.getNetworkConfig().setOutboundPorts(Arrays.asList(0));
    cfg.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
    cfg.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    cfg.getPartitionGroupConfig().setEnabled(false);

    HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
    IAtomicReference<String> leader = instance.getAtomicReference("leader");

    if (leader.get() == null) {
      ILock lock = instance.getLock("leader");
      lock.lock();
      try {
        if (leader.get() == null) {
          leader.set(instance.getLocalEndpoint().getUuid());
        }
      } finally {
        lock.unlock();
      }
    }
    System.out.println(instance.getCluster().getLocalMember().getUuid() + "| Leader : " + leader.get() + "Cluster state : " + instance.getCluster().getClusterState() + " size : " + instance.getCluster().getMembers().size());

    try {
      Thread.sleep(5 * 60 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    instance.shutdown();
    System.exit(0);
  }
}
