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
package gip.base.db;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;

import gip.base.callback.SimpleCallback;
import gip.base.common.OBContextInterface;
import gip.base.common.OBException;
import gip.base.db.drivers.OBDriverInterface;

/**
 * Allgemeingültiger Context für eine Session
 * für die Basis-Klassen.
 */
public class OBContext implements OBContextInterface {
  
  private transient static Logger logger = Logger.getLogger(OBContext.class);

  public static final String DATACON_TOKEN = ":dataCon"; //$NON-NLS-1$
  public static final String MESSCON_TOKEN = ":messCon"; //$NON-NLS-1$
  public static final String LOCKCON_TOKEN = ":lockCon"; //$NON-NLS-1$
  
  private OBConnectionInterface dataConnection;
  private OBConnectionInterface lockConnection;
  private OBConnectionInterface messConnection;

  private OBDriverInterface driver;

  private Hashtable<String,String> projectSchemata = new Hashtable<String,String>();

  private MessageGenerator messageGenerator;

  private long _staffId;
  private String _staffName=""; //$NON-NLS-1$
  private String _sessionId=null;
  private boolean _appInfoDataConSet=false;
  private boolean _appInfoMessConSet=false;
  private boolean _appInfoLockConSet=false;

  
  
  /** 
   * Eine Map zum Speichern von User-Objekten (z.B. zum Halten von CCB-Informationen), Attributes-API (vgl. HTTPSession etc.)
   */
  private Hashtable<String,Object> attributes;

  private SimpleCallback callBack;
  
  
  /**
   * 
   */
  public OBContext() {
    _sessionId = "" +(new Random(System.currentTimeMillis())).nextInt(1000000); //$NON-NLS-1$
    _staffName = ""; //$NON-NLS-1$
    dataConnection = null;
    lockConnection = null;
    messConnection = null;
    attributes = new Hashtable<String,Object>();
  }
  
  
  /**
   * Liefert die Daten-Connection. Wenn sie noch nicht existiert, wird sie erzeugt.
   * @return Daten-Connection
   */
  public OBConnectionInterface getDataConnection() {
    return dataConnection;
  }


  /**
   * Liefert die Lock-Connection. Wenn sie noch nicht existiert, wird sie erzeugt.
   * @return Lock-Connection
   */
  public OBConnectionInterface getLockConnection() {
    return lockConnection;
  }


  /**
   * Liefert die Message-Connection. Wenn sie noch nicht existiert, wird sie erzeugt.
   * @return Message-Connection
   */
  public OBConnectionInterface getMessConnection() {
    return messConnection;
  }


  /**
   * Setzt die Daten-Connection.
   * @param con neue Daten-Connection
   */
  public void setDataConnection(OBConnectionInterface con) {
    dataConnection = con;
    if (!(con instanceof OBDatabase)) {
      dataConnection = new OBDatabase(con);
    }
    if (con!=null) {
      setVSessionInfo(); // setVSessionInfo() greift auf dataConnection zu daher zum Schluss machen.
    }
  }


  /**
   * Setzt die Lock-Connection.
   * @param con neue Lock-Connection
   */
  public void setLockConnection(OBConnectionInterface con) {
    lockConnection = con;
    if (!(con instanceof OBDatabase)) {
      lockConnection = new OBDatabase(con);
    }
    if (con!=null) {
      setVSessionInfo(); // setVSessionInfo() greift auf lockConnection zu daher zum Schluss machen.
    }
  }


  /**
   * Setzt die Message-Connection.
   * @param con neue Message-Connection
   */
  public void setMessConnection(OBConnectionInterface con) {
    messConnection = con;
    if (!(con instanceof OBDatabase)) {
      messConnection = new OBDatabase(con);
    }
    if (con!=null) {
      setVSessionInfo(); // setVSessionInfo() greift auf messConnection zu daher zum Schluss machen.
    }
  }

  
  /**
   * @return MessageGenerator
   */
  public MessageGenerator getMessageGenerator() {
    return messageGenerator;
  }


  /**
   * @param mg
   */
  public void setMessageGenerator(MessageGenerator mg) {
    this.messageGenerator = mg;
  }


  /**
   * @return StaffName
   */
  public String getStaffName() {
    return _staffName;
  }


  /**
   * @param staffName
   */
  public void setStaffName(String staffName) {
    _staffName = staffName;
    setVSessionInfo();
  }


  /**
   * @return staffid
   */
  public long getStaffId() {
    return _staffId;
  }


  /**
   * @param staffId
   */
  public void setStaffId(long staffId) {
    _staffId = staffId;
  }


  /**
   * @return Treiber
   */
  public OBDriverInterface getDriver() {
    return driver;
  }


  /**
   * @param _driver
   */
  public void setDriver(OBDriverInterface _driver) {
    this.driver = _driver;
  }


  /**
   * @param project
   * @param schema
   */
  public void addProjectSchema(String project, String schema) {
    projectSchemata.put(project, schema);
  }


  /**
   * @param project
   * @return Schema zum Projekt
   */
  public String getSchema(String project) {
    return projectSchemata.get(project);
  }

  
  public Enumeration<String> getSchemata() {
    return projectSchemata.keys();
  }

  /**
   * Der Session-Identifier ist eine eindeutige ID für die angemeldete Session. 
   * @return Session-Identifier
   */
  public String getSessionIdentifier() {
    return _sessionId;
  }
  
  /**
   * Setzen der Session-Identifier (eindeutige ID für die angemeldete Session.) 
   * @param sessionId Session-Identifier
   */
  public void setSessionIdentifier(String sessionId) {
    _sessionId = sessionId;
  }


  /**
   * Der Session-Identifier mit dem gesetzten Staffname. 
   * @return <staffname>:<SessionId>
   */
  public String getSessionIdStaffname() {
    return _staffName+":"+_sessionId; //$NON-NLS-1$
  }

  
  /**
   * @param name
   * @return Attribute
   */
  public Object getAttribute(String name) {
    return attributes.get(name);
  }
  
  
  /**
   * @param name
   * @param object
   */
  public void setAttribute(String name, Object object) {
    attributes.put(name, object);
  }
  
  
  /**
   * @param name
   */
  public void removeAttribute(String name) {
    attributes.remove(name);
  }
  
  
  /**
   * @return Attribut-Namen
   */
  public Enumeration<String> getAttributeNames() {
    return attributes.keys();
  }

  
  /**
   * @return Returns the callBack.
   */
  public SimpleCallback getCallBack() {
    return callBack;
  }

  
  /**
   * @param callBack The callBack to set.
   */
  public void setCallBack(SimpleCallback callBack) {
    this.callBack = callBack; 
  }
  
  
  /**
   * @throws OBException
   */
  public void commit() throws OBException {
    getDataConnection().commit(this);
  }
  
  
  /**
   * @throws OBException
   */
  public void rollback() throws OBException {
    try {
      getDataConnection().rollback();
    }
    catch (SQLException e) {
      logger.error("Error while ROLLBACK",e); //$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.sqlFatalException);
    }
  }

  
  /**
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    destroy();
    super.finalize();
  }
  
  
  /**
   * Setzt den Context wieder zurueck.
   */
  public void destroy( ) {
    unsetVSessionInfo();
    setDataConnection(null);
    setMessageGenerator(null);
    setLockConnection(null);
    setMessageGenerator(null);
    setStaffName(""); //$NON-NLS-1$
    setStaffId(-1);
    projectSchemata = null;
    _appInfoDataConSet=false;
    _appInfoMessConSet=false;
    _appInfoLockConSet=false;
    
  }
  

  /**
   * Setzt die Felder v$session.module und v$session.action fuer die Data-Connection.
   * @param module 
   * @param action 
   * @throws OBException
   */
  public void setVSessionActionInfo(String module, String action) throws OBException {
    try {
      if (getDataConnection()!=null) {
        getDriver().setVSessionActionInfo(getDataConnection(), module, action);
      }
      if (getMessConnection()!=null) {
        getDriver().setVSessionActionInfo(getMessConnection(), module, action);
      }
      if (getLockConnection()!=null) {
        getDriver().setVSessionActionInfo(getLockConnection(), module, action);
      }
    }
    catch (OBException e) {
      logger.error("error setting sessioninfo",e); //$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.sqlFatalException);
    }
  }

  
  /**
   * Setzt das Feld v$session.client_info fuer alle Connections nach dem Schema
   * <staffName>:<sessionId>:<xyzCon>
   * @throws OBException
   */
  private void setVSessionInfo() {
    try {
      if (_staffName!=null && 
          _staffName.length()>0) {
        if (dataConnection!=null &&
            dataConnection.isClosed()==false &&
            _appInfoDataConSet==false) {
          _appInfoDataConSet=true;
          getDriver().setSessionInfo(getDataConnection(), getSessionIdStaffname()+DATACON_TOKEN);
        }
        if (messConnection!=null &&
            messConnection.isClosed()==false &&
            _appInfoMessConSet==false) {
          _appInfoMessConSet=true;
          getDriver().setSessionInfo(getMessConnection(),getSessionIdStaffname()+MESSCON_TOKEN);
        }
        if (lockConnection!=null &&
            lockConnection.isClosed()==false &&
            _appInfoLockConSet==false) {
          _appInfoLockConSet=true;
          getDriver().setSessionInfo(getLockConnection(),getSessionIdStaffname()+LOCKCON_TOKEN);
        }
      }
    }
    catch (Exception e) {
      logger.error("error setting sessioninfo",e); //$NON-NLS-1$
    }
  }

  
  /**
   * Setzt das Feld v$session.client_info fuer alle Connections wieder zurueck.
   */
  public void unsetVSessionInfo() {
    try {
      if (dataConnection!=null &&  
          dataConnection.isClosed()==false) {
        _appInfoDataConSet=false;
        getDriver().setSessionInfo(getDataConnection(),"");
      }
      if (messConnection!=null &&
          messConnection.isClosed()==false) {
        _appInfoMessConSet=false;
        getDriver().setSessionInfo(getMessConnection(),"");
      }
      if (lockConnection!=null &&
          lockConnection.isClosed()==false) {
        _appInfoLockConSet=false;
        getDriver().setSessionInfo(getLockConnection(),"");
      }
    }
    catch (Exception e) {
      logger.error("error unsetting sessioninfo",e); //$NON-NLS-1$
    }
  }


}
