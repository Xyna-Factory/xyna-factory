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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.DataModelParseContext;


/**
 *
 */
public abstract class TRObjectContainer {

  protected TRDataModelDefinition document;
  protected String name;
  protected List<TRObject> unsortedObjects; //noch nicht in Parent einsortierte
  protected List<TRObject> childObjects;
  protected List<TRObject> rootObjects;
  protected Map<String,TRObject> objectMap;
  protected TRObject rootObject;
  
  public TRObjectContainer(TRDataModelDefinition document, String name) {
    this.document = document;
    this.name = name.trim();
    this.unsortedObjects = new ArrayList<TRObject>();
    this.childObjects = new ArrayList<TRObject>();
    this.rootObjects = new ArrayList<TRObject>();
    this.objectMap = new HashMap<String,TRObject>();
  }
  
  public TRObjectContainer(TRDataModelDefinition document, TRObjectContainer parent) {
    this.document = document;
    this.name = parent.getName().trim();
    this.unsortedObjects = new ArrayList<TRObject>();
    this.rootObjects = new ArrayList<TRObject>();
    this.childObjects = new ArrayList<TRObject>();
    this.objectMap = new HashMap<String,TRObject>();
    migrateObjects( parent.getRootObjects(), rootObjects );
    migrateObjects( parent.getChildObjects(), childObjects );
    migrateObjects( parent.getUnsortedObjects(), unsortedObjects );
  }
  
  private void migrateObjects(List<TRObject> from, List<TRObject> to) {
    for( TRObject object : from ) {
      TRObject copy = new TRObject(this, object);
      to.add(copy);
      objectMap.put(object.getFqName(), copy);
    }
  }

  public void addObject(TRObject object) {
    sortParent( unsortedObjects, object);
    objectMap.put(object.getFqName(), object);
  }

  private boolean sortParent(List<TRObject> unsorted, TRObject object) {
    String parent = object.getParentObjectFqName();
    if( parent != null ) {
      TRObject parentObject = getObject(parent);
      if( parentObject == null ) {
        unsorted.add(object);
        return false;
      } else {
        parentObject.addChild(object);
        childObjects.add(object);
        return true;
      }
    } else {
      rootObjects.add(object);
      return true;
    }
  }

  public List<TRObject> getRootObjects() {
    return rootObjects;
  }
  public List<TRObject> getChildObjects() {
    return childObjects;
  }
  public List<TRObject> getUnsortedObjects() {
    return unsortedObjects;
  }
  
  public TRObject getObject(String fqName) {
    return objectMap.get(fqName);
  }

 
  public String getName() {
    return name;
  }

  public TRDataModelDefinition getDocument() {
    return document;
  }

  public String getFileName() {
    return document.getFileName();
  }


  public void treeify(DataModelParseContext context) { //FIXME besserer Name
    List<TRObject> newUnsorted = new ArrayList<TRObject>();
    for( TRObject object : unsortedObjects ) {
      if( ! sortParent( newUnsorted, object ) ) {
        context.warn("Parent "+object.getParentObjectFqName() +" missing in "+getPrettyName(true)); 
      }
    }
    unsortedObjects = newUnsorted;
    
    
    
    /*
    for( TRObject object: getObjects() ) {
      String parent = object.getParentObjectFqName();
      if( parent != null ) {
        TRObject parentObject = getObject(parent);
        if( parentObject == null ) {
          dataModelResult.warn("Parent "+parent +" missing in "+getPrettyName(true)); 
        } else {
          parentObject.addChild(object);
        }
      } else {
        if( rootObject == null ) {
          rootObject = object; 
        } else {
          dataModelResult.warn("Duplicate root object "+rootObject.getName()+", "+object.getName() +" in "+getPrettyName(true)); 
        }
      }
    }*/
  }
/*
  public void addRootObject(TRObject object) {
    objects.add(object);
    objectMap.put(object.getFqName(), object);
    rootObject = object;
  }
*/
  

  public void adoptRoot(String rootFqName, TRObject newParent) {
    TRObject root = null;
    for( TRObject r : rootObjects ) {
      if( r.getFqName().equals(rootFqName) ) {
        root = r;
        break;
      }
    }
    if( root == null ) {
      return; //nichts zu tun  TODO Fehler? 
    }
    if( ! objectMap.containsKey( newParent.getFqName() ) ) {
      return; //nichts zu tun  TODO Fehler? 
    }
    rootObjects.remove(root);
    newParent.addChild(root);
    childObjects.add(root);
  }

  
  
  public int getObjectCount() {
    return rootObjects.size() + childObjects.size() + unsortedObjects.size();
  }

  
  public abstract String getPrettyName(boolean withVersionAndDocument);

}
