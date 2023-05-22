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
package com.gip.xyna.xfmg.xfctrl.proxymgmt.code;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.GenericRMIAdapter.URLChooser;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.GenerationData;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyValidation;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.Call;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.CodeBlock;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.CodePart;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.GenericType;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.MethodDeclaration;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.Parameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.CodeBuilder.Statement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyCheckAfterwards;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xmcp.RMIChannelImpl;
import com.gip.xyna.xmcp.XynaRMIChannel;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;

public class ProxyCodeBuilder {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(ProxyCodeBuilder.class);
  
  private final ProxyRole proxyRole;
  private MethodDeclaration forwardingInterface;
  private CodeBuilder cbInterface;
  private CodeBuilder cbImpl;
  private List<Method> proxyMethods;
  private int allRmiMethodCount;
  private int proxyRmiMethodCount;
  private boolean onlyInterface = false;
  private String path;
  private String interfaceName;
  private String implName;
  
  public ProxyCodeBuilder(ProxyRole proxyRole) {
    this(proxyRole, "com.gip.xyna.xmcp", proxyRole.getName()+"_Proxy", proxyRole.getName()+"_ProxyImpl" );
  }
  
  public ProxyCodeBuilder(ProxyRole proxyRole, String path, String interfaceName, String implName) {
    this.proxyRole = proxyRole;
    this.path = path;
    this.interfaceName = interfaceName;
    this.implName = implName;
  }

  public void createRmiProxy(String rmiUrl, boolean local) {
    filterProxyMethods();
    
    boolean useDeprecated = proxyRole.getGenerationData().deprecated();
    
    cbInterface = CodeBuilder.newInterface( path, interfaceName).
        comment(createInterfaceComment()).
        extending(Remote.class);
    if( !onlyInterface ) {
      cbImpl = CodeBuilder.newClass( path, implName).
          implementing(path, interfaceName).
          implementing( InitializableRemoteInterface.class );
      initializeProxyImpl(cbImpl, rmiUrl, proxyRole.getName(), local);
    }
   
    
    for( Method m : proxyMethods ) {
      ProxyAccess pa = m.getAnnotation(ProxyAccess.class);
      
      MethodDeclaration md = cbInterface.declareMethod(m).
          renameParameter(pa.parameterNames()).
          comment( proxyRole.getRightsCoveredBy(pa.right(), pa.action()  ) );
      
      if( useDeprecated && m.getAnnotation(Deprecated.class) != null ) {
        md.annotate(Deprecated.class);
      }
      
      if( !onlyInterface ) {
        MethodDeclaration mdImpl = cbImpl.declareMethod(m).renameParameter(pa.parameterNames());
        implementMethod( cbImpl, mdImpl, m, pa, proxyRole);
      }
    }
    
  }
  
  public String createRmiProxyInterface() {
    onlyInterface = true;
    createRmiProxy(null, true);
    return cbInterface.toCode();
  }
  
  private String createInterfaceComment() {
    StringBuilder sb = new StringBuilder();
    GenerationData genData = proxyRole.getGenerationData();
    sb.append("Interface ").append(proxyRole.getName()).append(" generated at ").append(new Date() ).append("\n");
    sb.append("using ").append(proxyRmiMethodCount).append(" methods from ").append(allRmiMethodCount).append(" total.\n");
    appendList( sb, "Used role", genData.getRoles() );
    appendList( sb, "Additional right", genData.getAdditional() );
    appendList( sb, "Unrecognized right", genData.getUnrecognized() );
    appendList( sb, "All rights", genData.getAll() );
    
    
    if( ! genData.acceptPublic() ) {
      sb.append("Without public methods\n");
    }
    if( genData.deprecated() ) {
      sb.append("With deprecated methods\n");
    }
    
    return sb.toString();
  }

  private void appendList(StringBuilder sb, String name, List<String> list) {
    switch( list.size() ) {
    case 0:
      break;
    case 1:
      sb.append(name).append(": ").append(list.get(0)).append("\n");
      break;
    default:
      sb.append(name).append(": \n");
      for( String s : list ) {
        sb.append("    ").append(s).append("\n");
      }
    }
  }

  
  
  private static final Filter<Method> proxyRightFilter = new Filter<Method>() {
    @Override
    public boolean accept(Method value) {
      ProxyAccess pa = value.getAnnotation(ProxyAccess.class);
      return pa != null;
    }
  };
  
  private static final Filter<Method> deprecatedFilter = new Filter<Method>() {
    @Override
    public boolean accept(Method value) {
      Deprecated d = value.getAnnotation(Deprecated.class);
      return d == null;
    }
  };
  
  
  

  
  public void filterProxyMethods() {
    //Alle RMI-Methoden filtern
    
    List<Method> allRmiMethods = Arrays.asList(XynaRMIChannel.class.getMethods());
    this.allRmiMethodCount = allRmiMethods.size();
    if( logger.isDebugEnabled() ) {
      logger.debug( "allRmiMethods: "+allRmiMethods.size() );
    }
    
    List<Method> methods = allRmiMethods;
    if( ! proxyRole.getGenerationData().deprecated() ) {
      methods = CollectionUtils.filter(allRmiMethods, deprecatedFilter);
      if( logger.isDebugEnabled() ) {
        logger.debug( "allNotDeprecatedMethods: "+methods.size() );
      }
    }
    
    List<Method> allProxyRightMethods = CollectionUtils.filter(methods, proxyRightFilter);
    if( logger.isDebugEnabled() ) {
      logger.debug( "allProxyRightMethods: "+allProxyRightMethods.size() );
    }
    
    List<Method> proxyRightMethods1 = CollectionUtils.filter(allProxyRightMethods, new SimpleRoleFilter(proxyRole) );
    if( logger.isDebugEnabled() ) {
      logger.debug( "simple filtered: "+proxyRightMethods1.size() );
    }
    
    List<Method> proxyRightMethods = CollectionUtils.filter(proxyRightMethods1, new RoleFilter(proxyRole) );
    if( logger.isDebugEnabled() ) {
      logger.debug( "role filtered: "+proxyRightMethods.size() );
    }
   
    this.proxyMethods = proxyRightMethods;
    this.proxyRmiMethodCount =  proxyMethods.size();
  }
  
  public int getProxyRmiMethodCount() {
    return proxyRmiMethodCount;
  }
  

  private void implementMethod(CodeBuilder cb, MethodDeclaration md, Method m, ProxyAccess pa, ProxyRole proxyRole) {
    //Validierung
    boolean checkAfterwards = false;
    if( pa.right().needsValidation(proxyRole) ) {
      try {
        checkAfterwards = implementValidation(cb, md, pa);
      } catch( Exception e) {
        throw new ProxyGenerationFailedException( md.getName(), e );
      }
    }

    //eigentlicher Aufruf
    CodePart cp = null;
    if( md.isReturning() ) {
      cp = md.implementedBy().assign(md.getReturnType(), "ret");
    } else {
      cp = md.implementedBy();
    }
    cp.call(forwardingInterface).chain().call(md);
    
    if( checkAfterwards ) {
      md.implementedBy().comment("TODO Validierung des Ergebnisses");
    }
    
    if( md.isReturning() ) {
      cp = md.implementedBy().returning("ret");
    }
  }
  
  private boolean implementValidation(CodeBuilder cb, MethodDeclaration md, ProxyAccess pa) {
    List<Parameter> parameter = new ArrayList<Parameter>();
    if( pa.action() != Action.none ) {
      parameter.add( new Parameter( pa.action() ) );
      if( pa.checks().length == 0  !=  pa.nochecks() ) {
        throw new ProxyGenerationFailedException(md.getName(), 
            new IllegalStateException( "checks="+Arrays.toString(pa.checks()) +", nochecks="+pa.nochecks() ) );
      }
    }
    
    for( int c : pa.checks() ) {
      parameter.add( md.getParameter().get(c) );
    }
    
    Method check;
    try {
      check = ProxyValidation.class.getMethod("check_"+pa.right().name(), getClasses(parameter));
    } catch (Exception e) { //NoSuchMethodException, SecurityException
      throw new ProxyGenerationFailedException( md.getName(), e);
    }
   
    MethodDeclaration mdCheck = cb.new MethodDeclaration(check);
    md.implementedBy().on("proxyValidation").call(mdCheck, parameter);
    
    ProxyCheckAfterwards pca = check.getAnnotation(ProxyCheckAfterwards.class);
    return pca != null;
  }

  private Class<?>[] getClasses(List<Parameter> parameter) {
    Class<?>[] classes = new Class<?>[parameter.size()];
    for( int i=0; i<parameter.size(); ++i ) {
      classes[i] = parameter.get(i).getTypeClass();
    }
    return classes;
  }
  
  private void initializeProxyImpl(CodeBuilder cb, String rmiUrl, String proxyName, boolean local) {
    try {
      cb.field(String.class, "proxyName").modifier(Modifier.FINAL, Modifier.STATIC).assign().value(proxyName);
      
      forwardingInterface = create_getForwardingInterface(cb, local);
      MethodDeclaration buildProxyRole = create_buildProxyRole(cb);
      create_init(cb);
      
      CodeBlock constructor = cb.constructor().throwing(local ? null : RMIConnectionFailureException.class).implementedBy();
      
      if( !local ) {
        constructor.assign(String.class, "rmiUrl", rmiUrl);
      }
      
      if( local ) {
        constructor.assignField(XynaRMIChannel.class, "xynaRMIChannel").
        cast(XynaRMIChannel.class).
        call( Call.staticCall( cb, XynaFactory.class.getMethod("getInstance") ) ).
        chain().call(Call.call("getXynaMultiChannelPortal") ).
        chain().call(Call.call("getSection").parameter(RMIChannelImpl.DEFAULT_NAME) );
      } else {
        constructor.assign(String.class, "rmiUrl", rmiUrl);
        
        GenericType rmiAdapterType = new GenericType(GenericRMIAdapter.class, XynaRMIChannel.class);
        constructor.assignField( rmiAdapterType, "rmi" ).
          newInstance( rmiAdapterType, GenericRMIAdapter.class.getConstructor(URLChooser.class) ).
          parameterCall( 
               Call.staticCall( cb, GenericRMIAdapter.class.getMethod("getSingleURLChooser", String.class) ).
               parameterCode("rmiUrl") 
              );
        
      }
      
      constructor.assignField(ProxyValidation.class, "proxyValidation").
      newInstance( ProxyValidation.class, ProxyValidation.class.getConstructor(ProxyRole.class) ).
      parameterCall(buildProxyRole);
      
      
    } catch( Exception e ) {
      throw new RuntimeException(e);
    }
  }
  

  private MethodDeclaration create_getForwardingInterface(CodeBuilder cb, boolean local) {
    MethodDeclaration fi = cb.declareMethod("getForwardingInterface").
        modifier(Modifier.PRIVATE).
        returning(XynaRMIChannel.class).
        throwing(local ? null : RemoteException.class);
    
    if( local ) {
      fi.implementedBy().
        returning("xynaRMIChannel");
    } else {
      fi.implementedBy().
        trying().returning("rmi.getRmiInterface()").
        catching(RMIConnectionFailureException.class,"e").
        throwing( "new RemoteException(\"Connection failed\", e)" );
    }
    return fi;
  }

  private MethodDeclaration create_buildProxyRole(CodeBuilder cb) {
    MethodDeclaration bpr = cb.declareMethod("buildProxyRole").
        modifier(Modifier.PRIVATE).
        returning(ProxyRole.class);
    Statement st = bpr.implementedBy().
           returning("ProxyRole.newProxyRole(proxyName)");
    for( String right : proxyRole.getGenerationData().getAll() ) {
      st.chainNextLine().call( "addRight", right );
    }
    if( ! proxyRole.getGenerationData().acceptPublic() ) {
      st.chainNextLine().call( "withoutPublic" );
    }
    st.chainNextLine().call( "buildProxyRole");
    return bpr;
  }

  private MethodDeclaration create_init(CodeBuilder cb) {
    
    try {
      Method m = InitializableRemoteInterface.class.getMethod("init", Object[].class);
      MethodDeclaration i = cb.declareMethod(m).renameParameter("initParameters");
      return i;
    } catch (Exception e) { //NoSuchMethodException, SecurityException
      logger.warn(null, e);
      return null;
    }
  }
  
  public String getInterface() {
    return cbInterface.toCode();
  }

  public String getImplementation() {
    return cbImpl.toCode();
  }
  
  public JavaSourceFromString getInterfaceAsSource() {
    return new JavaSourceFromString(cbInterface.getFqClassName(), cbInterface.toCode() );
  }
  public JavaSourceFromString getImplementationAsSource() {
    return new JavaSourceFromString(cbImpl.getFqClassName(), cbImpl.toCode() );
  }
  
  
  /**
   * Grobe Filterung anhand der Enums
   *
   */
  public static class SimpleRoleFilter implements Filter<Method> {

    private final ProxyRole proxyRole;
    
    public SimpleRoleFilter(ProxyRole proxyRole) {
      this.proxyRole = proxyRole;
    }

    @Override
    public boolean accept(Method value) {
      ProxyAccess pa = value.getAnnotation(ProxyAccess.class);
      return pa.right().isAllowedIn(proxyRole, null); //keine Filterung auf Action
    }
    
  }

  /**
   * Feine Filterung anhand der unterschiedlichen ScopedRight-Auspr�gungen
   *
   */
  public static class RoleFilter implements Filter<Method> {

    private ProxyRole proxyRole;

    public RoleFilter(ProxyRole proxyRole) {
      this.proxyRole = proxyRole;
    }

    @Override
    public boolean accept(Method value) {
      ProxyAccess pa = value.getAnnotation(ProxyAccess.class);
      return pa.right().isAllowedIn(proxyRole, pa.action());
    }
    
  }

  public void compileImpl(File jarFile) throws XPRC_CompileError, Ex_FileAccessException {
    InMemoryCompilationSet cs = new InMemoryCompilationSet(true, false, false);
    cs.addToCompile( getInterfaceAsSource() );
    cs.addToCompile( getImplementationAsSource() );
    cs.compileToJar(jarFile, false);
  }
  
  public void compileInterface(File jarFile) throws XPRC_CompileError, Ex_FileAccessException {
    InMemoryCompilationSet cs = new InMemoryCompilationSet(true, true, false);
    cs.addToCompile( getInterfaceAsSource() );
    cs.compileToJar(jarFile, true);
  }
 
}
