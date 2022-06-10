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
package com.gip.xyna.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;




public class UpdateObjectInputStream extends ObjectInputStream {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateObjectInputStream.class);
  
  final Map<String, Class> outdatedLookups;
  
  public UpdateObjectInputStream(InputStream in, Map<String, Class> outdatedLookups) throws IOException {
    super(in);
    this.outdatedLookups = outdatedLookups;
  }
  
  
  @Override
  protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
    ObjectStreamClass desc = super.readClassDescriptor();
    if (logger.isDebugEnabled()) {
      logger.debug("Incoming lookup for: " + desc.getName());
    }
    Set<Entry<String, Class>> set = outdatedLookups.entrySet();    
    for (Entry<String, Class> entry : set) {
      if (desc.getName().equals(entry.getKey())) {
        if (logger.isDebugEnabled()) {
          logger.debug("RedirectionEntry found, sending lookup to: " + entry.getValue().getCanonicalName());
        }
        return ObjectStreamClass.lookup(entry.getValue());
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("No RedirectionEntry for: " + desc.getName());
    }
    return desc;
  }

}
