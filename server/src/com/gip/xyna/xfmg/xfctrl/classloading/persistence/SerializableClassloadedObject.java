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

package com.gip.xyna.xfmg.xfctrl.classloading.persistence;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.FilterClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;



/*
 *FIXME
 *konzept ähnlich zu classloaderobjectinputstream verwenden von apache
 *http://grepcode.com/file/repo1.maven.org/maven2/com.ning/metrics.serialization-all/2.0.0-pre5/org/apache/commons/io/input/ClassLoaderObjectInputStream.java
 */
public class SerializableClassloadedObject implements Serializable {

  private static final Logger logger = CentralFactoryLogging.getLogger(SerializableClassloadedObject.class);
  private static final long serialVersionUID = -4543656488374268288L;
  
  public static DeserializationFailedHandler deserializationFailedHandler;
    
  public static ClassLoaderDispatcher cld;
  
  public static ThreadLocal<Boolean> THROW_ERRORS = new ThreadLocal<Boolean>();
  
  public interface DeserializationFailedHandler {

    void failed(Object parent, String name);
    
  }

  private static boolean ignoreExceptionsWhileDeserializing = false;


  private transient Object object; 
  
  private final ClassLoaderType[] classLoaderType;
  private final String[] classLoaderID;
  private Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  private Long parentRevision = null;
  private int version; 
  private long[] revisions;
  private static final AtomicLong cnt = new AtomicLong(0);
  private final long uniqueId = cnt.getAndIncrement();
  private final String date = Constants.defaultUTCSimpleDateFormatWithMS().format(new Date());
  
  /**
   * für {@link SerializableClassloadedObject#deserializationFailedHandler} 
   */
  private transient boolean objectWasDeserializable;

  private static long determineRevision(Serializable object) {
    if (object == null) {
      return RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    ClassLoader objectsClassLoader = object.getClass().getClassLoader();
    if (objectsClassLoader instanceof ClassLoaderBase) {
      return ((ClassLoaderBase) objectsClassLoader).getRevision();
    }
    return RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  }


  private static Long determineParentRevision(Serializable object) {
    if (object == null) {
      return null;
    }
    ClassLoader objectsClassLoader = object.getClass().getClassLoader();
    if (objectsClassLoader instanceof FilterClassLoader) {
      return ((FilterClassLoader) objectsClassLoader).getParentRevision();
    }
    return null;
  }

  public SerializableClassloadedObject(Serializable object) {
    this(object, determineRevision(object), determineParentRevision(object));
  }

  public SerializableClassloadedObject(Serializable object, Long revision) {
    this(object, revision, null);
  }
  
  public SerializableClassloadedObject(Serializable object, Long revision, Long parentRevision) {
    this(object, (ClassLoader[]) null);
    this.revision = revision;
    this.parentRevision = parentRevision;
  }
  
  public static SerializableClassloadedObject useRevisionsIfReachable(Serializable object, Long revision, Long parentRevision) {
    if(object == null || !(object.getClass().getClassLoader() instanceof ClassLoaderBase))
      return new SerializableClassloadedObject(object, revision, parentRevision);
    
    ClassLoaderBase clb = (ClassLoaderBase)object.getClass().getClassLoader();
    long objRevision = clb.getRevision();
    if(canBeReached(objRevision, revision)) {
      return new SerializableClassloadedObject(object, revision, parentRevision);
    }
    Long objParentRevision = null;
    if(clb.getParent() != null && clb.getParent() instanceof ClassLoaderBase) {
      objParentRevision = ((ClassLoaderBase)clb).getParentRevision();
    }
    
    return new SerializableClassloadedObject(object, objRevision, objParentRevision);
  }
  
  private static boolean canBeReached(long revision, long targetRevision) {
    RuntimeContextDependencyManagement mgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<Long> revisions = new HashSet<Long>();
    mgmt.getDependenciesRecursivly(targetRevision, revisions);
    revisions.add(targetRevision);
    return revisions.contains(revision);
  }


  public SerializableClassloadedObject(Serializable object, ClassLoader... classLoaders) {
    //FIXME revision(s) von serialisierendem root-objekt verwenden, um kompatibilität bei änderung von dependency zu ermöglichen -> problem: service varianten!
    if (object == null) {
      classLoaderType = null;
      classLoaderID = null;
    } else if (classLoaders == null) {
      classLoaderType = new ClassLoaderType[1];
      classLoaderID = new String[1];
      ClassLoader cl = object.getClass().getClassLoader();
      if (cl instanceof ClassLoaderBase) {
        classLoaderType[0] = ((ClassLoaderBase) cl).getType();
        classLoaderID[0] = ((ClassLoaderBase) cl).getClassLoaderID();
        revision = ((ClassLoaderBase) cl).getRevision();
        if (cl instanceof FilterClassLoader) {
          parentRevision = ((FilterClassLoader) cl).getParentRevision();
        }
      } else {
        classLoaderType[0] = null;
        classLoaderID[0] = null;
      }
    } else {
      classLoaderType = new ClassLoaderType[classLoaders.length];
      classLoaderID = new String[classLoaders.length];
      revisions = new long[classLoaders.length];
      for (int i = 0; i < classLoaders.length; i++) {
        ClassLoader nextLoader = classLoaders[i];
        if (nextLoader instanceof ClassLoaderBase) {
          classLoaderType[i] = ((ClassLoaderBase) nextLoader).getType();
          classLoaderID[i] = ((ClassLoaderBase) nextLoader).getClassLoaderID();
          revisions[i] = ((ClassLoaderBase) nextLoader).getRevision();
        } else {
          revisions[i] = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
        }

      }
    }

    this.object = object;
    if (logger.isTraceEnabled()) {
      String classInfo;
      if (object != null) {
        classInfo = " of type " + object.getClass().getName() + " loaded by " + object.getClass().getClassLoader();
      } else {
        classInfo = "";
      }
      String s = String.valueOf(object);
      if (s != null && s.length() > 40) {
        s = s.substring(0, 40) + "...[len=" + s.length() + "]";
      }
      logger.trace("created " + SerializableClassloadedObject.class.getSimpleName() + " for serializable object=\"" + s + "\""
          + classInfo + ".");
      if (classLoaderType != null) {
        for (int i = 0; i < classLoaderType.length; i++) {
          logger.trace("  classloadertyp = " + classLoaderType[i] + " - " + classLoaderID[i] + " - "
              + (revisions != null ? revisions[i] : revision) + " [" + uniqueId + " - " + date + "]");
        }
      }
    }

  }


  /**
   * Sollen Exceptions beim Deserialisieren in readObject unterdrückt werden, wenn diese
   * Objekte nur mit einem speziellen ClassLoader angelegt werden können? 
   * Achtung: normale Einstellung sollte immer false sein! 
   * @param value
   */
  public static void setIgnoreExceptionsWhileDeserializing( boolean value ) {
    ignoreExceptionsWhileDeserializing = value;
  }
  
  private static Comparator<ClassLoaderBase> comparator = new Comparator<ClassLoaderBase>() {

    public int compare(ClassLoaderBase o1, ClassLoaderBase o2) {
      if (o1 == null) {
        return 1;
      }
      if (o2 == null) {
        return -1;
      }
      if (o1.getClassLoaderID() == null) {
        return 1;
      }
      int i = o1.getClassLoaderID().compareTo(o2.getClassLoaderID());
      if (i == 0) {
        if (o1.getType() == null) {
          return -1;
        }
        i = o1.getType().compareTo(o2.getType());
        return i;
      }
      return i;
    }
    
  };

  private static class ShrinkableByteArrayInputStream extends ByteArrayInputStream {

    public ShrinkableByteArrayInputStream(byte[] buf) {
      super(buf);
    }

    /**
     * alle bereits gelesenen bytes entfernen
     */
    public void shrink() {
      int start = pos;
      if (mark > 0 && mark < pos) {
        start = mark;
      }
      int len = count - start;
      byte[] newBuf = new byte[len];
      System.arraycopy(buf, start, newBuf, 0, len);
      buf = newBuf;
      count = len;
      pos -= start;
      mark = 0;
    }

    public int getCount() {
      return count;
    }

  }


  private void writeObject(ObjectOutputStream out) throws IOException {
    version = 2;
    out.defaultWriteObject();
    out.flush();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream oldOos = replaceUnderlyingOutputStream(out, baos);        
    out.writeObject(object);
    out.flush();
    replaceUnderlyingOutputStream(out, oldOos);
    
    byte[] bytes = baos.toByteArray();
    out.writeInt(bytes.length);
    out.write(bytes);
  }


  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    objectWasDeserializable = true;
    in.defaultReadObject();
    if (logger.isTraceEnabled()) {
      logger.trace("reading " + " [" + uniqueId + " - " + date + "]");
    }

    if (revision == null) {
      revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }

    ObjectInputStream oisBytes = getInnerObjectInputStream(in);
    try {
      if (classLoaderType == null || classLoaderType.length == 0 || classLoaderType[0] == null) {
        logger.trace("Loading internal container class with default java classloader");
        object = oisBytes.readObject();
        if (logger.isDebugEnabled()) {
          logger.debug("Finished loading " + (object == null ? "null" : object.getClass().getName()) + " with default java classloader");
        }
      } else {
        if (ignoreExceptionsWhileDeserializing) {
          try {
            ClassLoaderWrapperWrapper loader = createClassLoader();
            try {
              this.object = readObjectWithClassLoader(oisBytes, loader);
            } finally {
              closeClassLoader(loader.cl);
            }
          } catch (ClassNotFoundException e) {
            if (logger.isInfoEnabled()) {
              logger.info("ClassNotFoundException: " + e.getMessage());
            }
            objectWasDeserializable = false;
          } catch (RuntimeException e) {
            if (logger.isInfoEnabled()) {
              logger.info(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
            objectWasDeserializable = false;
          }
        } else {
          ClassLoaderWrapperWrapper loader = createClassLoader();
          try {
            this.object = readObjectWithClassLoader(oisBytes, loader);
          } finally {
            closeClassLoader(loader.cl);
          }
        }
        //TODO performance: nicht nur classloaderwrapper cachen, sondern containerclass:
        //dann kann man sich reflection sparen.
      }
    } finally {
      if (version == 2) {
        InputStream removed = replaceUnderlyingInputStream(in, old);
        if (logger.isTraceEnabled()) {
          int cnt = threadLocalByteArraySizeSum.get();
          cnt -= size((ByteArrayInputStream) removed);
          threadLocalByteArraySizeSum.set(cnt);
          logger.trace("cnt = " + cnt);
        }
        old = null;
      }
    }
  }

  private void closeClassLoader(ClassLoaderWrapper loader) {
    //ntbd, ist kein urlclassloader mehr
  }

  private static final ThreadLocal<Integer> threadLocalByteArraySizeSum = new ThreadLocal<Integer>(){

    @Override
    protected Integer initialValue() {
      return 0;
    }
    
  };
  
  private transient InputStream old;

  private ObjectInputStream getInnerObjectInputStream(ObjectInputStream in) throws IOException {
    if (version == 0) {
      return in;
    } else {
      final int len = in.readInt();
      byte[] bytes = new byte[len];
      if (len > 0) {
        int offset = 0;
        int got;
        while (-1 != (got = in.read(bytes, offset, bytes.length - offset))) {
          offset += got;
          if (offset >= len) {
            break;
          }
        }
        if (offset != len) {
          throw new RuntimeException("got unexpected number of bytes of serialized object. expected " + len + ", got " + offset + ".");
        }
      }
      ByteArrayInputStream bais = new ShrinkableByteArrayInputStream(bytes);
      if (version == 1) { //war nur ein zwischenstand
        return new ObjectInputStream(bais);
      } else if (version == 2) { //TODO damit peek funktioniert, noch den underlying stream an den ursprünglichen dranhängen?
        old = replaceUnderlyingInputStream(in, bais);
        if (old instanceof ShrinkableByteArrayInputStream) {
          ((ShrinkableByteArrayInputStream) old).shrink();
        }
        if (logger.isTraceEnabled()) {
          int cnt = threadLocalByteArraySizeSum.get();
          if (old instanceof ShrinkableByteArrayInputStream) {
          } else if (old instanceof ByteArrayInputStream) {
            cnt = size((ByteArrayInputStream) old);
          } else {
            cnt = 0;
          }
          cnt += len;
          threadLocalByteArraySizeSum.set(cnt);
          logger.trace("cnt = " + cnt);
        }
        return in;
      } else {
        throw new RuntimeException();
      }
    }
  }


  private int size(ByteArrayInputStream is) {
    if (is instanceof ShrinkableByteArrayInputStream) {
      return ((ShrinkableByteArrayInputStream) is).getCount();
    }
    try {
      return (Integer) ByteArrayInputStream.class.getDeclaredField("count").get(is);
    } catch (Exception e) {
      return 0;
    }
  }


  /*
   * Bugz 19270: Ziel: Fehler beim Deserialisieren sollen nur zu einer Warnung führen. Danach kann die Objektdeserialisierung weiterlaufen.
   * Das funktioniert nur, wenn der ObjectOutputStream die zu dem fehlerhaften Objekt gehörenden Bytes skippt.
   * Leider ist uns keine bessere Lösung dafür eingefallen als folgende:
   * - Serialisiere gefährliche Daten (SerializableClassloadedObject) in ein separates ByteArray, und schreibe dieses als ByteArray 
   *   in den ObjektOutputStream. Wenn dann beim Deserialisieren ein Fehler auftritt, wird das gesamte ByteArray ausgelesen, also damit
   *   automatisch alles übersprungen, was dazugehört.
   * - Bei verschachtelten SerializableClassloadedObjects muss man aufpassen, dass man den "Cache" des ObjectOutputStreams verwendet, damit
   *   man die serialisierten Daten nicht unnötig redundant aufbläst. Damit der Cache verwendet werden kann, muss man die gleiche ObjectOutputStream
   *   Instanz einmal in das bzw verschiedene ByteArrays schreiben lassen und einmal in den zugrundeliegenden Stream. Das kann man nur per Reflection
   *   oder bei vollständiger Kontrolle über den zugrundeliegenden Stream erreichen.
   * - Bei verschachtelten SerializableClassloadedObjects gibt es beim Deserialisieren auch verschachtelte ByteArrays. Dies kann naiv zu
   *   starker Redundanz im Speicherverbrauch (temporär) führen. Da man die Teil-Bereiche aus einem ByteArray nur einmal benötigt, wird deshalb
   *   der ShrinkableByteArrayInputStream verwendet, um bereits gelesene Teil-Bereiche aus den ByteArrays zu entfernen. 
   */  

  
  private OutputStream replaceUnderlyingOutputStream(ObjectOutputStream out, OutputStream newOos) {

    try {

      Field fBout = FieldCache.OBJECT_OUTPUT_STREAM.getField(ObjectOutputStream.class);
      Object blockDataOutputStream = fBout.get(out);

      Field fOut = FieldCache.F_OUT.getField(blockDataOutputStream.getClass());
      OutputStream old = (OutputStream) fOut.get(blockDataOutputStream);

      fOut.set(blockDataOutputStream, newOos);
      return old;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  private InputStream replaceUnderlyingInputStream(ObjectInputStream ois, InputStream is) {

    try {

      Field fBin = FieldCache.OBJECT_INPUT_STREAM.getField(ObjectInputStream.class);
      Object blockDataInputStream = fBin.get(ois);

      Field fIn = FieldCache.F_IN.getField(blockDataInputStream.getClass());
      Object peekInputStream = fIn.get(blockDataInputStream);

      Field fPin = FieldCache.F_PIN.getField(peekInputStream.getClass());
      InputStream old = (InputStream) fPin.get(peekInputStream);

      fPin.set(peekInputStream, is);
      return old;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  private enum FieldCache {

    OBJECT_OUTPUT_STREAM("bout"),
    F_OUT("out"),
    OBJECT_INPUT_STREAM("bin"),
    F_IN("in"),
    F_PIN("in");

    private FieldCache(String fieldName) {
      this.fieldName = fieldName;
    }
    private final String fieldName;
    private static Map<Class, Field> fields = Collections.synchronizedMap(new WeakHashMap<Class, Field>());

    public Field getField(Class c) {
      Field result = fields.get(c);
      if (result == null) {
        try {
          result = c.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
          throw new RuntimeException(null, e);
        } catch (SecurityException e) {
          throw new RuntimeException(null, e);
        }
        result.setAccessible(true);
        fields.put(c, result);
      }
      return result;
    }

  }


  /**
   * Erzeugen des ClassLoaders zum Lesen des serialisierten Objects
   * @return
   * @throws ClassNotFoundException 
   */
  private ClassLoaderWrapperWrapper createClassLoader() throws ClassNotFoundException {

    ClassLoaderBase[] loaders = new ClassLoaderBase[classLoaderType.length];
    for (int i = 0; i < loaders.length; i++) {
      ClassLoaderType currentClassLoaderType = classLoaderType[i];
      String currentClassLoaderID = classLoaderID[i];

      Long rev = revisions != null ? revisions[i] : revision;
      if (logger.isDebugEnabled()) {
        logger.debug("Loading internal container class " + currentClassLoaderID + " with classloader type "
            + currentClassLoaderType + ", rev=" + rev + " ...");
      }
      // if this is called outside the factory (e.g. when transmitted via RMI) there will be NPEs 
      ClassLoaderBase l = cld.findClassLoaderByType(currentClassLoaderID, rev, currentClassLoaderType, true);
      if (l == null) {
        logger.error("Classloader (ID: '" + currentClassLoaderID + "', type: '" + currentClassLoaderType
            + "' in revision " + rev + ") was null! This can be caused by undeploying or manually removing content that"
            + " is still used (e.g. in the orderbackup).");
        throw new ClassNotFoundException(currentClassLoaderType + " / " + currentClassLoaderID + " not found in revision " + rev + ".");
      }
      loaders[i] = l;
    }
    Arrays.sort(loaders, comparator);

    return getCachedClassLoaderWrapper(loaders);
  }
  
  private static class ClassLoaderWrapperWrapper {
    
    private final ClassLoaderWrapper cl;
    private final ReflectionData rd;
    
    private ClassLoaderWrapperWrapper(ClassLoaderWrapper cl) {
      this.cl = cl;
      Class<?> loadedClass;
      try {
        loadedClass = cl.loadClass(ContainerClass.class.getName());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("containerclass loaded by " + loadedClass.getClassLoader());
      }
      rd = new ReflectionData(loadedClass);
    }
    
  }
  
  private static class ClassLoadingKey {
    
    private final ClassLoaderBase[] loaders;
    
    private ClassLoadingKey(ClassLoaderBase[] loaders) {
      this.loaders = loaders;
    }
    
    private int h;
    
    @Override
    public int hashCode() {
      if (h > 0) {
        return h;
      }
      int result = Arrays.hashCode(loaders);
      h = result;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ClassLoadingKey other = (ClassLoadingKey) obj;
      if (!Arrays.equals(loaders, other.loaders))
        return false;
      return true;
    }

    
  }


  private ClassLoaderWrapperWrapper getCachedClassLoaderWrapper(ClassLoaderBase[] loaders) {
    ClassLoadingKey key = new ClassLoadingKey(loaders);
    ClassLoaderWrapperWrapper c;
    synchronized (cache) {
      c = cache.get(key);
    }
    if (c == null) {
      c = new ClassLoaderWrapperWrapper(new ClassLoaderWrapper(loaders));
      synchronized (cache) {
        cache.put(key, c);
      }
    }
    return c;
  }

  private static class ReflectionData {
    
    private final Method readObjectFromStream;
    private final Method getObject;
    private final Constructor<?> constructor;

    public ReflectionData(Class<?> c) {
      try {
        readObjectFromStream = c.getMethod("readObjectFromStream", ObjectInputStream.class);
        constructor = c.getConstructor();
        getObject = c.getMethod("getObject");
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      }
    }

    public Method getReadObjectFromStreamMethod() {
      return readObjectFromStream;
    }

    public Object newInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return constructor.newInstance();
    }

    public Method getGetObjectMethod() {
      return getObject;
    }
    
  }


  private static final LruCache<ClassLoadingKey, ClassLoaderWrapperWrapper> cache = new LruCache<>(1000);

  /**
   * Lesen des serialisierten Objects mit dem übergebenen ClassLoader:
   * Trick: ContainerClass mit dem ClassLoader laden, diese ContainerClass kann
   *        dann das serialisierte Object mit ihrem ClassLoader deserialisieren.
   * @param loader
   * @return
   * @throws ClassNotFoundException 
   */
  private Object readObjectWithClassLoader(ObjectInputStream in, ClassLoaderWrapperWrapper loader) throws ClassNotFoundException {
    Object readObject = null;
    Method m = null;
    try {
      ReflectionData rd = loader.rd;
      Object containerClassInstance = rd.newInstance();

      // invoke the readObject method to fill the object
      Method readObjectMethod = rd.getReadObjectFromStreamMethod();
      m = readObjectMethod;
      readObjectMethod.invoke(containerClassInstance, in);

      // extract the newly read object into this
      Method getObjectMethod = rd.getGetObjectMethod();
      m = getObjectMethod;
      readObject = getObjectMethod.invoke(containerClassInstance);

    } catch (InstantiationException e) {
      logger.error(null, e);
      throwReflectionErrors(e);
    } catch (IllegalAccessException e) {
      logger.error(null, e);
      throwReflectionErrors(e);
    } catch (SecurityException e) {
      logger.error(null, e);
      throwReflectionErrors(e);
    } catch (IllegalArgumentException e) {
      logger.error(null, e);
      throwReflectionErrors(e);
    } catch (InvocationTargetException e) {
      logger.error("Error while " + (m != null ? "executing method " + m.getName() : "calling newInstance")
          + " on object " + ContainerClass.class.getName(), e);
      throwReflectionErrors(e);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Finished loading classloaded object '" + readObject + "' with the following classloader: " + loader);
    }
    return readObject;
  }


  private void throwReflectionErrors(Throwable t) {
    Boolean throwValue = THROW_ERRORS.get();
    if (throwValue != null &&
        throwValue) {
      throw new RuntimeException(t);
    }
  }


  static URL[] getURLs() {
    ClassLoader cl = SerializableClassloadedObject.class.getClassLoader();
    while (cl != null && !(cl instanceof URLClassLoader)) {
      cl = cl.getParent();
    }
    if (cl == null) {
      logger.error("didn't find any urlclassloader");
      return new URL[0];
    } else {
      URLClassLoader ucl = (URLClassLoader) cl;
      return ucl.getURLs();
    }
  }


  public Object getObject() {
    return object;
  }
  
  public Object getObject(Object parent, String name) {
    if (deserializationFailedHandler != null && ! objectWasDeserializable) {
      deserializationFailedHandler.failed(parent, name);
    }
    return object;
  }


}
