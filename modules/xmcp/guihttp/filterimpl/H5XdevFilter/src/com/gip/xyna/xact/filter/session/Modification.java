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
package com.gip.xyna.xact.filter.session;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.json.PersistJson;
import com.gip.xyna.xact.filter.session.Clipboard.ClipboardCopyDirection;
import com.gip.xyna.xact.filter.session.Dataflow.LinkstateIn;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.filter.session.exceptions.InvalidRevisionException;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.modify.operations.ChangeConstantOperation;
import com.gip.xyna.xact.filter.session.modify.operations.ChangeOperation;
import com.gip.xyna.xact.filter.session.modify.operations.ConvertOperation;
import com.gip.xyna.xact.filter.session.modify.operations.CopyOperation;
import com.gip.xyna.xact.filter.session.modify.operations.DeleteConstantOperation;
import com.gip.xyna.xact.filter.session.modify.operations.DeleteOperation;
import com.gip.xyna.xact.filter.session.modify.operations.InsertOperation;
import com.gip.xyna.xact.filter.session.modify.operations.ModifyOperationBase;
import com.gip.xyna.xact.filter.session.modify.operations.MoveOperation;
import com.gip.xyna.xact.filter.session.modify.operations.TemplateCallOperation;
import com.gip.xyna.xact.filter.session.modify.operations.TypeOperation;
import com.gip.xyna.xact.filter.session.save.Persistence;
import com.gip.xyna.xact.filter.xmom.workflows.json.DataflowJson;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

import xmcp.processmodeller.datatypes.ContainerArea;
import xmcp.processmodeller.datatypes.ContentArea;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.ModellingItem;
import xmcp.processmodeller.datatypes.Variable;
import xmcp.processmodeller.datatypes.response.UpdateXMOMItemResponse;
import xmcp.xact.modeller.Hint;

public class Modification implements HasXoRepresentation {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Modification.class);
  
  private static final String PROPERTY_KEY_PERFORMANCE_OPTIMIZATION = "xmcp.xfm.processmodeller.dev.performanceoptimization";
  private static final String VARIABLE_MEMBER_NAME = "name";
  
  private static final XynaPropertyBoolean PERFORMANCE_OPTIMIZATION_ENABLED = new XynaPropertyBoolean(PROPERTY_KEY_PERFORMANCE_OPTIMIZATION, true)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die Performance-Optimierung eingeschaltet ist oder nicht.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the performance optimization is enabled or not.");

  
  private GenerationBaseObject gbo;
  private String focusCandidateId;
  private SessionBasedData session;
  private Clipboard clipboard;
  private List<Hint> hints;
  
  public Modification(SessionBasedData session, GenerationBaseObject gbo) {
    this(session, gbo, null);
  }

  public Modification(SessionBasedData session, GenerationBaseObject gbo, Clipboard clipboard) {
    this.session = session;
    this.gbo = gbo;
    this.clipboard = clipboard;
    this.hints = new ArrayList<Hint>();
  }
  
  public void setObject(GenerationBaseObject gbo) {
    this.gbo = gbo;
  }
  
  public GenerationBaseObject getObject() {
    return gbo;
  }


  @Override
  public GeneralXynaObject getXoRepresentation() {
    UpdateXMOMItemResponse response = new UpdateXMOMItemResponse();

    response.setRevision(gbo.getRevision());
    response.setFocusId(focusCandidateId);
    response.setDeploymentState(gbo.getDeploymentState());
    response.setSaveState(gbo.getSaveState());
    response.setModified(gbo.hasBeenModified());

    Item oldXoRepresentation = gbo.getLastXoRepresentation();
    Item newXoRepresentation = gbo.createXoRepresentation();
    if (PERFORMANCE_OPTIMIZATION_ENABLED.get() && gbo.getType() == XMOMType.WORKFLOW && oldXoRepresentation != null) {
      List<Item> itemsToUpdate = new ArrayList<>();
      try {
        collectItemsToUpdate(oldXoRepresentation, newXoRepresentation, itemsToUpdate);
        response.setUpdates(itemsToUpdate);
      } catch (Exception e) {
        logger.warn("Performance optimization: An error occured while trying to determine items for partial update. Falling back to updating whole document.", e);
        response.setUpdates(new ArrayList<>());
        response.addToUpdates(newXoRepresentation);
      }
    } else {
      response.addToUpdates(newXoRepresentation);
    }

    if (clipboard != null) {
      hints.addAll(clipboard.getHints());
    }

    response.setHints(hints);


    gbo.setLastXoRepresentation(newXoRepresentation);

    return response;
  }

  @SuppressWarnings("rawtypes")
  private void collectItemsToUpdate(XynaObject oldXoRepresentation, XynaObject newXoRepresentation, List<Item> itemsToUpdate) {
    if ( (newXoRepresentation instanceof ModellingItem) && !isEqual(oldXoRepresentation, newXoRepresentation) ) {
      itemsToUpdate.add((ModellingItem)newXoRepresentation);
      return;
    }

    for (String varName : newXoRepresentation.getVariableNames()) {
      try {
        Object memberNew = newXoRepresentation.get(varName);
        Object memberOld = null;
        if (oldXoRepresentation != null && oldXoRepresentation.getVariableNames().contains(varName)) { 
          memberOld =  oldXoRepresentation.get(varName);
        }

        if (memberNew instanceof XynaObject) {
          if (memberOld instanceof XynaObject) {
            collectItemsToUpdate((XynaObject)memberOld, (XynaObject)memberNew, itemsToUpdate);
          } else {
            collectItemsToUpdate(null, (XynaObject)memberNew, itemsToUpdate);
          }
        } else if (memberNew instanceof List) {
          List newListEntries = (List)memberNew;
          List oldListEntries = (memberOld instanceof List) ? (List)memberOld : null;
          for (int listEntryIdx = 0; listEntryIdx < newListEntries.size(); listEntryIdx++) {
            if (!(newListEntries.get(listEntryIdx) instanceof XynaObject)) {
              continue;
            }

            if (oldListEntries != null && oldListEntries.size() > listEntryIdx && (oldListEntries.get(listEntryIdx) instanceof XynaObject)) {
              collectItemsToUpdate((XynaObject)oldListEntries.get(listEntryIdx), (XynaObject)newListEntries.get(listEntryIdx), itemsToUpdate);
            } else {
              collectItemsToUpdate(null, (XynaObject)newListEntries.get(listEntryIdx), itemsToUpdate);
            }
          }
        }
      } catch (InvalidObjectPathException e) {
        // when equality can't be guaranteed, assume inequality
        logger.warn("Performance optimization: Could not determine whether step has changed, assuming it has.", e);
        if (newXoRepresentation instanceof ModellingItem) {
          itemsToUpdate.add((ModellingItem)newXoRepresentation);
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private boolean isEqual(Object objectA, Object objectB) {
    if (objectA == null || objectB == null) {
      return (objectA == null && objectB == null);
    } else if (!objectA.getClass().equals(objectB.getClass())) {
      return false;
    } else if (objectA instanceof List) {
      List listA = (List)objectA;
      List listB = (List)objectB;
      if (listA.size() != listB.size()) {
        return false;
      }

      for (int childIdx = 0; childIdx < listA.size(); childIdx++) {
        if (listA.get(childIdx) instanceof ContentArea && listB.get(childIdx) instanceof ContentArea) { // TODO: also for VariableArea if GUI can handle it
          if (!containSameItems((ContainerArea)listA.get(childIdx), (ContainerArea)listB.get(childIdx))) {
            // no recursion, since sub-steps are not relevant to decide whether the item has changed and are considered separately
            return false;
          }
        } else if (!isEqual(listA.get(childIdx), (listB.get(childIdx)))) {
          return false;
        }
      }

      return true;
    } else if (objectA instanceof XynaObject) {
      XynaObject itemA = (XynaObject)objectA;
      XynaObject itemB = (XynaObject)objectB;

      for (String varName : itemA.getVariableNames()) {
        try {
          if (isRelevantForComparision(itemA, itemB, varName) && !isEqual(itemA.get(varName), itemB.get(varName))) {
            return false;
          }
        } catch (InvalidObjectPathException e) {
          // when equality can't be guaranteed, assume inequality
          logger.warn("Performance optimization: Could not determine whether step has changed, assuming it has.", e);
          return false;
        }
      }

      return true;
    } else {
      // primitive object like String
      return objectA.equals(objectB);
    }
  }

  private boolean isRelevantForComparision(XynaObject containerA, XynaObject containerB, String varName) {
    if ( (containerA instanceof Variable || containerB instanceof Variable) &&
         (VARIABLE_MEMBER_NAME.equals(varName)) ) {
      return false;
    }

    return true;
  }

  private boolean containSameItems(ContainerArea containerAreaA, ContainerArea containerAreaB) {
    List<? extends Item> itemsA = containerAreaA.getItems();
    List<? extends Item> itemsB = containerAreaB.getItems();
    if (itemsA == null || itemsA.isEmpty() ||
        itemsB == null || itemsB.isEmpty() ) {
      return ( (itemsA == null || itemsA.isEmpty()) &&
               (itemsB == null || itemsB.isEmpty()) );
    }

    if (itemsA.size() != itemsB.size()) {
      return false;
    }

    for (int childIdx = 0; childIdx < itemsA.size(); childIdx++) {
      String idA = itemsA.get(childIdx).getId();
      String idB = itemsB.get(childIdx).getId();
      if (!idA.equals(idB)) {
        return false;
      }
    }

    return true;
  }
  
  public void checkRevision(int revision) throws InvalidRevisionException {
    if( revision != gbo.getRevision() ) {
      // TODO: for now, send warning instead of exception to GUI (revision isn't essential until multiuser-feature is implemented)
//      throw new InvalidRevisionException(revision,gbo.getRevision()); 
    }
  }

  public void modify(String objectId, Operation operation, String jsonRequest) throws Exception {
    ModifyOperationBase<?> mod = createModifyOperation(objectId, operation);
    int revision = mod.parseRequest(jsonRequest);
    checkRevision(revision);
    GBSubObject object = getObject(objectId);
    mod.modify(this, object);
    focusCandidateId = mod.getFocusCandidateId();
    if (operation != Operation.CopyToClipboard) {
      gbo.markAsModified();
    }
  }

  private GBSubObject getObject(String objectId) throws UnknownObjectIdException, MissingObjectException, XynaException {
    ObjectId oi = ObjectId.parse(objectId);
    if (oi.getType() != ObjectType.clipboardEntry) {
      return gbo.getObject(objectId);
    } else {
      return clipboard.getEntry(Integer.parseInt(oi.getBaseId()), false).getObject();
    }
  }

  public GenerationBaseObject load(FQNameJson fqName) throws XynaException {
    FQName fq = gbo.createFQName(fqName.getTypePath(),fqName.getTypeName());
    return session.load(fq);
  }

  private ModifyOperationBase<?> createModifyOperation(String objectId, Operation operation) {
    switch( operation ) {
    case Change:
    case Complete:
    case Decouple:
    case Sort:
      return new ChangeOperation();
    case Create:
      break;
    case DataflowSaved:
      break;
    case Delete:
      return createDeleteOperation(objectId);
    case Insert:
      return new InsertOperation();
    case Copy:
      return createCopyOperation(objectId);
    case CopyToClipboard:
      return new CopyOperation(session.getClipboard(), ClipboardCopyDirection.TO_CLIPBOARD);
    case Move:
      return createMoveOperation(objectId);
    case Save:
      break;
    case Session:
      break;
    case ViewSaved:
      break;
    case Type:
      return new TypeOperation();
    case ConstantChange:
      return new ChangeConstantOperation();
    case ConstantDelete:
      return new DeleteConstantOperation();
    case Convert:
      return new ConvertOperation();
    case TemplateCall:
      return new TemplateCallOperation();
    default:
      break;
    }
    throw new IllegalStateException("No Operation for "+operation);
  }
  
  
  private DeleteOperation createDeleteOperation(String objectId) {
    DeleteOperation result;
    if (isClipboardId(objectId)) {
      result = new DeleteOperation(clipboard);
    } else {
      result = new DeleteOperation();
    }
    return result;
  }

  private boolean isClipboardId(String objectId) {
    try {
      if(ObjectId.parse(objectId).getType() == ObjectType.clipboardEntry) {
        return true;
      }
    } catch (UnknownObjectIdException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
  
  
  private MoveOperation createMoveOperation(String objectId) {
    if(isClipboardId(objectId)) {
      return new MoveOperation(session.getClipboard());
    }
    
    return new MoveOperation();
  }
  
  
  private CopyOperation createCopyOperation(String objectId) {
    if (isClipboardId(objectId)) {
      return new CopyOperation(session.getClipboard(), ClipboardCopyDirection.FROM_CLIPBOARD);
    }
    
    return new CopyOperation();
  }

  public String save(PersistJson saveRequest, Long revision) throws InvalidRevisionException, XynaException {
    checkRevision(saveRequest.getRevision());
    Persistence persistence = new Persistence(gbo, revision, saveRequest, session.getSession());
    persistence.save();
    focusCandidateId = null;

    return persistence.getSaveFqn();
  }

  public String deploy(PersistJson deployRequest, Long revision) throws InvalidRevisionException, XynaException {
    checkRevision(deployRequest.getRevision());
    if(XMOMType.DATATYPE == gbo.getType()) {
      cleanJavaLibraries(gbo);
    }
    try {
      Persistence persistence = new Persistence(gbo, revision, deployRequest, session.getSession());
      return persistence.deploy();
    } catch (XPRC_MDMDeploymentException deploymentException) {
      if(deploymentException.getCause() instanceof XPRC_JarFileForServiceImplNotFoundException) {
        throw (XPRC_JarFileForServiceImplNotFoundException)deploymentException.getCause();
      }
      throw deploymentException;
    }
  }
  
  private void cleanJavaLibraries(GenerationBaseObject gbo) {
    if(XMOMType.DATATYPE != gbo.getType()) {
      return;
    }
    String savePath = GenerationBase.getFileLocationOfServiceLibsForSaving(gbo.getDOM().getFqClassName(), gbo.getDOM().getRevision());
    Set<String> xmlLibs = gbo.getDOM().getAdditionalLibraries();
    File saveFolder = new File(savePath);
    if(saveFolder.canRead()) {
      String[] fileNames = saveFolder.list();
      for (String fileName : fileNames) {
        if(!xmlLibs.contains(fileName)) {
          gbo.addSgLibToDelete(fileName);
        }
      }
    }
  }

  public String refactor(PersistJson moveRequest, Long revision, String serviceName) throws InvalidRevisionException, XynaException {
    Persistence persistence = new Persistence(gbo, revision, moveRequest, session.getSession(), serviceName);
    return persistence.refactor();
  }

  public void delete(PersistJson deleteRequest, Long revision) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidRevisionException {
    ObjectType objectType = ObjectType.of(gbo.getType());
    if(ObjectType.datatype == objectType && gbo.getViewType() == Type.serviceGroup) {
      objectType = ObjectType.servicegroup;
    }

    GBSubObject object = gbo.getObject(new ObjectId(objectType, null).getObjectId());
    switch (object.getType()) {
      case datatype:
        if(isHybrid(object)) {
          deleteAllInstanceMethods(object);
          deleteAllMemberVariables(object);
          save(deleteRequest, revision);
          deploy(deleteRequest, revision);
        } else {
          deleteXmomObject(deleteRequest, revision);
        }
        break;
      case servicegroup:
        if (isHybrid(object)) {
          deleteAllStaticMethods(object);
          save(deleteRequest, revision);
          deploy(deleteRequest, revision);
        } else {
          deleteXmomObject(deleteRequest, revision);
        }
        break;
      default:
        deleteXmomObject(deleteRequest, revision);
        break;
    }
  }

  private void deleteAllStaticMethods(GBSubObject object) {
    if(object.getDtOrException() instanceof DOM) {
      DOM dom = (DOM) object.getDtOrException();
      List<com.gip.xyna.xprc.xfractwfe.generation.Operation> operations = dom.getOperations();
      for (int i = 0; i < operations.size(); i++) {
        if(operations.get(i).isStatic()) {
          dom.removeOperation(i);
        }
      }
    }
  }
  
  private void deleteAllInstanceMethods(GBSubObject object) {
    if(object.getDtOrException() instanceof DOM) {
      DOM dom = (DOM) object.getDtOrException();
      List<com.gip.xyna.xprc.xfractwfe.generation.Operation> operations = dom.getOperations();
      for (int i = 0; i < operations.size(); i++) {
        if(!operations.get(i).isStatic()) {
          dom.removeOperation(i);
        }
      }
    }
  }
  
  private void deleteAllMemberVariables(GBSubObject object) {
    if(object.getDtOrException() instanceof DOM) {
      DOM dom = (DOM) object.getDtOrException();
      List<AVariable> vars = new ArrayList<>(dom.getMemberVars());
      for (AVariable var : vars) {
        dom.removeMemberVar(var);
      }
    }
  }
  
  private void deleteXmomObject(PersistJson deleteRequest, Long revision) throws XynaException {
    Persistence persistence = new Persistence(gbo, revision, deleteRequest, session.getSession());
    persistence.delete();
  }
  
  private boolean isHybrid(GBSubObject object) {
    switch(object.getType()){
      case datatype:
        if(object.getDtOrException() instanceof DOM) {
          DOM dom = (DOM) object.getDtOrException();
          List<com.gip.xyna.xprc.xfractwfe.generation.Operation> operations = dom.getOperations();
          for (com.gip.xyna.xprc.xfractwfe.generation.Operation operation : operations) {
            if(operation.isStatic()) {
              return true;
            }
          }
        }
        return false;
      case servicegroup:
        if(object.getDtOrException() instanceof DOM) {
          DOM dom = (DOM) object.getDtOrException();
          List<com.gip.xyna.xprc.xfractwfe.generation.Operation> operations = dom.getOperations();
          for (com.gip.xyna.xprc.xfractwfe.generation.Operation operation : operations) {
            if(!operation.isStatic()) {
              return true;
            }
          }
        }
        return false;
      default:
        return false;
    }
  }

  public void modifyDataflow(DataflowJson dataflowRequest, Long revision) throws InvalidRevisionException, UnknownObjectIdException, MissingObjectException, XynaException {
    checkRevision(dataflowRequest.getRevision());

    Dataflow dataflow = gbo.getDataflow();
    GBSubObject target = gbo.getObject(dataflowRequest.getTargetId());

    gbo.refreshDataflow();
    if (dataflowRequest.getType() == LinkstateIn.USER) {
      GBSubObject source = gbo.getObject(dataflowRequest.getSourceId());
      dataflow.addUserConnection(source, target, dataflowRequest.getBranchId());
    } else if (dataflowRequest.getType() == LinkstateIn.NONE) {
      dataflow.removeUserConnection(target, dataflowRequest.getBranchId());
    }
  }
  
  public SessionBasedData getSession() {
    return session;
  }

  public void upload(int revision, String uploadJson) throws InvalidRevisionException, InvalidJSONException, UnexpectedJSONContentException, XynaException {
    checkRevision(revision);
    switch( gbo.getType() ) {
    case DATATYPE:
//      uploadDatatype( jp.parse(uploadJson, Datatype.getJsonVisitor() ) );
      return;
    case EXCEPTION:
      break;
    case FORM:
      break;
    case ORDERINPUTSOURCE:
      break;
    case WORKFLOW:
      break;
    default:
      break;
    }
    throw new UnsupportedOperationException("Cannot upload for type "+gbo.getType() );
  }


  public void addHint(Hint hint) {
    hints.add(hint);
  }
}
