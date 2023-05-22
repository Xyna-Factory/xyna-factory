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
package com.gip.xyna.xact.rmi;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;



/**
 * Kapselt den Zugriff auf eine RMI Instanz. Bietet M�glichkeiten f�r Fallback auf zweite
 * Instanz, falls eine Instanz nicht erreichbar ist �ber URLChooser Interface. Zwei einfache Implementierungen davon
 * stehen �ber statische Methoden zur Verf�gung. 
 * TODO ssl unterst�tzung 
 */
public class GenericRMIAdapter<T extends Remote> {

  private static Logger logger = Logger.getLogger(GenericRMIAdapter.class.getName());
  
  public static void setLogger(Logger logger) {
    GenericRMIAdapter.logger = logger;
  }

  public interface URLChooser {

    /**
     * gibt null zur�ck, falls keine weitere url vorhanden ist. urls m�ssen folgende form haben: &lt;name&gt; oder
     * "//&lt;host&gt;:&lt;port&gt;/&lt;name&gt;", falls zb rmi port nicht default port (1099)
     * @return n�chste url oder null
     */
    public String nextUrl();

    /**
     * falls nextUrl null zur�ckgibt, weil alle urls durchlaufen wurden, kann hiermit wieder von vorne angefangen werden.
     */
    public void initialize();
  }
  
  public static class RMIUrl {
    private final String hostname;
    private final int port;
    private final RMIClientSocketFactory socketFactory;
    private final String bindingName;
    
    public RMIUrl(String hostname, int port, String bindingName, RMIClientSocketFactory socketFactory) {
      this.hostname = hostname;
      this.port = port;
      this.bindingName = bindingName;
      this.socketFactory = socketFactory;
    }
    
    public RMIUrl(String url) {
      try {
        URI uri = new URI(url);
        this.hostname = uri.getHost();
        this.port = uri.getPort();
        this.bindingName = uri.getRawPath();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
      socketFactory = null;
    }

    public String getHostname() {
      return hostname;
    }
    
    public int getPort() {
      return port;
    }
    
    public RMIClientSocketFactory getSocketFactory() {
      return socketFactory;
    }

    public String getBinding() {
      return bindingName;
    }
  }
  
  public interface ExtendedURLChooser extends URLChooser {
    
    public RMIUrl getExtendedUrl();
    
  }

  private static class SingleURLChooser implements ExtendedURLChooser {

    private boolean firstCall = true;
    private final String url;
    private final RMIUrl rmiUrl;

    private SingleURLChooser(String url) {
      this.url = url;
      this.rmiUrl = new RMIUrl(url);
    }
    
    private SingleURLChooser(String hostname, int port, String rmiBindingName, RMIClientSocketFactory socketFactory) {
      this.url = "//" + hostname + ":" + port + "/" + rmiBindingName;
      this.rmiUrl = new RMIUrl(hostname, port, rmiBindingName, socketFactory);
    }

    public String nextUrl() { 
      if (firstCall) {
        firstCall = false;
        return url;
      } else {
        return null;
      }
    }


    public void initialize() {
      firstCall = true;
    }


    public RMIUrl getExtendedUrl() {
      return rmiUrl;
    }

  }

  private static class MultipleURLChooser implements URLChooser {

    private String[] urls;
    private int tries;
    private int lastTryCounter; //anfangs 0, nach dem ersten versuch steht es auf 1. 
    private int lastUrlIdx;


    private MultipleURLChooser(String[] urls, boolean randomChoose, int retries) {
      if (retries < 0) {
        throw new IllegalArgumentException("retries must not be negative");
      }
      if (urls == null || urls.length == 0) {
        throw new IllegalArgumentException("list of urls must not be empty");
      }
      if (randomChoose) {
        List<String> l = Arrays.asList(urls);
        Collections.shuffle(l);
        urls = l.toArray(new String[l.size()]);
      }
      this.urls = urls;
      this.tries = retries + 1;
      lastTryCounter = 0;
      lastUrlIdx = -1;
    }


    public String nextUrl() {
      if (lastTryCounter == 0 || lastTryCounter == tries) {
        //n�chste url bestimmen

        lastUrlIdx++;
        if (lastUrlIdx >= urls.length) {
          return null;
        }
        lastTryCounter = 1;
        return urls[lastUrlIdx];
      } else {
        lastTryCounter++;
        return urls[lastUrlIdx];
      }
    }


    public void initialize() {
      lastTryCounter = 0;
      lastUrlIdx = -1;
    }

  }


  /**
   * der urlchooser macht keine retries sondern versucht einmal die angegebene url.
   * @return
   */
  public static URLChooser getSingleURLChooser(String url) {
    return new SingleURLChooser(url);
  }
  
  public static URLChooser getSingleURLChooser(String hostname, int port, String rmiBindingName,
                                               RMIClientSocketFactory socketFactory) {
    return new SingleURLChooser(hostname, port, rmiBindingName, socketFactory);
  }


  public static URLChooser getSingleURLChooser(String hostname, int port, String rmiBindingName) {
    return getSingleURLChooser(hostname, port, rmiBindingName, null);
  }


  /**
   * der urlchooser versucht pro url die mit "retries" angegebene zahl von versuchen sich zu verbinden. falls ohne
   * erfolg, wird die n�chste url probiert. die reihenfolge der versuche ist entweder sequentiell oder zuf�llig, wobei
   * keine url doppelt probiert wird (bis auf die retries).
   * @return
   */
  public static URLChooser getMultipleURLChooser(String[] urls, boolean randomChoose, int retries) {
    return new MultipleURLChooser(urls, randomChoose, retries);
  }


  //TODO factory f�r multiple urls wo man hostname+port tupels angibt.
  //TODO validierung der urls

  private URLChooser urlChooser;
  volatile T rmiChannel;
  private ClassLoader classLoader;

  public GenericRMIAdapter(URLChooser urlChooser) throws RMIConnectionFailureException {
    this(urlChooser, false);
  }

  /**
   * @param lazyConnect falls true, wird nicht im konstruktor versucht sich zu verbinden
   */
  public GenericRMIAdapter(URLChooser urlChooser, boolean lazyConnect) throws RMIConnectionFailureException {
    this.urlChooser = urlChooser;
    if (!lazyConnect) {
      rmiChannel = connect(urlChooser);
    }
  }


  /**
   * benutzt den singleurlchooser, urls m�ssen folgende form haben: &lt;name&gt; oder
   * "//&lt;host&gt;:&lt;port&gt;/&lt;name&gt;", falls zb rmi port nicht default port (1099)
   */
  public GenericRMIAdapter(String url) throws RMIConnectionFailureException {
    this(getSingleURLChooser(url));
  }
  

  /**
   * benutzt den singleurlchooser
   */
  public GenericRMIAdapter(String hostname, int port, String rmiBindingName) throws RMIConnectionFailureException {
    this(getSingleURLChooser(hostname, port, rmiBindingName));
  }

  /**
   * classloader festlegen, der verwendet wird, um die remoteklassen beim connect zu laden.
   * ersatz f�r die typische verwendung von
   * <code>
   * setContextClassloader()
   * connectToRMI()
   * unsetContextClassloader()
   * </code>
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }


  private T connect(URLChooser urlChooser) throws RMIConnectionFailureException {
    T rmi = null;
    List<Exception> lastExceptions = null;
    String urls = null;
    urlChooser.initialize();
    ClassLoader contextClassLoader = null;
    Thread t = null;
    while (rmi == null) {
      String url = urlChooser.nextUrl();
      if (url == null) {
        if (lastExceptions != null) {
          throw (RMIConnectionFailureException) new RMIConnectionFailureException(urls).initCauses(lastExceptions
                                                                                                   .toArray(new Exception[lastExceptions.size()]));
        } else {
          throw new RMIConnectionFailureException("<no url>");
        }
      }
      if (urls == null) {
        urls = url;
      } else {
        urls += ", url";
      }

      try {

        if (classLoader != null) {
          t = Thread.currentThread();
          contextClassLoader = t.getContextClassLoader();
          t.setContextClassLoader(classLoader);
        }
        try {
          if (urlChooser instanceof ExtendedURLChooser) {
            RMIUrl rmiUrl = ((ExtendedURLChooser) urlChooser).getExtendedUrl();

            if (rmiUrl.getSocketFactory() != null) {
              Registry registry =
                  LocateRegistry.getRegistry(rmiUrl.getHostname(), rmiUrl.getPort(), rmiUrl.getSocketFactory());
              rmi = (T) registry.lookup(rmiUrl.getBinding());
            } else {
              rmi = (T) Naming.lookup(url);
            }
          } else {
            rmi = (T) Naming.lookup(url);
          }
        } finally {
          if (classLoader != null) {
            t.setContextClassLoader(contextClassLoader);
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("got rmi connection to " + url + " successfully.");
        }
      } catch (Exception e) { // ConnectException
        logger.warn("could not connect to " + url + ". " + e.getMessage());
        if (lastExceptions == null) {
          lastExceptions = new ArrayList<Exception>();
        }
        lastExceptions.add(e);
      }
    }
    return rmi;
  }

  /**
   * synchrones reconnect
   */
  public void reconnect() throws RMIConnectionFailureException {
    rmiChannel = null;
    rmiChannel = connect(urlChooser);
  }

  /**
   * beim n�chsten aufruf von getRmiInterface wird reconnect durchgef�hrt 
   */
  public void reconnectOnNextTry() {
    rmiChannel = null;
  }


  public T getRmiInterface() throws RMIConnectionFailureException {
    if (rmiChannel == null) {
      synchronized (this) {
        if (rmiChannel == null) {
          reconnect();
        }
      }
    }
    return rmiChannel;
  }
}
