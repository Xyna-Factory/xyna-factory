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
package com.gip.xyna.xfmg.xfctrl;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.RegisteringClassLoader;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher.ClassLoaderBuilder;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.RMIClassLoader;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RMISSLClientSocketFactory;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RMISSLClientSocketFactory.ClientSocketConnectionParameter;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;



/**
 * kapselt funktionalit�t bzgl rmi-schnittstellen. z.b. class-reloading support.
 */
public class RMIManagement extends FunctionGroup implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RMIManagement.class); //wird in den inneren, serialisierten klassen ggfs auch auf einem rmi-client verwendet
    
  public static class XynaRMIClientSocketFactory implements Serializable, RMIClientSocketFactory {

    private static final long serialVersionUID = 1L;
    private final boolean noTimeout;
    private int timeoutMillis;
    
    public XynaRMIClientSocketFactory() {
      this.noTimeout = true;
      this.timeoutMillis = 0;
    }
    
    public XynaRMIClientSocketFactory(int timeoutInSeconds) {
      this.noTimeout = false;
      this.timeoutMillis = timeoutInSeconds * 1000;
    }
    
    public XynaRMIClientSocketFactory(Duration timeout) {
      if( timeout == null ) {
        this.noTimeout = true;
        this.timeoutMillis = 0;
      } else {
        long to = timeout.getDurationInMillis();
        if( to > Integer.MAX_VALUE ) {
          this.noTimeout = true;
          this.timeoutMillis = 0;
        } else {
          this.noTimeout = false;
          this.timeoutMillis = (int)timeout.getDurationInMillis();
        }
      }
    }

    public Socket createSocket(String host, int port) throws IOException {
      Socket socket = new Socket();
      socket.setSoTimeout(timeoutMillis);
      socket.setSoLinger(false, 0);
      socket.setKeepAlive(true);
      socket.connect(new InetSocketAddress(host, port), timeoutMillis);
      return socket;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof XynaRMIClientSocketFactory) {
        if (obj == this) {
          return true;
        }
        XynaRMIClientSocketFactory xcf = (XynaRMIClientSocketFactory)obj;
        if (xcf.noTimeout == noTimeout) {
          return true;
        }
        return false;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return noTimeout ? 3 : 5;
    }

    public String toString() {
      return XynaRMIClientSocketFactory.class.getSimpleName()+"@"+Integer.toHexString(System.identityHashCode(this))
      +"(noTimeout=" + noTimeout+")";
    } 
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      Pair<Duration, ClientSocketConnectionParameter> pair = RMISSLClientSocketFactory.threadLocalConParams.get();
      if (pair != null) {
        timeoutMillis = (int) pair.getFirst().getDurationInMillis();
      }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
    }
  }

  private static class XynaRMIServerSocketFactory implements Serializable, RMIServerSocketFactory {

    private static final long serialVersionUID = 1L;
    private String hostname;
    
    public XynaRMIServerSocketFactory(String hostname) {
      this.hostname = hostname;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
      if (logger.isTraceEnabled()) {
        logger.trace("opening rmi server socket " + hostname + ":" + port, new Exception());
      }
      ServerSocket socket = new ServerSocket() {

        @Override
        public Socket accept() throws IOException {
          Socket s = super.accept();
          s.setKeepAlive(true);
          s.setSoTimeout((int) XynaProperty.RMI_SERVER_SOCKET_TIMEOUT.get().getDurationInMillis());
          s.setSoLinger(false, 0);
          return s;
        }

        @Override
        public void close() throws IOException {
          if (logger.isTraceEnabled()) {
            logger.trace("closing rmi server socket " + hostname + ":" + getLocalPort(), new Exception());
          }
          super.close();
        }

      };
      socket.bind(new InetSocketAddress(hostname, port), 0);
      return socket;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof XynaRMIServerSocketFactory) {
        if (obj == this) {
          return true;
        }
        XynaRMIServerSocketFactory xsf = (XynaRMIServerSocketFactory)obj;
        if (xsf.hostname.equals(hostname)) {
          return true;
        }
        return false;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return hostname.hashCode();
    }


    public String toString() {
      return XynaRMIServerSocketFactory.class.getSimpleName()+"@"+Integer.toHexString(System.identityHashCode(this))
      +"hostname=" + hostname+")";
    }
 
  }
  
  private static class RegistryCache {
    
    private Map<String,Registry> cachedRegistries = new HashMap<String,Registry>();
    
    /**
     * im get-Fall kann timeout nicht mehr ber�cksichtigt werden
     * @param hostname
     * @param port
     * @param timeout
     * @return
     * @throws XMCP_RMI_BINDING_ERROR 
     */
    public Registry getOrCreateRegistry(String hostname, int port, Duration timeout ) throws XMCP_RMI_BINDING_ERROR {
      String key = hostname+":"+port;
      Registry r = cachedRegistries.get(key);
      if( r == null ) {
        synchronized (cachedRegistries) {
          r = cachedRegistries.get(key);
          if( r == null ) {
            r = createRegistry(hostname,port,timeout);
            cachedRegistries.put(key, r);
          }
        }
      }
      return r;
    }

    private Registry createRegistry(String hostname, int port, Duration timeout) throws XMCP_RMI_BINDING_ERROR {
      RMIClientSocketFactory csf = new XynaRMIClientSocketFactory(timeout);
      RMIServerSocketFactory ssf = new XynaRMIServerSocketFactory(hostname);
      
      boolean created = false;
      Registry r = null;
      try {
        RMIHostnameSetter rmiHS = new RMIHostnameSetter();
        try {
          rmiHS.setHostname(hostname);
          r = LocateRegistry.createRegistry(port, csf, ssf );
          created = true;
        } finally {
          rmiHS.resetHostname();
        }
      } catch (RemoteException e) {
        try {
          //Es ist nicht sonderlich sinnvoll, die eigene Registry als RemoteRegistry zu holen.
          //Aber wenn sie nicht �ber den RegistryCache erzeugt wurde, kann sie so wenigstens noch verf�gbar gemacht werden.
          r = LocateRegistry.getRegistry(hostname, port, csf );
        } catch (RemoteException e1) {
          throw new XMCP_RMI_BINDING_ERROR(hostname+":"+port, e1);
        }
      }
      if( created ) {
        if (logger.isDebugEnabled()) {
          logger.debug( "created " +"rmi registry " + hostname + ":" + port);
        }
      } else {
        logger.warn( "found existing "+"rmi registry " + hostname + ":" + port);
      }
      return r;
    }
    
  }
  
 
  
  public class RMIImplProxy<T extends Remote> {

    protected final Registry registry;
    protected T remoteImpl;
    private final String rmiBindingName;
    private final String hostname;
    private final int rmiPortRegistry;
    private int rmiPortCommunication;
    private final boolean noTimeout;
    private boolean registered = false;
    private boolean bound = false;
    private RMIParameter rmiParameter;

    RMIImplProxy(String rmiBindingName, final String hostname, int rmiPortRegistry, int rmiPortCommunication,
                 boolean noTimeout) throws XMCP_RMI_BINDING_ERROR {
      checkHostname(hostname);
      this.rmiBindingName = rmiBindingName;
      this.hostname = hostname;
      this.rmiPortRegistry = rmiPortRegistry;
      this.rmiPortCommunication = rmiPortCommunication;
      this.noTimeout = noTimeout;
      Duration timeout = null;
      if( ! noTimeout ) {
        timeout = new Duration(getRmiTimeout(),TimeUnit.SECONDS);
      }
      this.registry = registryCache.getOrCreateRegistry(hostname, rmiPortRegistry, timeout);
    }
    
    private void checkHostname(String hostname2) throws XMCP_RMI_BINDING_ERROR {
     InetAddress targetAddress;
      try {
        targetAddress = InetAddress.getByName(hostname);
      } catch (UnknownHostException e2) {
        throw new XMCP_RMI_BINDING_ERROR(rmiBindingName, new RuntimeException("Failed to resolve host " + hostname,
                                                                              e2));
      }
      if (!targetAddress.isAnyLocalAddress()) {
        Enumeration<NetworkInterface> interfaces;
        try {
          interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e2) {
          throw new XMCP_RMI_BINDING_ERROR(rmiBindingName, new RuntimeException("Failed to determine local interfaces",
                                                                                e2));
        }
        boolean validHostName = false;
        String targetHostAddress = targetAddress.getHostAddress();
        String targetHostName = targetAddress.getHostName();
        Set<String> localValues = new HashSet<String>();
        outer: for (NetworkInterface netInterface : Collections.list(interfaces)) {
          for (InterfaceAddress address: netInterface.getInterfaceAddresses()) {
            String foundHostAddress = address.getAddress().getHostAddress();
            String foundHostName = address.getAddress().getHostName();
            if (foundHostAddress.equals(targetHostAddress) || foundHostName.equals(targetHostName)) {
              validHostName = true;
              break outer;
            }
            localValues.add(foundHostAddress);
            localValues.add(foundHostName);
          }
        }
        if (!validHostName) {
          throw new XMCP_RMI_BINDING_ERROR(rmiBindingName, new RuntimeException("Target host name <" + hostname
              + "> is not one of the known local interfaces: " + localValues));
        }
      }

    }


    RMIImplProxy(T remoteImpl, String rmiBindingName, String hostname, int rmiPortRegistry, int rmiPortCommunication, boolean noTimeout) throws XMCP_RMI_BINDING_ERROR {
      this(rmiBindingName, hostname, rmiPortRegistry, rmiPortCommunication, noTimeout);
      this.remoteImpl = remoteImpl;
    }

    /**
     * macht unbind (falls notwendig) und unexportObject
     * gibt true zur�ck, falls erfolgreich
     */
    public boolean unregister(boolean force) {
      if (!registered) {        
        logger.warn("tried to unregister " + rmiBindingName + ", which is not registered.");
        return true;
      }
      if (logger.isTraceEnabled()) {
        logger.trace("unregistering remoteImpl " + remoteImpl + " from registry " + registry);
      }
      if (bound) {
        try {
          registry.unbind(rmiBindingName);
        } catch (AccessException e) {
          throw new RuntimeException(e);
        } catch (RemoteException e) {
          logger.warn("Failed to unbind object <" + rmiBindingName + ">", e);
        } catch (NotBoundException e) {
          throw new RuntimeException(e);
        }
        bound = false;
      }
      
      try {
        //falls force=true werden hier dann ggfs aktuell laufende remote-methodcalls abgeschossen
        if (UnicastRemoteObject.unexportObject(remoteImpl, force)) {
          registered = false;
          return true;
        }
      } catch (NoSuchObjectException e) {
        logger.warn("Failed to unexport object <" + rmiBindingName + ">", e);
      }
      return false;
    }


    protected void register() throws XMCP_RMI_BINDING_ERROR {
      if (registered) {
        logger.warn("called register twice");
        return;
      }
      rmiParameter = bindRemoteImpl();
      bound = true;
      registered = true;
    }


    private RMIParameter bindRemoteImpl() throws XMCP_RMI_BINDING_ERROR {
      return bindRemoteImpl(noTimeout ? new XynaRMIClientSocketFactory() : new XynaRMIClientSocketFactory(rmiTimeout),
                     new XynaRMIServerSocketFactory(hostname));
    }

    private RMIParameter bindRemoteImpl(RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory) throws XMCP_RMI_BINDING_ERROR {
      try {
        Remote stub;
        synchronized (RMIManagement.class) {
          //sorgt daf�r, dass im tcpendpoint innerhalb des erzeugten stubs der richtige hostname
          // (falls man rmi �ber verschiedene interfaces anbieten m�chte) steht.
          //vergleiche http://download.oracle.com/javase/1.5.0/docs/guide/rmi/relnotes.html
          // oder openjdk implementierung von exportObject (TCPEndpoint.getLocalEndpoint(...)
          
          RMIHostnameSetter rmiHS = new RMIHostnameSetter();
          //FIXME kl�ren, ob die property f�rs exportObject ben�tigt wird oder f�rs bind, und entpsrechend das tryfinally bauen
          
          try {
            rmiHS.setHostname(hostname);
            if (!(remoteImpl instanceof UnicastRemoteObject)) {
              stub = UnicastRemoteObject.exportObject(remoteImpl, rmiPortCommunication, clientSocketFactory, serverSocketFactory);
            } else {
              stub = remoteImpl;
            }
            try {
              registry.bind(rmiBindingName, stub);
            } catch (AlreadyBoundException e) {
              registry.rebind(rmiBindingName, stub);
            }
            
          } catch (ExportException e) {
            if (rmiPortCommunication == 0 && e.getMessage().contains("Port already in use: 0")
                && e.getCause() instanceof BindException
                && e.getCause().getMessage().contains("Cannot assign requested address")) {
              throw new Exception("Could not export rmi object to " + hostname
                  + ". Possible cause: the interface is down.", e);
            }
            throw new RuntimeException("Could not export rmi object to " + hostname + ":" + rmiPortCommunication, e);
          } finally {
            rmiHS.resetHostname();
          }
        }
        
        if (logger.isDebugEnabled()) {
          logger.debug(rmiBindingName + " bound to registry");
          if (logger.isTraceEnabled()) {
            logger.trace("bound remoteImpl " + remoteImpl + " to registry " + registry);
          }
        }
        return new RMIParameter(rmiBindingName, hostname, rmiPortRegistry, rmiPortCommunication);
      } catch (Exception e) {
        throw new XMCP_RMI_BINDING_ERROR(rmiBindingName, e);
      }
      
    }

    
    protected void register(RMISocketFactory socketFactory) throws XMCP_RMI_BINDING_ERROR {
      if (registered) {
        logger.warn("called register twice");
        return;
      }
      rmiParameter = bindRemoteImpl(socketFactory.getRMIClientSocketFactory(), socketFactory.getRMIServerSocketFactory());
      bound = true;
      registered = true;
    }

    public RMIParameter getRMIParameter() {
      return rmiParameter;
    }
   
  }

  /**
   * kapselt die rmi implementierung (der server-anteil, wo man die implementierung des rmi-interfaces hat), derart,
   * dass die eigentliche implementierung nicht fest ist. statt dessen wird diese per reflection ggfs mit
   * unterschiedlichem classloader zur laufzeit dynamisch neu erstellt und dann darauf
   * {@link RMIImplFactory#init(InitializableRemoteInterface)} aufgerufen. das gibt einem die chance, in die erstellte implementierung noch
   * kontext reinzugeben.
   */
  public interface RMIImplFactory<T extends InitializableRemoteInterface & Remote> {

    /**
     * wird aufgerufen, nachdem per reflection ein neues remote impl objekt erstellt wurde Achtung: hier kann man nicht
     * casten nach T, weil eventuell mit anderem classloader geladen. typischerweise ruft man hier
     * {@link InitializableRemoteInterface#init(Object...)} auf mit den objekten die den gew�nschten kontext enthalten.
     */
    public void init(InitializableRemoteInterface rmiImpl);

    /**
     * R�ckgabe eines ClassLoaderBuilder, falls spezieller RMI-ClassLoader ben�tigt wird.
     * Ansonsten ist null erlaubt.
     * TODO Erfordert �nderungen an Bestandscode
     */
    //public ClassLoaderBuilder getClassLoaderBuilder();
    
    /**
     * wird benutzt um per reflection ein neues remote impl objekt zu erstellen, wenn es zb aus classloading gr�nden neu
     * erstellt werden muss.
     */
    public String getFQClassName();
    
    public void shutdown(InitializableRemoteInterface rmiImpl);
  }

  private class RMIImplProxyClassReload<T extends InitializableRemoteInterface & Remote> extends RMIImplProxy<T> {

    private RMIImplFactory<T> factory;

    public RMIImplProxyClassReload(RMIImplFactory<T> factory, String rmiBindingName, String hostname, int rmiPortRegistry, int rmiPortCommunication, boolean noTimeout)
        throws XMCP_RMI_BINDING_ERROR {
      super(rmiBindingName, hostname, rmiPortRegistry, rmiPortCommunication, noTimeout);
      this.factory = factory;
      createImplAndRegister();
    }

    @SuppressWarnings("unchecked")
    private Class<T> createNewRMIImplClass() {
      ClassLoaderBuilder clb = null;
      if( factory instanceof ClassLoaderBuilder ) {
        clb = (ClassLoaderBuilder)factory;
      } else {
        clb = rmiClassLoaderBuilder;
      }
      ClassLoaderBase cl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
          .getOrCreateClassLoaderByType(ClassLoaderType.RMI, -1L, clb );
      
      if( cl instanceof RegisteringClassLoader ) {
        //daf�r sorgen, dass die klasse vom rmiclassloader beachtet wird.
        ((RegisteringClassLoader)cl).register( factory.getFQClassName() );
      }

      try {
        return (Class<T>) cl.loadClass(factory.getFQClassName());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("rmi class could not be loaded", e); //das kann unter normalen umst�nden nicht passieren
      }
    }
    
    public void shutdown() {
      if (!unregister(false)) {
        if (!unregister(true)) {
          logger.warn("could not unregister rmiImpl " + remoteImpl);
        }
      }
      factory.shutdown(remoteImpl);
    }

    public void swapInstance() throws XMCP_RMI_BINDING_ERROR {
      shutdown();
      createImplAndRegister();
    }


    private void createImplAndRegister() throws XMCP_RMI_BINDING_ERROR {
      try {
        remoteImpl = createNewRMIImplClass().getConstructor().newInstance();
      } catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      factory.init(remoteImpl);
      if (factory instanceof RMISocketFactory) {
        register((RMISocketFactory)factory);
      } else {
        register();
      }
    }

  }
  
  
  public static interface RMISocketFactory {
    
    RMIClientSocketFactory getRMIClientSocketFactory();
    
    RMIServerSocketFactory getRMIServerSocketFactory();
    
  }

  public interface InitializableRemoteInterface {

    public void init(Object... initParameters); //TODO generics besser als object[] ?
    
  }

  public static final String DEFAULT_NAME = "RMIManagement";

  private transient ClassLoaderBuilder rmiClassLoaderBuilder = new ClassLoaderBuilder() {
    @Override
    public String getId() {
      return "RMI";
    }
    @Override
    public ClassLoaderBase createClassLoader() {
      return new RMIClassLoader(VersionManagement.REVISION_WORKINGSET);
    }
    
  };

  
  private final transient Map<String, RMIImplProxyClassReload<?>> proxies = new HashMap<String, RMIImplProxyClassReload<?>>();
  private volatile int rmiTimeout = Constants.RMI_DEFAULT_TIMEOUT; //sekunden
  private final transient RegistryCache registryCache;

  protected RMIManagement() throws XynaException {
    super();
    this.registryCache = new RegistryCache();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {

    //TODO: rmichannel impl hier auch registrieren und entsprechende sonderbehandlung im rmiclassloader rausnehmen
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(RMIManagement.class, "RMIManagement").
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() { initProperties(); } });
  }
  
  private void initProperties() {
    String user = RMIManagement.class.getSimpleName();
    XynaProperty.RMI_TIMEOUT.registerDependency(UserType.XynaFactory, user);
    XynaProperty.RMI_PORT_REGISTRY.registerDependency(UserType.XynaFactory, user);
    XynaProperty.RMI_HOSTNAME_REGISTRY.registerDependency(UserType.XynaFactory, user);
  }

  @Override
  protected void shutdown() throws XynaException {
    synchronized (proxies) {
      for (RMIImplProxyClassReload<?> proxy : proxies.values()) {
        proxy.shutdown();
      }
    }
  }

  
  public RMIClientSocketFactory getClientSocketFactory(int timeout) {
    if (timeout <= 0) {
      return new XynaRMIClientSocketFactory();
    } else {
      return new XynaRMIClientSocketFactory(timeout);
    }
  }
  
  
  public RMIServerSocketFactory getServerSocketFactory(String hostname) {
    return new XynaRMIServerSocketFactory(hostname);
  }
  
  /**
   * Erzeugen einer Registry: im get-Fall kann timeout nicht mehr ber�cksichtigt werden
   * @param hostname
   * @param port
   * @param timeout
   * @return
   * @throws XMCP_RMI_BINDING_ERROR 
   */
  public Registry getOrCreateRegistry(String hostname, int port, Duration timeout ) throws XMCP_RMI_BINDING_ERROR {
    return registryCache.getOrCreateRegistry(hostname, port, timeout);
  }

  /**
   * SystemProperty "java.rmi.server.hostname" muss gesetzt sein, 
   * damit die Endpoints richtig initialisiert werden. Falls diese nicht richtig initialisiert werden,
   * lernen sie bei der ersten Verwendung ihren lokalen Namen. Dieser weicht jedoch evtl vom gew�nschten Namen ab.
   * Dies ist aber nicht schlimm, da die ServerSocketFactory den richtigen Host kennt.
   * 
   * Siehe sun.rmi.transport.tcp.TCPEndpoint.setLocalHost(...)
   *
   */
  private static class RMIHostnameSetter {

    private static final String PROPERTY_JAVA_RMI_HOSTNAME = "java.rmi.server.hostname";
    
    private String hostname_old;
    
    public RMIHostnameSetter() {
      this.hostname_old = System.getProperty(PROPERTY_JAVA_RMI_HOSTNAME);
    }
    
    public void setHostname(String hostname) {
      System.setProperty(PROPERTY_JAVA_RMI_HOSTNAME, hostname);
    }
    
    public void resetHostname() {
      if (hostname_old == null) {
        System.clearProperty(PROPERTY_JAVA_RMI_HOSTNAME);
      } else {
        System.setProperty(PROPERTY_JAVA_RMI_HOSTNAME, hostname_old); 
      }
    }

  }
  
  public static class RMIParameter {
    private final String rmiName;
    private final String registryHost;
    private final int registryPort;
    private final int communicationPort;
    
    public RMIParameter(String rmiName, String registryHost, int registryPort, int communicationPort ) {
      this.rmiName = rmiName;
      this.registryHost = registryHost;
      this.registryPort = registryPort;
      this.communicationPort = communicationPort;
    }
    
    public String getUrl() {
      return "//"+registryHost+":"+registryPort+"/"+rmiName;
    }
    
    public int getCommunicationPort() {
      return communicationPort;
    }
    public String getRegistryHost() {
      return registryHost;
    }
    public int getRegistryPort() {
      return registryPort;
    }
    public String getRmiName() {
      return rmiName;
    }
    
    public String getRegistryHost(String defVal) {
      if( registryHost == null ) {
        return defVal;
      }
      return registryHost;
    }
    public int getRegistryPort(int defVal) {
      if( registryPort == 0 ) {
        return defVal;
      }
      return registryPort;
    }
  }
  
  public <T extends InitializableRemoteInterface & Remote> RMIImplProxy<T> registerClassreloadableRMIImplFactory(
      RMIImplFactory<T> factory, RMIParameter parameter, boolean noTimeout)
          throws XMCP_RMI_BINDING_ERROR {
    return registerClassreloadableRMIImplFactory(
        factory, 
        parameter.getRmiName(),
        parameter.getRegistryHost( getRMIHostname() ),
        parameter.getRegistryPort( getRMIPortForRegistry() ),
        parameter.getCommunicationPort(), 
        noTimeout);
  }
    
  public <T extends InitializableRemoteInterface & Remote> RMIImplProxy<T> registerClassreloadableRMIImplFactory(
                                                                                                                 RMIImplFactory<T> factory,
                                                                                                                 String rmiBindingName,
                                                                                                                 String hostname,
                                                                                                                 int rmiPortRegistry)
      throws XMCP_RMI_BINDING_ERROR {
    return registerClassreloadableRMIImplFactory(factory, rmiBindingName, hostname, rmiPortRegistry, 0, false);
  }


  public <T extends InitializableRemoteInterface & Remote> RMIImplProxy<T> registerClassreloadableRMIImplFactory(
                                                                                                                 RMIImplFactory<T> factory,
                                                                                                                 String rmiBindingName,
                                                                                                                 String hostname,
                                                                                                                 int rmiPortRegistry,
                                                                                                                 int rmiPortCommunication, boolean noTimeout)
      throws XMCP_RMI_BINDING_ERROR {
 
    //proxy erstellen, so dass bei jedem class-reload die innere instanz erneuert wird
    RMIImplProxyClassReload<T> proxy = new RMIImplProxyClassReload<T>(factory, rmiBindingName, hostname, rmiPortRegistry, rmiPortCommunication, noTimeout);
    synchronized (proxies) {
      proxies.put(rmiBindingName, proxy);
    }
    return proxy;
  }

  
 

  public <T extends InitializableRemoteInterface & Remote> RMIImplProxy<T> registerClassreloadableRMIImplFactory(
      RMIImplFactory<T> factory, String rmiBindingName,
      int rmiPortCommunication, boolean noTimeout) throws XMCP_RMI_BINDING_ERROR {
    return registerClassreloadableRMIImplFactory(factory, rmiBindingName, 
        getRMIHostname(), getRMIPortForRegistry(), //Default aus XynaProperty
        rmiPortCommunication, noTimeout);
  }
  
  public void unregisterRemoteInterface(String rmiBindingName) {
    RMIImplProxyClassReload<?> rmiImplProxyClassReload;
    synchronized (proxies) {
      rmiImplProxyClassReload = proxies.remove(rmiBindingName);
    }
    if (rmiImplProxyClassReload != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Deregistering rmi interface " + rmiBindingName);
      }
      rmiImplProxyClassReload.shutdown();
    }
  }


  public <T extends Remote> RMIImplProxy<T> createRMIImplProxy(final T remoteImpl, final String rmiBindingName,
                                                               String hostname, int rmiPortRegistry) throws XMCP_RMI_BINDING_ERROR {
    return createRMIImplProxy(remoteImpl, rmiBindingName, hostname, rmiPortRegistry, 0, false);
  }


  public <T extends Remote> RMIImplProxy<T> createRMIImplProxy(final T remoteImpl, final String rmiBindingName,
                                                               String hostname, int rmiPortRegistry,
                                                               int rmiPortCommunication, boolean noTimeout) throws XMCP_RMI_BINDING_ERROR {
    RMIImplProxy<T> proxy =
        new RMIImplProxy<T>(remoteImpl, rmiBindingName, hostname, rmiPortRegistry, rmiPortCommunication, noTimeout);
    proxy.register();
    //leerer proxy, der nur unregister kann
    return proxy;
  }


  /**
   * wird vom classloading aufgerufen
   */
  public void redeployRMIImpls() throws XMCP_RMI_BINDING_ERROR {
    if (logger.isTraceEnabled()) {
      logger.trace("rmi impls will be recreated");
    }
    //in den proxys die instanzen austauschen
    synchronized (proxies) {
      for (RMIImplProxyClassReload<?> proxy : proxies.values()) {
        proxy.swapInstance();
      }
    }
  }

  public int getRmiTimeout() {
    return (int) XynaProperty.RMI_TIMEOUT.get();
  }

  public int getRMIPortForRegistry() {
    return XynaProperty.RMI_PORT_REGISTRY.get();
  }

  public String getRMIHostname() {
    return XynaProperty.RMI_HOSTNAME_REGISTRY.get();
  }

  /**
   * erstellt rmi adapter mit geeigneter socketfactory 
   */
  public <T extends Remote> GenericRMIAdapter<T> createRMIAdapter(String hostname, int port, String rmiBindingName)
      throws RMIConnectionFailureException {
    return new GenericRMIAdapter<T>(GenericRMIAdapter.getSingleURLChooser(hostname, port, rmiBindingName,
                                                                          new XynaRMIClientSocketFactory(getRmiTimeout())), true);
  }

}
