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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsmiparser.smi.SmiModuleIdentity;
import org.jsmiparser.smi.SmiModule;
import org.jsmiparser.smi.SmiObjectType;
import org.jsmiparser.smi.SmiOidNode;
import org.jsmiparser.smi.SmiOidValue;
import org.jsmiparser.smi.SmiPrimitiveType;
import org.jsmiparser.smi.SmiSymbol;
import org.jsmiparser.smi.SmiTable;
import org.jsmiparser.smi.SmiVariable;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.Builder;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.DataModel;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Meta;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class MIBModuleImport {
  
  private static Logger logger = CentralFactoryLogging.getLogger(MIBModuleImport.class);
  
  private enum SimpleType {
    //was ist das?
    //ENUM(SmiPrimitiveType.ENUM, null, null),
    
    // From the SimpleSyntax choice:
    INTEGER( SmiPrimitiveType.INTEGER, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER),
    OCTET_STRING( SmiPrimitiveType.OCTET_STRING, "xact.snmp.types.SNMPOctet", PrimitiveType.STRING),
    //OBJECT_IDENTIFIER( SmiPrimitiveType.OBJECT_IDENTIFIER, null , null); //TODO
    
    // Defined ASN.1 type:
    INTEGER_32( SmiPrimitiveType.INTEGER_32, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER), //TODO
    
    // From the ApplicationSyntax choice:
    IP_ADDRESS( SmiPrimitiveType.IP_ADDRESS, "xact.snmp.types.SNMPIpAddress", PrimitiveType.STRING),
    COUNTER_32( SmiPrimitiveType.COUNTER_32, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER ), //TODO
    GAUGE_32( SmiPrimitiveType.GAUGE_32, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER), //TODO
    UNSIGNED_32( SmiPrimitiveType.UNSIGNED_32, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER ), //TODO
    TIME_TICKS( SmiPrimitiveType.TIME_TICKS, "xact.snmp.types.SNMPInteger", PrimitiveType.INTEGER ), //TODO
    //OPAQUE( SmiPrimitiveType.OPAQUE,  null ); //TODO
    COUNTER_64( SmiPrimitiveType.COUNTER_64, "xact.snmp.types.SNMPCounter64", PrimitiveType.LONG_OBJ),
    
    // From the TextualConvention macro:
    //BITS( SmiPrimitiveType.BITS, null, null ); //TODO
    ;
    
    //TODO wof�r ?
    //"xact.snmp.types.SNMPNull"
    //"xact.snmp.types.SNMPString"
    //"xact.snmp.types.SNMPUnsignedInteger"
    //"xact.snmp.types.SNMPVariableType"
    
    
    private SmiPrimitiveType type;
    private String snmpType;
    private PrimitiveType simpleType;
    private SimpleType(SmiPrimitiveType type, String snmpType, PrimitiveType simpleType) {
      this.type = type;
      this.snmpType = snmpType;
      this.simpleType = simpleType;
    }
    private static final EnumMap<SmiPrimitiveType,SimpleType> SNMP_TYPES;
    static {
      SNMP_TYPES = new EnumMap<SmiPrimitiveType,SimpleType>(SmiPrimitiveType.class);
      for( SimpleType st : SimpleType.values() ) {
        SNMP_TYPES.put( st.type, st);
      }
    }
    public static SimpleType get(SmiPrimitiveType primitiveType) {
      return SNMP_TYPES.get(primitiveType);
    }
    public PrimitiveType getSimpleType() {
      return simpleType;
    }
    public String getSnmpType() {
      return snmpType;
    }
  }
  
  private static final Pattern javaName = Pattern.compile("[^\\w.]");
  
  private SmiModule module;
  private ImportParameter parameter;
  private List<Datatype> dataTypes = new ArrayList<Datatype>();
  private String xmomPath;
  private String modelName;
  private XmomType baseType;
  private com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel dataModel;
  private List<OID> importOnlyOids;
  private DataModelSpecificsBuilder dataModelSpecificsBuilder;
  private NodeData moduleData;
  private String dataModelName;
  
  public MIBModuleImport(SmiModule module, ImportParameter parameter, List<OID> oids) {
    this.module = module;
    this.parameter = parameter;
    this.importOnlyOids = oids;
    initialize();
  }
  
  private void initialize() {
    SmiModuleIdentity mi = module.getModuleIdentity();
    StringBuilder xmomPath = new StringBuilder();
    xmomPath.append(parameter.getBasePath());
    xmomPath.append('.');
    xmomPath.append(module.getId().replaceAll("-","_"));
    
    Versioning versioning = parameter.getVersioning();
    xmomPath.append(".").append( versioning.createPathVersion(mi) );
    this.xmomPath = xmomPath.toString();
    
    SmiOidValue moduleIdentity = getModuleIdentityAsSmiOidValue(); //Module Identity noch einmal als SmiOidValue ermitteln, da in SmiModuleIdentity name und node nicht enthalten sind
    String name = moduleIdentity.getId();
    String Name = name.substring(0,1).toUpperCase()+name.substring(1);
    Name = javaName.matcher(Name).replaceAll("_");
    try {
      Name = GenerationBase.transformNameForJava(Name);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    
    this.modelName = xmomPath.toString()+"."+Name;
    this.baseType = parameter.getBaseType();
    
    this.moduleData = new NodeData(moduleIdentity.getNode(), true);
    
    XmomType type = new XmomType( xmomPath.toString(), Name, module.getId()+"::"+name);
    dataModelName = type.getFQTypeName();
    
    moduleData.setType(baseType, type);
    String documentation = MIBTools.stripLeadingWhitespaces(mi.getDescription());
    moduleData.setMeta( Meta.dataModel_documentation(     
                                                     MIBDataModel.baseModel(moduleIdentity.getNode().getOidStr(), baseType),
                                                     documentation ) );
    
    dataModelSpecificsBuilder = new DataModelSpecificsBuilder(module.getId(), 
                                                              moduleIdentity.getNode().getOidStr(), importOnlyOids);
    
    Builder dataModelBuilder = new Builder();
    
    dataModelBuilder.baseType(baseType.toXynaObject());
    dataModelBuilder.type(type.toXynaObject() );
    dataModelBuilder.dataModelType(parameter.getDataModelTypeName());
    dataModelBuilder.version( versioning.createDataModelVersion(mi) );
    dataModelBuilder.documentation(documentation);
    
    dataModel = dataModelBuilder.instance();
  }

  private static class DataModelSpecificsBuilder {

    private String module;
    private String oid;
    private List<OID> restrictions;
    private int datatypeCount;
    
    public DataModelSpecificsBuilder(String module, String oid, List<OID> restrictions) {
      this.module = module;
      this.oid = oid;
      this.restrictions = restrictions;
    }

    public void setDatatypeCount(int datatypeCount) {
      this.datatypeCount = datatypeCount;
    }

    public List<DataModelSpecific> asList() {
      List<DataModelSpecific> dms = new ArrayList<DataModelSpecific>();
      dms.add( new DataModelSpecific(null, module, "module" ) );
      dms.add( new DataModelSpecific(null, oid, "OID") );
      dms.add( new DataModelSpecific(null, String.valueOf(datatypeCount), "datatypeCount" ) );
      if( restrictions != null ) {
        int cnt = 0;
        for( OID r : restrictions ) {
          String key = "%0%.oids[\""+cnt+"\"]";
          ++cnt;
          dms.add( new DataModelSpecific( key, r.toString(), "restriction") );
        }
      }
      return dms;
    }
  }

  public void createDataTypes() {
    try {
      OIDMap<SmiOidValue> oidValueMap = new OIDMap<SmiOidValue>();
      for (SmiOidValue value : module.getOidValues()) {
        if (value.getNode() != null) {
          oidValueMap.put(value.getOidStr(), value);
        } else {
          logger.warn("Could not determine oid for " + value.getId());
        }
      }
      
      //OIDs die nicht unterhalb der ModuleIdentity liegen importieren
      List<Variable> additionalVariables = new ArrayList<Variable>();
      List<OID> rootOids = oidValueMap.getChildren("1.", false);
      for (OID oid : rootOids) {
        if (!oid.equals(new OID(getModuleIdentityAsSmiOidValue().getNode().getOidStr()))) {
          SmiOidValue oidValue = oidValueMap.get(oid);
          NodeData nodeData = createChildData(oidValue, moduleData);
          
          Variable var = nodeData.getVariable();
          additionalVariables.add(var);
          
          createDataType( nodeData, null );
        }
      }
      
      //OIDs unterhalb der ModuleIdentity importieren
      createDataType( moduleData , additionalVariables);
      
      dataModelSpecificsBuilder.setDatatypeCount(dataTypes.size());
      dataModel.setDataModelSpecifics( dataModelSpecificsBuilder.asList() );
    } catch( Exception e) {
      throw new RuntimeException( "Exception while creating data types in module "+getModuleName(), e );
    }
  }
  
  public com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel getDataModel() {
    return dataModel;
  }
  
  public List<Datatype> getDataTypes() {
    return dataTypes;
  }
  
  public String getDataModelName() {
    return dataModelName;
  }

  public String getModuleName() {
    return module.getId();
  }
  
  private void createDataType(NodeData nodeData, List<Variable> additionalVariables) {
    if (nodeData.isSimple()) {
      return;
    }
    List<NodeData> childData = getNodeDataList(nodeData);
    
    List<Variable> variables = new ArrayList<Variable>();
    for( NodeData cd : childData ) {
      if( cd.isSameModule() ) {
        variables.add(cd.getVariable());
      } else {
        //TODO Unterst�tzung von DataModel-Joints
      }
    }
    
    if (additionalVariables != null) {
      variables.addAll(additionalVariables);
    }
    dataTypes.add(nodeData.createDatatype(variables));    
    
    for( NodeData cd : childData ) {
      if( cd.isSimple() ) {
        continue; //f�r simple Variablen keine Rekursion
      }
      //rekursiv nun weitere Datentypen anlegen, falls diese noch zum gleichen Module geh�ren
      if( cd.isSameModule() ) {
        createDataType(cd, null);
      }
    }
  }

  private static class NodeData {

    private Variable variable;
    private XmomType type;
    private XmomType baseType;
    private SmiOidNode node;
    private Meta meta;
    private boolean sameModule;
    private boolean table;
    private String oidPrefix;
    
    public NodeData(SmiOidNode node, boolean sameModule) {
      this.node = node;
      this.sameModule = sameModule;
    }
    
    @Override
    public String toString() {
      return "NodeData("+variable.getName()+","+type+","+node.getOidStr()+")";
    }

    public void setMeta(Meta meta) {
      this.meta = meta;
    }

    public void setType(XmomType baseType, XmomType type) {
      this.baseType = baseType;
      this.type = type;
    }

    public boolean isSameModule() {
      return sameModule;
    }

    public Variable getVariable() {
      return variable;
    }
    
    public String getLabel() {
      return type.getLabel();
    }

    public Datatype createDatatype(List<Variable> variables) {
      return Datatype.meta(type, null, meta, variables);
    }

    public boolean isSimple() {
      return type == null;
    }

    public OID getOID() {
      return new OID( node.getOidStr() );
    }

    public SmiOidNode getNode() {
      return node;
    }

    public boolean isTable() {
      return table;
    }

    public void simple(SmiVariable var, NodeData parent) {
      String childName = MIBTools.getName(var.getNode());
      Pair<String, Boolean> oid = MIBTools.getOid(var, parent.getNode(), parent.oidPrefix);
      SimpleType simpleType = SimpleType.get(var.getPrimitiveType());
      if( simpleType == null ) {
        boolean addMissingType = true; //TODO Property
        String msg = null;
        if( var.getPrimitiveType() == null ) {
          String unknownType = var.getType().getIdToken().getValue();
          msg = "Unknown Type "+unknownType +" is not supported";
        } else {
          msg = var.getPrimitiveType() +" is not supported";
        }
            
        if( addMissingType ) {
          simpleType = SimpleType.OCTET_STRING;
          logger.info( msg);
        } else {
          throw new UnsupportedOperationException(msg);
        }
      }
      
      DataModel childDM;
      if (oid.getSecond()) {
        childDM = MIBDataModel.rootOid_type(oid.getFirst(), simpleType.getSnmpType());
      } else {
        childDM = MIBDataModel.oid_type(oid.getFirst(), simpleType.getSnmpType());
      }
      
      String description = MIBTools.stripLeadingWhitespaces(var.getDescription());
      Meta meta = Meta.dataModel_documentation(childDM, description);
      variable = Variable.create(childName).simpleType(simpleType.getSimpleType()).meta(meta).isList( parent.isTable()).build();
    }

    public void complex(SmiOidValue val, String xmomPath, XmomType baseType, String modelName, NodeData parent) {
      String childName = MIBTools.getName(val.getNode());
      String ChildName = childName.substring(0,1).toUpperCase()+childName.substring(1);
      ChildName = javaName.matcher(ChildName).replaceAll("_");

      
      this.type = new XmomType( xmomPath, ChildName, parent.getLabel()+"."+childName );
      this.baseType = baseType;
      
      Pair<String, Boolean> oid = MIBTools.getOid(val, parent.getNode(), parent.oidPrefix);
      
      DataModel childDM;
      if (oid.getSecond()) {
        childDM = MIBDataModel.rootOid(oid.getFirst());
      } else {
        childDM = MIBDataModel.oid(oid.getFirst());
      }
      
      variable = Variable.create(childName).complexType(type).meta(Meta.dataModel(childDM)).isList(parent.isTable()).build();
      
      DataModel dataModel = MIBDataModel.modelName(node.getOidStr(), modelName);
      
      if( val instanceof SmiObjectType ) {
        String description = MIBTools.stripLeadingWhitespaces(((SmiObjectType)val).getDescription());
        meta = Meta.dataModel_documentation(dataModel, description );
      } else {
        meta = Meta.dataModel(dataModel);
      }
      if( val instanceof SmiTable ) {
        table = true;
      }
    }
        
  }

  private List<NodeData> getNodeDataList(NodeData nodeData) {
    List<NodeData> childData = new ArrayList<NodeData>();
    
    boolean checkImportOnly = importOnlyOids != null;
    if( checkImportOnly ) {
      //evtl. müssen alle Children genommen werden. Dies nun pr�fen
      OID oid = nodeData.getOID();
      for( OID ioo : importOnlyOids ) {
        if( ioo.hasChild(oid) ) {
          checkImportOnly = false; //alle Children nehmen, d.h. checkImportOnly deaktivieren
          break;
        }
      }
    }
    
    for (SmiOidNode c : nodeData.getNode().getChildren() ) {
      if( checkImportOnly ) {
        boolean includeChild = false;
        OID oid = new OID( c.getOidStr() );
        for( OID ioo : importOnlyOids ) {
          if( oid.hasChild(ioo) ) {
            includeChild = true;
            break;
          }
        }
        if( ! includeChild ) {
          continue;
        }
      }
      
      switch( c.getValues().size() ) {
        case 0:
          //Sonderbehandlung f�r Traps
          NodeData nd = new NodeData(c, true);
          nd.type = new XmomType(xmomPath, "", nodeData.getLabel()+"."+c.getValue());
          nd.oidPrefix = String.valueOf(c.getValue());
          childData.addAll( getNodeDataList(nd) );
          break;
        case 1:
          childData.add( createChildData( c.getSingleValue(), nodeData ) );
          break;
        default:
          logger.warn( c.getValues().size() +" values for "+c.getOidStr() );
          for( SmiOidValue val : c.getValues() ) {
            NodeData child = createChildData( val, nodeData );
            logger.warn( "-> child "+child );
            childData.add(child);
          }
          break;
      }
    }
    return childData;
  }


  private NodeData createChildData(SmiOidValue val, NodeData nodeData) {
    //zum selben Module geh�ren alle, die entweder im gleichen Module sind 
    //... oder in einem Module ohne Identity vorkommen 
    boolean sameModule = val.getModule() == module || val.getModule().getModuleIdentity() == null;
    
    NodeData nd = new NodeData(val.getNode(), sameModule);
    if( val instanceof SmiVariable) {
      nd.simple( (SmiVariable)val, nodeData ); 
    } else {
      nd.complex( val, xmomPath, baseType, modelName, nodeData );
    }
    return nd;
  }

  private SmiOidValue getModuleIdentityAsSmiOidValue() {
    List<SmiSymbol> symbols = (List<SmiSymbol>) module.getSymbols();
    return (SmiOidValue) symbols.get(0); //Module Identity ist der erste Eintrag nach den Imports
  }
}
