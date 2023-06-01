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
package xact.ldap.dictionary;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.novell.ldap.LDAPAttribute;


public enum SyntaxDefinition {
    //TODO do we need something like incoming & outgoing java classes?
    IA5String("1.3.6.1.4.1.1466.115.121.1.26", String.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        /*
         * IA5 or more properly now International Reference Alphabet No. 5 (IRA5) and previously International Alphabet No. 5 (ISO 646)
         * is defined in ITU-T T.50 and is a 7-bit character code (but encoded as 8 bits). ASCII is the US implementation of IA5 -
         *  there are multiple other national versions.
         */
        try {
          if (obj instanceof String) {
            return ((String) obj).getBytes("ISO 646");
          } else {
            return obj.toString().getBytes("ISO 646");
          }
        } catch (UnsupportedEncodingException e) {
          return null;
        }
      }

      @Override
      public String transformToString(Object obj) {
        return convertObjectIntoString(obj);
      }

      @Override
      public String transformLDAPAttributeToObject(LDAPAttribute attribute) {
        return transformLDAPAttributeToString(attribute);
      }

      @Override
      public List<String> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        return transformLDAPAttributeToStringList(attribute);
      }
    },
    Integer("1.3.6.1.4.1.1466.115.121.1.27", Integer.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        return convertIntToByteArray(obj);
      }

      @Override
      public String transformToString(Object obj) {
        return convertObjectIntoString(obj);
      }

      @Override
      public Integer transformLDAPAttributeToObject(LDAPAttribute attribute) {
        return java.lang.Integer.parseInt(transformLDAPAttributeToString(attribute));
      }

      @Override
      public List<Integer> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        List<Integer> list = new ArrayList<Integer>();
        for (String value : attribute.getStringValueArray()) {
          list.add(java.lang.Integer.parseInt(value));
        }
        return list;
      }
    },
    GeneralizedTime("1.3.6.1.4.1.1466.115.121.1.24", String.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        // TODO what do we expect?
        return convertObjectIntoStringBytesByCharset(obj, utf8Charset);
      }

      @Override
      public String transformToString(Object obj) {
        return convertObjectIntoString(obj);
      }

      @Override
      public String transformLDAPAttributeToObject(LDAPAttribute attribute) {
        return transformLDAPAttributeToString(attribute);
      }

      @Override
      public List<String> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        return transformLDAPAttributeToStringList(attribute);
      }
    },
    Boolean("1.3.6.1.4.1.1466.115.121.1.7", Boolean.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        if (obj instanceof Boolean && (Boolean)obj) {
          return TRUE_BYTES;
        } else {
          return FALSE_BYTES;
        }
      }

      @Override
      public String transformToString(Object obj) {
        if (obj instanceof Boolean && (Boolean)obj) {
          return TRUE_STRING;
        } else {
          return FALSE_STRING;
        }
      }

      @Override
      public Object transformLDAPAttributeToObject(LDAPAttribute attribute) {
        String value = attribute.getStringValue();
        if (value.equals(TRUE_STRING)) {
          return java.lang.Boolean.TRUE;
        } else {
          return java.lang.Boolean.FALSE;
        }
      }

      @Override
      public List<Boolean> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        List<Boolean> list = new ArrayList<Boolean>();
        for (String value : attribute.getStringValueArray()) {
          if (value.equals(TRUE_STRING)) {
            list.add(java.lang.Boolean.TRUE);
          } else {
            list.add(java.lang.Boolean.FALSE);
          }
        }
        return list;
      }
    },
    Binary("1.3.6.1.4.1.1466.115.121.1.5", String.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        // we expect an Base64Encoded-String
        if (obj instanceof String) {
          //BASE64Decoder decoder = new BASE64Decoder();
          return ((String) obj).getBytes();
        } else {
          return null;
        }
      }

      @Override
      public String transformToString(Object obj) {
        //BASE64Decoder decoder = new BASE64Decoder();
        return (String) obj;
      }

      @Override
      public Object transformLDAPAttributeToObject(LDAPAttribute attribute) {
        // TODO get as bytes and base64 encode?
        return transformLDAPAttributeToString(attribute);
      }

      @Override
      public List transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        // TODO get as bytes and base64 encode?
        return transformLDAPAttributeToStringList(attribute);
      }
    },
    BitString("1.3.6.1.4.1.1466.115.121.1.6", String.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        return convertObjectIntoStringBytesByCharset(obj, utf8Charset);
      }

      @Override
      public String transformToString(Object obj) {
        return convertObjectIntoString(obj);
      }

      @Override
      public String transformLDAPAttributeToObject(LDAPAttribute attribute) {
        return transformLDAPAttributeToString(attribute);
      }

      @Override
      public List<String> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        return transformLDAPAttributeToStringList(attribute);
      }
    },
    Default("", String.class) {

      @Override
      public byte[] transformToByteArray(Object obj) {
        return convertObjectIntoStringBytesByCharset(obj, utf8Charset);
      }

      @Override
      public String transformToString(Object obj) {
        return convertObjectIntoString(obj);
      }

      @Override
      public String transformLDAPAttributeToObject(LDAPAttribute attribute) {
        return transformLDAPAttributeToString(attribute);
      }

      @Override
      public List<String> transformLDAPAttributeToObjectList(LDAPAttribute attribute) {
        return transformLDAPAttributeToStringList(attribute);
      }
    };
    
    private static final Charset utf8Charset;
    
    static {
      try {
        utf8Charset = Charset.forName("UTF-8");
      } catch (Throwable t) {
        throw new RuntimeException("",t);
      }
    
    } 
    private final String oid;
    private final Class<?> correspondingJavaClass;
    
    private static byte[] TRUE_BYTES;
    private static byte[] FALSE_BYTES;
    
    private final static String TRUE_STRING = "TRUE";
    private final static String FALSE_STRING = "FALSE";
    
    static {
      try {
        TRUE_BYTES = TRUE_STRING.getBytes("UTF-8");
        FALSE_BYTES = FALSE_STRING.getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
        
      }
    }
    
    private SyntaxDefinition(String oid, Class<?> correspondingJavaClass) {
      this.oid = oid;
      this.correspondingJavaClass = correspondingJavaClass;
    }
    
    public abstract byte[] transformToByteArray(Object obj);
    
    public abstract String transformToString(Object obj);
    
    public abstract Object transformLDAPAttributeToObject(LDAPAttribute attribute);
    
    public abstract List transformLDAPAttributeToObjectList(LDAPAttribute attribute);
    
    private static String transformLDAPAttributeToString(LDAPAttribute attribute) {
      return attribute.getStringValue();
    }
    
    private static List<String> transformLDAPAttributeToStringList(LDAPAttribute attribute) {
      return Arrays.asList(attribute.getStringValueArray());
    }
    
    private static byte[] convertObjectIntoStringBytesByCharset(Object obj, Charset charset) {
      try {
        return convertObjectIntoString(obj).getBytes(charset.name());
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("",e);
      }
    }
    
    
    private static String convertObjectIntoString(Object obj) {
      if (obj instanceof String) {
        return (String) obj;
      } else {
        return obj.toString();
      }
    }
    
    
    private static byte[] convertIntToByteArray(Object obj) {
      if (obj instanceof Integer) {
        int value = ((Integer) obj).intValue();
        byte[] intBytes = new byte[4];
        intBytes[0] = (byte) (value >>> 24);
        intBytes[1] = (byte) (value >>> 16);
        intBytes[2] = (byte) (value >>> 8);
        intBytes[3] = (byte) (value >>> 0);
        return intBytes;
      } else {
        return null;
      }
    }
    
    
    public static SyntaxDefinition getSyntaxDefinitionByOid(String oid) {
      for (SyntaxDefinition definition : values()) {
        if (definition.oid.equals(oid)) {
          return definition;
        }
      }
      return Default;
    }
    
    
    public Class<?> getCorrespondingJavaClass() {
      return correspondingJavaClass;
    }
    
    public String getSyntaxOid() {
      return oid;
    }
    
    
}
