/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger;



import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.NetConfNotificationReceiverSharedLib;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;


public class NetConfNotificationReceiverTriggerConnection extends TriggerConnection {

  //private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTriggerConnection.class);


  public NetConfNotificationReceiverTriggerConnection() {
  }

  private String client_hello = ""
      + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
      + "  <capabilities>\n"
      + "    <capability>\n"
      //+ "      urn:ietf:params:netconf:base:1.1\n"
      + "      urn:ietf:params:netconf:base:1.0\n"
      + "    </capability>\n"
      + "  </capabilities>\n"
      + "</hello>\n"
      + "]]>]]>";

  private String client_goodbye = ""
        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<rpc message-id=\"104\"\n"
        + "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
        + "  <close-session/>\n"
        + "</rpc>\n"
        + "]]>]]>";

  private String subscription_notification = ""
        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"102\">\n"
        + "   <create-subscription xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\"/>\n"
        + "</rpc>\n"
        + "]]>]]>";

  private static String subscription_notification_placeholder = ""
      + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"110\">\n"
      + "   <create-subscription xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">\n"
      + "     <startTime>PLACEHOLDER</startTime>\n"
      + "   </create-subscription>\n"
      + "</rpc>\n"
      + "]]>]]>";

  /*
  private String notification = ""
        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"103\">\n"
        + "  <action xmlns=\"urn:ietf:params:xml:ns:yang:1\">\n"
        + "    <alarms xmlns=\"urn:ietf:params:xml:ns:yang:ietf-alarms\">\n"
        + "      <control><notify-all-raise-and-clear xmlns=\"http://www.adtran.com/ns/yang/adtran-ietf-alarms-ns-test\"/></control>\n"
        + "    </alarms>\n"
        + "  </action>\n"
        + "</rpc>\n"
        + "]]>]]>";
*/

  private String get_serial_num = ""
       + "<nc:rpc xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"201\"><nc:get>\n"
       + "    <nc:filter>\n"
       + "      <hardware xmlns=\"urn:ietf:params:xml:ns:yang:ietf-hardware\">\n"
       + "        <component>\n"
       + "          <class xmlns:ianahw=\"urn:ietf:params:xml:ns:yang:iana-hardware\">ianahw:chassis</class>\n"
       + "          <serial-num/>\n"
       + "        </component>\n"
       + "        <component>\n"
       + "          <class xmlns:ianahw=\"urn:ietf:params:xml:ns:yang:iana-hardware\">ianahw:module</class>\n"
       + "          <parent/>\n"
       + "          <serial-num/>\n"
       + "        </component>\n"
       + "      </hardware>\n"
       + "    </nc:filter>\n"
       + "  </nc:get>\n"
       + "</nc:rpc>\n"
       + "]]>]]>";

  private String delimiter_regex="([\\w\\W].*?)]]>]]>";
  private String delimiter_serialnum_1="(<nc:rpc-reply[\\w\\W].*?message-id=\"201\"[\\w\\W].*?<\\/nc:rpc-reply>)";
  private String delimiter_serialnum_2="[\\w\\W].*?<serial-num>([\\w\\W].*?)<\\/serial-num>[\\w\\W].*?";
  private String delimiter_capinterleave="<hello[\\w\\W].*?(urn:ietf:params:netconf:capability:interleave:1.0)[\\w\\W].*?<\\/hello>";
  private String delimiter_neconfhello="<hello[\\w\\W].*?<\\/hello>";

  private String delimiter_SubscriptionWithStartTime="(<rpc-reply[\\w\\W].*?message-id=\"110\"[\\w\\W].*?<\\/rpc-reply>)";
  private String delimiter_SubscriptionWithStartTime_Okay="(<rpc-reply[\\w\\W].*?<ok\\/>[\\w\\W].*?rpc-reply>)";

  private String buffer;
  private boolean clearbuffer;
  private long buffer_updatetime;
  private long command_delay_before;
  private long command_delay_after;
  private long buffer_maxlength;
  private long buffer_updatetime_offset;
  private long whilewait;
  private LinkedList<String> message;
  private LinkedList<String> internal_message;

  private NetConfConnection NetConfConn;

  private String username;
  private String password;
  private String filter_targetWF;
  private String ConnectionID;
  private String RD_IP;
  private String HostKeyAuthenticationMode;

  private long replayinminutes;

  private boolean Feature_CapInterleave;
  private String RDHash;
  private boolean ConnectionInit;
  private boolean ReplayInit;


  public NetConfNotificationReceiverTriggerConnection(String newConnectionID, String filter_targetWF, String OldConnectionID,
                                                      BasicCredentials cred) {

    try {
      this.cleanupOldConnectionStep1(OldConnectionID); // Only one connection per RD_IP allowed (e.g. uncontrolled reboot of RD)

      this.ConnectionID = newConnectionID;

      this.username = cred.getUsername();
      this.password = cred.getPassword();
      this.HostKeyAuthenticationMode = cred.getHostKeyAuthenticationMode();
      this.replayinminutes = cred.getReplayInMinutes();
      this.filter_targetWF = filter_targetWF;

      this.Feature_CapInterleave = true;
      this.ConnectionInit = true;
      this.ReplayInit = false; //Default-Value before subscription
      String UUID_Hash = UUID.randomUUID().toString().replace("-", "");
      this.RDHash = UUID_Hash;

      this.clearbuffer = true;
      this.buffer_updatetime_offset = NetConfNotificationReceiverStartParameter.buffer_updatetime_offset;
      this.whilewait = NetConfNotificationReceiverStartParameter.PushDelimiter_RequestInterval;
      this.buffer_maxlength = NetConfNotificationReceiverStartParameter.buffer_maxlength;

      this.command_delay_before = NetConfNotificationReceiverStartParameter.command_delay_before;
      this.command_delay_after = NetConfNotificationReceiverStartParameter.command_delay_after;
      this.buffer = "";
      this.message = new LinkedList<String>();
      this.internal_message = new LinkedList<String>();

      this.NetConfConn = new NetConfConnection(this.ConnectionID, this.username, this.password, this.HostKeyAuthenticationMode);

      this.RD_IP = this.NetConfConn.getIP();

      this.open_connection_ssh(cred);

      ConnectionList.addConnection(this.ConnectionID, this);

      NetConfNotificationReceiverSharedLib.addSharedNetConfConnectionID(this.RD_IP, this.ConnectionID);

      this.cleanupOldConnectionStep2(OldConnectionID); // Only one connection per RD_IP allowed (e.g. uncontrolled reboot of RD)

    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "Initialization of NetConfNotificationReceiverTriggerConnection failed", t);
      this.close_connection();
    }

  }


  private void internalMessageProcessing_NetConfHello(String element) {
    try {
      Pattern pattern_netconfhello = Pattern.compile(delimiter_neconfhello);
      Matcher matcher_netconfhello = pattern_netconfhello.matcher(element);
      if (matcher_netconfhello.matches()) {
        Pattern pattern = Pattern.compile(delimiter_capinterleave);
        Matcher matcher = pattern.matcher(element);
        if (matcher.matches()) {
          this.Feature_CapInterleave = true;
          NetConfNotificationReceiverSharedLib.addRDHash(this.RDHash, this.RD_IP);
          if (logger.isDebugEnabled()) {
            logger.debug("NetConfNotificationReceiver: Feature_CapInterleave: true");
          }
        } else {
          this.Feature_CapInterleave = false;
          this.ConnectionInit = false;
          if (logger.isDebugEnabled()) {
            logger.debug("NetConfNotificationReceiver: Feature_CapInterleave: false");
          }
        }
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: internalMessageProcessing_NetConfHello failed", t);
    }
  }


  private void internalMessageProcessing_SerialNum(String element) {
    try {
      String message_element_hash = this.RDHash;
      Pattern pattern_serialnum_1 = Pattern.compile(delimiter_serialnum_1);
      Matcher matcher_serialnum_1 = pattern_serialnum_1.matcher(element);
      if (matcher_serialnum_1.find()) {
        Pattern pattern_serialnum_2 = Pattern.compile(delimiter_serialnum_2);
        Matcher matcher_serialnum_2 = pattern_serialnum_2.matcher(matcher_serialnum_1.group(1));
        String message_element = "";
        while (matcher_serialnum_2.find()) {
          message_element = message_element + matcher_serialnum_2.group(1);
        }
        if (NetConfNotificationReceiverSharedLib.containsRDHashfromRDID(this.RDHash)) {
          NetConfNotificationReceiverSharedLib.removeRDHash(this.RDHash);
        }
        message_element_hash = Long.toHexString(message_element.hashCode());
        this.RDHash = message_element_hash;
        if (this.Feature_CapInterleave) {
          NetConfNotificationReceiverSharedLib.addRDHash(this.RDHash, this.RD_IP);
        }
        this.ConnectionInit = false;
        if (logger.isDebugEnabled()) {
          logger.debug("NetConfNotificationReceiver: " + "SerialString: " + message_element + " HASH: " + message_element_hash);
        }
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver:  internalMessageProcessing_SerialNum failed", t);
    }
  }


  private void internalMessageProcessing_NetConfOperationRD(String element) {
    try {
      List<String> ListMessageID = NetConfNotificationReceiverSharedLib.listInputQueueMessageID();
      for (Iterator<String> iter = ListMessageID.iterator(); iter.hasNext();) {
        String MessageID = iter.next();
        if (element.contains(MessageID)) {
          NetConfNotificationReceiverSharedLib.addInputQueueNetConfMessageElement(this.ConnectionID, this.RDHash, MessageID, element, true);
          NetConfNotificationReceiverSharedLib.removeInputQueueMessageID(MessageID);
          if (logger.isDebugEnabled()) {
            logger.debug("NetConfNotificationReceiver: " + "Received MessageID: " + MessageID);
          }
        }
      }

    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: internalMessageProcessing_NetConfOperationRD failed", t);
    }
  }

  private void internalMessageProcessing_SubscriptionWithStartTime(String element) {
    try {
      Pattern pattern_SubscriptionWithStartTime = Pattern.compile(delimiter_SubscriptionWithStartTime);
      Matcher matcher_SubscriptionWithStartTime = pattern_SubscriptionWithStartTime.matcher(element);
      if (matcher_SubscriptionWithStartTime.find()) {
        Pattern pattern_SubscriptionWithStartTime_Okay = Pattern.compile(delimiter_SubscriptionWithStartTime_Okay);
        Matcher matcher_SubscriptionWithStartTime_Okay = pattern_SubscriptionWithStartTime_Okay.matcher(matcher_SubscriptionWithStartTime.group(1));
        if (matcher_SubscriptionWithStartTime_Okay.find()) {
          this.ReplayInit = false;
          if (logger.isDebugEnabled()) {
            logger.debug("NetConfNotificationReceiver: Replay successful");
          }
        } else {
          this.ReplayInit = false;
          logger.warn("NetConfNotificationReceiver: Replay failed for "+this.RD_IP+" - Retry without replay");
          this.command_send(subscription_notification, 0, 0);
        }
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver:  internalMessageProcessing_SubscriptionWithStartTime failed", t);
    }
  }

  private void internalMessageProcessing() {
    try {
      for (Iterator<String> iter = this.internal_message.iterator(); iter.hasNext();) {
        String element = iter.next();
        if (this.ConnectionInit) {
          internalMessageProcessing_NetConfHello(element);
          internalMessageProcessing_SerialNum(element);
        }
        if (this.ReplayInit) {
          internalMessageProcessing_SubscriptionWithStartTime(element);
        }
        internalMessageProcessing_NetConfOperationRD(element);
      }
      this.internal_message.clear();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: internalMessageProcessing failed", t);
    }
  }


  private void push_delimiter() {
    try {
      while (((this.buffer_updatetime + buffer_updatetime_offset) > System.currentTimeMillis())
          & (buffer.length() < this.buffer_maxlength)) {
        try {
          Thread.sleep(this.whilewait);
        } catch (Exception ex) {
          logger.warn("NetConfNotificationReceiver: push_delimiter - sleep failed", ex);
        }
      } ;

      Pattern pattern = Pattern.compile(delimiter_regex);
      Matcher matcher = pattern.matcher(this.buffer);

      while (matcher.find()) {
        String message_element = matcher.group(1);
        this.message.add(message_element);
        ConnectionQueue.push(this);
        this.internal_message.add(message_element);
      }
      this.buffer = "";
      this.clearbuffer = true;

      Thread t = new Thread() {

        public void run() {
          internalMessageProcessing();
        }
      };
      t.start();

    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: push_delimiter failed", t);
    }
  }


  /*
  private void push_timer()
  {
      try {
          while( ((this.buffer_updatetime+buffer_updatetime_offset) > System.currentTimeMillis()) & (buffer.length()<this.buffer_maxlength) ) {
              try {
                  Thread.sleep(this.whilewait);
              } catch(Exception ex) {
                  logger.warn( "NetConfNotificationReceiver: push_timer - sleep failed", ex);
              }
          };
          message.add(buffer);
          this.buffer="";
          this.clearbuffer=true;
          ConnectionQueue.push(this);
      } catch(Throwable t) {
          logger.warn( "NetConfNotificationReceiver: "+" push_timer failed", t);
      }    
  }
  */

  public String getIP() {
    String IP = "";
    try {
      IP = this.RD_IP;
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: getIP failed", t);
    }
    return IP;
  };


  public String getID() {
    String ID = "";
    try {
      ID = this.RDHash;
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: getID failed", t);
    }
    return ID;
  };


  public String getConnectionID() {
    String ID = "";
    try {
      ID = this.ConnectionID;
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: getConnectionID failed", t);
    }
    return ID;
  };


  public String getMessage() {
    String poll_message = "";
    try {
      poll_message = message.poll();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: getMessage failed", t);
    }
    return poll_message;
  };


  public int message_size() {
    int size_message = 0;
    try {
      size_message = message.size();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: message_size failed", t);
    }
    return size_message;
  };


  public String getFilterTargetWF() {
    String filter_targetWF = "";
    try {
      filter_targetWF = this.filter_targetWF;
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: getFilterTargetWF failed", t);
    }
    return filter_targetWF;
  };


  private void listener() {
    try {
      int read;
      while ((read = this.NetConfConn.read()) > -1) {
        if (this.clearbuffer) {
          this.clearbuffer = false;
          Thread t = new Thread() {

            public void run() {
              //push_timer();
              push_delimiter();
            };
          };
          t.start();
        } ;
        this.buffer = this.buffer + (char) read;
        this.buffer_updatetime = System.currentTimeMillis();
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: listener failed", t);
    }
  }


  private void startListener() {
    try {
      Thread t = new Thread() {

        public void run() {
          listener();
        };
      };
      t.start();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: startListener failed", t);
    }
  };


  private void command_send(String NETCONF_Command, long delay_before, long delay_after) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("NetConfNotificationReceiver: " + "SENDING: " + NETCONF_Command);
      }
      Thread.sleep(delay_before);
      this.NetConfConn.send(NETCONF_Command);
      Thread.sleep(delay_after);
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: command_send failed", t);
    }
  }


  public void sendNetConfOperation(String NETCONF_Operation) {
    try {
      String NETCONF_Command = NETCONF_Operation + "]]>]]>";
      this.command_send(NETCONF_Command, 0, 0);
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: sendNetConfOperation failed", t);
    }
  }


  private void open_connection_ssh(BasicCredentials cred) throws Throwable {
    this.NetConfConn.openNetConfConnection(cred);
    this.command_send(client_hello, 0, 0);
    this.startListener();
    this.command_send(get_serial_num, this.command_delay_before, this.command_delay_after);
    if (replayinminutes==0) {
      this.ReplayInit = false;
      this.command_send(subscription_notification, 0, 0);
    } else {
      this.ReplayInit = true;
      String subscription_notification_with_starttime = getSubscriptionNotificationWithStarttime();
      this.command_send(subscription_notification_with_starttime, 0, 0);
    }
  };

  private String getSubscriptionNotificationWithStarttime() throws Throwable {
    
    Clock cl = Clock.systemUTC();
    Instant lt = Instant.now();
    long minutes = replayinminutes;
    Instant tm = lt.minus(minutes, ChronoUnit.MINUTES);
    Instant tf = tm.truncatedTo(ChronoUnit.SECONDS);
    String tstr = tf.toString();
    String return_subscription_notification = subscription_notification_placeholder.replace("PLACEHOLDER",tstr);
    
    return return_subscription_notification;
  }

  public void close_connection() {
    try {
      this.command_send(client_goodbye, 0, this.command_delay_after);
      this.NetConfConn.closeNetconfConnection();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: close_connection (NETCONF) failed", t);
    }
    try {
      this.NetConfConn.closeSocket();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: close_connection (Socket) failed", t);
    }
    try {
      NetConfNotificationReceiverSharedLib.removeRDHash(this.RDHash);
      NetConfNotificationReceiverSharedLib.removeSharedNetConfConnectionID(this.RD_IP);
      ConnectionList.removeConnection(this.ConnectionID);
      ConnectionList.release(this.ConnectionID);
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: Remove entries in close_connection failed", t);
    }
  };


  private void cleanupOldConnectionStep1(String OldConnectionID) {
    try {
      if (!OldConnectionID.isEmpty()) {
        NetConfNotificationReceiverTriggerConnection OldConn = ConnectionList.getConnection(OldConnectionID);
        NetConfNotificationReceiverSharedLib.removeRDHash(OldConn.RDHash);
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: cleanup_oldconnection failed", t);
    }
  };


  private void cleanupOldConnectionStep2(String OldConnectionID) {
    try {
      if (!OldConnectionID.isEmpty()) {
        NetConfNotificationReceiverTriggerConnection OldConn = ConnectionList.getConnection(OldConnectionID);
        OldConn.cleanup_connection();
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: cleanup_oldconnection failed", t);
    }
  };


  public void cleanup_connection() {
    try {
      this.NetConfConn.closeNetconfConnection();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: cleanup_connection (NETCONF) failed", t);
    }
    try {
      this.NetConfConn.closeSocket();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: cleanup_connection (Socket) failed", t);
    }
    try {
      ConnectionList.removeConnection(this.ConnectionID);
      ConnectionList.release(this.ConnectionID);
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: Remove entries in cleanup_connection failed", t);
    }
  };

}
