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

package com.gip.xyna.xprc.remotecallserialization;



import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.IntArray;
import com.gip.xyna.BijectiveMap;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization.Deserializer;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization.RevisionSerialization;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class KryoRevisionSerialization implements RevisionSerialization {

  private static final RuntimeContextDependencyManagement rcdm =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

  private XynaXmomSerialization.Serializer serializer;
  private Map<String, Deserializer> deserializers;
  private Kryo kryo;
  private IntArray kryoIntArray;
  private Long revision;
  private Object _lock = new Object();
  
  
  private final BijectiveMap<Class<?>, Integer> classIds;
  
  
  private Logger logger = CentralFactoryLogging.getLogger(getClass());
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  private final Serializer<GeneralXynaObjectList> emptySerializer = new Serializer() {

    @Override
    public Object read(Kryo arg0, Input arg1, Class arg2) {
      return null;
    }


    @Override
    public void write(Kryo arg0, Output arg1, Object arg2) {
    }

  };


  public KryoRevisionSerialization(Long revision) {
    this.revision = revision;
    deserializers = new ConcurrentHashMap<String, Deserializer>();
    
    classIds = new BijectiveMap<Class<?>, Integer>();
    kryo = createKryo();
    
    ClassLoader classLoader = getClassLoaderOfRevision(revision);
    kryo.setClassLoader(classLoader);
    
    
    try {
      Field kryoReadReferences = Kryo.class.getDeclaredField("readReferenceIds");
      kryoReadReferences.setAccessible(true);
      kryoIntArray = (IntArray) kryoReadReferences.get(kryo);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    serializer = new XynaXmomSerialization.Serializer() {

      @Override
      public byte[] serialize(GeneralXynaObject obj) {
        Output output = new Output(1, -1);
        synchronized (kryo) {
          kryo.writeObjectOrNull(output, obj, obj.getClass());
        }
        return output.getBuffer();
      }
    };

  }
  

  private ClassLoader getClassLoaderOfRevision(Long revision) {
    ClassLoader cl;
    try {
      cl = new RevisionClassloader(revision);
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      throw new RuntimeException(e);
    }

    return cl;

  }
  
  /**
   * delegiert alle classloading versuche an entsprechende mdm-/exceptionclassloader
   */
  private static class RevisionClassloader extends ClassLoader {

    private final Long revision;


    protected RevisionClassloader(Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {
      super(KryoRevisionSerialization.class.getClassLoader());
      this.revision = revision;
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        // erst bei parent schauen, dann bei sich selbst
        ClassLoader parent = getParent();
        try {
          c = parent.loadClass(name);
        } catch (ClassNotFoundException e) {
          // ignorieren
        }
      }
      if (c == null) {
        c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .loadMDMClass(name, false, null, null, revision);
      }
      if (c == null) {
        c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .loadExceptionClass(name, false, null, null, revision);
      }
      if (c == null) {
        throw new ClassNotFoundException(name);
      }
      if (resolve) {
        resolveClass(c);
      }

      return c;
    }

  }


  private Kryo createKryo() {
    Kryo kryo = new Kryo();
    
    registerSpecialClasses(kryo);

    InstantiatorStrategy strategy = createInstatiatorStrategy();
    kryo.setInstantiatorStrategy(strategy);
    FieldSerializerFactory factory = createFieldSerializerFactory();
    factory.getConfig().setSerializeTransient(true);
    kryo.setReferences(true);
    kryo.setDefaultSerializer(factory);
    kryo.setRegistrationRequired(false);
    logger.debug("created Kryo object. nextId: " + kryo.getNextRegistrationId());;

    return kryo;
  }


  private FieldSerializerFactory createFieldSerializerFactory() {
    FieldSerializerFactory result = new FieldSerializerFactory() {

      @SuppressWarnings("rawtypes")
      @Override
      public FieldSerializer newSerializer(Kryo kryo, Class type) {
        FieldSerializer result = super.newSerializer(kryo, type);
        try {
          result.removeField(DOM.INSTANCE_METHODS_IMPL_VAR);
        } catch (IllegalArgumentException e) {
          //class does not have instanceMethods
        }
        return result;
      }
    };
    return result;
  }


  private InstantiatorStrategy createInstatiatorStrategy() {
    return new InstantiatorStrategy() {

      DefaultInstantiatorStrategy defaultStrat = new DefaultInstantiatorStrategy();
      ObjectInstantiator versionInstantiator = new ObjectInstantiator() {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Object newInstance() {
          return new XOUtils.Version(-6, null);
        }
      };
      ObjectInstantiator listWithConstantSizeInstantiator = new ObjectInstantiator() {

        @Override
        public Object newInstance() {
          return new ArrayList<String>();
        }

      };

      ObjectInstantiator containerInstantiator = new ObjectInstantiator() {

        @Override
        public Object newInstance() {
          return new Container();
        }
      };

      ObjectInstantiator generalXynaObjectListInstantiator = new ObjectInstantiator() {

        @Override
        public Object newInstance() {
          return new GeneralXynaObjectList<GeneralXynaObject>(GeneralXynaObject.class);
        }
      };


      @Override
      public ObjectInstantiator newInstantiatorOf(@SuppressWarnings("rawtypes") Class arg0) {
        if (arg0 == XOUtils.Version.class) {
          return versionInstantiator;
        } else if (arg0 == XOUtils.ListWithConstantSize.class) {
          return listWithConstantSizeInstantiator;
        } else if (arg0 == Container.class) {
          return containerInstantiator;
        } else if (arg0 == GeneralXynaObjectList.class) {
          return generalXynaObjectListInstantiator;
        }
        return defaultStrat.newInstantiatorOf(arg0);
      }
    };
  }


  private void registerSpecialClasses(Kryo kryo) {
    
    List<Class<?>> regularSpecialClasses = new ArrayList<Class<?>>();
    regularSpecialClasses.add(String[].class);
    regularSpecialClasses.add(short[].class);
    regularSpecialClasses.add(int[].class);
    regularSpecialClasses.add(Class.class);
    regularSpecialClasses.add(java.util.Collections.EMPTY_LIST.getClass());
    regularSpecialClasses.add(java.util.ArrayList.class);
    regularSpecialClasses.add(java.util.LinkedList.class);
    regularSpecialClasses.add(Container.class);
    regularSpecialClasses.add(GeneralXynaObject[].class);
    regularSpecialClasses.add(GeneralXynaObject.class);
    regularSpecialClasses.add(XynaObject[].class);
    regularSpecialClasses.add(XynaObject.class);
    regularSpecialClasses.add(XynaObjectList[].class);
    
    for(Class<?> clazz : regularSpecialClasses) {
      kryo.register(clazz);
    }


    @SuppressWarnings("rawtypes")
    Serializer XynaObjectListSerializer = createXynaObjectListSerializer();
    kryo.register(GeneralXynaObjectList.class, XynaObjectListSerializer);
    kryo.register(XynaObjectList.class, XynaObjectListSerializer);
    
    kryo.register(Object[].class, emptySerializer);
    kryo.register(StackTraceElement.class, emptySerializer);
    kryo.register(StackTraceElement[].class, emptySerializer);
    kryo.register(Throwable.class, emptySerializer);
    kryo.register(StackTraceElement[].class, emptySerializer);
    kryo.register(XOUtils.VersionedObject.class, emptySerializer);
    
    
    regularSpecialClasses.add(GeneralXynaObjectList.class);
    regularSpecialClasses.add(XynaObjectList.class);  

    regularSpecialClasses.add(Object[].class);
    regularSpecialClasses.add(StackTraceElement.class);
    regularSpecialClasses.add(StackTraceElement[].class);
    regularSpecialClasses.add(Throwable.class);
    regularSpecialClasses.add(StackTraceElement[].class);
    regularSpecialClasses.add(XOUtils.VersionedObject.class);
    
    for(Class<?> clazz : regularSpecialClasses) {
      classIds.put(clazz, kryo.getRegistration(clazz).getId());
    }
    
  }


  //Remote call inputs are always GeneralXynaObjectList - objects.
  //However, the workflow expects a XynaObjectList if Input is a List of DataTypes
  //Lists of ExceptionTypes need to remain a GeneralXynaObjectList
  //XML changed the type accordingly.
  //objToSet.getClass() => com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList
  //XynaObject.generalFromXml(objToSet.toXml(), getRevision()) => com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList
  @SuppressWarnings("rawtypes")
  private Serializer createXynaObjectListSerializer() {
    Serializer result;
    result = new Serializer<GeneralXynaObjectList<?>>() {

      @SuppressWarnings("unchecked")
      @Override
      public GeneralXynaObjectList<?> read(Kryo arg0, Input arg1, Class<? extends GeneralXynaObjectList<?>> arg2) {
        String xmlname = arg1.readString();
        String xmlpath = arg1.readString();
        Class<? extends GeneralXynaObject> containedClass = determineXmomClass(xmlpath + "." + xmlname, revision);
        int length = arg1.readInt(true);
        
        Map<Integer, Class<?>> dynamicClasses = new HashMap<Integer, Class<?>>();

        boolean isExceptionType = XynaException.class.isAssignableFrom(containedClass);
        
        List<GeneralXynaObject> exceptionList = isExceptionType ? new ArrayList<>(length) : null;
        List<XynaObject> datatypeList = isExceptionType ? null : new ArrayList<>(length);
        Consumer<Object> adder = (isExceptionType) ? (o) -> exceptionList.add((GeneralXynaObject) o): (o) -> datatypeList.add((XynaObject) o);

        for (int i = 0; i < length; i++) {
          boolean newClass = arg1.readBoolean();
          int id = arg1.readInt(true);
          if (newClass) {
            String fqn = arg1.readString();
            Class<? extends GeneralXynaObject> nClass = determineXmomClass(fqn, revision);
            dynamicClasses.put(id, nClass);
          }
          Class<?> targetClass = classIds.getInverse(id);
          if (targetClass == null) {
            targetClass = dynamicClasses.get(id);
          }
          Object obj = arg0.readObjectOrNull(arg1, targetClass);
          adder.accept(obj);
        }

        GeneralXynaObjectList<?> result = null;
        if (isExceptionType) {
          result = new GeneralXynaObjectList<GeneralXynaObject>(exceptionList, xmlname, xmlpath);
        } else {
          result = new XynaObjectList<XynaObject>(datatypeList, (Class<XynaObject>) containedClass);
        }
        
        return result;
      }


      @Override
      public void write(Kryo arg0, Output arg1, GeneralXynaObjectList<?> arg2) {
        String originalXmlFqn = arg2.getOriginalXmlPath() + "." + arg2.getOriginalXmlName();
        arg1.writeString(arg2.getOriginalXmlName());
        arg1.writeString(arg2.getOriginalXmlPath());
        
        int nextId = arg0.getNextRegistrationId(); //lowest free id
        Map<Class<?>, Integer> dynamicClasses = new HashMap<Class<?>, Integer>();
        
        Object[] data = arg2.toArray();
        arg1.writeInt(data.length, true);
        Class<?> currentClass;
        for (int i = 0; i < data.length; i++) {
          currentClass = determineClassWithFallback(data[i], originalXmlFqn, revision);
          Integer id = classIds.get(currentClass);
          boolean newId = id == null;
          arg1.writeBoolean(newId); //true -> next is class name
          if(newId) {
            id = dynamicClasses.get(currentClass);
            if(id == null) {
              
              id = nextId++;
              dynamicClasses.put(currentClass, id);
            }
          }
          arg1.writeInt(id, true);
          if(newId) {
            arg1.writeString(currentClass.getCanonicalName());
          }
          arg0.writeObjectOrNull(arg1, data[i], currentClass);
        }
      }
    };
    return result;
  }
  
  
  //object may be null
  private Class<?> determineClassWithFallback(Object obj, String originalXmlFqn, Long revision) {
    if (obj != null) {
      return obj.getClass();
    }

    return determineXmomClass(originalXmlFqn, revision);
  }


  @Override
  public XynaXmomSerialization.Serializer getSerializer(Class<? extends GeneralXynaObject> clazz) {
    return serializer;
  }


  @Override
  public Deserializer getDeserializer(String fqn) {
    Deserializer ds;
    ds = deserializers.get(fqn);
    if (ds != null) {
      return ds;
    }

    synchronized (_lock) {
      ds = deserializers.get(fqn);
      if (ds != null) {
        return ds;
      }
      ds = new Deserializer() {

        Class<? extends GeneralXynaObject> classToConvert = determineXmomClass(fqn, revision);


        @Override
        public GeneralXynaObject deserialize(Long revision, byte[] data) {
          Input kryoInput = new Input(data, 0, data.length);
          GeneralXynaObject result;

          synchronized (kryo) {
            result = kryo.readObjectOrNull(kryoInput, classToConvert);
            kryoIntArray.clear();
          }
          
          return result;
        }
      };
      deserializers.put(fqn, ds);
    }
    return ds;
  }


  @SuppressWarnings("unchecked")
  public static Class<? extends GeneralXynaObject> determineXmomClass(String fqn, Long revision) {
    Class<? extends GeneralXynaObject> xmomClass = null;

    if (fqn.equals(Container.class.getCanonicalName())) {
      return Container.class;
    } else if (fqn.equals(GeneralXynaObjectList.class.getCanonicalName())) {
      return GeneralXynaObjectList.class;
    } else if (fqn.equals(XynaObjectList.class.getCanonicalName())) {
      return XynaObjectList.class;
    }


    if (GenerationBase.isReservedServerObjectByFqClassName(fqn)) {
      try {
        xmomClass = (Class<? extends GeneralXynaObject>) GenerationBase.class.getClassLoader().loadClass(fqn);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else if (GenerationBase.isReservedServerObjectByFqOriginalName(fqn)) {
      xmomClass = (Class<? extends GeneralXynaObject>) GenerationBase.getReservedClass(fqn);
    } else {
      Long correctRevision = rcdm.getRevisionDefiningXMOMObject(fqn, revision);
      ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
      ClassLoaderBase cl = null;
      try {
        cl = cld.getClassLoaderByType(ClassLoaderType.MDM, fqn, correctRevision);
      } catch (Exception e) {
        cl = null;
      }
      if (cl == null) {
        try {
          cl = cld.getClassLoaderByType(ClassLoaderType.Exception, fqn, correctRevision);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      if (cl == null) {
        return null;
      }
      try {
        xmomClass = (Class<? extends GeneralXynaObject>) cl.loadClass(fqn);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return xmomClass;
  }


  public void close() {
  }
}
