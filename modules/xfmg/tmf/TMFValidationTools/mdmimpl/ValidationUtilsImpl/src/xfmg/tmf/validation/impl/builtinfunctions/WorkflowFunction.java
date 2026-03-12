/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl.builtinfunctions;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;

import base.Text;
import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;



public class WorkflowFunction implements TMFDirectFunction {

  public static class WorkflowInfo {

    public enum InputType {
      SINGLE_TEXT, TEXT_ARRAY, TWO_TEXT, THREE_TEXT, FOUR_TEXT
    }


    public final String fqWorkflowName;
    public final String simpleWorkflowName;
    public final InputType inputType;


    public WorkflowInfo(String fqWorkflowName, String simpleWorkflowName, InputType inputType) {
      super();
      this.fqWorkflowName = fqWorkflowName;
      this.simpleWorkflowName = simpleWorkflowName;
      this.inputType = inputType;
    }

  }


  private final WorkflowInfo wi;


  public WorkflowFunction(WorkflowInfo wi) {
    this.wi = wi;
  }


  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    GeneralXynaObject wfInput;
    switch (wi.inputType) {
      case SINGLE_TEXT :
        wfInput = asText(args[0]);
        break;
      case TWO_TEXT :
      case THREE_TEXT :
      case FOUR_TEXT :
        GeneralXynaObject[] payload = new GeneralXynaObject[args.length];
        for (int i = 0; i < args.length; i++) {
          payload[i] = asText(args[i]);
        }
        wfInput = new Container(payload);
        break;
      case TEXT_ARRAY :
        GeneralXynaObjectList<Text> list = new GeneralXynaObjectList<Text>(Text.class);
        for (int i = 0; i < args.length; i++) {
          list.add(asText(args[i]));
        }
        wfInput = list;
        break;
      default :
        throw new RuntimeException("unexpected case " + wi.inputType);
    }

    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(context.rtc);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }

    ChildOrderStorageStack _childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
    //sets parent order id, inherits custom fields, etc
    XynaOrderServerExtension cxo = _childOrderStorageStack.createOrGetXynaOrder(wi.fqWorkflowName, revision);
    try {
      cxo.setInputPayload(wfInput);
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
          .startOrderSynchronous(cxo, !_childOrderStorageStack.isFirstExecution());
      GeneralXynaObject output = cxo.getOutputPayload();
      if (output == null) {
        throw new RuntimeException("Workflow function <" + getName() + "> returned null (orderid=" + cxo.getId() + ").");
      } else if (output instanceof GeneralXynaObjectList) {
        List<String> l = new ArrayList<>();
        for (GeneralXynaObject xo : (GeneralXynaObjectList<? extends GeneralXynaObject>) output) {
          if (xo instanceof Text) {
            l.add(((Text) xo).getText());
          } else {
            throw new RuntimeException("Workflow function <" + getName() + "> returned an unexpected type: " + xo.getClass().getName());
          }
        }
        return l;
      } else if (output instanceof Text) {
        return ((Text) output).getText();
      } else if (output instanceof Container) {
        throw new RuntimeException("Workflow function <" + getName() + "> did not return exactly one output");
      } else {
        throw new RuntimeException("Workflow function <" + getName() + "> returned an unexpected type: " + output.getClass().getName());
      }
    } catch (XynaException e) {
      throw new RuntimeException("Exception occured executing workflow function <" + getName() + "> with order ID " + cxo.getId(), e);
    } catch (ProcessSuspendedException e) {
      _childOrderStorageStack.suspended();
      throw e;
    }
  }


  private Text asText(Object object) {
    if (object == null) {
      return null;
    }
    String s = ConversionUtils.getString(object);
    return new Text(s);
  }


  @Override
  public String getName() {
    return wi.simpleWorkflowName;
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    switch (wi.inputType) {
      case SINGLE_TEXT :
        validate(args, 1);
        break;
      case TWO_TEXT :
        validate(args, 2);
        break;
      case THREE_TEXT :
        validate(args, 3);
        break;
      case FOUR_TEXT :
        validate(args, 4);
        break;
      case TEXT_ARRAY : //ok
        break;
      default :
        throw new RuntimeException();
    }
  }


  private void validate(SyntaxTreeNode[] args, int n) {
    if (args.length != n) {
      throw new RuntimeException("Workflow function " + getName() + " expects " + n + " inputs, but got " + args.length);
    }
  }

}
