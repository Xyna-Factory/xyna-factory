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

package com.gip.xyna.xact.filter.session.gb.adapter;



import java.util.AbstractList;
import java.util.List;

import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.VariableListAdapter;



public abstract class ListAdapter<E> extends AbstractList<E> {

  public static class Move<E> extends com.gip.xyna.utils.collections.ListUtils.Move<E> {


    public Move(List<? extends E> in) {
      super(in);
    }


    public Move(E in) {
      super(in);
    }


    @Override
    public void at(int index) {
      if (this.in instanceof VariableListAdapter) {
        ((VariableListAdapter) this.in).move(this.index, index);
      } else {
        super.at(index);
      }
    }

  }


  public void move(int sourceIndex, int destinationIndex) {
    E element = remove(sourceIndex);
    if (destinationIndex < 0) {
      add(element);
    } else {
      add(destinationIndex, element);
    }
  }

}
