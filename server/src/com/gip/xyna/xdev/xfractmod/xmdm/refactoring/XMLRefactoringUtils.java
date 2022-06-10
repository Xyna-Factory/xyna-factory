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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.MDMUpdate;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class XMLRefactoringUtils {

  //TODO dran denken: das ändert nur xml im mdm ordner, nicht aber zb im orderarchive alte audit-daten. 
  //es wird auch kein redeploy oder sowas durchgeführt. dazu müsste man in services bei schnittstellenänderungen
  //die codesnippets löschen.

  private static final Logger logger = CentralFactoryLogging.getLogger(XMLRefactoringUtils.class);


  protected static enum DocumentOrderType {
    DELETE, MOVE, SAVE, NOTHING;
    
    public static DocumentOrderType getMostRelevantType(DocumentOrderType one, DocumentOrderType other) {
      switch (one) {
        case NOTHING :
          return other;
        case SAVE :
          switch (other) {
            case DELETE :
            case MOVE :
              return other;
            default :
              return one;
          }
        case DELETE :
        case MOVE :
          return one;
        default :
          throw new IllegalArgumentException("WTF!");
      }
    }
  }

  protected static class DocumentOrder {

    final DocumentOrderType type;
    final XMOMObjectRefactoringResult result;

    protected DocumentOrder(DocumentOrderType type, XMOMObjectRefactoringResult result) {
      this.type = type;
      this.result = result;
    }

    public static DocumentOrder getNothing() {
      return new DocumentOrder(DocumentOrderType.NOTHING, null);
    }
    
    public DocumentOrder merge(DocumentOrder otherOrder) {
      if (type == DocumentOrderType.NOTHING) {
        return otherOrder;
      }
      if (otherOrder.type != DocumentOrderType.NOTHING) {
        if (otherOrder.result.fqXmlNameNew != null) {
          result.fqXmlNameNew = otherOrder.result.fqXmlNameNew;
        }
        result.unmodifiedLabels.addAll(otherOrder.result.unmodifiedLabels);
      }
      return new DocumentOrder(DocumentOrderType.getMostRelevantType(type, otherOrder.type), result);
    }
    
    @Override
    public String toString() {
      return "DocumentOrderType " + type.toString() + " " + (result == null ? "" : String.valueOf(result.fqXmlNameOld)) + " -> " + (result == null ? "" : String.valueOf(result.fqXmlNameNew));
    }

  }
  
  
  protected static class FinalizationDocumentOrder extends DocumentOrder {

    final File file;
    final Document doc;
    
    protected FinalizationDocumentOrder(File file, Document doc, DocumentOrderType type, XMOMObjectRefactoringResult result) {
      super(type, result);
      this.file = file;
      this.doc = doc;
    }
    
    public static FinalizationDocumentOrder getNothing() {
      return new FinalizationDocumentOrder(null, null, DocumentOrderType.NOTHING, null);
    }
    
  }

  
  protected static interface Work {

    /**
     * dokument bearbeiten. kann geändert werden.
     * @param doc
     * @return was soll mit der datei geschehen? löschen, verschieben (wohin?), speichern
     */
    public DocumentOrder work(Document doc);
    
    public List<FinalizationDocumentOrder> finalizeWork();

  }
  
  
  protected static class DecomposedWork implements Work {
    
    private final Map<RefactoringTargetType, List<WorkUnit>> workUnitsByType;
    private final List<WorkFinalizer> finalizers;
    
    
    public DecomposedWork() {
      this.workUnitsByType = new EnumMap<RefactoringTargetType, List<WorkUnit>>(RefactoringTargetType.class);
      this.finalizers = new ArrayList<WorkFinalizer>();
    }
    
    
    public DecomposedWork(BaseWorkCollection<? extends RefactoringElement> workCollection) {
      this();
      for (WorkUnit unit : workCollection.getWorkUnits()) {
        addUnit(unit);
      }
      for (WorkFinalizer finalizer : workCollection.getWorkFinalizers()) {
        addFinalizer(finalizer);
      }
    }
    
    
    
    public DocumentOrder work(Document doc) {
      Element root = doc.getDocumentElement();
      XMOMType type = XMOMType.getXMOMTypeByRootTag(root.getTagName());
      if (type != null) {
        Element typeInformationCarrier;
        if (type == XMOMType.EXCEPTION) {
          typeInformationCarrier = XMLUtils.getChildElementByName(root, GenerationBase.EL.EXCEPTIONTYPE);
        } else {
          typeInformationCarrier = root;
        }
        String typePath = typeInformationCarrier.getAttribute(GenerationBase.ATT.TYPEPATH);
        String typeName = typeInformationCarrier.getAttribute(GenerationBase.ATT.TYPENAME);
        String currentFqXmlName =  typePath + "." + typeName;
        RefactoringTargetType targetType = RefactoringTargetType.fromXMOMType(type);
        if (workUnitsByType.containsKey(targetType)) {
          List<WorkUnit> workUnits = workUnitsByType.get(targetType);
          DocumentOrder result = null;
          for (WorkUnit unit : workUnits) {
            DocumentOrder unitResult = unit.work(targetType, doc, currentFqXmlName, typeInformationCarrier);
            if (result == null) {
              result = unitResult;
            } else {
              result = result.merge(unitResult);
            }
          }
          return result;
        } else {
          return DocumentOrder.getNothing();  
        }
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
    
    public void addUnit(WorkUnit workUnit) {
      for (RefactoringTargetType type : workUnit.getRelevantTargetTypes()) {
        List<WorkUnit> workUnits = workUnitsByType.get(type);
        if (workUnits == null) {
          workUnits = new ArrayList<WorkUnit>();
        }
        workUnits.add(workUnit);
        workUnitsByType.put(type, workUnits);
      }
    }
    
    
    public void addFinalizer(WorkFinalizer finalizer) {
      finalizers.add(finalizer);
    }


    public List<FinalizationDocumentOrder> finalizeWork() {
      List<FinalizationDocumentOrder> finalOrders = new ArrayList<FinalizationDocumentOrder>();
      for (WorkFinalizer finalizer : finalizers) {
        finalOrders.addAll(finalizer.finalizeWork());
      }
      return finalOrders;
    }
    

    
  }
  
  
  public static interface WorkFinalizer {
    
    public List<FinalizationDocumentOrder> finalizeWork();
    
  }
  
  
  protected static abstract class WorkUnit {
    
    protected final Set<RefactoringTargetType> relevantTargetTypes;
    
    protected WorkUnit(RefactoringTargetType... relevantTargetTypes) {
      this.relevantTargetTypes = new HashSet<RefactoringTargetType>();
      for (RefactoringTargetType refactoringTargetType : relevantTargetTypes) {
        this.relevantTargetTypes.add(refactoringTargetType);
      }
    }
    
    public Set<RefactoringTargetType> getRelevantTargetTypes() {
      return relevantTargetTypes;
    }
    
    public abstract DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier); 
    
  }
  
  
  
  
  public enum XMLElementType {
    SERVICE_REFERENCE(GenerationBase.EL.SERVICEREFERENCE),
    OPERATION(GenerationBase.EL.OPERATION),
    FUNCTION(GenerationBase.EL.FUNCTION),
    WORKFLOW(GenerationBase.EL.SERVICE),
    DATA(GenerationBase.EL.DATA),
    CALL(GenerationBase.EL.WORKFLOW_CALL),
    EXCEPTION(GenerationBase.EL.EXCEPTION);
    
    private final String tagName;
    
    private XMLElementType(String tagName) {
      this.tagName = tagName;
    }
    
    public static XMLElementType fromTagName(String tagName) {
      for (XMLElementType type : values()) {
        if (type.tagName.equals(tagName)) {
          return type;
        }
      }
      throw new IllegalArgumentException(tagName);
    }
    
  }
  
  public static class LabelInformation implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String label;
    private final int xmlElementId;
    private final XMLElementType elementType;
    
    public LabelInformation(String label, int xmlElementId, XMLElementType type) {
      this.label = label;
      this.xmlElementId = xmlElementId;
      this.elementType = type;
    }
    
    public String getLabelValue() {
      return label;
    }
    
    public int getXmlElementId() {
      return xmlElementId;
    }
    
    public XMLElementType getType() {
      return elementType;
    }
  }
  
  public static class XMOMObjectRefactoringResult {
    private String fqXmlNameOld;
    protected String fqXmlNameNew; //null falls nicht geändert
    private long revision;
    protected final List<LabelInformation> unmodifiedLabels = new ArrayList<LabelInformation>();
    protected final List<String> deactivatedOperationSnippets = new ArrayList<String>();
    private final RefactoringTargetType type;
    
    public XMOMObjectRefactoringResult(String fqXmlNameOld, RefactoringTargetType type) {
      this.fqXmlNameOld = fqXmlNameOld;
      this.type = type;
    }
    
    public String getFqXmlNameOld() {
      return fqXmlNameOld;
    }
    
    public String getFqXmlNameNew() {
      if (fqXmlNameNew == null) {
        return fqXmlNameOld;
      }
      return fqXmlNameNew;
    }
    
    public RefactoringTargetType getType() {
      return type;
    }
    
    public LabelInformation[] getLabelInformation() {
      return unmodifiedLabels.toArray(new LabelInformation[unmodifiedLabels.size()]);
    }
    
    public long getRevision() {
      return revision;
    }
    
    public void setRevision(long revision) {
      this.revision = revision;
    }
    
    public XMOMObjectRefactoringResult getInverse() {
      XMOMObjectRefactoringResult res = new XMOMObjectRefactoringResult(fqXmlNameNew, type);
      res.fqXmlNameNew = fqXmlNameOld;
      res.revision = revision;
      return res;
    }
  }

  public static class Result {

    private final Map<Long, List<String>> deleted = new HashMap<>();
    private final List<XMOMObjectRefactoringResult> moved = new ArrayList<>();
    private final List<XMOMObjectRefactoringResult> changed = new ArrayList<>();


    private void deleted(Document doc, long revision) {
      Element root = doc.getDocumentElement();
      String path = root.getAttribute(GenerationBase.ATT.TYPEPATH);
      String name = root.getAttribute(GenerationBase.ATT.TYPENAME);
      getDeleted(revision).add(path + "." + name);
    }


    private List<String> getDeleted(long revision) {
      List<String> d = deleted.get(revision);
      if (d == null) {
        d = new ArrayList<String>();
        deleted.put(revision, d);
      }
      return d;
    }


    private void moved(XMOMObjectRefactoringResult r, long revision) {
      r.setRevision(revision);
      moved.add(r);
    }


    protected void changed(XMOMObjectRefactoringResult r, long revision) {
      r.setRevision(revision);
      changed.add(r);
    }


    public Map<Long, List<String>> deleted() {
      return deleted;
    }


    public XMOMObjectRefactoringResult[] moved() {
      return moved.toArray(new XMOMObjectRefactoringResult[moved.size()]);
    }


    public XMOMObjectRefactoringResult[] changed() {
      return changed.toArray(new XMOMObjectRefactoringResult[changed.size()]);
    }


    public void merge(Result r) {
      for (Entry<Long, List<String>> e : r.deleted.entrySet()) {
        getDeleted(e.getKey()).addAll(e.getValue());
      }
      changed.addAll(r.changed);
      moved.addAll(r.moved);
    }

  }
  
  public final static XynaPropertyBoolean dryRunRefactorings = new XynaPropertyBoolean("bg.test.refactoring.dryrun", false); 

  public static class Configuration {

    final boolean refactorInDeploymentDir;
    private static final AtomicLong cnt = new AtomicLong(System.currentTimeMillis());
    private static final String BACKUP_EXTENSION = ".rfb";
    private final long backupId;
    private final List<File> backups = new ArrayList<File>();
    private final List<File> newlyCreatedFiles = new ArrayList<File>();
    private final boolean exceptionIfTargetFileExists;
    final boolean exceptionIfSourceFileDoesntExist;
    private final boolean executeDocumentOrders;
    private final EnumMap<DeploymentLocation, GenerationBaseCache> deploymentItemCache =
        new EnumMap<DeploymentLocation, GenerationBaseCache>(DeploymentLocation.class);

    
    public Configuration(boolean refactorInDeploymentDir, boolean exceptionIfTargetFileExists, boolean exceptionIfSourceFileDoesntExist) {
      this(refactorInDeploymentDir, exceptionIfTargetFileExists, exceptionIfSourceFileDoesntExist, true);
    }


    public Configuration(boolean refactorInDeploymentDir, boolean exceptionIfTargetFileExists,
                         boolean exceptionIfSourceFileDoesntExist, boolean executeDocumentOrders) {
      this.refactorInDeploymentDir = refactorInDeploymentDir;
      this.exceptionIfTargetFileExists = exceptionIfTargetFileExists;
      this.exceptionIfSourceFileDoesntExist = exceptionIfSourceFileDoesntExist;
      if (dryRunRefactorings.get()) {
        this.executeDocumentOrders = false;
      } else {
        this.executeDocumentOrders = executeDocumentOrders;
      }
      backupId = cnt.incrementAndGet();
    }

    
    public boolean deactivateRefactoredOperationCodeSnippets() {
      return refactorInDeploymentDir;
    }
    
    
    protected EnumMap<DeploymentLocation, GenerationBaseCache> getDeploymentItemCache() {
      return deploymentItemCache;
    }


    private File getBaseDir(Long revision) {
      if (refactorInDeploymentDir) {
        String deployedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision);
        return new File(deployedMdmDir);
      }
      String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
      return new File(savedMdmDir);
    }


    protected File getFileLocation(String fqXmlName, Long revision) {
      if (refactorInDeploymentDir) {
        String fileLocationOfXmlNameForDeployment = GenerationBase.getFileLocationOfXmlNameForDeployment(fqXmlName, revision);
        return new File(fileLocationOfXmlNameForDeployment + ".xml");
      }
      String fileLocationOfXmlNameForSaving = GenerationBase.getFileLocationOfXmlNameForSaving(fqXmlName, revision);
      return new File(fileLocationOfXmlNameForSaving + ".xml");
    }


    protected void backup(File f) {
      File backup = new File(f.getAbsolutePath() + BACKUP_EXTENSION + backupId);
      try {
        FileUtils.copyFile(f, backup);
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      }
      backups.add(backup);
    }


    private void newFile(File f) {
      newlyCreatedFiles.add(f);
    }


    public void rollbackFileChanges() {
      for (File f : newlyCreatedFiles) {
        FileUtils.deleteFileWithRetries(f);
      }
      List<Ex_FileAccessException> exceptions = new ArrayList<Ex_FileAccessException>();
      for (File f : backups) {
        String name = f.getAbsolutePath();
        try {
          FileUtils.moveFile(f, new File(name.substring(0, name.lastIndexOf(BACKUP_EXTENSION))));
        } catch (Ex_FileAccessException e) {
          exceptions.add(e);
        }
      }
      if (exceptions.size() > 0) {
        int i = 0;
        for (Ex_FileAccessException e : exceptions) {
          logger.error(++i + "/ " + exceptions.size() + ". exception restoring backupped file", e);
        }
        throw new RuntimeException("could not restore " + exceptions.size()
            + " backupped files. see log for further details", exceptions.get(0));
      }
    }


    public void cleanupBackuppedFiles() {
      for (File f : backups) {
        FileUtils.deleteFileWithRetries(f);
      }
    }
    
    private enum RefactoringScope {
      
      LOCAL_NO_ERROR, GLOBAL; //TODO LOCAL_VALIDATE, WORKSPACES_ONLY
      
    }

    private static final XynaPropertyEnum<RefactoringScope> refactoringScope = new XynaPropertyEnum<>("xdev.xfractmod.xmdm.refactoring.scope", RefactoringScope.class, RefactoringScope.LOCAL_NO_ERROR);

    public boolean refactorInParentRuntimeContexts() {
      return refactoringScope.get() == RefactoringScope.GLOBAL;
    }

  }
  
  protected static final String PERSISTENCE_PATH = "xnwh.persistence";
  protected static final String PERSISTENCE_SELECTIONMASK_NAME = "SelectionMask";
  protected static final String PERSISTENCE_SELECTIONMASK_VARNAME = "rootType";


  
  protected static String refactorXFLTypes(String xfl, Element root, String fqXmlNameOld, String fqXmlNameNew) {
    if (xfl == null) {
      return xfl;
    }
    if (!xfl.contains(Functions.TYPE_OF_FUNCTION_NAME) && 
        !xfl.contains("#" + Functions.CAST_FUNCTION_NAME + "(\"") && 
        !xfl.contains(Functions.NEW_FUNCTION_NAME + "(\"")) {
      return xfl;
    }
    if (!xfl.contains(fqXmlNameOld)) {
      return xfl;
    }
    try {
      ModelledExpression me = ModelledExpression.parse(new VariableContextIdentification() {

        public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException,
            XPRC_InvalidVariableMemberNameException {
          return null;
        }

        public TypeInfo getTypeInfo(String originalXmlName) {
          return null;
        }
        
        public Long getRevision() {
          return null;
        }

        public VariableInfo createVariableInfo(TypeInfo resultType) {
          return null;
        }

      }, xfl);
      FunctionTypeReferenceRefacoringVisitor icv = new FunctionTypeReferenceRefacoringVisitor(fqXmlNameOld, fqXmlNameNew);
      me.visitTargetExpression(icv);
      String newTargetExpression = icv.getXFLExpression();
      if (me.getFoundAssign() != null) {
        icv = new FunctionTypeReferenceRefacoringVisitor(fqXmlNameOld, fqXmlNameNew);
        me.visitSourceExpression(icv);
        newTargetExpression = newTargetExpression + me.getFoundAssign().toXFL() + icv.getXFLExpression();
      }
      return newTargetExpression;
    } catch (XPRC_ParsingModelledExpressionException e) {
      String referencePath = root.getAttribute(GenerationBase.ATT.TYPEPATH);
      String referenceName = root.getAttribute(GenerationBase.ATT.TYPENAME);
      logger.warn("could not replace type " + fqXmlNameOld + " in expression \"" + xfl + "\" in workflow " + referencePath + "."
          + referenceName + ".", e);
      return xfl;
    }
  }
  
  
  private static class FunctionTypeReferenceRefacoringVisitor extends IdentityCreationVisitor {

    private final Stack<FunctionExpression> functionStack = new Stack<FunctionExpression>();
    private final String fqXmlNameOld;
    private final String fqXmlNameNew;
    
    public FunctionTypeReferenceRefacoringVisitor(String fqXmlNameOld, String fqXmlNameNew) {
      this.fqXmlNameNew = fqXmlNameNew;
      this.fqXmlNameOld = fqXmlNameOld;
    }
    
    @Override
    public void functionEnds(FunctionExpression fe) {
      super.functionEnds(fe);
      functionStack.pop();
    }

    @Override
    public void functionStarts(FunctionExpression fe) {
      super.functionStarts(fe);
      functionStack.push(fe);
    }

    @Override
    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      super.functionSubExpressionEnds(fe, parameterCnt);
    }

    @Override
    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      super.functionSubExpressionStarts(fe, parameterCnt);
    }

    @Override
    public void literalExpression(LiteralExpression expression) {
      if (skipNextLiteral) {
        skipNextLiteral = false;
      } else {
        if (!functionStack.isEmpty() && 
            isAffectedFunction(functionStack.peek()) &&
            expression.getValue().equals(fqXmlNameOld)) {
          sb.append("\"").append(fqXmlNameNew).append("\"");
        } else {
          super.literalExpression(expression);
        }
      }
    }
    
    
    private boolean isAffectedFunction(FunctionExpression fe) {
      String functionName = fe.getFunction().getName();
      return functionName.equals(Functions.TYPE_OF_FUNCTION_NAME) ||
             functionName.equals(Functions.CAST_FUNCTION_NAME) ||
             functionName.equals(Functions.NEW_FUNCTION_NAME);
    }
    
  }
  
  

  /*
   * TODO:
   * We already get locks based on DependencyRegister & XMOM-DB 
   * if we restrict our workExecution to only those registered dependencies we'd have a smaller set to work on
   * furthermore the processing is 'easy' to run concurrently (if our workunits support it (those with finalizers currently might not))
   */
  private static void executeWorkOnXMLsRecursively(File dir, Work w, Configuration config,
                                                   Map<String, FinalizationDocumentOrder> orders,
                                                   List<String> fileLocations)
      throws Ex_FileAccessException, XPRC_XmlParsingException {

    File[] files;
    if (fileLocations != null) {
      List<File> filesList = new ArrayList<File>();
      for (String fileLocation: fileLocations) {
        File f = new File(fileLocation);
        if (f.exists()) {
          filesList.add(f);
        }
      }
      files = filesList.toArray(new File[filesList.size()]);
    } else {
      files = dir.listFiles(MDMUpdate.xmlFilter);
    }

    Arrays.sort(files); //File ist comparable
    for (File f : files) {
      if (f.isDirectory()) {
        // should only happen if no file locations are passed into the method
        executeWorkOnXMLsRecursively(f, w, config, orders, null);
      } else {
        Document doc = XMLUtils.parse(f.getAbsolutePath());
        DocumentOrder docOrder = w.work(doc);
        if (docOrder.type != DocumentOrderType.NOTHING) {
          orders.put(f.getAbsolutePath(), new FinalizationDocumentOrder(f, doc, docOrder.type, docOrder.result));
        }
      }
    }

  }
  
  
  private static void executeDocumentOrder(FinalizationDocumentOrder fdo, Result r, Configuration config, Long revision) throws Ex_FileAccessException {
    switch (fdo.type) {
      case DELETE :
        if (config.executeDocumentOrders) {
          config.backup(fdo.file);
          fdo.file.delete();
        }
        r.deleted(fdo.doc, revision);
        break;
      case MOVE :
        if (config.executeDocumentOrders) {
          config.backup(fdo.file);
          File fNew = config.getFileLocation(fdo.result.fqXmlNameNew, revision);
          config.newFile(fNew);
          XMLUtils.saveDom(fNew, fdo.doc);
          try {
            if (!fNew.getCanonicalPath().equals(fdo.file.getCanonicalPath())) {
              fdo.file.delete();
            }
          } catch (IOException e) {
            throw new Ex_FileAccessException(fdo.file.getAbsolutePath(), e);
          }
        }
        r.moved(fdo.result, revision);
        break;
      case NOTHING :
        break;
      case SAVE :
        if (config.executeDocumentOrders) {
          config.backup(fdo.file);
          XMLUtils.saveDom(fdo.file, fdo.doc);
          if (fdo.result != null && fdo.result.type == RefactoringTargetType.FILTER) {
            //FIXME das ist nicht besonders schön. und der filename ist auch noch "nur" der simpleclassname. das passt alles nicht besonders schön
            fdo.result.fqXmlNameOld = fdo.file.getName().substring(0, fdo.file.getName().lastIndexOf(".xml"));              
          }
        }
        r.changed(fdo.result, revision);
        break;
      default :
        throw new RuntimeException("unsupported type: " + fdo.type);
    }
  }



  protected static LabelInformation refactorLabel(Element element, String oldTargetLabel, String newTargetLabel, XMLElementType type) {
    String oldLabel = element.getAttribute(GenerationBase.ATT.LABEL);
    if (oldLabel == null || oldLabel.length() == 0 || oldLabel.equalsIgnoreCase(oldTargetLabel) || oldLabel.replaceAll("\\s+", "").equalsIgnoreCase(oldTargetLabel)) {
      //altes label, fqName als label oder kein label vorhanden -> label updaten
      element.setAttribute(GenerationBase.ATT.LABEL, newTargetLabel);
      return null;
    } else {
      int id;
      try {
        id = Integer.valueOf(element.getAttribute(GenerationBase.ATT.ID));
      } catch (NumberFormatException e) {
        id = -1; //falls das objekt keine id hat
      }
      return new LabelInformation(oldLabel, id, type);
    }
  }


  protected static Result executeWork(Work xmlRefactoring, Configuration config, Long revision,
                                      RefactoringContext context) throws Ex_FileAccessException,
      XPRC_XmlParsingException {
    File dir = config.getBaseDir(revision);
    Result r = new Result();
    Map<String, FinalizationDocumentOrder> orders = new HashMap<String, FinalizationDocumentOrder>();
    List<String> fileLocations = null;
    if (context != null) {
      fileLocations = new ArrayList<String>();
      if (config.refactorInDeploymentDir) {
        fileLocations.addAll(calcluateDeployedFilenamesForDependencies(context, revision));
      } else {
         fileLocations.addAll(calcluateSavedFilenamesForDependencies(context, revision));
      }
    }
    executeWorkOnXMLsRecursively(dir, xmlRefactoring, config, orders, fileLocations);
    if (config.refactorInDeploymentDir) {
      //Additional Dependencies von Filtern beachten:
      dir = new File(RevisionManagement.getPathForRevision(PathType.FILTER, revision));
      //Prüfen ob die Revision ein filter Verzeichnis hat
      if(dir.exists()) {
        executeWorkOnXMLsRecursively(dir, xmlRefactoring, config, orders, null);
      }
    }

    List<FinalizationDocumentOrder> finalOrders = xmlRefactoring.finalizeWork();
    for (FinalizationDocumentOrder finalOrder : finalOrders) {
      if (finalOrder.type != DocumentOrderType.NOTHING) {
        // this might override orders from normal processing 
        orders.put(finalOrder.file.getAbsolutePath(), finalOrder);
      }
    }
    for (FinalizationDocumentOrder fdo : orders.values()) {
      executeDocumentOrder(fdo, r, config, revision);
    }

    return r;
  }


  private static Collection<String> calcluateSavedFilenamesForDependencies(RefactoringContext context, Long revision) {

    Set<String> result = new HashSet<String>();
    Set<XMOMDatabaseSearchResultEntry> dependencies = new HashSet<>();

    if (context.isRefactorOperation()) {

      DeploymentItemStateManagementImpl deplStateMgmt = (DeploymentItemStateManagementImpl) XynaFactory.getInstance()
          .getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      Iterator<? extends RefactoringElement> iter = context.getRefactoringElements().iterator();
      while (iter.hasNext()) {
        RefactoringElement e = iter.next();
        result.add(GenerationBase.getFileLocationForSavingStaticHelper(e.fqXmlNameOld, revision) + ".xml");
        result.add(GenerationBase.getFileLocationForSavingStaticHelper(e.fqXmlNameNew, revision) + ".xml");
        DeploymentItemStateImpl item = (DeploymentItemStateImpl) deplStateMgmt.get(e.fqXmlNameOld, revision);
        Set<DeploymentItemState> callingSites = item.getInvocationSitesPerOperation(DeploymentLocation.SAVED,
            ((OperationRefactoringElement) e).operationNameOld);
        for (DeploymentItemState callingSite : callingSites) {
          result.add(GenerationBase.getFileLocationForSavingStaticHelper(callingSite.getName(), revision) + ".xml");
        }

      }
    } else {

      Iterator<? extends RefactoringElement> iter = context.getRefactoringElements().iterator();
      while (iter.hasNext()) {
        RefactoringElement e = iter.next();
        result.add(GenerationBase.getFileLocationForSavingStaticHelper(e.fqXmlNameOld, revision) + ".xml");
      }
      dependencies = context.getSavedDependencies();

      for (XMOMDatabaseSearchResultEntry d : dependencies) {
        String fqName;
        if (d.getType() == XMOMDatabaseType.OPERATION) {
          fqName = XMOMDatabase.getFqOriginalNameFromFqServiceOperationName(d.getFqName());
        } else {
          fqName = d.getFqName();
        }
        result.add(GenerationBase.getFileLocationForSavingStaticHelper(fqName, revision) + ".xml");
      }
    }
    return result;

  }


  private static Set<String> calcluateDeployedFilenamesForDependencies(RefactoringContext context, Long revision) {

    Set<String> result = new HashSet<String>();

    if (context.isRefactorOperation()) {

      DeploymentItemStateManagementImpl deplStateMgmt = (DeploymentItemStateManagementImpl) XynaFactory.getInstance()
          .getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      Iterator<? extends RefactoringElement> iter = context.getRefactoringElements().iterator();
      while (iter.hasNext()) {
        RefactoringElement e = iter.next();
        result.add(GenerationBase.getFileLocationForDeploymentStaticHelper(e.fqXmlNameOld, revision) + ".xml");
        result.add(GenerationBase.getFileLocationForDeploymentStaticHelper(e.fqXmlNameNew, revision) + ".xml");
        DeploymentItemStateImpl item = (DeploymentItemStateImpl) deplStateMgmt.get(e.fqXmlNameOld, revision);
        Set<DeploymentItemState> callingSites = item.getInvocationSitesPerOperation(DeploymentLocation.DEPLOYED,
            ((OperationRefactoringElement) e).operationNameOld);
        for (DeploymentItemState callingSite : callingSites) {
          result.add(GenerationBase.getFileLocationForDeploymentStaticHelper(callingSite.getName(), revision) + ".xml");
        }
      }
    } else {
      Iterator<? extends RefactoringElement> iter = context.getRefactoringElements().iterator();
      while (iter.hasNext()) {
        result
            .add(GenerationBase.getFileLocationForDeploymentStaticHelper(iter.next().fqXmlNameOld, revision) + ".xml");
      }

      Set<DependencyNode> dependencies = context.getDeployedDependencies();
      for (DependencyNode d : dependencies) {
        if (d.getType() == DependencySourceType.WORKFLOW || d.getType() == DependencySourceType.DATATYPE
            || d.getType() == DependencySourceType.XYNAEXCEPTION) {
          result.add(GenerationBase.getFileLocationForDeploymentStaticHelper(d.getUniqueName(), revision) + ".xml");
        }
      }
    }
    return result;
  }


  public static List<RefactoringElement> discoverPathRefactoringTargets(String oldPath, String newPath,
                                                                        Configuration config, Long revision)
      throws Ex_FileAccessException, XPRC_XmlParsingException {
    File dir = config.getBaseDir(revision);
    File subDir = new File(dir.getAbsolutePath() + Constants.fileSeparator + oldPath.replaceAll("\\.", Constants.fileSeparator));
    DiscoverPathRefactoringTargets dprt = new DiscoverPathRefactoringTargets(oldPath, newPath);
    DecomposedWork work = new DecomposedWork();
    work.addUnit(dprt);
    Map<String, FinalizationDocumentOrder> orders = new HashMap<String, FinalizationDocumentOrder>();
    executeWorkOnXMLsRecursively(subDir, work, config, orders, null);
    return dprt.getDiscoveredElements();
  }
  
  
  private static class DiscoverPathRefactoringTargets extends WorkUnit {

    private final String oldPath;
    private final String newPath;
    
    private final List<RefactoringElement> discoveredElements;
    
    protected DiscoverPathRefactoringTargets(String oldPath, String newPath) {
      super(RefactoringTargetType.DATATYPE, RefactoringTargetType.EXCEPTION, RefactoringTargetType.WORKFLOW, RefactoringTargetType.FORM);
      this.oldPath = oldPath;
      this.newPath = newPath;
      this.discoveredElements = new ArrayList<RefactoringElement>();
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      if (fqXmlName.startsWith(oldPath + ".")) {
        String fqXmlNameNew = newPath + fqXmlName.substring(oldPath.length());
        String lable = typeInformationCarrier.getAttribute(GenerationBase.ATT.LABEL);
        discoveredElements.add(new RefactoringElement(fqXmlName, fqXmlNameNew, lable, lable, type));
      }
      return DocumentOrder.getNothing();
    }
    
    
    public List<RefactoringElement> getDiscoveredElements() {
      return discoveredElements;
    }
    
  }


  public static List<OperationRefactoringElement> discoverOperationRenameRefactoringTargets(OperationRefactoringElement baseOperation,
                                                                                            Configuration config,
                                                                                            Long revision,
                                                                                            RefactoringContext context)
      throws Ex_FileAccessException, XPRC_XmlParsingException {
    DiscoverOperationRenameTargets dort = new DiscoverOperationRenameTargets(baseOperation);
    DecomposedWork work = new DecomposedWork();
    work.addUnit(dort);
    work.addFinalizer(dort);
    executeWork(work, config, revision, context);
    return dort.getDiscoveredElements();
  }
  
  
  private static class DiscoverOperationRenameTargets extends WorkUnit implements WorkFinalizer {
    
    private final OperationRefactoringElement baseOperation;
    //enthält auch operations, die nicht refactored werden müssen, weil sie in einer disjunkten typ-hierarchie enthalten sind
    private final List<OperationRefactoringElement> instanceOperationsWithSameName;
    //das sind die wirklich zu refactornden operations
    private final List<OperationRefactoringElement> operationsToBeRefactored;
    private final Map<String, String> superTypeRelations = new HashMap<String, String>();
    
    protected DiscoverOperationRenameTargets(OperationRefactoringElement baseOperation) {
      super(RefactoringTargetType.DATATYPE);
      this.baseOperation = baseOperation;
      this.instanceOperationsWithSameName = new ArrayList<OperationRefactoringElement>();
      this.operationsToBeRefactored = new ArrayList<OperationRefactoringElement>();
    }


    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      String superPath = typeInformationCarrier.getAttribute(GenerationBase.ATT.BASETYPEPATH);
      String superName = typeInformationCarrier.getAttribute(GenerationBase.ATT.BASETYPENAME);
      if (superPath == null || superPath.length() <= 0 || superName == null || superName.length() <= 0) {
      } else {
        superTypeRelations.put(fqXmlName, superPath + "." + superName);
      }

      List<Element> serviceElements = XMLUtils.getChildElementsByName(typeInformationCarrier, GenerationBase.EL.SERVICE);
      for (Element serviceElement : serviceElements) {
        String serviceName = serviceElement.getAttribute(GenerationBase.ATT.TYPENAME);
        List<Element> operationElements =
            XMLUtils.getChildElementsByName(serviceElement, GenerationBase.EL.OPERATION);
        for (Element operationElement : operationElements) {
          String operationName = operationElement.getAttribute(GenerationBase.ATT.OPERATION_NAME);
          if (operationName.equals(baseOperation.operationNameOld) && !XMLUtils.isTrue(operationElement, GenerationBase.ATT.ISSTATIC)) {
            String fqXmlNameOld = fqXmlName + "." + serviceName + "." + operationName;
            String fqXmlNameNew = fqXmlName + "." + serviceName + "." + baseOperation.operationNameNew;
            instanceOperationsWithSameName.add(new OperationRefactoringElement(fqXmlNameOld, fqXmlNameNew, baseOperation.labelNew,
                                                                               operationElement.getAttribute(GenerationBase.ATT.LABEL)));
          }
        }
      }
      return DocumentOrder.getNothing();
    }
    
    public List<FinalizationDocumentOrder> finalizeWork() {
      /*
       * es kann mehrere hierarchien von datentypen geben, die operations mit dem namen enthalten. beispiel:
       * 
       *    A                    H
       *    |                   / \
       *    B                  I   J
       *   /  \ 
       *  C     D
       *  |    / \
       *  E   F   G
       *  
       * angenommen, es wird auf die operation von C refactor() aufgerufen, und die operation existiert in B und ist in F auch überschrieben.
       * dann muss man erstmal die datentyp hierarchie in richtung A laufen, und dann alle davon abgeleiteten typen finden, die auch die operation enthalten
       * bzw sie überschreiben.
       * 
       * achtung: es kann auch durchaus sein, dass eine gleichbenamte operation sowohl in der teilhierarchie von C als auch in D existiert, ohne dass 
       * sie auch in A oder B definiert ist. dann muss natürlich beim refactoring von C NICHT auch die operation in D refactored werden!
       */
      
      operationsToBeRefactored.add(baseOperation);
      Set<String> discoveredSuperTypes = new HashSet<String>();
      discoveredSuperTypes.add(baseOperation.fqXmlNameOld);
      
      String relevantSuperType = baseOperation.fqXmlNameOld;
      
      //den typ suchen, der den operationnamen in der hierarchie zuerst definiert.
      //dazu an den super-typen entlang hangeln, bis man keinen mehr findet, der die operation definiert.
      String firstType = null;
      while (relevantSuperType != null) {
        //hat der aktuelle typ die operation mit dem namen?
        for (OperationRefactoringElement op : instanceOperationsWithSameName) {
          if (op.fqXmlNameOld.equals(relevantSuperType)) {
            firstType = op.fqXmlNameOld;
            break;
          }
        }
          
        //next
        relevantSuperType = superTypeRelations.get(relevantSuperType);        
      }
      
      if (firstType == null) {
        //kann nicht passieren, aber bessere fehlermeldung als NPE
        throw new RuntimeException("Did not find " + baseOperation.fqXmlNameOld);
      }
            
      
      // jetzt für alle kandidaten-operations überprüfen, ob sie von firstType abgeleitet sind
      for (OperationRefactoringElement op : instanceOperationsWithSameName) {
        //typ-hierarchie checken:
        String type = op.fqXmlNameOld;
        while (type != null) {
          if (type.equals(firstType)) {
            //ok, gefunden, dass es in der gleichen hierarchie ist
            operationsToBeRefactored.add(op);
            break;
          }
          
          //next
          type = superTypeRelations.get(type);
        }
      }
      instanceOperationsWithSameName.clear();
      superTypeRelations.clear();
      
      return Collections.singletonList(FinalizationDocumentOrder.getNothing());
    }
    
    public List<OperationRefactoringElement> getDiscoveredElements() {
      return operationsToBeRefactored;
    }
    
  }

  
}
