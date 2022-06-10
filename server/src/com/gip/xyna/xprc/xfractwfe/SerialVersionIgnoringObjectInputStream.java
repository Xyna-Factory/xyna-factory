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
package com.gip.xyna.xprc.xfractwfe;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;

//TODO trennung von aufgaben in "behandlung von classloading dingen" und "serialversionignoring"
public class SerialVersionIgnoringObjectInputStream extends ObjectInputStream {
  
  private static Logger logger = CentralFactoryLogging.getLogger(SerialVersionIgnoringObjectInputStream.class);
  
  private Long revision; //bzgl dieser revision wird die aktuelle klasse ermittelt, die die soll-serialversionuid vorgibt.
  private static final Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
  
  public static void addClassDescriptorMapping(String sourceClassName, Class<?> targetClass) {
    classMap.put(sourceClassName, targetClass);
  }
  
  public static void clearClassDescriptorMappings() {
    classMap.clear();
  }
  
  public SerialVersionIgnoringObjectInputStream(InputStream in, Long revision) throws IOException {    
    super(in);
    this.revision = revision;
  }
  
  //ersetze in dem im stream gelesenen descriptor die serialversionuid durch die aktuelle serialversionuid
  @Override
  protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
    ObjectStreamClass desc = super.readClassDescriptor();
    String fqClassName = desc.getName();
    Class<?> clazz = classMap.get(fqClassName);
    if (clazz != null) {
      return ObjectStreamClass.lookup(clazz);
    }
    clazz = tryGenerationClassLoader(fqClassName); //class bzgl revision
    if (clazz == null) {
      return desc;
    } else {
      ObjectStreamClass test = invokeLookupWithDifferentClassLoader(clazz); //serialversionuid von clazz
      if (test == null) {
        test = ObjectStreamClass.lookup(clazz);
      }
      renewSerialVersionUID(desc, test); //set serialversionUID from test in desc
      return desc;
    }

  }

  // isnt this the same as ObjectStreamClass.lookup(clazz); because clazz.getClassLoader() should delegate to the factory cl?
  // if it wouldn't the cast would fail anyway 
  private ObjectStreamClass invokeLookupWithDifferentClassLoader(Class<?> clazz) {
    try {
      Class<?> objectStreamClass = clazz.getClassLoader().loadClass(ObjectStreamClass.class.getName());
      Method lookup = objectStreamClass.getDeclaredMethod("lookup", Class.class);
      Object obj = lookup.invoke(null, clazz);
      return (ObjectStreamClass) obj;
    } catch (ClassNotFoundException e) {
      logger.warn(null, e);
    } catch (SecurityException e) {
      logger.warn(null, e);
    } catch (NoSuchMethodException e) {
      logger.warn(null, e);
    } catch (IllegalArgumentException e) {
      logger.warn(null, e);
    } catch (IllegalAccessException e) {
      logger.warn(null, e);
    } catch (InvocationTargetException e) {
      logger.warn(null, e);
    }
    return null;
  }


  private Class<?> tryGenerationClassLoader(String fqClassName) {
    try {
      Class.forName(fqClassName);
      return null; //Klasse konnte mit AppClassLoader geladen werden
    } catch (ClassNotFoundException e1) {
      //weiter delegieren
    }
    
    //FIXME classloader dürfen nicht lazy erstellt werden, was sie durch die mock factory hier aber werden!
    ClassLoader cl =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .findClassLoaderByType(fqClassName, revision, ClassLoaderType.MDM, true);

    //ClassLoaderByType(ClassLoaderType.MDM, fqClassName, revision);
    if (cl == null) {
      cl =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .findClassLoaderByType(fqClassName, revision, ClassLoaderType.Exception, true);
      if (cl == null) {
        cl =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                .findClassLoaderByType(fqClassName, revision, ClassLoaderType.WF, true);
        if (cl == null) {
          if (fqClassName.contains("$")) {
            cl =
                XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                    .findClassLoaderByType(fqClassName.split("\\$")[0], revision, ClassLoaderType.WF, true);
          }
        }
      }
    }

    if (cl != null) {
      try {
        return cl.loadClass(fqClassName);
      } catch (ClassNotFoundException e) {
        return null;
      }
    } else {
      return null;
    }

  }
  
  // Necessary in java8 to circumvent the localCaches
  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    if (classMap.containsKey(desc.getName())) {
      return classMap.get(desc.getName());
    } else {
      try {
        return super.resolveClass(desc);
      } catch (ClassNotFoundException e) {
        try {
          Class<?> clazz = tryGenerationClassLoader(desc.getName());
          if (clazz == null) {
            return tryGenerationClassLoader(desc.getName());  
          } else {
            return clazz;
          }
        } catch (Throwable ee) {
          return tryGenerationClassLoader(desc.getName());
        }
      }
    }
  }
  
  
  private void renewSerialVersionUID(ObjectStreamClass oldClass, ObjectStreamClass currentClass) {
    if (oldClass.getSerialVersionUID() == currentClass.getSerialVersionUID()) {
      return;
    }
    Field suid = null;
    try {
      suid = oldClass.getClass().getDeclaredField("suid");
      suid.setAccessible(true);
    } catch (SecurityException e) {
      logger.error("Could not access SerialVersionUID-field of current generation",e);
    } catch (NoSuchFieldException e) {
      logger.error("No SerialVersionUID-field in current generation",e);
    }
    try {
      suid.set(oldClass, Long.valueOf(currentClass.getSerialVersionUID()));
    } catch (IllegalArgumentException e) {
      logger.error("Types of SerialVersionUID-fields in old and new ClassDescriptor do not match",e);
    } catch (IllegalAccessException e) {
      logger.error("Could not access SerialVersionUID-field of old generation",e);
    }
  }
 
  
}
