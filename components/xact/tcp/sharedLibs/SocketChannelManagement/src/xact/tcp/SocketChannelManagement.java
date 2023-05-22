/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package xact.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
 * Written and tested with CONTEX-Usecase in mind:
 * Single user that is in most cases sync waiting for a response
 */
public class SocketChannelManagement {
  
  private final static long DEFAULT_CHANNEL_CREATION_TIMEOUT = 3000;

  private static volatile SocketChannelManagement instance;
  
  private final transient ConcurrentMap<String, SocketChannelComposition> channelCache;
  
  private final ScheduledExecutorService keepAliveExecutor;
  
  private SocketChannelManagement() {
    keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
    channelCache = new ConcurrentHashMap<String, SocketChannelComposition>();
  }
  
  
  public static SocketChannelManagement getInstance() {
    if (instance == null) {
      synchronized (SocketChannelManagement.class) {
        if (instance == null) {
          instance = new SocketChannelManagement();
        }
      }
    }
    return instance;
  }
  
  
  public SocketChannel getSocketChannel(String name) {
    long timeout = System.currentTimeMillis() + DEFAULT_CHANNEL_CREATION_TIMEOUT;
    SocketChannelComposition channel = channelCache.get(name);
    while (channel == null) {
      try {
        Thread.sleep(DEFAULT_CHANNEL_CREATION_TIMEOUT / 10);
      } catch (InterruptedException e) {
        // ntbd
      }
      channel = channelCache.get(name);
      if (channel == null && System.currentTimeMillis() > timeout) {
        throw new RuntimeException("Channel was not created within timeout");
      }
    }
    try {
      while (!channel.getSocketChannel().finishConnect()) {
        try {
          Thread.sleep(DEFAULT_CHANNEL_CREATION_TIMEOUT / 10);
        } catch (InterruptedException e) {
          // ntbd
        }
        if (System.currentTimeMillis() > timeout) {
          throw new RuntimeException("Channel was not connected within timeout");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return channel.getSocketChannel();
  }
  
  
  public void createSocketChannel(SocketChannelCreationParameter sccp) throws IOException {
    sccp.validate();
    if (channelCache.containsKey(sccp.name)) {
      throw new RuntimeException("Channel already registered");
    }
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    SocketChannelComposition composition = new SocketChannelComposition(channel);
    if (channelCache.putIfAbsent(sccp.name, composition) == null) { 
      channel.connect(sccp.address);
      ScheduledFuture<?> keepAlive = null;
      if (sccp.hasKeepAliveHandling()) {
        keepAlive = keepAliveExecutor.scheduleAtFixedRate(new AliveKeeper(channel, sccp.keepAliveHandler), sccp.keepAliveInterval, sccp.keepAliveInterval, sccp.keepAliveUnit);
        composition.setKeepAlive(keepAlive);
      }
      channel.register(sccp.selector, sccp.ops);
      long timeout = System.currentTimeMillis() + DEFAULT_CHANNEL_CREATION_TIMEOUT;
      try {
        while (!channel.finishConnect()) {
          try {
            Thread.sleep(DEFAULT_CHANNEL_CREATION_TIMEOUT / 10);
          } catch (InterruptedException e) {
            // ntbd
          }
          if (System.currentTimeMillis() > timeout) {
            throw new RuntimeException("Channel was not connected within timeout");
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("Channel already registered");
    }
  }
  
  
  public void addKeepAlive(SocketChannelCreationParameter sccp) {
    SocketChannelComposition composition = channelCache.get(sccp.name);
    if (composition != null) {
      if (composition.keepAlive != null) {
        composition.keepAlive.cancel(false);
        while (!composition.getKeepAlive().isDone()) { // TODO timeout
          try {
            Thread.sleep(DEFAULT_CHANNEL_CREATION_TIMEOUT/10);
          } catch (InterruptedException e) {
            // ntbd
          }
        }
      }
      ScheduledFuture<?> keepAlive = keepAliveExecutor.scheduleAtFixedRate(new AliveKeeper(composition.getSocketChannel(), sccp.keepAliveHandler), sccp.keepAliveInterval, sccp.keepAliveInterval, sccp.keepAliveUnit);
      composition.setKeepAlive(keepAlive);
    }
  }
  
  
  public void destroySocketChannel(String name) throws IOException {
    SocketChannelComposition composition = channelCache.remove(name);
    if (composition != null) {
      if (composition.getKeepAlive() != null) {
        composition.getKeepAlive().cancel(false);
        while (!composition.getKeepAlive().isDone()) { // TODO timeout
          try {
            Thread.sleep(DEFAULT_CHANNEL_CREATION_TIMEOUT/10);
          } catch (InterruptedException e) {
            // ntbd
          }
        }
      }
      composition.socketChannel.close();
    }
  }
  
  
  public void shutdown() {
    for (SocketChannelComposition composition : channelCache.values()) {
      try {
        composition.getKeepAlive().cancel(true);
        composition.getSocketChannel().close();
      } catch (IOException e) {
        // TODO log
      }
    }
    keepAliveExecutor.shutdown();
    instance = null;
  }
  
  
  private static class SocketChannelComposition {
    
    private final SocketChannel socketChannel;
    private ScheduledFuture<?> keepAlive;
    
    
    SocketChannelComposition(SocketChannel socketChannel) {
      this.socketChannel = socketChannel;
    }
    
    
    public SocketChannel getSocketChannel() {
      return socketChannel;
    }

    
    public ScheduledFuture<?> getKeepAlive() {
      return keepAlive;
    }
    
    
    public void setKeepAlive(ScheduledFuture<?> keepAlive) {
      this.keepAlive = keepAlive;
    }
    
  }
  
  
  private static class AliveKeeper implements Runnable {
    
    private final SocketChannel channel;
    private final KeepAliveHandler handler;
    
    AliveKeeper(SocketChannel channel, KeepAliveHandler handler) {
      this.channel = channel;
      this.handler = handler;
    }

    public void run() {
      handler.keepAlive(channel);
    }
    
  }
  
  
  public static void main(String[] args) throws IOException, InterruptedException {
    SocketChannelManagement scm = SocketChannelManagement.getInstance();
    try {
      Selector select = Selector.open();
      scm.createSocketChannel(SocketChannelCreationParameter.create()
                              .address(new InetSocketAddress("127.0.0.1", 1789))
                              .name("baum")
                              .selector(select, SelectionKey.OP_READ)
                              .keepAlive(new StaticMessageKeepAliveHandler("live!"), 5, TimeUnit.SECONDS));
      SocketChannel channel = scm.getSocketChannel("baum");
      channel.write(ByteBuffer.wrap("ECHO baum".getBytes()));
      Thread.sleep(1000);
      printSelector(select);
      channel.write(ByteBuffer.wrap("NO ECHO".getBytes()));
      Thread.sleep(1000);
      channel.write(ByteBuffer.wrap("ECHO argel".getBytes()));
      Thread.sleep(1000);
      printSelector(select);
      Thread.sleep(11000);
      printSelector(select);
      channel.write(ByteBuffer.wrap("CLOSE".getBytes()));
    } finally {
      scm.shutdown();
    }
  }
  
  
  private static void printSelector(Selector selector) throws IOException {
    selector.select(500);
    System.out.println("printSelect for key set: " + selector.selectedKeys());
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    for (SelectionKey key : selector.selectedKeys()) {
      SocketChannel channel = (SocketChannel) key.channel();
      buffer.clear();
      if (channel.read(buffer) != -1) {
        buffer.flip();
        String line = new String(buffer.array(), buffer.position(), buffer.remaining());
        System.out.println(line);
      }
    }
  }
  
}
