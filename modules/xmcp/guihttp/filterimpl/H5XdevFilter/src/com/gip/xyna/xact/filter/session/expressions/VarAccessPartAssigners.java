/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.session.expressions;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xmcp.processmodeller.datatypes.expression.CastExpression;
import xmcp.processmodeller.datatypes.expression.ExpressionVariable;
import xmcp.processmodeller.datatypes.expression.FunctionExpression;
import xmcp.processmodeller.datatypes.expression.VariableAccessPart;



public class VarAccessPartAssigners {

  public interface VariableAccessPartAssigner {

    public void assign(VariableAccessPart part);
  }

  
  public static void assign(GeneralXynaObject container, VariableAccessPart part) {
    assignerCreatorMap.get(container.getClass()).apply(container).assign(part);
  }

  private final static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, VariableAccessPartAssigner>> assignerCreatorMap =
      createAssignerCreatorMap();


  private static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, VariableAccessPartAssigner>> createAssignerCreatorMap() {
    Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, VariableAccessPartAssigner>> map = new HashMap<>();
    map.put(ExpressionVariable.class, (x) -> new ExpressionVariableAccessPartAssigner((ExpressionVariable) x));
    map.put(FunctionExpression.class, (x) -> new FunctionExpressionAccessPartAssigner((FunctionExpression) x));
    map.put(CastExpression.class, (x) -> new FunctionExpressionAccessPartAssigner((CastExpression) x));
    return Collections.unmodifiableMap(map);
  }


  private static class AccessPartListAssigner implements VariableAccessPartAssigner {

    private Supplier<List<? extends VariableAccessPart>> getter;
    private Consumer<List<VariableAccessPart>> setter;


    public AccessPartListAssigner(Supplier<List<? extends VariableAccessPart>> getter, Consumer<List<VariableAccessPart>> setter) {
      this.getter = getter;
      this.setter = setter;
    }


    @Override
    public void assign(VariableAccessPart part) {
      List<? extends VariableAccessPart> oldParts = getter.get();
      List<VariableAccessPart> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
      parts.add(part);
      setter.accept(parts);
    }

  }

  private static class FunctionExpressionAccessPartAssigner extends AccessPartListAssigner {

    public FunctionExpressionAccessPartAssigner(FunctionExpression exp) {
      super(() -> exp.getParts(), (x) -> exp.unversionedSetParts(x));
    }


  }

  private static class ExpressionVariableAccessPartAssigner extends AccessPartListAssigner {


    public ExpressionVariableAccessPartAssigner(ExpressionVariable exp) {
      super(() -> exp.getParts(), (x) -> exp.unversionedSetParts(x));
    }
  }
}
