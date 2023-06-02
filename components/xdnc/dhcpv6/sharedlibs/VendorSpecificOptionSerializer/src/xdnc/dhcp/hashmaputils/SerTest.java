package xdnc.dhcp.hashmaputils;
import java.io.*;
import java.util.HashMap;


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


public class SerTest {
 
  public static void main(String[] args) {
    
    String s="{key0=value0,key1=value1,key2=value2,key3=value3,key4=value4,key5=value5,key6=value6,key7=value7,key8=value8,key9=value9}";
    
    HashMap hm=new HashMap();
    for (int i=0;i<10;i++){
      hm.put ("key"+i,"value"+i);       
      
      if (i==5){
        HashMap hm3=new HashMap();
        HashMap hm2=new HashMap();
        hm2.put("key5.1","value5.1\n\t\r,=");
        hm2.put("key5.2",hm3);
        hm.put ("key"+i,hm2);      
      }
      
    }
  
    String serial=new HashMapSerializer().serialize(hm);
      
    System.out.println(serial);
    
    HashMap hm2=new HashMapDeserializer().deserializeHm(serial);
    System.out.println(new HashMapSerializer().serialize(hm2));
   
    
  }

}
