package xmcp.gitintegration.impl;

import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.ReferenceManagement;
import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceObjectType;
import xmcp.gitintegration.storage.ReferenceStorage;

public class ReferenceUpdater {

  public void update(List<ItemDifference<Reference>> idr, Long revision, ReferenceObjectType objType, String fromName, String toName) {
    String ws = getWorkspaceName(revision);
    for(ItemDifference<Reference> id: idr) {
      update(id, ws, revision, objType, fromName, toName);
    }
  }
  
  public void update(ItemDifference<Reference> idr, String ws, Long rev, ReferenceObjectType objType, String fromName, String toName) {
    ReferenceStorage storage = new ReferenceStorage();
    ReferenceConverter converter = new ReferenceConverter();
    XynaContentDifferenceType typeName = idr.getType();
      if (typeName == XynaContentDifferenceType.CREATE) {
        ReferenceData.Builder builder = new ReferenceData.Builder();
        builder.objectName(toName).objectType(objType.toString()).path(idr.getTo().getPath())
            .referenceType(idr.getTo().getType()).workspaceName(ws);
        ReferenceManagement.addReference(builder.instance());
      } else if (typeName == XynaContentDifferenceType.MODIFY) {
        InternalReference irFrom = converter.convert(idr.getFrom());
        InternalReference irTo = converter.convert(idr.getTo());
        storage.modify(irFrom, irTo, rev, toName, objType);
      } else if (typeName == XynaContentDifferenceType.DELETE) {
        storage.deleteReference(idr.getFrom().getPath(), rev, fromName);
      }
  }


  private String getWorkspaceName(long revision) {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return revMgmt.getWorkspace(revision).getName();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }
}