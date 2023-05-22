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



import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import xact.templates.templateprovider.exceptions.Codes;

import com.gip.xyna.XynaFactory;

import com.gip.xyna.templateprovider.*;
import com.gip.xyna.templateprovider.persistence.TemplatePersistence;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xdnc.xnwh.VelocityTemplateStorable;




/**
 * Velocity template provider.
 */
public final class TemplateProviderImpl implements DeploymentTask, IPropertyChangeListener{


  private static Logger logger = Logger.getLogger(TemplateProviderImpl.class.getName());
  private static TemplateProviderImpl INSTANCE;
  volatile private static String mappingString="";
  
  
  
  public static String getMappingString() {
    return mappingString;
  }


  private static final String XYNAPROPERTY_ACS_VELOCITY_ALIASES = "xact.acs.velocity.aliases";


  // can safely be public since no one will be able to instantiate this class and the variable is not static
  public TypeAwareTemplateGenerator templateGenerator;

  //for use in Xyna Factory only. Initialising templateGenerator -> templates is done in onDeployment;
  protected TemplateProviderImpl() throws XynaException {  
  }
  
 
  private static List<TemplatePart> velocityTemplatePartListToTemplatePartList(List<VelocityTemplatePart> velocityTemplatePartList) {
    List<TemplatePart> ret=new ArrayList<TemplatePart> ();
    for(VelocityTemplatePart velocityTemplatePart:velocityTemplatePartList){
      TemplatePart tp=new TemplatePart(velocityTemplatePart);
      ret.add(tp);      
    }
    return ret;
  }

  
  private TemplateProviderImpl(List<TemplatePart> templateParts) throws XynaException {
    new Codes(); //damit exceptions korrekt initialisiert werden
    try {
      templateGenerator = new TypeAwareTemplateGenerator(templateParts);
    } catch (RuntimeException e) {
      throw new XynaException(Codes.CODE_VELOCITY_TEMPLATE_PROVIDER_INITIALIZATION_FAILURE).initCause(e);
    }
  }


  public static VelocityTemplate buildTemplate(TemplateType templateType, TemplateInputData templateInputData)
      throws XynaException {
    if (templateType == null) {
      throw new IllegalArgumentException("Template type may not be null.");
    } else if (templateInputData == null) {
      throw new IllegalArgumentException("Template input data may not be null.");
    }
    if (INSTANCE == null) {//in xyna Factory load will be done in onDeployment
      init(); // lazy loading - note that first request will take longer than consecutive.
    }
    try {
      String templateString =
          INSTANCE.templateGenerator.generateTemplate(templateType.getTemplateType(),
                                                      new InputDataAdapter(templateInputData));
      return new VelocityTemplate(templateString);
    } catch (UnknownTemplateTypeException e) {
      throw new XynaException(Codes.CODE_VELOCITY_TEMPLATE_PROVIDER_UNKNOWN_TEMPLATE_TYPE(templateType
          .getTemplateType())).initCause(e);
    } catch (GenerationFailureException e) {
      throw new XynaException(Codes.CODE_VELOCITY_TEMPLATE_PROVIDER_GENERATION_FAIL).initCause(e);
    }
  }


  public static synchronized void reload(List<? extends VelocityTemplatePart> templateParts) throws XynaException {
    ArrayList<TemplatePart> al=new ArrayList<TemplatePart>();
    for(VelocityTemplatePart tp:templateParts){
      
      al.add(new TemplatePart(tp));
    }
    TemplateProviderImpl newImpl = new TemplateProviderImpl(al); //dont override working instance if it exists and an error occurs
    INSTANCE = newImpl;
    logger.debug("Template provider successfully reinitialized.");
  }
  
  public static List<VelocityTemplatePart> readFromHistory() throws XynaException {
    return TemplatePersistence.readFromHistory();
  }
  

 public static List<? extends VelocityTemplatePart> loadTemplateDataFromPersistence() {
   try{
     return readFromHistory();
   } catch (Exception e){
     throw new RuntimeException("error loading template dataq",e);
   }
  }

  
  private static synchronized void init() throws XynaException {//for use outside xyna factory
    if (INSTANCE == null) {
      List<TemplatePart> templateParts = velocityTemplatePartListToTemplatePartList(readFromHistory());
      INSTANCE = new TemplateProviderImpl(templateParts);
      logger.debug("Template provider successfully initialized.");
    }
  }
  
  //Initialising the templates in Xyna Factory
  public void onDeployment() {
    if (logger.isDebugEnabled()) {
      logger.debug("Start OnDeployment:");
    }
    try{
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().registerStorable(VelocityTemplateStorable.class);
      new Codes(); //damit exceptions korrekt initialisiert werden
      templateGenerator = new TypeAwareTemplateGenerator(velocityTemplatePartListToTemplatePartList(readFromHistory()));
      INSTANCE=this;
    } catch (Exception e){
      throw new IllegalStateException("onDeployment failed", new XynaException(Codes.CODE_VELOCITY_TEMPLATE_PROVIDER_INITIALIZATION_FAILURE).initCause(e));
    }
    propertyChanged();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().addPropertyChangeListener(this);

    if (logger.isDebugEnabled()) {
      logger.debug("End OnDeployment");
    }
    
  }
  
  public void onUndeployment() throws XynaException {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
    .removePropertyChangeListener(this);

  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> al=new ArrayList<String>();
    al.add(XYNAPROPERTY_ACS_VELOCITY_ALIASES);
    return al;
  }


  public void propertyChanged() {
    mappingString=XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(XYNAPROPERTY_ACS_VELOCITY_ALIASES); 
    if (mappingString == null) {
      mappingString = "";
    }
  }

  
}
