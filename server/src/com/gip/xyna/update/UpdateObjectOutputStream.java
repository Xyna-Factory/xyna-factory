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
package com.gip.xyna.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;




public class UpdateObjectOutputStream extends ObjectOutputStream {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateObjectOutputStream.class);
  
  final Map<Class, Class> lookups;
  
  public UpdateObjectOutputStream(OutputStream out, Map<Class, Class> lookups) throws IOException {
    super(out);
    this.lookups = lookups;
  }
  
  
  @Override
  protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
    ObjectStreamClass descToWrite = desc;
    Set<Entry<Class, Class>> set = lookups.entrySet();    
    for (Entry<Class, Class> entry : set) {
      if (logger.isDebugEnabled()) {
        logger.debug("Incoming lookup for: " + desc.getName());
      }
      if (desc.getName().equals(entry.getKey().getName())) {
        if (logger.isDebugEnabled()) {
          logger.debug("RedirectionEntry found, sending lookup to: " + entry.getValue().getCanonicalName());
        }
        descToWrite = ObjectStreamClass.lookup(entry.getValue());
        break;
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("No RedirectionEntry for: " + desc.getName());
        }
      }
    }
    super.writeClassDescriptor(descToWrite);
  }

}
