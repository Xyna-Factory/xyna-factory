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
package com.gip.xyna.xprc.xprcods.orderarchive.selectorder;



import junit.framework.TestCase;

import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class OrderInstanceSelectTest extends TestCase {

  public void test0() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois =
        new OrderInstanceSelect().selectAllForOrderInstance().whereId().isEqual(3).finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test1() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois =
        new OrderInstanceSelect().selectAllForOrderInstance().whereStatusEnum().isEqual(OrderInstanceStatus.FINISHED)
            .finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test2() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois =
        new OrderInstanceSelect().selectAllForOrderInstance().whereStatusEnum().isEqual(OrderInstanceStatus.XYNA_ERROR)
            .finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test3() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois =
        new OrderInstanceSelect().selectAllForOrderInstance().whereStatusEnum()
            .isEqual(OrderInstanceStatus.SCHEDULING).finalizeSelect(OrderInstanceSelect.class);
    assertFalse(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test4() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois =
        new OrderInstanceSelect().selectAllForOrderInstance().whereStatusEnum()
            .isEqual(OrderInstanceStatus.SCHEDULING).or().whereId().isEqual(1).finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test5() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois = new OrderInstanceSelect();
    WhereClausesConnection<WhereClausesContainerImpl> wcc = ois.newWC().whereId().isEqual(1);
    ois.selectAllForOrderInstance().whereStatusEnum().isEqual(OrderInstanceStatus.SCHEDULING).or().whereNot(wcc)
        .finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }


  public void test6() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois = new OrderInstanceSelect();
    WhereClausesConnection<WhereClausesContainerImpl> wcc =
        ois.newWC().whereStatusEnum().isEqual(OrderInstanceStatus.FINISHED).or().whereStatusEnum()
            .isEqual(OrderInstanceStatus.XYNA_ERROR).or().whereId().isEqual(2);
    ois.selectAllForOrderInstance().whereStatusEnum().isEqual(OrderInstanceStatus.SCHEDULING).or().whereNot(wcc)
        .finalizeSelect(OrderInstanceSelect.class);
    assertFalse(ois.doesQueryStatusFinishedOrFailed());
  }

  
  public void test7() throws XNWH_WhereClauseBuildException {
    OrderInstanceSelect ois = new OrderInstanceSelect();
    WhereClausesConnection<WhereClausesContainerImpl> wcc2 =
        ois.newWC().whereId().isEqual(2).or().whereStatusEnum().isEqual(OrderInstanceStatus.SCHEDULING);
    WhereClausesConnection<WhereClausesContainerImpl> wcc =
        ois.newWC().whereStatusEnum().isEqual(OrderInstanceStatus.FINISHED).or().whereStatusEnum()
            .isEqual(OrderInstanceStatus.XYNA_ERROR).or().whereId().isEqual(2).and().whereNot(wcc2);
    ois.selectAllForOrderInstance().whereStatusEnum().isEqual(OrderInstanceStatus.SCHEDULING).or().whereNot(wcc)
        .finalizeSelect(OrderInstanceSelect.class);
    assertTrue(ois.doesQueryStatusFinishedOrFailed());
  }
}
