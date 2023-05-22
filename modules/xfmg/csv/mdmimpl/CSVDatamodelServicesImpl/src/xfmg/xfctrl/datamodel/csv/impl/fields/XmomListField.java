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
package xfmg.xfctrl.datamodel.csv.impl.fields;

import java.util.Iterator;
import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

public class XmomListField extends AbstractXmomField {

  private CurrentIterator currentIter;

  public XmomListField(String label, String name, XmomField parent, FieldType fieldType, String fqClassName) {
    super(label, name, parent, fieldType, fqClassName);
  }
  
  public boolean hasMore(GeneralXynaObject gxo) {
    if( currentIter == null ) {
      currentIter = new CurrentIterator((List<?>)getBaseObject(gxo));
    }
    if( currentIter.hasNext() ) {
      return true;
    } else {
      currentIter = null;
      return false;
    }
  }
  
  @Override
  public Object getObject(GeneralXynaObject gxo) {
    if( currentIter == null ) {
      currentIter = new CurrentIterator((List<?>)getBaseObject(gxo));
    }
    return currentIter.get();
  }
  
  @Override
  public String getPath() {
    return super.getPath() + "[";
  }
  
  /**
   * keine next()-Methode: get()-Methode führt einmal nach dem 
   * hasNext()-Aufruf intern next() auf, ansonsten wird immer das letzte Element ausgegeben.
   *
   */
  public static class CurrentIterator {

    private Iterator<?> iter;
    private Object current;
    boolean next;
    
    public CurrentIterator(List<?> list) {
      if( list != null ) {
        iter = list.iterator();
        next = iter.hasNext();
      }
    }

    public Object get() {
      if( next ) {
        current = iter.next();
        next = false;
      }
      return current;
    }

    public boolean hasNext() {
      if( iter == null ) {
        return false; 
      } else {
        next = true;
        return iter.hasNext();
      }
    }
    
  }

  public boolean isList() {
    return true;
  }

}
