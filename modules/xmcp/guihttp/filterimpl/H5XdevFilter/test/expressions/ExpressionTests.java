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
package expressions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import com.gip.xyna.xact.filter.session.ModelledExpressionConverter;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;

import junit.framework.TestCase;
import xmcp.processmodeller.datatypes.expression.CastExpression;
import xmcp.processmodeller.datatypes.expression.Expression;
import xmcp.processmodeller.datatypes.expression.ExpressionVariable;
import xmcp.processmodeller.datatypes.expression.FunctionExpression;
import xmcp.processmodeller.datatypes.expression.LiteralExpression;
import xmcp.processmodeller.datatypes.expression.ModelledExpression;
import xmcp.processmodeller.datatypes.expression.NotExpression;
import xmcp.processmodeller.datatypes.expression.SingleVarExpression;
import xmcp.processmodeller.datatypes.expression.VariableAccessPart;
import xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation;
import xmcp.processmodeller.datatypes.expression.Expression2Args;

public class ExpressionTests extends TestCase {

  
  public void testBasicExpressions() {
    ModelledExpression exp = convert("%1%.text=%0%.test");
    ModelledExpression expectedResult = new ModelledExpression(
        new SingleVarExpression(
            new ExpressionVariable(0, 
               Arrays.asList(new VariableAccessPart[] {
                   new VariableAccessPart("test", null)
               }),
               null)                 
            ),
        new SingleVarExpression(
          new ExpressionVariable(1, 
               Arrays.asList(new VariableAccessPart[] {
                   new VariableAccessPart("text", null)
               }),
               null)
            )
        );
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testIndexExpressionOfVariable() {
    ModelledExpression exp = convert("%1%[\"0\"].text=%0%.test");
    ModelledExpression expectedResult = new ModelledExpression( 
         new SingleVarExpression(
             new ExpressionVariable(0, 
                Arrays.asList(new VariableAccessPart[] {
                    new VariableAccessPart("test", null)
                }),
                null)                 
             ),
         new SingleVarExpression(
               new ExpressionVariable(1,
                   Arrays.asList(new VariableAccessPart[] {
                       new VariableAccessPart("text", null)
                   }),
                   new LiteralExpression("0")
               )
             )
        );
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testExp2Args() {
    ModelledExpression exp = convert("%0%.text!=\"test\"");
    ModelledExpression expectedResult = new ModelledExpression(null,
               new Expression2Args(
                   new SingleVarExpression(
                       new ExpressionVariable(0, Arrays.asList(new VariableAccessPart[] {
                           new VariableAccessPart("text", null)
                       }), null)), 
                   new LiteralExpression("test"), "!=")
        );
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testNotExpression() {
    ModelledExpression exp = convert("!\"False\"");
    ModelledExpression expectedResult = new ModelledExpression(null,
        new NotExpression(new LiteralExpression("False")));
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testInstanceFunction() {
    ModelledExpression exp = convert("%1%.bData=%0%.isNullOrEmpty()");
    ModelledExpression expectedResult = new ModelledExpression(
        new SingleVarExpression(
            new ExpressionVariable(0,
                                   Arrays.asList(new VariableAccessPart[] {
                                       new VariableInstanceFunctionIncovation("isNullOrEmpty", null, null) }),
                                   null)
            ),
        new SingleVarExpression(
            new ExpressionVariable(1, 
                                   Arrays.asList(new VariableAccessPart[] { 
                                       new VariableAccessPart("bData", null) }), 
                                   null)));
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testInstanceFunctionParameter() {
    ModelledExpression exp = convert("%1%.bData=%0%.singleVarMethod(\"test\")");
    ModelledExpression expectedResult = new ModelledExpression(
       new SingleVarExpression(
           new ExpressionVariable(0,
                                  Arrays.asList(new VariableAccessPart[] {
                                      new VariableInstanceFunctionIncovation("singleVarMethod", null, 
                                          Arrays.asList(new Expression[] {
                                              new LiteralExpression("test")
                                          })) }),
                                  null)
             ),
        new SingleVarExpression(
            new ExpressionVariable(1, 
                                   Arrays.asList(new VariableAccessPart[] { 
                                       new VariableAccessPart("bData", null) }), 
                                   null)));
    
    assertTrue(compare(exp, expectedResult));
  }
  
  public void testInstanceFunctionIndexDefinition() {
    ModelledExpression exp = convert("%1%.bData=%0%.singleVarMethod(\"test\")[\"5\"]");
    ModelledExpression expectedResult = new ModelledExpression(
       new SingleVarExpression(
          new ExpressionVariable(0,
                                 Arrays.asList(new VariableAccessPart[] {
                                     new VariableInstanceFunctionIncovation("singleVarMethod", 
                                         new LiteralExpression("5"), 
                                         Arrays.asList(new Expression[] {
                                             new LiteralExpression("test")
                                         })) }),
                                 null)
            ),
       new SingleVarExpression(
           new ExpressionVariable(1, 
                                  Arrays.asList(new VariableAccessPart[] { 
                                      new VariableAccessPart("bData", null) }), 
                                  null)));

                                  assertTrue(compare(exp, expectedResult));
  }
  
  public void testInstanceFunctionComplexMember() {
    ModelledExpression exp = convert("%1%.bData=%0%.singleVarMethod(\"test\", %0%.other[\"1\"])[\"2\"]");
    ModelledExpression expectedResult = new ModelledExpression(
     new SingleVarExpression(
        new ExpressionVariable(0,
                               Arrays.asList(new VariableAccessPart[] {
                                   new VariableInstanceFunctionIncovation("singleVarMethod", 
                                       new LiteralExpression("2"), 
                                       Arrays.asList(new Expression[] {
                                           new LiteralExpression("test"),
                                           new SingleVarExpression(new ExpressionVariable(0, 
                                                                    Arrays.asList(new VariableAccessPart[] {
                                                                        new VariableAccessPart("other", new LiteralExpression("1"))
                                                                    }), null))
                                       }))}),
                               null)
          ),
     new SingleVarExpression(
         new ExpressionVariable(1, 
                                Arrays.asList(new VariableAccessPart[] { 
                                    new VariableAccessPart("bData", null) }), 
                                null)));

     assertTrue(compare(exp, expectedResult));

  }
  
  public void testInstanceFunctionComplexMemberListVar() {
    ModelledExpression exp = convert("%1%.bData=%0%[\"4\"].singleVarMethod(\"test\", %0%.other[\"1\"])[\"2\"]");
    ModelledExpression expectedResult = new ModelledExpression(
     new SingleVarExpression(
        new ExpressionVariable(0,
                               Arrays.asList(new VariableAccessPart[] {
                                   new VariableInstanceFunctionIncovation("singleVarMethod", 
                                       new LiteralExpression("2"), 
                                       Arrays.asList(new Expression[] {
                                           new LiteralExpression("test"),
                                           new SingleVarExpression(new ExpressionVariable(0, 
                                                                    Arrays.asList(new VariableAccessPart[] {
                                                                        new VariableAccessPart("other", new LiteralExpression("1"))
                                                                    }), null))
                                       }))}),
                               new LiteralExpression("4"))
          ),
     new SingleVarExpression(
         new ExpressionVariable(1, 
                                Arrays.asList(new VariableAccessPart[] { 
                                    new VariableAccessPart("bData", null) }), 
                                null)));

     assertTrue(compare(exp, expectedResult));

  }
  
  public void testConcat() {
   
    ModelledExpression exp = convert( "%2%.bData=concat(%0%.text, %1%.text2)");
    ModelledExpression expectedResult = new ModelledExpression(
        new FunctionExpression(
            Arrays.asList(new Expression[] {
                new SingleVarExpression(
                  new ExpressionVariable(0, 
                     Arrays.asList(new VariableAccessPart[] { 
                         new VariableAccessPart("text", null) }), 
                     null)),
                new SingleVarExpression(
                    new ExpressionVariable(1, 
                       Arrays.asList(new VariableAccessPart[] { 
                           new VariableAccessPart("text2", null) }), 
                       null))
            }), 
            "concat", 
            null, 
            null),
        new SingleVarExpression(
          new ExpressionVariable(2, 
             Arrays.asList(new VariableAccessPart[] { 
                 new VariableAccessPart("bData", null) }), 
             null))
        );
                                                               

    assertTrue(compare(exp, expectedResult));
  }
  
  public void testConcatListsWithIndex() {
    
    ModelledExpression exp = convert("%2%.bData=concatlists(%0%.text, %1%.text2)[\"5\"]");
    ModelledExpression expectedResult = new ModelledExpression(
        new FunctionExpression(
            Arrays.asList(new Expression[] {
                new SingleVarExpression(
                  new ExpressionVariable(0, 
                     Arrays.asList(new VariableAccessPart[] { 
                         new VariableAccessPart("text", null) }), 
                     null)),
                new SingleVarExpression(
                    new ExpressionVariable(1, 
                       Arrays.asList(new VariableAccessPart[] { 
                           new VariableAccessPart("text2", null) }), 
                       null))
            }), 
            "concatlists", 
            new LiteralExpression("5"), 
            null),
        new SingleVarExpression(
          new ExpressionVariable(2, 
             Arrays.asList(new VariableAccessPart[] { 
                 new VariableAccessPart("bData", null) }), 
             null))
        );

    assertTrue(compare(exp, expectedResult));
  }
  
  public void testDynamicResultTypeFunction() {
    ModelledExpression exp = convert("%1%=new(\"some.tests.DTWithBoolean\").test");
    ModelledExpression expectedResult = new ModelledExpression(
        new FunctionExpression(
            Arrays.asList(new Expression[] {new LiteralExpression("some.tests.DTWithBoolean")}),
            "new",
            null,
            Arrays.asList(new VariableAccessPart[] {new VariableAccessPart("test", null)})
            ),
        new SingleVarExpression(new ExpressionVariable(1, null, null))
        );
    assertTrue(compare(exp, expectedResult));
  }
  
  
  public void testVariableAccessPartMultiWithIndexDef() {
    ModelledExpression exp = convert("%1%.text=%0%.test[\"0\"].text2");
    ModelledExpression expectedResult = new ModelledExpression(
        new SingleVarExpression(
            new ExpressionVariable(0,
                 Arrays.asList(new VariableAccessPart[] { 
                     new VariableAccessPart("test", new LiteralExpression("0")),
                     new VariableAccessPart("text2", null)
                 }),
                 null
                )
            ),
        new SingleVarExpression(
            new ExpressionVariable(1, 
               Arrays.asList(new VariableAccessPart[] { 
                   new VariableAccessPart("text", null) }), 
               null))
        );

    assertTrue(compare(exp, expectedResult));
  }
  
  public void testVarIndexDefIsVarExp() {
    ModelledExpression exp = convert("%2%.text=%0%[%1%.count]");
    ModelledExpression expectedResult = new ModelledExpression(
        new SingleVarExpression(new ExpressionVariable(0,
            null,
            new SingleVarExpression(new ExpressionVariable(1, 
                   Arrays.asList(new VariableAccessPart[] { new VariableAccessPart("count", null) })
                   , null))
            )),
        new SingleVarExpression(
          new ExpressionVariable(2, 
             Arrays.asList(new VariableAccessPart[] { new VariableAccessPart("text", null) }), 
             null))
        );
    

    assertTrue(compare(exp, expectedResult));
  }
  
  public void testCastExpression() {
    ModelledExpression exp = convert("%0%#cast(\"some.Dt\")=%2%");
    ModelledExpression expectedResult = new ModelledExpression(
        new SingleVarExpression(new ExpressionVariable(2, null, null)),
        new CastExpression(Arrays.asList(new Expression[] {    
            new LiteralExpression("some.Dt"), 
            new SingleVarExpression(new ExpressionVariable(0,null, null))}
            ), Functions.CAST_FUNCTION_NAME, null, null)
        );
    

    assertTrue(compare(exp, expectedResult));
  }
  
  private static ModelledExpression convert(String expression) {
    com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression me;
    try {
      me = com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.parse(helperContext(), expression);
      ModelledExpressionConverter converter = new ModelledExpressionConverter();
      xmcp.processmodeller.datatypes.expression.ModelledExpression out = converter.convert(me);
      return out;
    } catch (XPRC_ParsingModelledExpressionException e) {
      throw new RuntimeException("Invalid Modelled Expression");
    }
  }
  
  
  private static VariableContextIdentification helperContext() {
    return new VariableContextIdentification() {
      public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
        throw new RuntimeException();
      }
      public TypeInfo getTypeInfo(String originalXmlName) {
        throw new RuntimeException();
      }
      public Long getRevision() {
        return null;
      }
      public VariableInfo createVariableInfo(TypeInfo resultType) {
        throw new RuntimeException();
      }
    };
  }
  
  private boolean objCmp(Object obj1, Object obj2) {
    return Objects.isNull(obj1) == Objects.isNull(obj2) && (Objects.isNull(obj1) || obj1.getClass() == obj2.getClass());
  }
  
  private boolean compare(ModelledExpression obj1, ModelledExpression obj2) {
    return objCmp(obj1, obj2) && 
        compare(obj1.getSourceExpression(), obj2.getSourceExpression()) && 
        compare(obj1.getTargetExpression(), obj2.getTargetExpression());
  }


  private boolean compare(Expression obj1, Expression obj2) {
    return objCmp(obj1, obj2) && (Objects.isNull(obj1)
        || (obj1.getClass().equals(FunctionExpression.class) && compare((FunctionExpression)obj1, (FunctionExpression)obj2))
        || (obj1.getClass().equals(Expression2Args.class) && compare((Expression2Args)obj1, (Expression2Args)obj2))
        || (obj1.getClass().equals(LiteralExpression.class) && compare((LiteralExpression)obj1, (LiteralExpression)obj2))
        || (obj1.getClass().equals(NotExpression.class) && compare((NotExpression)obj1, (NotExpression)obj2))
        || (obj1.getClass().equals(SingleVarExpression.class) && compare((SingleVarExpression)obj1, (SingleVarExpression)obj2))
        || (obj1.getClass().equals(CastExpression.class) && compare((FunctionExpression)obj1, (FunctionExpression)obj2))
        
        ) ;
  }
  
  private boolean compare(FunctionExpression obj1, FunctionExpression obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1)
        || (Objects.equals(obj1.getFunction(), obj2.getFunction())
            && compare(obj1.getIndexDef(), obj2.getIndexDef())
            && compareLists(obj1.getParts(), obj2.getParts(), this::compare)
            && compareLists(obj1.getSubExpressions(), obj2.getSubExpressions(), this::compare));
  }
  
  private <T> boolean compareLists(List<? extends T> obj1, List<? extends T> obj2, BiFunction<T, T, Boolean> cmp) {
    if(objCmp(obj1, obj2)) {
      if(obj1 == null) {
        return true;
      }
      if(obj1.size() != obj2.size()) {
        return false;
      }
      for(int i=0; i<obj1.size(); i++) {
        if(!cmp.apply(obj1.get(i), obj2.get(i))) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
  private boolean compare(VariableAccessPart obj1, VariableAccessPart obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1)
        || (Objects.equals(obj1.getName(), obj2.getName())
            && compare(obj1.getIndexDef(), obj2.getIndexDef()))
            &&     (obj1.getClass().equals(VariableInstanceFunctionIncovation.class) && compare((VariableInstanceFunctionIncovation)obj1, (VariableInstanceFunctionIncovation)obj2)
                || (obj1.getClass().equals(VariableAccessPart.class))
            );
  }
  
  private boolean compare(VariableInstanceFunctionIncovation obj1, VariableInstanceFunctionIncovation obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1)
        || (Objects.equals(obj1.getName(), obj2.getName())
            && compareLists(obj1.getFunctionParameter(), obj2.getFunctionParameter(), this::compare)
            && compare(obj1.getIndexDef(), obj2.getIndexDef()));
  }
  
  private boolean compare(Expression2Args obj1, Expression2Args obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1)
        || (Objects.equals(obj1.getOperator(), obj2.getOperator())
            && compare(obj1.getVar1(), obj2.getVar1())
            && compare(obj1.getVar2(), obj2.getVar2()));
  }
  
  private boolean compare(LiteralExpression obj1, LiteralExpression obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1) || Objects.equals(obj1.getValue(), obj2.getValue());
  }

  private boolean compare(NotExpression obj1, NotExpression obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1) || compare(obj1.getExpression(), obj2.getExpression());
  }
  private boolean compare(SingleVarExpression obj1, SingleVarExpression obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1) || compare(obj1.getVariable(), obj2.getVariable());
  }

  private boolean compare(ExpressionVariable obj1, ExpressionVariable obj2) {
    return objCmp(obj1, obj2) && Objects.isNull(obj1) 
        || (Objects.equals(obj1.getVarNum(), obj2.getVarNum())
            && compareLists(obj1.getParts(), obj2.getParts(), this::compare)
            && compare(obj1.getIndexDef(), obj2.getIndexDef()));
  }
  
}
