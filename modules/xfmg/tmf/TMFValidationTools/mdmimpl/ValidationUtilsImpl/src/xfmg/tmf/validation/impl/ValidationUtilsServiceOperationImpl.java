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
package xfmg.tmf.validation.impl;



import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.Text;
import internalized.tmf633.datamodel.AllowedChanges;
import internalized.tmf633.datamodel.ChangeList;
import xfmg.tmf.validation.Constraint;
import xfmg.tmf.validation.ConstraintV2;
import xfmg.tmf.validation.ConstraintValidationResult;
import xfmg.tmf.validation.ConstraintValidationResultV2;
import xfmg.tmf.validation.JSONPathExpressionV2;
import xfmg.tmf.validation.ValidationUtilsServiceOperation;
import xfmg.tmf640.validation.NotAnAllowedChange;



public class ValidationUtilsServiceOperationImpl
    implements
      ExtendedDeploymentTask,
      ValidationUtilsServiceOperation,
      ProjectCreationOrChangeListener {

  private final static String LISTENER_ID = "TMFValidationUtil" + System.currentTimeMillis();


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


  public List<? extends ConstraintValidationResult> validateConstraints(Text text2, List<? extends Constraint> constraint1) {
    // Implemented as code snippet!
    return null;
  }


  public List<? extends ConstraintValidationResultV2> validateConstraintsV2(XynaOrderServerExtension xo, Text json,
                                                                            List<? extends ConstraintV2> constraintV26) {
    TMFExpressionParser parser = ParserCache.getParser(xo.getRootOrder().getRevision());
    RuntimeContext rtc;
    try {
      rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRuntimeContext(xo.getRootOrder().getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    String jsonString = json.getText();

    DependencyGraphExecutor dge = new DependencyGraphExecutor();
    List<ConstraintValidationResultV2> r = dge.execute(constraintV26, c -> eval(jsonString, parser, c.getCondition(), rtc),
                       c -> eval(jsonString, parser, c.getRule(), rtc));
    reorder(r, constraintV26);
    return r;
  }


  public static void reorder(List<ConstraintValidationResultV2> r, List<? extends ConstraintV2> c) {
    //order elements in r the same way as they are ordered in c 
    Map<String, Integer> idxMap = new HashMap<>();
    for (int i = 0; i < c.size(); i++) {
      idxMap.put(c.get(i).getName(), i);
    }
    r.sort((a, b) -> idxMap.get(a.getName()) - idxMap.get(b.getName()));
  }


  private boolean eval(String json, TMFExpressionParser parser, JSONPathExpressionV2 expr, RuntimeContext rtc) {
    TMFExpressionContext ctx = new TMFExpressionContext(json, expr.getJsonPaths(), rtc);
    /*
     * TODO cache
     * 1. cache parse()-result (SyntaxTreeNode) + validate
     * 2. cache in context.eval? or does library already provide caching for that?
     */
    SyntaxTreeNode n = parser.parse(expr.getExpression(), 0, true);
    n.validate();
    Object result = n.eval(ctx);
    if (result instanceof Boolean) {
      return (Boolean) result;
    } else if (result instanceof String) {
      String r = (String) result;
      if ("true".equalsIgnoreCase(r)) {
        return true;
      } else if ("false".equalsIgnoreCase(r)) {
        return false;
      }
    }
    throw new RuntimeException("Result of expr " + expr.getExpression() + " was not a boolean");
  }


  public void onDeployment() throws XynaException {
    ProjectCreationOrChangeProvider.getInstance().addListener(LISTENER_ID, this);
  }


  public void onUndeployment() throws XynaException {
    ProjectCreationOrChangeProvider.getInstance().removeListener(LISTENER_ID);
  }


  public void projectCreatedOrModified(Collection<? extends ProjectCreationOrChangeEvent> event, Long revision, String commitMsg) {
    for (ProjectCreationOrChangeEvent pcoce : event) {
      if (pcoce.getType().equals(EventType.SERVICE_DEPLOY)) {
        ParserCache.removeFromCache(revision);
      }
    }
  }
  


  @Override
  public void validateAllowedChanges(AllowedChanges changes, Text json1, Text json2) throws NotAnAllowedChange {
    if (changes == null || (isEmpty(changes.getAllof()) && isEmpty(changes.getAnyof()))) {
      return;
    }
    String s1 = WhiteListUtils.removePathsFromJSON(json1.getText(), changes.getIgnored());
    String s2 = WhiteListUtils.removePathsFromJSON(json2.getText(), changes.getIgnored());

    if (!isEmpty(changes.getAllof())) {
      listloop : for (ChangeList cl : changes.getAllof()) {
        if (cl != null) {
          for (String path : cl.getChanges()) {
            if (WhiteListUtils.isJSONPartTheSame(s1, s2, path)) {
              //not changed -> next list
              continue listloop;
            }
          }
          //json is different, but is there no other change?
          if (WhiteListUtils.isJSONTheSameExceptPaths(s1, s2, cl.getChanges())) {
            return; //ok
          }
        }
      }
    }

    if (!isEmpty(changes.getAnyof())) {
      for (ChangeList cl : changes.getAnyof()) {
        if (cl != null) {
          if (WhiteListUtils.isJSONTheSameExceptPaths(s1, s2, cl.getChanges())) {
            return; //ok
          }
        }
      }
    }

    throw new NotAnAllowedChange(msg(WhiteListUtils.createJsonPathListOfAllChanges(s1, s2)));
  }


  private String msg(List<String> diffs) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < diffs.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(i + 1).append(". difference: ");
      sb.append(diffs.get(i));
      sb.append("\n");
    }
    return sb.toString();
  }


  private boolean isEmpty(List<? extends ChangeList> l) {
    return l == null || l.isEmpty();
  }


  @Override
  public Text evaluateExpression(XynaOrderServerExtension xo, Text json, JSONPathExpressionV2 expr) {
    TMFExpressionParser parser = ParserCache.getParser(xo.getRootOrder().getRevision());
    RuntimeContext rtc;
    try {
      rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRuntimeContext(xo.getRootOrder().getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    String jsonString = json.getText();
    TMFExpressionContext ctx = new TMFExpressionContext(jsonString, expr.getJsonPaths(), rtc);
    SyntaxTreeNode n = parser.parse(expr.getExpression(), 0, true);
    n.validate();
    Object result = n.eval(ctx);
    String resultString = ConversionUtils.getString(result);
    if (resultString == null) {
      return null;
    }
    return new Text(resultString);
  }


  @Override
  public List<? extends Text> evaluateJSONPath(Text json, Text jsonPath) {
    TMFExpressionContext ctx = new TMFExpressionContext(json.getText(), Collections.emptyList(), null);
    Object result = ctx.eval(jsonPath.getText());
    return ConversionUtils.getStringList(result).stream().map(s -> new Text(s)).collect(Collectors.toList());
  }
}
