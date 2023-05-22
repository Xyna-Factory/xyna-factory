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

package xact.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


/* VelocityTemplateExt ueberschreibt VelocityTemplate, so das VelocityTemplate.toString zum Erzeugen einer aussagekraeftigen Fehlermeldung
 * - z.B. f�rs Log und in der Benutzeroberfl�che verwendet werden kann- 
 */
public class VelocityTemplateExt extends VelocityTemplate {
  private String cause; //Grund f�r die Exception
  private String additionalInfo; //zusaetze Informationen, z.B. die verwendeten Daten
  
  
  public VelocityTemplateExt(String cause, VelocityTemplate velocityTemplate, String additionalInfo) {
    super();
    setTemplate(velocityTemplate.getTemplate());
    this.additionalInfo=additionalInfo;
    this.cause=cause;
  }
  
  @Override
  public String toString() {
    StringBuffer sb=new StringBuffer("\n"+cause+"\n");
    String template=getTemplate();
    if(template==null){
      template="template stored in exception is null";
    }
    BufferedReader br=new BufferedReader(new StringReader(template));
    
    int i=0;//template mit Zeilennumern durchnummerieren
    String line;
    try {
      while((line=br.readLine())!=null){
        i++;
        sb.append(String.format("%03d %s\n", i,line));
      }
    }
    catch (IOException e) {
      //ignore it
    }
    return sb.toString()+additionalInfo;
  }

}
