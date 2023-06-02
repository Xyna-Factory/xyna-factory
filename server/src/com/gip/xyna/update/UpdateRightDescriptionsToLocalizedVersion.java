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
package com.gip.xyna.update;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.update.utils.StorableUpdater;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Localization;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope.ScopePart;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;



public class UpdateRightDescriptionsToLocalizedVersion extends UpdateJustVersion {

  public UpdateRightDescriptionsToLocalizedVersion(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion, false);
  }


  @Override
  protected void update() throws XynaException {
    Collection<com.gip.xyna.update.outdatedclasses_7_0_2_7.RightScope> oldRightsScopes =
        StorableUpdater.update(com.gip.xyna.update.outdatedclasses_7_0_2_7.RightScope.class, RightScope.class, new TransformRightScope(),
                               ODSConnectionType.HISTORY);
    Collection<com.gip.xyna.update.outdatedclasses_7_0_2_7.Right> oldRights = StorableUpdater
        .update(com.gip.xyna.update.outdatedclasses_7_0_2_7.Right.class, Right.class, new TransformRight(), ODSConnectionType.HISTORY);
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(Localization.class);
    try {
      List<Localization> localizedRights = new ArrayList<Localization>();
      long id = -1; //negative zahlen verwenden, damit man später nicht mit den vom idgenerator erzeugten zahlen kollidiert.
      for (com.gip.xyna.update.outdatedclasses_7_0_2_7.Right r : oldRights) {
        id--;
        Localization l = new Localization(id, Localization.Type.RIGHT.toString(), r.getName(), "EN", r.getDescription());
        localizedRights.add(l);
      }
      for (com.gip.xyna.update.outdatedclasses_7_0_2_7.RightScope r : oldRightsScopes) {
        id--;
        Localization l = new Localization(id, Localization.Type.RIGHT_SCOPE.toString(), r.getName(), "EN", r.getDocumentation());
        localizedRights.add(l);
      }

      ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        hisCon.ensurePersistenceLayerConnectivity(Localization.class);
        hisCon.persistCollection(localizedRights);
        hisCon.commit();
      } finally {
        hisCon.closeConnection();
      }
    } finally {
      ods.unregisterStorable(Localization.class);
    }
  }


  private static class TransformRight implements Transformation<com.gip.xyna.update.outdatedclasses_7_0_2_7.Right, Right> {

    public Right transform(com.gip.xyna.update.outdatedclasses_7_0_2_7.Right from) {
      return new Right(from.getName());
    }

  }

  private static class TransformRightScope implements Transformation<com.gip.xyna.update.outdatedclasses_7_0_2_7.RightScope, RightScope> {

    public RightScope transform(com.gip.xyna.update.outdatedclasses_7_0_2_7.RightScope from) {
      List<ScopePart> oldParts = from.getParts(); //weil die parts javaserialisiert gespeichert sind, stimmen sie vom typ überein.
      String definition = from.getDefinition();
      if (from.getName().equals(ScopedRight.APPLICATION_DEFINITION.getKey())) {
        definition = ScopedRight.APPLICATION_DEFINITION.getDefinition();
      } else if (from.getName().equals(ScopedRight.APPLICATION.getKey())) {
        definition = ScopedRight.APPLICATION.getDefinition();
      }
      return new RightScope(definition, from.getName(), oldParts);
    }

  }
}
