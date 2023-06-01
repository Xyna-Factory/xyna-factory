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

package com.gip.xyna.coherence.remote;

import java.rmi.RMISecurityManager;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.CacheControllerFactory;



public class CacheControllerRemoteInterfaceFactory {
  public static void main(String[] args) {
    getCacheControllerRemoteInterface(CacheControllerFactory.newCacheController(), -1);
  }


  public static CacheControllerRemoteInterfaceWithInit getCacheControllerRemoteInterface(CacheController controller,
                                                                                         int port) {

    synchronized (CacheControllerRemoteInterfaceFactory.class) {
      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
      }
    }
//
//    try {
//      Class<?> bla = RMIClassLoader.loadClass(CacheControllerRemoteInterfaceImpl.class.getName());
//      Constructor<?> con = bla.getConstructors()[0];//(CacheController.class, Integer.class);
//      return (CacheControllerRemoteInterfaceWithInit) con.newInstance(controller, port);
//    } catch (MalformedURLException e) {
//      throw new RuntimeException(e);
//    } catch (ClassNotFoundException e) {
//      throw new RuntimeException(e);
//    } catch (InstantiationException e) {
//      throw new RuntimeException(e);
//    } catch (IllegalAccessException e) {
//      throw new RuntimeException(e);
//    } catch (SecurityException e) {
//      throw new RuntimeException(e);
//    } catch (IllegalArgumentException e) {
//      throw new RuntimeException(e);
//    } catch (InvocationTargetException e) {
//      throw new RuntimeException(e);
//    }
    return new CacheControllerRemoteInterfaceImpl(controller, port);

  }

}
