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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * OctetString DOCSIS TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class FlowDHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    public FlowDHCPv6TlvDecoder(final long typeEncoding, final String typeName) {
        super(typeEncoding, typeName);
    }

    @Override
    protected String decodeTlvValue(byte[] value) {
      if (value.length < 1) {
        return "";
    }
    
    String result="";
    int length;
    byte[] substring = new byte[1024];
    
    if(value[0]!=0)
    {
      throw new IllegalArgumentException("Expected 0 at beginning of FlowValue but got: <" + value[0] + ">.");
    }
    if(value[value.length-1]!=0)
    {
      throw new IllegalArgumentException("Expected 0 at end of FlowValue but got: <" + value[value.length-1] + ">.");
    }
    
            
    InputStream inp = new ByteArrayInputStream(value);
    
    
    try {
      
      inp.read(); // 00 wegwerfen
      
      boolean fertig=false;
      
      while(!fertig) // solange End 00 nicht erreicht
      {
        length = inp.read();
        
        if(length==0)fertig=true;
        if(length>value.length-1) throw new IllegalArgumentException("Strange length value!");
        if(length==-1) throw new IllegalArgumentException("Reached end of Stream before expected!");

        
        if(!fertig)
        {
          inp.read(substring,0,length);
          
          for(int z=0;z<length;z++)
          {
            result = result + (char)substring[z];
            if(substring[z]>90) throw new IllegalArgumentException("Character may only be uppercase in Flow Option");
          }
          result = result + ".";
        }
        
      }
      result = result.substring(0, result.length()-1);
      
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    
    
    return result;
}

}
