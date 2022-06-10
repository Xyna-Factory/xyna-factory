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
package xfmg.xfctrl.datamodel.csv.impl;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.CsvUtils.CSVDocument;
import com.gip.xyna.utils.misc.CsvUtils.CSVIterator;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;

import xact.templates.CSV;
import xact.templates.Document;
import xfmg.xfctrl.datamodel.csv.CSVDatamodelServicesServiceOperation;
import xfmg.xfctrl.datamodel.csv.impl.CSVXynaObjectWriter.Header;
import xfmg.xfctrl.datamodel.csv.impl.fields.AbstractXmomField;
import xfmg.xfctrl.datamodel.csv.impl.fields.NoParentXmomField;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomField;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomField.FieldType;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomFieldParser;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomListField;
import xfmg.xfctrl.datamodel.csv.parameter.CSVParameter;
import xfmg.xfctrl.datamodel.csv.parameter.CSVReadOptions;
import xfmg.xfctrl.datamodel.csv.parameter.CSVWriteOptions;
import xfmg.xfctrl.datamodel.csv.parameter.StringTransformation;


public class CSVDatamodelServicesServiceOperationImpl implements ExtendedDeploymentTask, CSVDatamodelServicesServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(CSVDatamodelServicesServiceOperationImpl.class);

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public CSVParameter fillDefaultCSVParameter(CSVParameter csvParameter) {
    if( csvParameter.getMasker() == null ) {
      csvParameter.setMasker(CSVWriter.DEFAULT_MASKER);
    }
    if( csvParameter.getSeparator() == null ) {
      csvParameter.setSeparator(CSVWriter.DEFAULT_SEPARATOR);
    }
    if( csvParameter.getLineSeparator() == null ) {
      csvParameter.setLineSeparator(CSVWriter.DEFAULT_LINE_SEPARATOR);
    }
    if( csvParameter.getEndOfFile()== null ) {
      csvParameter.setEndOfFile(CSVWriter.DEFAULT_EOF);
    }
    if( csvParameter.getNullRepresentation() == null ) {
      csvParameter.setNullRepresentation(CSVWriter.DEFAULT_NULL_REPRESENTATION);
    }
    if( csvParameter.getHeader() == null ) {
      csvParameter.setHeader(CSVWriter.DEFAULT_HEADER);
    }
    return csvParameter;
  }

  public Document writeCSVDocument(CSVParameter csvParameter, List<GeneralXynaObject> data) {
    escapeLineSeparator(csvParameter);
    CSVXynaObjectWriter csvWriter = new CSVXynaObjectWriter(csvParameter);
    initCsvWriter(csvWriter, data);
    String text = csvWriter.write(data);
    return new Document.Builder().documentType(new CSV()).text(text).instance();
    
  }

  public Document writeCSVDocumentWithOptions(CSVParameter csvParameter, List<GeneralXynaObject> data, CSVWriteOptions csvWriteOptions) {
    escapeLineSeparator(csvParameter);
    CSVXynaObjectWriter csvWriter = new CSVXynaObjectWriter(csvParameter, csvWriteOptions);
    initCsvWriter(csvWriter, data);
    String text = csvWriter.write(data);
    return new Document.Builder().documentType(new CSV()).text(text).instance();
  }

  private void initCsvWriter(CSVXynaObjectWriter csvWriter, List<GeneralXynaObject> data) {
    if( data instanceof XynaObjectList ) {
      if( ((XynaObjectList)data).getContainedClass() != null ) {
        csvWriter.setClass( ((XynaObjectList)data).getContainedClass() );
      } else {
        if( data.size() > 0 ) {
          csvWriter.setClass( data.get(0).getClass() );
        }
        //csvWriter.setClass( ((XynaObjectList)data).getContainedFQTypeName() );
        //((XynaObjectList)data).ge
      }
    } else {
      logger.info("##### no XynaObjectList");
      if( data.size() > 0 ) {
        csvWriter.setClass( data.get(0).getClass() );
      }
    }
  }

  public List<GeneralXynaObject> readCSVDocument(Document doc, CSVParameter para, GeneralXynaObject templ) {
    CSVReadOptions options = new CSVReadOptions();
    return readCSVDocumentWithOptions(doc, para, templ, options);
  }


  public List<GeneralXynaObject> readCSVDocumentWithOptions(Document doc, CSVParameter para, GeneralXynaObject templ,
                                                            CSVReadOptions options) {
    if (templ == null) {
      throw new RuntimeException("Template Object must not be null");
    }
    Header h = getHeader(para.getHeader());
    escapeLineSeparator(para);
    fillDefaultCSVParameter(para);
    CSVDocument d = new CSVDocument(doc.getText(), para.getSeparator(), para.getMasker(), para.getLineSeparator(), para.getNullRepresentation());
    List<CSVIterator> lines = d.getLines();
    Map<String, StringTransformation> stringTransformations = new HashMap<>(); // FIXME support von stringtransformationen?
    XmomFieldParser fieldParser = new XmomFieldParser(Collections.emptySet(), stringTransformations);
    try {
      fieldParser.parse(templ.getClass());
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
        | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }
    XynaObjectCreator creator = new XynaObjectCreator(templ);
    Pair<Mapper[], Boolean> mapperCreation = createMappers(lines.get(0), fieldParser, h, options, creator);
    Mapper[] mappers = mapperCreation.getFirst();
    GeneralXynaObjectList<GeneralXynaObject> result = new GeneralXynaObjectList(templ.getClass());
    GeneralXynaObject currentObj = null;
    for (int i = 1; i < lines.size(); i++) {
      CSVIterator csvit = lines.get(i);
      /*
       * die erkennung ist nicht eindeutig (falls zwei objekte mit identischen feldern geschrieben wurden, werden sie und ihre listen gemerged)
       * if (isListExtensionLine(csvit, lastline)) {
       *   obj = previousObj
       * } else {
       *   obj = creator.create();
       * }
       */
      
      //beim create wird immer bereits die gesamte (teil-)baumstruktur erzeugt, die für die befüllung notwendig ist.
      //damit spart man sich das lazy checken später
      boolean isRootMappings;
      List<PathPart> pathParts;
      if (mapperCreation.getSecond()) {
        String meta = csvit.next();
        String type = meta.substring(meta.indexOf('=') + 1); // TODO use type for object creation
        String access = meta.substring(0, meta.indexOf('='));
        pathParts = parsePath(access, type);
        isRootMappings = meta.startsWith("root=");
      } else {
        pathParts = Collections.emptyList();
        isRootMappings = true;
      }
      
      if (isRootMappings) {
        currentObj = creator.create(pathParts);
        result.add(currentObj);
      } else {
        creator.createNewSubs(currentObj, pathParts);
      }
      int idx = 0;
      while (csvit.hasNext()) {
        String field = csvit.next();
        if (field == null || para.getNullRepresentation().equals(field)) {
          //null-werte überspringen
          idx++;
          continue;
        }
        if (isRootMappings) {
          mappers[idx++].fill(currentObj, field);
        } else {
          mappers[idx++].fill(currentObj, field, pathParts);
        }
      }
      
    }
    return result;
  }

  
  private static List<PathPart> parsePath(String meta, String type) {
    String[] metaParts = meta.split("[.]");
    List<PathPart> pathParts = new ArrayList<>();
    for (int i = 0; i < metaParts.length; i++) {
      String metaPart = metaParts[i];
      if (metaPart.endsWith("]") &&
          metaPart.contains("[")) {
        String path = metaPart.substring(0, metaPart.indexOf('['));
        int index = Integer.parseInt(metaPart.substring(metaPart.indexOf('[') + 1, metaPart.length() - 1));
        boolean isPrimitive = i + 1 < metaParts.length ? false : type.startsWith("java.lang"); // TODO ugly
        pathParts.add(new PathPart(path, index, isPrimitive));
      } else {
        pathParts.add(new PathPart(metaPart));
      }
    }
    return pathParts;
  }




  private void escapeLineSeparator(CSVParameter para) {
    if (para.getLineSeparator() == null) {
      return;
    }
    if (para.getLineSeparator().contains("\\")) {
      para.setLineSeparator(para.getLineSeparator().replace("\\n", "\n"));
      para.setLineSeparator(para.getLineSeparator().replace("\\r", "\r"));
      para.setLineSeparator(para.getLineSeparator().replace("\\t", "\t"));
    }
  }

  private Pair<Mapper[], Boolean> createMappers(CSVIterator header, XmomFieldParser fieldParser, Header htype, CSVReadOptions options,
                                 XynaObjectCreator creator) {
    Set<String> xmomfieldPaths = new HashSet<String>();
    if( options.getStrict() ) {
      for( XmomField xf : fieldParser.getXmomFields() ) {
        xmomfieldPaths.add(xf.getPath());
      }
    }
    
    Boolean includesMeta = null;
    List<Mapper> mappers = new ArrayList<>();
    while (header.hasNext()) {
      String headerVal = header.next();
      if (includesMeta == null && 
          headerVal.equals(CSVXynaObjectWriter.META_HEADER)) {
        includesMeta = Boolean.TRUE;
      } else {
        if (includesMeta == null) {
          includesMeta = Boolean.FALSE;
        }
        mappers.add(createMapper(headerVal, fieldParser, htype, creator, includesMeta));
      }
    }
    if( options.getStrict() ) {
      for( Mapper m : mappers ) {
        xmomfieldPaths.remove(m.getPath());
      }
      if( ! xmomfieldPaths.isEmpty() ) {
        throw new RuntimeException("Fields "+xmomfieldPaths+" missing!");
      }
    }
    return Pair.of(mappers.toArray(new Mapper[0]), includesMeta);
  }


  private Mapper createMapper(String headerVal, XmomFieldParser fieldParser, Header htype, XynaObjectCreator creator, Boolean supportsLists) {
    XmomField field = fieldParser.findField(headerVal, htype);
    if (field == null) {
      field = fieldParser.findListField(headerVal, htype);
      if (field == null) {
        throw new RuntimeException("Could not identify member var described by header <" + headerVal + "> of type <" + htype.name() + ">.");        
      }
    }
    String path = field.getPath();
    if (path.contains(".")) {
      creator.addMemberCreation(field);
    }
    return new Mapper(path, field.getType());
  }


  private static class XynaObjectCreator {

    private static class MemberCreation {

      public List<MemberCreation> members;
      private final Constructor<? extends GeneralXynaObject> constructor;
      private final String varName;
      private final boolean isList;


      public MemberCreation(Class<? extends GeneralXynaObject> clazz, String varName, boolean isList) {
        try {
          this.constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
        this.varName = varName;
        this.isList = isList;
      }

      
      public Object createMember(GeneralXynaObject parent) {
        Object value = null;
        if (!isList) {
          try {
            if (parent != null) {
              value = parent.get(varName);
            }
          } catch (InvalidObjectPathException e) {
            throw new RuntimeException(e);
          }
        }
        if (value == null) {
          GeneralXynaObject child;
          try {
            child = constructor.newInstance();
          } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Could not instantiate member " + varName, e);
          }
          if (!isList) {
            try {
              if (parent != null) {
                parent.set(varName, child);
              }
            } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
              throw new RuntimeException(e);
            }
          }
          return child;
        } else {
          if (!isList) {
            return value;
          } else {
            return null;
          }
        }
      }
      
      
      public boolean isList() {
        return isList;
      }

    }


    private final Constructor<? extends GeneralXynaObject> constructor;
    private final Class<?> clazz;
    private final List<MemberCreation> memberCreation = new ArrayList<>(1);


    public XynaObjectCreator(GeneralXynaObject templ) {
      try {
        constructor = templ.getClass().getConstructor();
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
      clazz = templ.getClass();
    }


    public void addMemberCreation(XmomField field) {
      field = field.getParent();
      List<AbstractXmomField> fields = new ArrayList<>(3);
      while (!(field instanceof NoParentXmomField)) {
        if (field instanceof AbstractXmomField) {
          fields.add((AbstractXmomField) field);
        } else {
          throw new RuntimeException("unexpected field type: " + field.getClass().getName());
        }

        field = field.getParent();
      }
      List<MemberCreation> currentMembers = memberCreation;
      for (int i = fields.size() - 1; i >= 0; i--) {
        AbstractXmomField axf = fields.get(i);
        //hier wäre eine map performanter, aber es ist nicht zu erwarten, dass man so viele verschiedene members hat, dass das eine rolle spielt
        //check, ob member bereits erzeugt wird
        boolean found = false;
        for (MemberCreation mc : currentMembers) {
          if (mc.varName.equals(axf.getVarName())) {
            found = true;
            currentMembers = mc.members;
            if (i > 0 && currentMembers == null) {
              currentMembers = new ArrayList<>(3);
              mc.members = currentMembers;
            }
            break;
          }
        }
        if (!found) {
          try {
            MemberCreation mc =
                new MemberCreation((Class<? extends GeneralXynaObject>) clazz.getClassLoader().loadClass(axf.getFqClassName()),
                                   axf.getVarName(), axf instanceof XmomListField);
            currentMembers.add(mc);
            if (i > 0) {
              mc.members = new ArrayList<>(3);
              currentMembers = mc.members;
            }
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }


    //erzeugt zielobjekt mit leer initialisierten komplexwertigen members, falls diese für die befüllung durch csv-spalten benötigt werden
    public GeneralXynaObject create(List<PathPart> meta) {
      try {
        GeneralXynaObject gxo = constructor.newInstance();
        createNewSubs(gxo, meta);
        return gxo;
      } catch (Exception e) {
        throw new RuntimeException("Could not create instance of xyna object of type " + clazz.getCanonicalName(), e);
      }
    }
    
    public void createNewSubs(GeneralXynaObject obj, List<PathPart> pathParts) {
      createMembers(obj, memberCreation, pathParts, 0);
    }


    private void createMembers(GeneralXynaObject parent, List<MemberCreation> mc, List<PathPart> meta, int metaIndex) {
      PathPart currentPath = null;
      if (metaIndex  < meta.size()) {
        currentPath = meta.get(metaIndex);  
      }
      for (MemberCreation m : mc) {
        if (m.isList() && 
            currentPath != null &&
            currentPath.index >= 0) {
          if (m.varName.equals(currentPath.varName)) {
            try {
              List list = (List) parent.get(m.varName);
              if (list == null) {
                list = new ArrayList<>();
                parent.set(m.varName, list);
              }
              while (list.size() <= currentPath.index) {
                list.add(null);
              }
              Object obj = list.get(currentPath.index);
              if (obj == null) {
                obj = m.createMember(parent);
                list.set(currentPath.index, obj);
              }
              if (meta.size() > 0 &&
                  m.members != null) {
                createMembers((GeneralXynaObject) obj, m.members, meta, metaIndex+1);
              }
            } catch (InvalidObjectPathException | XDEV_PARAMETER_NAME_NOT_FOUND e) {
              throw new RuntimeException(e);
            }
          }
        } else {
          try {
            GeneralXynaObject mgxo = (GeneralXynaObject) m.createMember(parent);
            if (m.members != null) {
              createMembers(mgxo, m.members, meta, metaIndex+1);
            }
          } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

  }

  private static class Mapper {

    private final String path;
    private final List<PathPart> pathParts;
    private final FieldType type;


    public Mapper(String path, FieldType type) {
      this.path = path;
      pathParts = parsePath(path, "notRelevant");
      this.type = type;
    }


    public void fill(GeneralXynaObject obj, String fieldValue) {
      try {
        XynaObject.set(obj, path, convertValue(fieldValue));
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND | InvalidObjectPathException e) {
        logger.trace("Error in fill on " + path);
        throw new RuntimeException(e);
      }
    }
    
    
    public void fill(GeneralXynaObject parent, String fieldValue, List<PathPart> meta) {
      logger.trace("fill " + parent.getClass().getName() + " with " + fieldValue + " in " + meta.toString());
      logger.trace("local path: " + path);
      GeneralXynaObject current = parent;
      int followedIndex;
      for (followedIndex = 0; followedIndex < meta.size(); followedIndex++) {
        PathPart subPart = meta.get(followedIndex);
        Object obj = null;
        try {
          obj = current.get(subPart.getVarName());
        } catch (InvalidObjectPathException e) {
          throw new RuntimeException(e);
        }
        if (subPart.isListPart()) {
          if (subPart.isPrimitive()) {
            // primitive list
            List list = (List)obj;
            if (list == null) {
              list = new ArrayList();
              try {
                current.set(subPart.getVarName(), list);
              } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
                throw new RuntimeException(e);
              }
            }
            while (list.size() <= subPart.index) {
              list.add(null);
            }
            list.set(subPart.index, convertValue(fieldValue));
            return;
          } else {
            current = (GeneralXynaObject) ((List)obj).get(subPart.index);
            logger.trace("new current " + current.getClass().getName());
          }
        } else {
          current = (GeneralXynaObject) obj;
          logger.trace("new current " + current.getClass().getName());
        }
      }
      List<PathPart> restParts = pathParts.subList(followedIndex, pathParts.size());
      String restPath = restParts.stream().map(PathPart::getVarName).collect(Collectors.joining("."));
      try {
        logger.trace("final part set on " + current.getClass().getName() + " with " +restPath);
        XynaObject.set(current, restPath, convertValue(fieldValue));
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND | InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
    }


    private Object convertValue(String fieldValue) {
      switch (type) {
        case BOOLEAN :
          return Boolean.parseBoolean(fieldValue);
        case DOUBLE :
          return Double.parseDouble(fieldValue);
        case FLOAT :
          return Float.parseFloat(fieldValue);
        case INT :
          return Integer.parseInt(fieldValue);
        case LONG :
          return Long.parseLong(fieldValue);
        case STRING :
          return fieldValue;
        default :
          throw new RuntimeException("missing support for type " + type);
      }
    }

    public String getPath() {
      return path;
    }
  }
  
  
  
  private static class PathPart {
    
    private String varName;
    private int index = -1;
    private boolean isPrimitive;
    
    PathPart(String varName) {
      this(varName, -1, false);
    }
    
    PathPart(String varName, int i, boolean isPrimitive) {
      this.varName = varName;
      this.index = i;
      this.isPrimitive = isPrimitive;
    }
    
    public String getVarName() {
      return varName;
    }
    
    boolean isListPart() {
      return index >= 0;
    }

    public boolean isPrimitive() {
      return isPrimitive;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(varName);
      if (index >= 0) {
        sb.append('[')
          .append(index)
          .append(']');  
      }
      return sb.toString();
    }
  }


  private Header getHeader(String header) {
    if (header == null) {
      header = CSVWriter.DEFAULT_HEADER;
    } else {
      header = header.toLowerCase();
    }
    if ("none".equals(header)) {
      throw new RuntimeException("Header may not be <none>. It is needed to map columns to datatype members.");
    } else if ("labels".equals(header)) {
      return Header.LABELS;
    } else if ("paths".equals(header)) {
      return Header.PATHS;
    } else {
      throw new RuntimeException("Unexpected header value: " + header);
    }
  }

}
