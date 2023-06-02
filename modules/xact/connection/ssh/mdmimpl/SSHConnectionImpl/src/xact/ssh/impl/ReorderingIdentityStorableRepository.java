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
package xact.ssh.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import com.jcraft.jsch.Identity;

import xact.ssh.EncryptionType;
import xact.ssh.XynaIdentityRepository;


public class ReorderingIdentityStorableRepository implements XynaIdentityRepository {
  
  private final XynaIdentityRepository wrapped;
  private final IdentityComparator comperator;
  
  public ReorderingIdentityStorableRepository(XynaIdentityRepository wrapped, EncryptionType prefered) {
    this.wrapped = wrapped;
    this.comperator = new IdentityComparator(prefered);
  }
  
  public Vector<Identity> getIdentities() {
    @SuppressWarnings("unchecked")
    Vector<Identity> identities = wrapped.getIdentities();
    Identity[] sortedArr = new Identity[identities.size()]; 
    identities.copyInto(sortedArr);
    Arrays.sort(sortedArr, comperator);
    Vector<Identity> sorted = new Vector<Identity>();
    for (Identity identity : sortedArr) {
      sorted.addElement(identity);
    }
    return sorted;
  };
  
  public Identity tryAdd(byte[] identityBytes, byte publicBytes[]) {
    return wrapped.tryAdd(identityBytes, publicBytes);
  }
  
  public void init() {
    wrapped.init();
  }
  
  public void shutdown() {
    wrapped.shutdown();
  }
  
  public String getName() {
    return wrapped.getName();
  }
  
  public int getStatus() {
    return wrapped.getStatus();
  }
  
  public boolean add(byte[] identity) {
    return wrapped.add(identity);
  }
  
  public boolean remove(byte[] blob) {
    return wrapped.remove(blob);
  }
  
  public void removeAll() {
    wrapped.removeAll();
  }
  
  
  private final static class IdentityComparator implements Comparator<Identity> {
    
    private final EncryptionType prefered;
    
    public IdentityComparator(EncryptionType prefered) {
      this.prefered = prefered;
    }

    public int compare(Identity o1, Identity o2) {
      EncryptionType o1Enc = EncryptionType.getBySshStringRepresentation(o1.getAlgName());
      EncryptionType o2Enc = EncryptionType.getBySshStringRepresentation(o2.getAlgName());
      int nameCompare = o1.getName().compareTo(o2.getName());
      if (o1Enc == prefered && o2Enc == prefered) {
        return nameCompare;
      } else if (o1Enc == prefered) {
        return -1;
      } else if (o2Enc == prefered) {
        return 1;
      } else {
        return nameCompare;
      }
    }
    
  }
  
  
}
