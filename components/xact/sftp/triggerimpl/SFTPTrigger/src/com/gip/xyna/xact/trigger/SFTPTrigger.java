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
package com.gip.xyna.xact.trigger;



import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.ByteUtil;
import org.apache.sshd.server.sftp.CustomContent;
import org.apache.sshd.server.sftp.MyAuthenticator;
import org.apache.sshd.server.sftp.SFTPInputListener;
import org.apache.sshd.server.sftp.SftpSubsystem;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv6ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.database.ClientKeyStorable;
import com.gip.xyna.xact.trigger.database.ClientPasswordStorable;
import com.gip.xyna.xact.trigger.database.FileContentStorable;
import com.gip.xyna.xact.trigger.database.FirmwareMgmtStorable;
import com.gip.xyna.xact.triggers.SFTPTRIGGER_COULD_NOT_BE_STARTED;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdnc.xnwh.XynaContentStorable;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class SFTPTrigger extends EventListener<SFTPTriggerConnection, SFTPTriggerStartParameter>
                implements
                  SFTPInputListener,IPropertyChangeListener {

  private static Logger logger = CentralFactoryLogging.getLogger(SFTPTrigger.class);

  private static SshServer sshd;
  private ODS ods;

  //private Collection<FileContentStorable> filecontent = null;
  private Collection<ClientKeyStorable> clientkeys = null;
  private Collection<ClientPasswordStorable> clientpasswords = null;
  //private Collection<XynaContentStorable> xynacontent = null;
  private Collection<FirmwareMgmtStorable> firmwaremgmt = null;
  
  
  private BlockingQueue<QueueEntry> requests = new ArrayBlockingQueue<QueueEntry>(10);
  //private BlockingQueue<QueueEntry> replies = new ArrayBlockingQueue<QueueEntry>(10);
  private ConcurrentHashMap<Long, String> replies = new ConcurrentHashMap<Long, String>(10);

  public static final String RELOAD = "xact.sftp.config.reload";
  public static final String RELOADFIRMWARETIME = "xact.sftp.config.reloadfirmwaretime";
  
  private InetAddress ip;
  private int port;
  private boolean passwordauth;
  private boolean publickeyauth;
  private boolean alwaysauthenticateoption;
  
  private long firmwaretimestamp = 0;
  private int reloadfwtime = 30;

  public SFTPTrigger() {
  }


  private void loadFirmwareMgmtDB()
  {
    logger.debug("Loading FirmwareMgmt DB ...");
    try
    {
      firmwaremgmt = loadFirmwareMgmtEntries();
      firmwaretimestamp = System.currentTimeMillis();
      //xynacontent = loadXynaContentEntries();
    }
    catch(PersistenceLayerException e)
    {
      logger.warn("SFTPTrigger: Problems loading from Persistencelayer: ", e);
    }
  }
  
  private void loadFromDB() {
    try {
      //filecontent = loadFileContentEntries();
      clientkeys = loadClientKeyEntries();
      clientpasswords = loadUserPasswordEntries();
      //xynacontent = loadXynaContentEntries();
    }
    catch (PersistenceLayerException e) {
      logger.warn("SFTPTrigger: Problems loading from Persistencelayer: ", e);
    }

  }


  private void init(InetAddress ip, int port, boolean password, boolean publickey, boolean alwaysauthenticate) {

    try
    {
      reloadfwtime = Integer.parseInt(XynaFactory.getInstance().getFactoryManagement().getProperty(RELOADFIRMWARETIME));
    }
    catch(Exception e)
    {
      logger.info("No refreshtime for FirmwareManagment configured. Using 30 seconds ...");
      reloadfwtime = 30;
    }
    
    sshd = SshServer.setUpDefaultServer();
    sshd.setPort(port);
    sshd.setHost(ip.getHostAddress());
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey2.ser","RSA"));
    // sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));
    // sshd.setCommandFactory(new ScpCommandFactory());


    List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
    if(publickey)userAuthFactories.add(new UserAuthPublicKey.Factory());
    if(password)userAuthFactories.add(new UserAuthPassword.Factory());

    if(!password && !publickey)userAuthFactories.add(new UserAuthNone.Factory());


    sshd.setUserAuthFactories(userAuthFactories);
    

    List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();

    SftpSubsystem.Factory sftpfactory = new SftpSubsystem.Factory();

    namedFactoryList.add(sftpfactory);
    sshd.setSubsystemFactories(namedFactoryList);

    String initdir = System.getProperty("user.dir");

    ScpCommandFactory scpcf = new ScpCommandFactory();
    ScpCommandFactory.setInputListener(this);



    loadFromDB();


//    if (filecontent != null) {
//      SftpSubsystem.clearCustomContent();
//      ScpCommandFactory.clearCustomContent();
//      for (FileContentStorable f : filecontent) {
//        SftpSubsystem.addCustomContent(initdir + "/" + f.getFile(), f.getContent(), f.getFileType());
//        ScpCommandFactory.addCustomContent(initdir + "/" + f.getFile(), f.getContent(), f.getFileType());
//        }
//    }
//    else {
//      logger.info("SFTPTrigger: No Files with Content given!");
//    }


    SftpSubsystem.setLimitAccess(true);
    SftpSubsystem.setInitialDir(initdir);


    MyAuthenticator auth = new MyAuthenticator(new HashMap<String, String>(),new HashMap<String,String>(), alwaysauthenticate, logger);

    if (clientkeys != null) {
      for (ClientKeyStorable s : clientkeys) {
        auth.addUserKey(s.getName(), s.getPublickey());
      }
    }
    
    if (clientpasswords != null) {
      for (ClientPasswordStorable s : clientpasswords) {
        auth.addUserPassword(s.getUsername(), s.getPassword());
      }
    }
    
    
    sshd.setPublickeyAuthenticator(auth);
    sshd.setPasswordAuthenticator(auth);

    
    
    SftpSubsystem.setSFTPInputListener(this);
    sshd.setCommandFactory(scpcf);
  }


  public Collection<ClientKeyStorable> loadClientKeyEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(ClientKeyStorable.class);
    }
    finally {
      connection.closeConnection();
    }

  }

  
  public Collection<ClientPasswordStorable> loadUserPasswordEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(ClientPasswordStorable.class);
    }
    finally {
      connection.closeConnection();
    }

  }


//  public Collection<FileContentStorable> loadFileContentEntries() throws PersistenceLayerException {
//    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
//    try {
//      return connection.loadCollection(FileContentStorable.class);
//    }
//    finally {
//      connection.closeConnection();
//    }
//
//  }

  
  public XynaContentStorable loadXynaContentWithId(long id) throws PersistenceLayerException {
    synchronized(SFTPTrigger.class)
    {
      logger.debug("Myclass: "+SFTPTrigger.class+"@"+System.identityHashCode(SFTPTrigger.class)+" called loadXynaContentWithId");
      ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        XynaContentStorable result = new XynaContentStorable(id);
        try {
          connection.queryOneRow(result);
        }
        catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.error("No Xynacontent with id "+id+" found!");
          return null;
        }
        catch(Throwable e)
        {
          logger.error("Error loading Xynacontent with id "+id+": ",e);
          return null;
        }
        logger.debug("Myclass: "+SFTPTrigger.class+"@"+System.identityHashCode(SFTPTrigger.class)+" finished with loadXynaContentWithId");
        return result;
      }
      finally {
        connection.closeConnection();
      }
    }

  }

  public Collection<FirmwareMgmtStorable> loadFirmwareMgmtEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(FirmwareMgmtStorable.class);
    }
    finally {
      connection.closeConnection();
    }

  }

  

  public void createListOfFileContentEntry(List<FileContentStorable> liste) throws PersistenceLayerException {


    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      connection.persistCollection(liste);
      connection.commit();


    }
    finally {
      connection.closeConnection();
    }

  }


  public void createListOfClientKeyEntry(List<ClientKeyStorable> liste) throws PersistenceLayerException {


    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      connection.persistCollection(liste);
      connection.commit();


    }
    finally {
      connection.closeConnection();
    }

  }


  // private void temporaryInitialDBFill() {
  // List<ClientKeyStorable> cl = new ArrayList<ClientKeyStorable>();
  //
  // ClientKeyStorable cks = new ClientKeyStorable();
  // cks.setIdentifier(1);
  // cks.setName("root");
  // cks.setType("RSA");
  // cks.setPublickey("0x30820122300D06092A864886F70D01010105000382010F003082010A0282010100DAFA7E9B71527D62E582E6E8F3EAD2F3411FC7CF0D3FCBE58A5877B022665DB87A386877260EDB82727A5B8D4C952B85027EA9394DB64F2019CB2FAF28B2A517B16924CDCB02745268338C68615A5CE5D8A6FB35EAC1B1F4F7A4CACAEB0ECDDC679E1940DFA3E21291AF9204ABF51563FA4167CE7F4150B5EC44801872D32D8565E70F16DCC2031B322CBC1F8C437C814D4C7431F55CFFA98FD3D446A7197640A4D9FAD8E571CF7E848714C2235445202471209EDE01A016E6FE626BCF747FE328DED25D03EEFA544ACCB241568298ACFD8574FDD8C30920664C4C870495F573CF1A2ECB77DA50434505C9092A62ACD1B54AA7CAB54CDB42A78CDE09BE8AAFC50203010001");
  //
  // cl.add(cks);
  //
  // List<FileContentStorable> fc = new ArrayList<FileContentStorable>();
  //
  // FileContentStorable fcs = new FileContentStorable("blabla", "Hallo!");
  // FileContentStorable fcs2 = new FileContentStorable("blub", "Hallo...");
  //
  // fc.add(fcs);
  // fc.add(fcs2);
  //
  // try {
  // createListOfClientKeyEntry(cl);
  // createListOfFileContentEntry(fc);
  // }
  // catch (PersistenceLayerException e) {
  // logger.warn("Error persisting temporary initial fill: ", e);
  // }
  //
  // }


  private void initDB() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(ClientKeyStorable.class);
      //ods.registerStorable(FileContentStorable.class);
      ods.registerStorable(ClientPasswordStorable.class);
      ods.registerStorable(XynaContentStorable.class);
      ods.registerStorable(FirmwareMgmtStorable.class);
    }
    catch (Exception e) {
      logger.warn("SFTPTrigger: InitDB failed: ", e);
    }
  }


  public void start(SFTPTriggerStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {


    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
    .addPropertyChangeListener(this);
    
    initDB();
    try {
    init(sp.getIP(), sp.getPort(), sp.getPasswordAuth(), sp.getPublicKeyAuth(), sp.getAlwaysAuth());

    ip = sp.getIP();
    } catch (XACT_NetworkInterfaceNotFoundException e) {
      throw new SFTPTRIGGER_COULD_NOT_BE_STARTED(e);
    } catch (XACT_InterfaceNoIPv6ConfiguredException e) {
      throw new SFTPTRIGGER_COULD_NOT_BE_STARTED(e);
    } catch (XACT_InterfaceNoIPConfiguredException e) {
      throw new SFTPTRIGGER_COULD_NOT_BE_STARTED(e);
    } 
    port = sp.getPort();
    passwordauth = sp.getPasswordAuth();
    publickeyauth = sp.getPasswordAuth();
    alwaysauthenticateoption = sp.getAlwaysAuth();
    
    ExceptionMonitor.setInstance(new ExceptionMonitor() {
      public void exceptionCaught(Throwable t) {
      logger.error("Error from ExceptionMonitor",t);
      };
      });
    
    try {
      sshd.start();
    }
    catch (Exception e) {
      logger.error("Problems starting SSH Server: " + e);
      throw new SFTPTRIGGER_COULD_NOT_BE_STARTED(e);
    }


  }

  
  private void refreshConfiguration()
  {
    logger.debug("Refreshing Configuration ...");
    try {
      sshd.stop();
      //sshd.stop(true);
    }
    catch (InterruptedException e) {
      logger.error("Problems stopping SSH Server: ",e);
    }
    
    logger.info("Waiting 5 seconds for ssh server to stop!");
    try {
      Thread.sleep(5000);
    }
    catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      logger.error("Error waiting: ",e1);
    }
    
    init(ip, port, passwordauth, publickeyauth, alwaysauthenticateoption);
    
    
    try {
      sshd.start();
    }
    catch (IOException e) {
      logger.error("Problems starting SSH Server: ",e);
    }
    
    
  }
  

  public SFTPTriggerConnection receive() {
    QueueEntry req = null;
    while (true) {

       try {
        req = requests.poll(1000,TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException e) {
        logger.warn("Error polling from requests: ",e);
      }
       if(req!=null)
       {
         logger.debug("New SFTPTriggerConnection ...");
         return new SFTPTriggerConnection(replies,req);
       }
       
    }


  }


  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, SFTPTriggerConnection con) {
    // TODO implementation
  }


  /**
   * called by Xyna Processing to stop the Trigger. should make sure, that start() may be called again directly
   * afterwards. connection instances returned by the method receive() should not be expected to work after stop() has
   * been called.
   */
  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    try {
      sshd.stop();
    }
    catch (InterruptedException e) {
      throw new RuntimeException("Problems stopping SSH Server: " + e);
    }
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
    .removePropertyChangeListener(this);
    
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   * 
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(SFTPTriggerConnection con) {
    // TODO implementation
    // TODO update dependency xml file
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    // TODO implementation
    // TODO update dependency xml file
    return "SFTP Trigger. Receives SFTP Requests.";
  }


  public CustomContent getRequest(String path, String username, String password, String ip) {
    long timestamp = System.currentTimeMillis();
    logger.debug("Request from MINA SSH Server received. Timestamp: "+timestamp);
    logger.debug("Path requested: "+path);
    logger.debug("Username: "+username);
    logger.debug("Password: "+password);
    logger.debug("IP: "+ip);
    
    long id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    
    QueueEntry qe = new QueueEntry(id,path, username, password, ip);
    
    try {
      requests.put(qe);
    }
    catch (InterruptedException e) {
     logger.warn("Problems putting new Request in queue: ",e);
    }
    logger.debug("Request put to Queue!");
    
    String answer = null;
    int count = 0;
    boolean workflowsuccessful = true;
    while(answer == null)
    {
      count++;

      answer = replies.get(id);

      if(answer==null)
      {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException e) {
          logger.warn("Error while waiting for reply: ",e);
        }
      }
      else
      {
        replies.remove(id);
      }
      
      if(count>600)
      {
        logger.info("Timeout while waiting for Reply...");
        workflowsuccessful = false;
        break;
      }
    }
    
    
    
    if(workflowsuccessful) // nur wenn Workflow erfolgreich durchlief wegen Authentifizierung
    {
      if((System.currentTimeMillis()-firmwaretimestamp)/1000 > reloadfwtime)
      {
        loadFirmwareMgmtDB();
      }
      logger.debug("### Looking into firmwaremgmt for xynacontent ...");
      for(FirmwareMgmtStorable current:firmwaremgmt)
      {
        String fwmgmturl = XynaFactory.getInstance().getFactoryManagement().getProperty("xact.firmwareManagement.downloadPath")+current.getUrl();
        logger.debug("### firmwaremgmt entry ("+current.getId()+"): "+fwmgmturl+" requested Path: "+path);
        if(fwmgmturl.equals(path))
        {
              logger.debug("### found matching path, going to get xynacontent entry (firmware data) from MySQL ...");
              long starttime = System.currentTimeMillis();
              XynaContentStorable currentxynacontent=null;
              try {
                currentxynacontent = loadXynaContentWithId(current.getXynacontentid());
              }
              catch (PersistenceLayerException e) {
                logger.error("Error reading Xynacontent with id "+current.getXynacontentid()+": ",e);
              }
              
              logger.debug("### time to get xynacontent: "+((System.currentTimeMillis()-starttime)/1000)+" seconds");
              
              if(currentxynacontent!=null)
              {
                logger.debug("### xynacontent entry id : "+currentxynacontent.getId());
                if(currentxynacontent.getData()!=null)
                {
                  byte[] result = (byte[])currentxynacontent.getData();
                  logger.debug("### found data with size: "+result.length);
                  //logger.debug("Returning answer: "+answer);
                  timestamp = System.currentTimeMillis();
                  logger.debug("Returning result to MINA SSH Server. Timestamp: "+timestamp);
                  return new CustomContent("",result,"");
                }
              }
              else
              {
                logger.error("No Xynacontent found with id "+current.getXynacontentid());
              }
        }
      }
    }
    
    
    if(answer!=null)
    {
      timestamp = System.currentTimeMillis();
      logger.debug("Returning result to MINA SSH Server. Timestamp: "+timestamp);
      return new CustomContent(answer,null,"text");
    }
    else
    {
      timestamp = System.currentTimeMillis();
      logger.debug("Returning empty result to MINA SSH Server. Timestamp: "+timestamp);
      return new CustomContent("",null,"text");
    }
    
    

  }


  public void request(Buffer buffer) {
    {
      int length = -1;
      int type = -1;
      int id = -1;
      String content = "";
      try {
        length = buffer.getInt();
        type = buffer.getByte();
        id = buffer.getInt();
        content = buffer.getString();
      }
      catch (Exception e) {

      }
      logger.info("Received: ");
      logger.info("length: " + length);
      logger.info("type: " + type);
      logger.info("id: " + id);
      logger.info("content: " + content);
      logger.info("============================");
    }
  }


  public void sent(int laenge, byte[] data, int rpos) {
    logger.info("Sent: ");

    byte[] l = ByteUtil.toByteArray(laenge, 4);

    byte[] sentBytes = new byte[4 + data.length];

    System.arraycopy(l, 0, sentBytes, 0, 4);
    System.arraycopy(data, 0, sentBytes, 4, data.length);

    logger.info(ByteUtil.toHexValue(sentBytes));

    logger.info("Trying to decode:");
    int length = laenge;
    logger.info("length: " + length);

    try {
      int type = sentBytes[rpos + 4];
      int id = sentBytes[rpos + 5] * 256 * 256 * 256 + sentBytes[rpos + 6] * 256 * 256 + sentBytes[rpos + 7] * 256 + sentBytes[rpos + 8];
      String content = new String(sentBytes, rpos + 9, sentBytes.length - 9);

      logger.info("type: " + type);
      logger.info("id: " + id);
      logger.info("content: " + content);
    }
    catch (Exception e) {

    }
    logger.info("==============================");


  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> list = new ArrayList<String>();
    list.add(RELOAD);
    list.add(RELOADFIRMWARETIME);
    return list;
  }


  public void propertyChanged() {
    refreshConfiguration();
    
  }


}
