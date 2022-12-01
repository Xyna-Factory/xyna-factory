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
package xact.templates;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import xact.templates.velocitytemplate.exceptions.Codes;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

/**
 * This class implements a singleton instance interface between workflows and the apache Velocity Template Engine.
 */
public final class VelocityServiceImpl implements DeploymentTask, IPropertyChangeListener{

  private static Logger logger = Logger.getLogger(VelocityServiceImpl.class.getName());
  private static final String XYNAPROPERTY_ACS_VELOCITY_ALIASES = "xact.acs.velocity.aliases";
  volatile private static String mappingString="";

  /**
   * This property has to be set for the implementation to work. An exception will be thrown if no value is present.
   */
  public static final String XYNA_PROPERTY_KEY_PARSER_POOL_SIZE = "velocity.parser.pool.size";

  //possible change request: multiple instances of the template generator should be supported with different settings.
  //This request conflicts using global Xyna properties
  private static VelocityServiceImpl INSTANCE = null;

  private VelocityEngine velocityEngine;

  public VelocityServiceImpl() {
  }

  public VelocityServiceImpl(boolean b) throws XynaException {
    new Codes(); //damit exceptions korrekt initialisiert werden
    try {
      this.velocityEngine = new VelocityEngine();
      // VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS has been removed in velocity-engine-core 2.0 but this is the value that was stored in there
      velocityEngine.setProperty("runtime.log.logsystem.class",
                                 "org.apache.velocity.runtime.log.Log4JLogChute");
      velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", logger.getName());
      velocityEngine.setProperty(VelocityEngine.VM_PERM_INLINE_LOCAL, true);
      String parserPoolSizeXynaProperty = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_KEY_PARSER_POOL_SIZE);
      checkParserPoolSizeXynaProperty(parserPoolSizeXynaProperty);
      velocityEngine.setProperty(VelocityEngine.PARSER_POOL_SIZE, Integer.parseInt(parserPoolSizeXynaProperty));
      velocityEngine.init();
    } catch (Exception e) {
      throw new XynaException(Codes.CODE_VELOCITY_ENGINE_INITIALIZATION_FAILURE).initCause(e);
    }
  }

  //checks XynaProperty value 
  private static void checkParserPoolSizeXynaProperty(String value) {       
        if (value == null) {
            throw new IllegalStateException("Xyna property <" + XYNA_PROPERTY_KEY_PARSER_POOL_SIZE + "> is not defined.");
        }
        int parserPoolSize;
        try {
            parserPoolSize = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Expected Xyna propery <" + XYNA_PROPERTY_KEY_PARSER_POOL_SIZE
                    + "> to be an integer value, but was: <" + value + ">.", e);
        }
        if (parserPoolSize < 1) {
            throw new IllegalStateException("Expected Xyna propery <" + XYNA_PROPERTY_KEY_PARSER_POOL_SIZE
                    + "> to be a positive integer value, but was: <" + parserPoolSize + ">.");
        }
    }
    
    private static List<String> logTagTokens = new ArrayList<String>();//das logTagToken wird von Velocity intern zum cachen von Macros verwendet.
    private static int highestToken = -1;
    
    private static synchronized String getVelocityToken() {
      if (logTagTokens.size() == 0) {
        StringBuffer sb = new StringBuffer("Velocity<");
        sb.append(highestToken++).append(">");
        return sb.toString(); 
      } else {
        return logTagTokens.remove(logTagTokens.size()-1);
      }
    }
    
    private static synchronized void freeVelocityToken(String token) {
      logTagTokens.add(token);
    }
    
    public String evaluate(final VelocityTemplate velocityTemplate, final TemplateInputData templateInputData)
            throws XynaException {
        if (velocityTemplate == null) {
            throw new IllegalArgumentException("Velocity template may not be null.");
        } else if (templateInputData == null) {
            throw new IllegalArgumentException("Template input data may not be null.");
        }
        StringWriter writer = new StringWriter();
        //bugz 8908: logTag muss unique sein! darf aber nicht zu stark variieren, weil velocity dies als key benutzt, um macros zu cachen.
        //=> gefahr von OOM durch wachsenden cache von velocity
        String logTagToken = getVelocityToken(); 
        try {
            INSTANCE.velocityEngine.evaluate(toVelocityContext(templateInputData), writer, logTagToken,
                    new StringReader(velocityTemplate.getTemplate()));
            return writer.toString();
        } catch (Exception e) {          
          String additionalInfo="\ndata:"+printTemplateInputdata("", templateInputData);          
          throw new VelocityTemplateEvaluationException(new VelocityTemplateExt(e.getMessage(), velocityTemplate, additionalInfo), templateInputData, e);
        } finally {
          freeVelocityToken(logTagToken);
            try {
                writer.close();
            } catch (IOException e) {
                logger.warn("Failed to perform close on StringWriter object.", e);
            }
        }
    }


    private String printTemplateInputdata(String prefix,XynaObject xo) {
      StringBuffer sb=new StringBuffer();
      String[] keys=getVariables(xo);
      for (String key:keys){
        try {
          Object o=xo.get(key);
          String newPrefix=prefix.length()>0?prefix+"."+key:key;
          if (o instanceof XynaObject){            
            sb.append("\n"+printTemplateInputdata(newPrefix, (XynaObject)o));
          } else if (o instanceof ArrayList){
            ArrayList al=(ArrayList)o;
            if(al.size()==0){
              sb.append("\n"+newPrefix+"=[]");//Array der Laenge 0
            }
            for (int i=0;i<al.size();i++){
              Object oi=al.get(i);
              if (oi instanceof XynaObject){
                sb.append("\n"+printTemplateInputdata(newPrefix+"["+i+"]", (XynaObject)oi));
              } else {
                sb.append("\n"+newPrefix+"["+i+"]="+oi);
              }
            }
          } else {
            sb.append("\n"+newPrefix+"="+o);
          }
        }
        catch (InvalidObjectPathException e) {
          logger.error("failed",e);//sollte nicht vorkommen
        }

      }
      return sb.toString();
    }
    
     private static int extractIndex(String key){
      String number=key.substring(key.indexOf("(")+1, key.indexOf(")"));    
      return Integer.parseInt(number);    
    }

    private Object getObjectByPath(Object xynaObject, final String path) {
      Object obj;
      try {
        String[] keyParts=path.split("\\.");
        obj = xynaObject;
        for (String keyPart:keyParts){
          if (obj instanceof XynaObject){
            obj = ((XynaObject)obj).get(keyPart);        
          } else if (obj instanceof List){
            int index=extractIndex(keyPart);
            if (index>=((List)obj).size()){
              return null;
            }
            obj=((List)obj).get(index);           
          }
        }
      } catch (InvalidObjectPathException e) {
        logger.warn("Failed to get key: <" + path + ">.", e);
        obj= null;
      } catch (NullPointerException e) {
        logger.warn("Failed to get key: <" + path + ">.", e);
        obj= null;
      } catch (ClassCastException e) {
        logger.warn("Failed to get key: <" + path + ">.", e);
        obj= null;
      } catch (StringIndexOutOfBoundsException e) {
        logger.warn("Failed to get key: <" + path + ">.", e);
        obj= null;
      }
      return obj;
    }


    //per reflektion, da getVariableNames bisher nicht in der abstrakten XynaObject Basis-Klasse
    private String[] getVariables(XynaObject xo) {
      try {
        Method method;
        method = xo.getClass().getMethod("getVariableNames");
        Set<String> retSet=(Set<String>)method.invoke(xo);
        String[] ret= retSet.toArray(new String[retSet.size()]);
        Arrays.sort(ret);        
        return ret;
      }
      catch (Exception e) {
        logger.error("failed",e);//sollte nicht vorkommen
      }
      return new String[0];
    }
    
    private static HashMap getHashMap(Object object) {
      if (object instanceof XynaObject){
        Method m;
        try {
          m = object.getClass().getMethod("getVariableNames");
          Set<String> variableNames=(Set<String>) m.invoke(object);
          HashMap hm=new HashMap();
          for(String key:variableNames){
            hm.put(key, ((XynaObject)object).get(key));
          }
          return hm;
        }
        catch (Exception e) {
          logger.error("error conve ",e);
        }

      }    

      return (HashMap) object;   
    }

    private VelocityContext toVelocityContext(final TemplateInputData templateInputData) {
      VelocityContext velocityContext = new VelocityContext();
      for (String key : templateInputData.getVariableNames()) {
        try {
          velocityContext.put(key, templateInputData.get(key));
        } catch (XynaException e) {
          throw new IllegalArgumentException("Failed to get key <" + key + "> from input data.", e);
        }
      }
      String[] mapping=mappingString.split(";");
      HashMap aliases=new HashMap();
      for(String oneMapping:mapping){
        if (oneMapping.indexOf('=')<0) continue;
        String[] parts=oneMapping.split("=");
        String alias=parts[0].trim();
        String path=parts[1].trim();
        Object obj=getObjectByPath(templateInputData,path);
        if (obj==null){
          continue;
        }
        if (alias.indexOf('.')<0){
          aliases.put(alias, obj);
        } else {
          String[] aliasParts=alias.split("\\.");
          HashMap hm=aliases;
          
          for(int i=0;i<aliasParts.length-1;i++){
            if(hm.containsKey(aliasParts[i])){
              HashMap hmNew=getHashMap(hm.get(aliasParts[i]));
              hm.put(aliasParts[i],hmNew);//falls Konvertierung durchgef�hrt!
              hm=hmNew;
            } else {
              HashMap hmNeu=new HashMap();
              hm.put(aliasParts[i], hmNeu);
              hm=hmNeu;
            }            
          }
          hm.put(aliasParts[aliasParts.length-1],obj);
          //aliases.put(aliasParts[0],hm);  //falsch!!!
        }        
      }
      Iterator it=aliases.keySet().iterator();
      
      while(it.hasNext()){
        String key=(String)it.next();
        velocityContext.put(key, aliases.get(key));
      }
      return velocityContext;   
    }


    public static ConfigFile evaluateTemplate(final VelocityTemplate velocityTemplate, final TemplateInputData templateInputData) throws XynaException {
        if (INSTANCE == null) {
            init(); // lazy loading - note that first request will take longer than consecutive.
        }
        return new ConfigFile(INSTANCE.evaluate(velocityTemplate, templateInputData));
    }

    private static synchronized void init() throws XynaException { //synchronized!!!!
        if (INSTANCE == null) {
            INSTANCE = new VelocityServiceImpl(true);
            logger.debug("Velocity engine successfully initialized.");
        }
    }

    
    private static final VelocityServiceImpl instanceForPropertyChangeListener = new VelocityServiceImpl();

    public void onDeployment() throws XynaException {
      
      
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                    .addPropertyChangeListener(instanceForPropertyChangeListener);
      mappingString=XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(XYNAPROPERTY_ACS_VELOCITY_ALIASES);
    }


    public void onUndeployment() throws XynaException {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                    .removePropertyChangeListener(instanceForPropertyChangeListener);
      
    }


    public ArrayList<String> getWatchedProperties() {
      ArrayList al=new ArrayList<String>();
      al.add(XYNAPROPERTY_ACS_VELOCITY_ALIASES);
      return al;
    }


    public void propertyChanged() {
      mappingString=XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(XYNAPROPERTY_ACS_VELOCITY_ALIASES);      
    }


    
}
