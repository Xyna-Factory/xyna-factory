/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package xact.ssh;



import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;

import org.apache.log4j.Logger;
import com.gip.xyna.CentralFactoryLogging;

import net.schmizz.sshj.SSHClient;



public class SSHJReflection {

  private static final Logger logger = CentralFactoryLogging.getLogger(SSHJReflection.class);


  //Method "recall" for NetConf Call-Home-Feature (RFC8071)
  public static void recall(SSHClient client, Socket socket) throws Exception {
    if (client instanceof SSHClient) {
      if (socket != null) {
        int port = socket.getPort();
        setDeclaredField((SSHClient) client, "socket", socket, "net.schmizz.sshj.SocketClient");
        setDeclaredField((SSHClient) client, "port", port, "net.schmizz.sshj.SocketClient");
        invokeDeclaredVoidMethod((SSHClient) client, "onConnect", "net.schmizz.sshj.SocketClient");
      } else {
        logger.warn("Socket is null");
      }
    }
  }


  private static void invokeDeclaredVoidMethod(Object instance, String getterName, String controlSuperclassName) {
    try {
      if (instance.getClass().getSuperclass().getName().equalsIgnoreCase(controlSuperclassName)) {
        Method getter = instance.getClass().getSuperclass().getDeclaredMethod(getterName);
        getter.setAccessible(true);
        getter.invoke(instance);
      } else {
        logger.warn("Error in Reflection");
      }
    } catch (Exception e) {
      logger.warn("Error in Reflection", e);
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings("unchecked")
  private static <T> T getDeclaredField(Object instance, String fieldName, String controlSuperclassName) {
    try {
      if (instance.getClass().getSuperclass().getName().equalsIgnoreCase(controlSuperclassName)) {
        Field field = instance.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(instance);
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }


  private static void setDeclaredField(Object instance, String fieldName, Object setFieldValue, String controlSuperclassName) {
    try {
      if (instance.getClass().getSuperclass().getName().equalsIgnoreCase(controlSuperclassName)) {
        Field field = instance.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, setFieldValue);
      } else {
        logger.warn("Error in Reflection");
      }
    } catch (Exception e) {
      logger.warn("Error in Reflection", e);
      throw new RuntimeException(e);
    }
  }


}