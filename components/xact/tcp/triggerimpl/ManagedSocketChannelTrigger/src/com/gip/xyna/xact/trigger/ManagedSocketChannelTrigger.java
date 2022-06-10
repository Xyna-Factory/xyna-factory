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
package com.gip.xyna.xact.trigger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;

import org.apache.log4j.Logger;

import xact.tcp.SocketChannelManagement;

import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;


/*
 * Written and tested with CONTEX-Usecase in mind:
 * Single user that is in most cases sync waiting for a response
 */
public class ManagedSocketChannelTrigger extends EventListener<ManagedSocketChannelTriggerConnection, ManagedSocketChannelStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(ManagedSocketChannelTrigger.class);
  
  private final ByteBuffer buffer = ByteBuffer.allocate(1024);
  
  private AtomicBoolean running = new AtomicBoolean(false);
  private String socketMgmtName;
  private Selector selector;

  public ManagedSocketChannelTrigger() {
  }

  public void start(ManagedSocketChannelStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    running.set(true);
    socketMgmtName = sp.getSocketMgmtName();
    selector = sp.getSelector();
    SocketChannelManagement scm = SocketChannelManagement.getInstance();
    try {
      scm.createSocketChannel(sp.getSocketChannelCreationParameter());
    } catch (IOException e) {
      throw new XACT_TriggerCouldNotBeStartedException(new String[] {"ManagedSocketChannelTrigger could not be started."}, e) {
        private static final long serialVersionUID = 42038939005663060L;};
    }
  }
  

  public ManagedSocketChannelTriggerConnection receive() {
    SelectionKey selectionKey = null;
    try {
      while (selector.select() <= 0) { // would a timeout help in anyway?
      }
      Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
      selectionKey = keyIter.next();
      keyIter.remove();
      SocketChannel channel = (SocketChannel) selectionKey.channel();
      buffer.clear();
      if (channel.read(buffer) != -1) {
        buffer.flip();
        byte[] message = new byte[buffer.remaining() - buffer.position()];
        buffer.get(message, buffer.position(), buffer.remaining());
        return new ManagedSocketChannelTriggerConnection(socketMgmtName, message);
      } else {
        selfTerminate(selectionKey, new RuntimeException("Connection closed by peer"));
        return null;
      }
    } catch (IOException e) {
      selfTerminate(selectionKey, e);
      return null;
    } 
  }

  private void selfTerminate(SelectionKey key, final Throwable t) {
    if (key != null) {
      key.cancel();
    }
    if (running.compareAndSet(true, false)) {
      final TriggerInstanceIdentification self = getTriggerInstanceIdentification();
      new Thread(new Runnable() {
        public void run() {
          try {
            XynaFactory.getInstance().getActivation().disableTriggerInstance(self.getInstanceName());
            logger.warn("Disabled trigger instance " + self.getInstanceName() + ", caused by:",t);
          } catch (PersistenceLayerException e) {
            logger.warn("Could not terminate trigger", e);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            logger.warn("Could not terminate trigger", e);
          } catch (XACT_TriggerNotFound e) {
            logger.warn("Could not terminate trigger", e);
          } catch (XACT_TriggerInstanceNotFound e) {
            logger.warn("Could not terminate trigger", e);
          }
        }
      }).start();
    }
  }

  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, ManagedSocketChannelTriggerConnection con) {
    // TODO implementation
  }

  /**
   * called by Xyna Processing to stop the Trigger.
   * should make sure, that start() may be called again directly afterwards. connection instances
   * returned by the method receive() should not be expected to work after stop() has been called.
   */
  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    //TODO implementation
    //TODO update dependency xml file
    running.set(false);
    SocketChannelManagement scm = SocketChannelManagement.getInstance();
    try {
      if (selector != null && selector.isOpen()) {
        selector.close();
      }
      scm.destroySocketChannel(socketMgmtName);
    } catch (IOException e) {
      throw new XACT_TriggerCouldNotBeStoppedException(new String[] {"ManagedSocketChannelTrigger could not be succesfully stopped."}, e) {
        private static final long serialVersionUID = -4576188371408157670L;};
    }
  }

  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter
   * registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(ManagedSocketChannelTriggerConnection con) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return "ManagedSocketChannelTrigger connected with socket " + socketMgmtName;
  }

}
