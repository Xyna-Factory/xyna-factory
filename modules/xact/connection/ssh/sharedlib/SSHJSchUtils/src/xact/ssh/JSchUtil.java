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
package xact.ssh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.gip.xyna.FileUtils;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.KnownHosts;
import com.jcraft.jsch.Session;


public class JSchUtil {
  
  // only getConfig will be invoked, as long as we don't reconfigure it we're fine using a fresh instance
  private final static JSch utilityJSchInstance = new JSch(); 
  
  private static volatile Class<?> utilClass;

  
  private static volatile Method fromBase64Method;
  
  public static byte[] fromBase64(byte[] buf, int start, int length) {
    try {
      if (fromBase64Method == null) {
        if (utilClass == null) {
          utilClass = Class.forName("com.jcraft.jsch.Util"); 
        }
        Method fromBase64 = utilClass.getDeclaredMethod("fromBase64", byte[].class, int.class, int.class);
        fromBase64.setAccessible(true);
        fromBase64Method = fromBase64;
      }
      return (byte[]) fromBase64Method.invoke(null, buf, start, length);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Method toBase64Method;
  
  public static byte[] toBase64(byte[] buf, int start, int length) {
    try {
      if (toBase64Method == null) {
        if (utilClass == null) {
          utilClass = Class.forName("com.jcraft.jsch.Util"); 
        }
        Method toBase64 = utilClass.getDeclaredMethod("toBase64", byte[].class, int.class, int.class);
        toBase64.setAccessible(true);
        toBase64Method = toBase64;
      }
      return (byte[]) toBase64Method.invoke(null, buf, start, length);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Method str2byteMethod;
  
  public static byte[] str2byte(String str) {
    try {
      if (str2byteMethod == null) {
        if (utilClass == null) {
          utilClass = Class.forName("com.jcraft.jsch.Util"); 
        }
        Method str2byte = utilClass.getDeclaredMethod("str2byte", String.class);
        str2byte.setAccessible(true);
        str2byteMethod = str2byte;
      }
      return (byte[]) str2byteMethod.invoke(null, str);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Method byte2strMethod;
  
  public static String byte2str(byte[] str) {
    try {
      if (byte2strMethod == null) {
        if (utilClass == null) {
          utilClass = Class.forName("com.jcraft.jsch.Util"); 
        }
        Method byte2str = utilClass.getDeclaredMethod("byte2str", byte[].class);
        byte2str.setAccessible(true);
        byte2strMethod = byte2str;
      }
      return (String) byte2strMethod.invoke(null, str);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Class<?> identitiyFileClass;
  
  private static volatile Method newInstanceMethod;
  
  public static Identity createIdentity(byte[] identity, byte[] publicBytes, String name) {
    try {
      if (newInstanceMethod == null) {
        if (identitiyFileClass == null) {
          identitiyFileClass = Class.forName("com.jcraft.jsch.IdentityFile"); 
        }
        Method newInstance = identitiyFileClass.getDeclaredMethod("newInstance", String.class, byte[].class, byte[].class, JSch.class);
        newInstance.setAccessible(true);
        newInstanceMethod = newInstance;
      }
      byte[] defensiveCopy = new byte[identity.length];
      System.arraycopy(identity, 0, defensiveCopy, 0, identity.length);
      Identity creation = (Identity) newInstanceMethod.invoke(null, name, defensiveCopy, publicBytes, utilityJSchInstance);
      return creation;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Field identityField;
  
  public static void adjustIdentityName(Identity identity, String name) {
    try {
      if (identitiyFileClass == null) {
        identitiyFileClass = Class.forName("com.jcraft.jsch.IdentityFile"); 
      }
      if (identityField == null) {
        Field field = identitiyFileClass.getDeclaredField("identity");
        field.setAccessible(true);
        identityField = field;
      }
      identityField.set(identity, name);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Field keyPairField;
  private static volatile Field dataField;
  
  private static volatile Method getPrivateKeyMethod;
  
  public static byte[] exractPrivateKey(Identity identity) {
    try {
      if (identitiyFileClass == null) {
        identitiyFileClass = Class.forName("com.jcraft.jsch.IdentityFile"); 
      }
      if (keyPairField == null) {
        Field field = identitiyFileClass.getDeclaredField("kpair");
        field.setAccessible(true);
        keyPairField = field;
      }
      KeyPair kpair = (KeyPair) keyPairField.get(identity);
      if (getPrivateKeyMethod == null) {
        Method method = KeyPair.class.getDeclaredMethod("getPrivateKey");
        method.setAccessible(true);
        getPrivateKeyMethod = method;
      }
      try {
        return (byte[]) getPrivateKeyMethod.invoke(kpair);
      } catch (InvocationTargetException e) {
        if (dataField == null) {
          Field field = KeyPair.class.getDeclaredField("data");
          field.setAccessible(true);
          dataField = field;
        }
        return (byte[]) dataField.get(kpair);
      }
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  

    
  private static volatile Constructor<?> knownHostsConstructor;
  
  private static volatile Field knownHostsPool;
  
  private static volatile Method dumpMethod;
  
  
  public static void exportKnownHosts(HostKey[] hosts, String filename) {
    try {
      KnownHosts knownHostsInstance = getKnownHostsInstance(utilityJSchInstance);
      if (knownHostsPool == null) {
        Field field = KnownHosts.class.getDeclaredField("pool");
        field.setAccessible(true);
        knownHostsPool = field;
      }
      Vector<HostKey> knownHostsVector = (Vector<HostKey>) knownHostsPool.get(knownHostsInstance);
      knownHostsVector.addAll(Arrays.asList(hosts));
      
      File file = new File(filename);
      if (!file.exists()) {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
      }
      if (dumpMethod == null) {
        Method method = KnownHosts.class.getDeclaredMethod("dump", OutputStream.class);
        method.setAccessible(true);
        dumpMethod = method;
      }
      FileOutputStream fos = new FileOutputStream(file, true);
      try {
        dumpMethod.invoke(knownHostsInstance, fos);
        fos.flush();
      } finally {
        fos.close();
      }
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Method setHostFileMethod;
  
  public static List<HostKey> importKnownHosts(String filename) {
    try {
      KnownHosts knownHostsInstance = getKnownHostsInstance(utilityJSchInstance);
      if (setHostFileMethod == null) {
        Method method = KnownHosts.class.getDeclaredMethod("setKnownHosts", String.class);
        method.setAccessible(true);
        setHostFileMethod = method;
      }
      setHostFileMethod.invoke(knownHostsInstance, filename);
      return Arrays.asList(knownHostsInstance.getHostKey());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public static volatile KnownHosts knownHostsInstance;
  
  
  private static KnownHosts getKnownHostsInstance(JSch jsch) {
    try {
      if (knownHostsInstance == null) {
        if (knownHostsConstructor == null) {
          Constructor<?> constructor = KnownHosts.class.getDeclaredConstructor(JSch.class);
          constructor.setAccessible(true);
          knownHostsConstructor = constructor;
        }
        initializeSessionRandom();
        knownHostsInstance = (KnownHosts) knownHostsConstructor.newInstance(jsch);
      }
      if (knownHostsPool == null) {
        Field field = KnownHosts.class.getDeclaredField("pool");
        field.setAccessible(true);
        knownHostsPool = field;
      }
      Vector pool = (Vector) knownHostsPool.get(knownHostsInstance);
      pool.clear();
      return knownHostsInstance;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public static EncryptionType getKeyType(byte[] key) {
    if(key[8]=='d') return EncryptionType.DSA;
    if(key[8]=='r') return EncryptionType.RSA;
    return EncryptionType.UNKNOWN;
  }
  
  
  public static EncryptionType getKeyType(String key) {
    byte[] keyBytes = JSchUtil.str2byte(key);
    byte[] decodedBytes = JSchUtil.fromBase64(keyBytes, 0, keyBytes.length);
    return getKeyType(decodedBytes);
  }
  
  
  private static volatile Field sessionRandomField;
  
  private static void initializeSessionRandom() {
    try {
      if (sessionRandomField == null) {
        Field field = Session.class.getDeclaredField("random");
        field.setAccessible(true);
        sessionRandomField = field;
      }
      Class c=Class.forName(JSch.getConfig("random"));
      sessionRandomField.set(null, c.newInstance());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  
  private static volatile Class<?> hashedHostKeyClass;
  
  private static volatile Constructor<?> hashedHostKeyConstructor;
  
  private static volatile Method hashMethod;
  
  public static HostKey instantiateHashedHostKey(String hostname, EncryptionType type, String publicKey, JSch jsch, boolean hashKey, String comment) {
    try {
      if (hashedHostKeyConstructor == null) {
        if (hashedHostKeyClass == null) {
          hashedHostKeyClass = Class.forName("com.jcraft.jsch.KnownHosts$HashedHostKey");
        }
        Constructor<?> constructor = hashedHostKeyClass.getDeclaredConstructor(KnownHosts.class, String.class, String.class, int.class, byte[].class, String.class);
        constructor.setAccessible(true);
        hashedHostKeyConstructor = constructor;
      }
      HostKey creation = (HostKey) hashedHostKeyConstructor.newInstance(getKnownHostsInstance(jsch), "", hostname, type.getNumericRepresentation(), base64StringToPublicKeyBlob(publicKey), comment);
      if (hashKey && HostKeyStorable.canBeHashed(hostname)) {
        if (hashMethod == null) {
          Method method = hashedHostKeyClass.getDeclaredMethod("hash");
          method.setAccessible(true);
          hashMethod = method;
        }
        hashMethod.invoke(creation); // method checks if already hashed
      }
      return creation;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static volatile Method isHashedMethod;
  
  public static boolean isHostKeyHashed(HostKey key) {
    try {
      if (hashedHostKeyClass == null) {
        hashedHostKeyClass = Class.forName("com.jcraft.jsch.KnownHosts$HashedHostKey");
      }
      if (hashedHostKeyClass.isInstance(key)) {
        if (isHashedMethod == null) {
          Method method = hashedHostKeyClass.getDeclaredMethod("isHashed");
          method.setAccessible(true);
          isHashedMethod = method;
        }
        return (Boolean) isHashedMethod.invoke(key);
      } else {
        return false;
      }
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  
  private static volatile Method isMatchedMethod;
  
  
  public static boolean isHostNameMatched(HostKey hostkey, String hostname) {
  try {
    if (isMatchedMethod == null) {
      Method method = HostKey.class.getDeclaredMethod("isMatched", String.class);
      method.setAccessible(true);
      isMatchedMethod = method;
    }
    return (Boolean)isMatchedMethod.invoke(hostkey, hostname);
  } catch (IllegalArgumentException e) {
    throw new RuntimeException(e);
  } catch (IllegalAccessException e) {
    throw new RuntimeException(e);
  } catch (InvocationTargetException e) {
    throw new RuntimeException(e);
  } catch (SecurityException e) {
    throw new RuntimeException(e);
  } catch (NoSuchMethodException e) {
    throw new RuntimeException(e);
  }
}
  
  
  public static byte[] base64StringToPublicKeyBlob(String key) {
    byte[] publicKeyBlob = JSchUtil.str2byte(key);
    return JSchUtil.fromBase64(publicKeyBlob, 0, publicKeyBlob.length);
  }
  
  public static String publicKeyBlobTobase64String(byte[] blob) {
    return byte2str(JSchUtil.toBase64(blob, 0, blob.length));
  }
  

}
