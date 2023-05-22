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
package xact.mail.account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;

import xact.mail.account.MailAccountData.MailAccountProperty;
import xact.mail.account.MailAccountStorageException.Type;

public class MailAccountStorage {

  private static final Logger logger = CentralFactoryLogging.getLogger(MailAccountStorage.class);

  private HashParallelReentrantLock<String> persistLock = new HashParallelReentrantLock<>(8);
  private HashParallelReentrantLock<String> externalLock = new HashParallelReentrantLock<>(8);
  private ConcurrentHashMap<String,MailAccountData> mailAccounts = new ConcurrentHashMap<>();
  private ODSImpl ods;

  private static MailAccountStorage instance = new MailAccountStorage();

  public static MailAccountStorage getInstance() {
    return instance;
  }
  
  public void init() throws PersistenceLayerException {
    if( ods == null ) {
      ods = ODSImpl.getInstance();

      ods.registerStorable(MailAccountDataStorable.class);
      ods.registerStorable(MailAccountPropertyStorable.class);
      
      WarehouseRetryExecutor.buildCriticalExecutor().
      connection(ODSConnectionType.HISTORY).
      storables(new StorableClassList(MailAccountDataStorable.class, MailAccountPropertyStorable.class)).
      execute( new InitMailAccounts() );
    } else {
      logger.warn("MailAccountStorage is already initialized");
    }
  }
  
  public Collection<MailAccountData> getMailAccounts() {
    return Collections.unmodifiableCollection(mailAccounts.values());
  }
  
  public MailAccountData getMailAccount(String name) {
    return mailAccounts.get(name);
  }
  
  public void addNewMailAccount(MailAccountData mad) throws PersistenceLayerException, MailAccountStorageException {
    persist( mad.getName(), PersistMailAccountData.addNewMailAccount(mad) );
  }

  public void replaceMailAccount(MailAccountData mad) throws PersistenceLayerException, MailAccountStorageException {
    persist( mad.getName(), PersistMailAccountData.replaceMailAccount(mad) );
  }

  public void removeMailAccount(String name) throws PersistenceLayerException, MailAccountStorageException {
    persist( name, PersistMailAccountData.removeMailAccount(name) );
  }

  public void addMailAccountProperty(String name, MailAccountProperty map) throws PersistenceLayerException, MailAccountStorageException {
    persist( name, PersistMailAccountData.addMailAccountProperty(name, map) );
  }

  public void removeMailAccountProperty(String name, MailAccountProperty map) throws PersistenceLayerException, MailAccountStorageException {
    persist( name, PersistMailAccountData.removeMailAccountProperty(name, map) );
  }
  
  private void persist(String name, PersistMailAccountData persist) throws PersistenceLayerException, MailAccountStorageException {
    persistLock.lock(name);
    try {
      MailAccountData existing = mailAccounts.get(name);
      if( existing != null && persist.mustNotExist() ) {
        throw new MailAccountStorageException(Type.ALREADY_REGISTERED, name);
      }
      if( existing == null && persist.mustExist() ) {
        throw new MailAccountStorageException(Type.NOT_REGISTERED, name);
      }
      MailAccountData next = persist.persist();
      if( next == null ) {
        mailAccounts.remove(name);
      } else {
        mailAccounts.put(name, next);
      }
    } finally {
      persistLock.unlock(name);
    }
  }
  
  private static class PersistMailAccountData implements WarehouseRetryExecutableNoException<MailAccountData> {
    
    private enum Mode {
       ADD(false, true), 
       REPLACE(true, false), 
       REMOVE(true, false), 
       ADD_PROP(true, false), 
       REMOVE_PROP(true, false);
      
      private boolean mustNotExist;
      private boolean mustExist;

      private Mode(boolean mustExist, boolean mustNotExist) {
        this.mustExist = mustExist;
        this.mustNotExist = mustNotExist;
      }
      
      public boolean isMustExist() {
        return mustExist;
      }
      public boolean isMustNotExist() {
        return mustNotExist;
      }
    }
    
    private String name;
    private Mode mode;
    private MailAccountData mad;
    private String propertyKey;
    private MailAccountProperty property;

    public PersistMailAccountData(String name, Mode mode, 
                  MailAccountData mad, String propertyKey, MailAccountProperty property) {
      this.name = name;
      this.mode = mode;
      this.mad = mad;
      this.propertyKey = propertyKey;
      this.property = property;
    }

    public static PersistMailAccountData addNewMailAccount(MailAccountData mad) {
      return new PersistMailAccountData(mad.getName(), Mode.ADD, mad, null, null);
    }

    public static PersistMailAccountData replaceMailAccount(MailAccountData mad) {
      return new PersistMailAccountData(mad.getName(), Mode.REPLACE, mad, null, null);
    }
    
    public static PersistMailAccountData removeMailAccount(String name) {
      return new PersistMailAccountData(name, Mode.REMOVE, null, null, null);
    }

    public static PersistMailAccountData addMailAccountProperty(String name, MailAccountProperty map) {
      return new PersistMailAccountData(name, Mode.ADD_PROP, null, map.getKey(), map);
    }

    public static PersistMailAccountData removeMailAccountProperty(String name, MailAccountProperty map) {
      return new PersistMailAccountData(name, Mode.REMOVE_PROP, null, map.getKey(), null);
    }
    
    public boolean mustNotExist() {
      return mode.isMustNotExist();
    }

    public boolean mustExist() {
      return mode.isMustExist();
    }

    public MailAccountData persist() throws PersistenceLayerException {
      return WarehouseRetryExecutor.buildMinorExecutor().
        connection(ODSConnectionType.HISTORY).
        storables(new StorableClassList(MailAccountPropertyStorable.class, MailAccountPropertyStorable.class)).
        execute( this );
    }
    
    @Override
    public MailAccountData executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      switch( mode ) {
      case ADD:
      case REPLACE:
        con.persistObject( MailAccountDataStorable.of(mad) );
        break;
      case ADD_PROP:
        con.persistObject( MailAccountPropertyStorable.of(name, property) );
        break;
      case REMOVE:
        con.deleteOneRow( new MailAccountDataStorable(name) );
        break;
      case REMOVE_PROP:
        con.deleteOneRow( new MailAccountPropertyStorable(name, propertyKey) );
        break;
      default:
        throw new IllegalStateException("Unexpected mode "+mode);
      }
      return readMailAccountData(con);
    }

    private MailAccountData readMailAccountData(ODSConnection con) throws PersistenceLayerException {
      boolean foundMad = false;
      MailAccountDataStorable mads = new MailAccountDataStorable(name);
      try {
        con.queryOneRow(mads);
        foundMad = true;
      } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
        if( mode != Mode.REMOVE ) {
          throw new XNWH_GeneralPersistenceLayerException("Unexpected: MailAccountData not found for \""+name+"\"", e);
        }
      }
      List<MailAccountPropertyStorable> properties = MailAccountPropertyStorable.readAllPropertiesForName( con, name);
      
      if( mode == Mode.REMOVE ) {
        if( foundMad || ! properties.isEmpty() ) {
          throw new XNWH_GeneralPersistenceLayerException("Unexpected: MailAccountData found for \""+name+"\"");
        } else {
          return null;
        }
      }
      
      MailAccountData.Builder builder = new MailAccountData.Builder();
      mads.fill(builder);
      
      for( MailAccountPropertyStorable maps : properties ) {
         maps.fill(builder);
      }
      return builder.build();
    }
    
  }

  
  private class InitMailAccounts implements WarehouseRetryExecutableNoResult {
    
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      Collection<MailAccountPropertyStorable> allProps = con.loadCollection(MailAccountPropertyStorable.class);
      Map<String, ArrayList<MailAccountPropertyStorable>> gr = CollectionUtils.group(allProps, MailAccountPropertyStorable.toNameTransformation );

      for( MailAccountDataStorable mads : con.loadCollection(MailAccountDataStorable.class) ) {
        MailAccountData.Builder builder = new MailAccountData.Builder();
        mads.fill(builder);
        
        List<MailAccountPropertyStorable> props = gr.get(mads.getName());
        if( props != null ) {
          for( MailAccountPropertyStorable maps : props ) {
            maps.fill(builder);
          }
        }
        MailAccountData dup = mailAccounts.putIfAbsent(mads.getName(), builder.build());
        if( dup != null ) {
          logger.warn("Duplicate MailAccountData for "+mads.getName());
        }
      }
      
    }
  }


  /**
   * Lock, damit nicht MailAccounts mit gleichem Namen gleichzeitig verwendet werden. 
   * Ist beispielsweise bei POP3 wichtig, da der POP3-Server nur single-threaded angesprochen werden darf.
   * @param name
   */
  public void lock(String name) {
    externalLock.lock(name);
  }

  public void unlock(String name) {
    externalLock.unlock(name);
  }
}
