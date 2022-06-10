/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xdnc.dhcpv6.communication.dhcpadapter;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.XynaThreadPoolExecutor;



public class DHCPAdapterSender implements StatisticsReporterLegacy {

  private ThreadPoolExecutor tpe =
      new XynaThreadPoolExecutor(1, 1, 20, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(500), Executors
          .defaultThreadFactory(), "DHCPAdapterSender", true);

  private Socket socket;
  private OutputStream out;
  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPAdapterSender.class);
  
  private AtomicLong failedcalls = new AtomicLong(0);

  private class SendRunnable extends XynaRunnable {

    private byte[] packet;


    public SendRunnable(byte[] packet) {
      this.packet = packet;
    }


    public void sendPacket() throws Exception
    {

      out.write(packet);
      out.flush();
      if(logger.isDebugEnabled())logger.debug("XXX Packet sent...");
      
    }
    
    public void tryTransmission() throws Exception
    {
      if(socket!=null){
        sendPacket();
      }
      else
      {
        if(logger.isDebugEnabled())logger.debug("XXX No Socket! Trying to make socket ...");
        socket = new Socket(InetAddress.getLocalHost(), 2601);
        out = socket.getOutputStream();
        if(socket==null)
        {
          if(logger.isDebugEnabled())logger.debug("XXX Could not create Socket! Got no connection to DHCPAdapter!");
        }
        else
        {
          sendPacket();
        }
      }

    }
    
    public void run() {
      try {
        if(logger.isDebugEnabled())logger.debug("XXX Ringbuffer component sending packet ...");
        
        tryTransmission();
        
        
      } 
      catch (Exception e) 
      {
        if(logger.isDebugEnabled())logger.debug("Exception while trying to send packet to DHCPAdapter: ",e);
        if(logger.isDebugEnabled())logger.debug("Trying one more time: ");

        
        try {
          socket=null;
          tryTransmission();
        }
        catch (Exception e1) {
          logger.warn("Could not establish connection to DHCPAdapter! Packet discarded! Exception: "+e);
          failedcalls.incrementAndGet();
        }
        
      }
    }

  }

  private class CheckSocketRunnable implements Runnable {

    public void run() {
      try {
        while(true)
        {
          while(socket.getInputStream().read()!=-1){
            Thread.sleep(100);
          }
          socket.close();
          socket=null;
        }
      }
      catch (Exception e) {
        if (logger.isDebugEnabled())
          logger.debug("Socket unexpected closed!", e);
        socket = null;
      }

    }


  }


  public DHCPAdapterSender() {
    //addStatistics
    XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().registerNewStatistic("DHCPAdapterSender", this);
    
    try {
      socket = new Socket(InetAddress.getLocalHost(), 2601);
      out = socket.getOutputStream();
      Thread t = new Thread(new CheckSocketRunnable());
      t.setDaemon(true);
      t.start();

    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("Failed to create Socket.");
    }

  }
  
  public void shutdown() {
    tpe.shutdown();
    try {
      socket.close();
    }
    catch (IOException e) {
      if(logger.isDebugEnabled())logger.debug("Socket did not close correctly: "+e.getStackTrace());
    }
    
    //removeStatistics
    XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().unregisterStatistics("DHCPAdapterSender");
  }

  public long getCallsCount()
  {
    return ((XynaThreadPoolExecutor)tpe).getTaskCount();
  }

  public long getActiveCalls()
  {
    return ((XynaThreadPoolExecutor)tpe).getActiveCount();
  }

  
  public long getRejectedCalls()
  {
    return ((XynaThreadPoolExecutor)tpe).getRejectedTasks();
  }
  
  public long getFailedCalls()
  {
    return failedcalls.get();
  }


  public long getSuccessfulCalls()
  {
    return ((XynaThreadPoolExecutor)tpe).getCompletedTaskCount();
  }

  
  /**
   * asynchron, mit ringbuffer
   */
  public void sendToDHCPAdapter(byte[] packetcontent) {
    if(logger.isDebugEnabled())logger.debug("XXX trying to send packet to DHCPAdapter (in ringbuffer) ...");

    try
    {
      if(packetcontent!=null)tpe.execute(new SendRunnable(packetcontent));
    }
    catch (Exception e)
    {
      logger.warn("Error creating new DHCPAdapter Sender! Packet discarded! Exception: "+e);
    }
  }
    

  public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {
    StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[5];
    report[0] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getActiveCalls();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of active calls";
      }
    };

    report[1] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getCallsCount();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of calls scheduled for execution";
      }
    };

    report[2] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getFailedCalls();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of failed calls";
      }
    };

    report[3] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getRejectedCalls();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of rejected calls";
      }
    };

    report[4] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return getSuccessfulCalls();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of completed calls (either win or fail)";
      }
    };

    return report;
  }

}
