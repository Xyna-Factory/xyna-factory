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
package xact.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;



public class IdentityStorableRepository implements XynaIdentityRepository {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(IdentityStorableRepository.class);
  
  private final static ODS ods = ODSImpl.getInstance();
  private final static ReadWriteLock identityLock = new ReentrantReadWriteLock();

  
  public IdentityStorableRepository() {
  }
  
  
  public boolean add(byte[] identityBytes) {
    return tryAdd(identityBytes, null) != null;
  }
  
  
  public Identity tryAdd(byte[] identityBytes, byte[] publicBytes) {
    Identity identity = JSchUtil.createIdentity(identityBytes, publicBytes, "Placeholder");
    IdentityStorable newIdentity = identityToStorable(identity);
    JSchUtil.adjustIdentityName(identity, generateIdentityName(newIdentity));
    newIdentity.setName(identity.getName());
    newIdentity.setPrivatekey(identityBytes);
    newIdentity.setPublickey(publicBytes);
    identityLock.writeLock().lock();
    try {
      Collection<IdentityStorable> identities;
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        identities = con.loadCollection(IdentityStorable.class);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not load identities", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
      for (IdentityStorable identityStorable : identities) {
        CheckResult result = check(identityStorable, newIdentity);
        if (result == CheckResult.OK) {
          return null; // already contained
        }
      }
      //add as new
      con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        con.persistObject(newIdentity);
        con.commit();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.writeLock().unlock();
    }
    return identity;
  }


  public Vector<Identity> getIdentities() {
    identityLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<IdentityStorable> storables = con.loadCollection(IdentityStorable.class);
        Vector<Identity> result = new Vector<Identity>();
        for (IdentityStorable identity : storables) {
          result.add(storableToIdentity(identity));
        }
        return result;
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.readLock().unlock();
    }
  }


  public String getName() {
    return "IdentityStorableRepository@" + System.identityHashCode(this);
  }


  public int getStatus() {
    return IdentityRepository.RUNNING;
  }


  public boolean remove(byte[] identity) {
    identityLock.writeLock().lock();
    try {
      Collection<IdentityStorable> identities;
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        identities = con.loadCollection(IdentityStorable.class);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
      Collection<IdentityStorable> toDelete = new ArrayList<IdentityStorable>();
      for (IdentityStorable identityStorable : identities) {
        /*if (Arrays.equals(identityStorable.getPrivatekey(), identity)) {
          toDelete.add(identityStorable);
        }*/
        Identity id = JSchUtil.createIdentity(identityStorable.getPrivatekey(), identityStorable.getPublickey(), generateIdentityName(identityStorable));
        if (Arrays.equals(JSchUtil.exractPrivateKey(id), identity)) {
          toDelete.add(identityStorable);
        }
      }
      if (toDelete.size() <= 0) {
        return false;
      } else {
        con = ods.openConnection(ODSConnectionType.DEFAULT);
        try {
          con.delete(toDelete);
          con.commit();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("", e);
        } finally {
          try {
            con.closeConnection();
          } catch (PersistenceLayerException e) {
            logger.debug("Error while trying to close connection",e);
          }
        }
        return true;
      }
    } finally {
      identityLock.writeLock().unlock();
    }
  }


  public void removeAll() {
    identityLock.writeLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        con.deleteAll(IdentityStorable.class);
        con.commit();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.writeLock().unlock();
    }
  }



  public Identity update(byte[] identityBytes, byte[] publicBytes) {
    removeAll();
    return tryAdd(identityBytes, publicBytes);
  }
  
  
  private IdentityStorable identityToStorable(Identity identity) {
    return new IdentityStorable(identity.getName(),
                                EncryptionType.getBySshStringRepresentation(identity.getAlgName()).getStringRepresentation(),
                                identity.getPublicKeyBlob(),
                                JSchUtil.exractPrivateKey(identity));
  }
  
  
  private Identity storableToIdentity(IdentityStorable identity) {
    return JSchUtil.createIdentity(identity.getPrivatekey(), identity.getPublickey(), generateIdentityName(identity));
  }

  public void shutdown() {
    try {
      if (!ods.isSamePhysicalTable(IdentityStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        ods.replace(IdentityStorable.class, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    } catch (PersistenceLayerException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    }
  }
  
  
  public CheckResult check(IdentityStorable one, IdentityStorable another) { // TODO name? O.o
    if (one.getType().equals(another.getType())) {
      if (Arrays.equals(one.getPublickey(), another.getPublickey()) &&
          Arrays.equals(one.getPrivatekey(), another.getPrivatekey()) ) {
        return CheckResult.OK;
      } else {
        return CheckResult.CHANGED;
      }
    } else {
      return CheckResult.NOT_INCLUDED;
    }
  }
  
  
  private static String generateIdentityName(IdentityStorable identity) {
    return "Identity@" + Long.toString(identity.getId());
  }

  public void init() {
    try {
      ods.registerStorable(IdentityStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("", e);
    }
    try {
      if (!ods.isSamePhysicalTable(IdentityStorable.TABLE_NAME, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT)) {
        ods.replace(IdentityStorable.class, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      throw new RuntimeException("Could not initialize IdentityStorableRepository.", e);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not initialize IdentityStorableRepository.", e);
    }
  }

}
