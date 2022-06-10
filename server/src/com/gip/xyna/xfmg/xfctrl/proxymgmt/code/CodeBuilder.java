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
package com.gip.xyna.xfmg.xfctrl.proxymgmt.code;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.misc.StringReplacer;



public class CodeBuilder {

  
  private CodeType type;
  private String path;
  private String name;
  private List<MethodDeclaration> methods;
  private Map<String, Type> imports;
  private List<Type> implementing;
  private List<Type> extending;
  private List<MethodDeclaration> constructors;
  private Map<String,Field> fields;
  private String comment;
    
  private enum CodeType {
    Interface, Class, Constructor, Call;
  }
  
  
  public CodeBuilder(CodeType type, String path, String name) {
    this.type = type;
    this.path = path;
    this.name = name;
    methods = new ArrayList<MethodDeclaration>();
    imports = new HashMap<String, Type>();
    implementing = new ArrayList<Type>();
    extending = new ArrayList<Type>();
    constructors = new ArrayList<MethodDeclaration>();
    fields = new HashMap<String,Field>();
  }

  public String getFqClassName() {
    return path+"."+name;
  }
  
  public static CodeBuilder newInterface(String path, String name) {
    return new CodeBuilder(CodeType.Interface,path,name);
  }

  public static CodeBuilder newClass(String path, String name) {
    return new CodeBuilder(CodeType.Class,path,name);
  }
  
  public CodeBuilder implementing(String path , String name) {
    return implementing( new GenericType(path, name) );
  }
  
  public CodeBuilder implementing(Type type) {
    addImport(type);
    implementing.add(type);
    return this;
  }
  
  
  public CodeBuilder extending(Type type) {
    addImport(type);
    extending.add(type);
    return this;
  }

  public CodeBuilder comment(String comment) {
    this.comment = comment;
    return this;
  }
  
  public MethodDeclaration constructor() {
    MethodDeclaration md = new MethodDeclaration(name, CodeType.Constructor);
    md.modifier(Modifier.PUBLIC);
    this.constructors.add(md);
    return md;
  }


  
  public MethodDeclaration declareMethod(Method m) {
    MethodDeclaration md = new MethodDeclaration(m, type);
    this.methods.add(md);
    
    /*
    imports.add(m.getReturnType());
    imports.addAll( Arrays.asList( m.getParameterTypes() ) );
    imports.addAll( Arrays.asList( m.getExceptionTypes() ) );
    */
    return md;
  }
  
  public MethodDeclaration declareMethod(String name) {
    MethodDeclaration md = new MethodDeclaration(name, type);
    this.methods.add(md);
    return md;
  }
  
  


  public String toCode() {
    StringBuilder sb = new StringBuilder();
    sb.append("package "+path+";\n");
    sb.append("\n");
    
    appendImports(sb);
    sb.append("\n");
    appendDeclaration(sb);
    
    if( fields.size() > 0 ) {
      sb.append("\n");
      for( Field field : fields.values() ) {
        field.appendTo(sb);
      }
    }
    
    for( MethodDeclaration md : constructors ) {
      sb.append("\n");
      md.appendTo(sb);
    }
    
    for( MethodDeclaration md : methods ) {
      sb.append("\n");
      md.appendTo(sb);
    }
   
    sb.append("\n}\n");
    return sb.toString();
  }
 
  /* FIXME
  private void appendImports(StringBuilder sb) {
    //alle verwendeten Klassen sammeln
    HashSet<Class<?>> imps = new HashSet<Class<?>>();
    for( Type t : imports.values() ) {
      imps.addAll( TypeUtils.getAllUsedClasses(t) );
    }
    //nicht zu importierende Klassen filtern und Namen sammeln
    List<String> list = new ArrayList<String>();
    for( Class<?> c : imps ) {
      if( c.isPrimitive() ) {
        continue;
      }
      if( c.getPackage().getName().equals("java.lang") ) {
        continue;
      }
      list.add( c.getCanonicalName() );
    }
    //sortiert ausgeben
    Collections.sort(list);
    for( String imp : list ) {
      sb.append("import "+imp+";\n");
    }
  }*/
  private void appendImports(StringBuilder sb) {
    //alle verwendeten Klassen sammeln
    HashSet<Type> imps = new HashSet<Type>();
    for( Type t : imports.values() ) {
      imps.addAll( TypeUtils.getAllUsedTypes(t) );
    }
    //nicht zu importierende Klassen filtern und Namen sammeln
    List<String> list = new ArrayList<String>();
    list = CollectionUtils.transformAndSkipNull(imps, TypeUtils.importFilter);
    
    //sortiert ausgeben
    Collections.sort(list);
    for( String imp : list ) {
      sb.append("import "+imp+";\n");
    }
  }
  private CodeBuilder addImport(Type type) {
    imports.put(type.toString(), type);
    return this;
  }

  
  
  public Field field(Type type, String name) {
    Field field = fields.get(name);
    if( field == null ) {
      addImport(type);
      field = new Field(type, name);
      fields.put( name, field);
    }
    return field;
  }
  
  private void addFieldIfNew(Type type, String name) {
    if( ! fields.containsKey(name) ) {
      field(type, name);
    }
  }



  private void appendDeclaration(StringBuilder sb) {
    if( comment != null ) {
      if( comment.indexOf('\n') > 0 ) {
        String indentedComment = comment;
        if( comment.endsWith("\n") ) {
          indentedComment = comment.substring(0, comment.length()-1);
        }
        indentedComment = indentedComment.replace("\n", "\n  * ");
        sb.append(" /*\n  * ").append(indentedComment).append("\n  */\n");
      } else {
        sb.append("  //").append(comment).append("\n");
      }
    }

    sb.append("public ").append(type.toString().toLowerCase()).append(" ").append(name);
    String sep = " extends ";
    for( Type type : extending ) {
      sb.append(sep).append( TypeUtils.getName(type) );
      sep = ", ";
    }
    sep = " implements ";
    for( Type type : implementing ) {
      sb.append(sep).append( TypeUtils.getName(type) );
      sep = ", ";
    }
    sb.append(" { \n");
    
  }

 


  public class MethodDeclaration extends CodePart {

    private String name;
    private Type returnType;
    private List<Parameter> parameter = new ArrayList<Parameter>();
    private List<Class<?>> throwTypes = new ArrayList<Class<?>>();
    private List<Annotation> annotations = new ArrayList<Annotation>();
    private CodeBlock code;
    private String comment;
    private int modifiers;
    private CodeType codeType;
    
    public MethodDeclaration(String name, CodeType codeType) {
      super(null);
      this.codeType = codeType;
      this.name = name;
    }
    

    public MethodDeclaration(Method m) {
      this( m, CodeType.Call );
    }
    
    public MethodDeclaration(Method m, CodeType codeType) {
      this( m.getName(), codeType);
      this.name = m.getName();
      this.returnType = m.getGenericReturnType();
      addImport(returnType);
      
      this.modifiers = ModifierUtils.methodModifier(m.getModifiers());
      
      if( Modifier.isAbstract(this.modifiers)) {
        this.modifiers &= ~Modifier.ABSTRACT; //soll nicht abstrakt sein TODO raus?
      }
      Type[] params = m.getGenericParameterTypes();
   
      if( ! Modifier.isTransient(m.getModifiers()) ) {
        for( int i=0; i<params.length; ++i ) {
          addParameter("arg"+i, params[i]);
        }
      } else {
        //letzter Parameter ist ein varargs
        for( int i=0; i<params.length-1; ++i ) {
          addParameter("arg"+i, params[i]);
        }
        int last = params.length-1;
        if( last >= 0 ) {
          addVarargsParameter("arg"+last, params[last]);
        }
      }
      
      for( Class<?> t : m.getExceptionTypes() ) {
        addThrow(t);
      }
    }
    
    

    public MethodDeclaration modifier(int ... mods) {  //TODO wie entfernen? toggeln, removeModifier?
      this.modifiers = ModifierUtils.add( this.modifiers, mods);
      return this;
    }

    private void addParameter(String name, Type p) {
      addImport(p);
      parameter.add( new Parameter( name, p) );
    }
    private void addVarargsParameter(String name, Type p) {
      addImport(p);
      parameter.add( new Parameter( name, TypeUtils.removeArray(p), true) );
    }
    
    private void addThrow(Class<?> t) {
      addImport(t);
      throwTypes.add(t);
    }
    
    public MethodDeclaration parameter(String name, Type type ) {
      addParameter(name, type);
      return this;
    }
    
    public MethodDeclaration renameParameter(String ... parameterNames) {
      int max = Math.min( parameterNames.length, parameter.size() );
      for( int i=0; i<max; ++i) {
        parameter.get(i).rename(parameterNames[i]);
      }
      return this;
    }
    
    public MethodDeclaration returning(Type returnType) {
      this.returnType = returnType;
      addImport(returnType);
      return this;
    }
    
    public MethodDeclaration throwing(Class<?> t) {
      if( t != null ) {
        addThrow(t);
      }
      return this;
    }
    
    public MethodDeclaration comment(String comment) {
      if( this.comment == null ) {
        this.comment = comment;
      } else {
        this.comment = this.comment+"\n"+indentation()+"//"+comment; //FIXME mehrzeilig
      }
      return this;
    }
    
    public CodeBlock implementedBy() {
      if( this.code == null ) {
        this.code = new CodeBlock("Impl method "+name, this);
      }
      return this.code;
    }
    
    @Override
    protected String indentation() {
      return "  ";
    }
    
    protected void appendTo(StringBuilder sb) {
      appendComment(sb);
      for( Annotation annotation : annotations ) {
        annotation.appendTo(sb);
      }
      appendMethodDeclaration(sb, codeType);
      if( type == CodeType.Interface ){
        sb.append(";\n");
      } else {
        sb.append(" {\n");
        if( code != null ) {
          code.appendTo(sb);
        }
        sb.append(indentation()).append("}\n");
      }
    }
    
    private void appendComment(StringBuilder sb) {
      if( comment == null ) {
        return;
      }
      sb.append("  //").append(comment).append("\n"); //FIXME mehrzeilig
    }

    private void appendMethodDeclaration(StringBuilder sb, CodeType type) {
      sb.append(indentation());
      ModifierUtils.appendTo(sb, modifiers);
      if( type != CodeType.Constructor ) {
        TypeUtils.appendGenericTypeVariableTo( sb, returnType );
        TypeUtils.appendTo( sb, returnType );
        sb.append(" ");
      }
      sb.append( name ).append("(");
      String sep = "";
      for( Parameter param : parameter ) {
        sb.append(sep);
        param.appendTo(sb);
        sep = ", ";
      }
      
      sb.append(")");
      sep = " throws ";
      for( Class<?> e : throwTypes ) {
        sb.append(sep).append(e.getSimpleName());
        sep = ", ";
      }
    }


    public boolean isReturning() {
      return returnType != null && returnType != void.class;
    }

    public List<Parameter> getParameter() {
      return parameter;
    }

    public String getName() {
      return name;
    }

    @Override
    public Statement call(MethodDeclaration method) {
      return null; //FIXME 
    }


    public Annotation annotate(Class<?> annotation) {
      addImport(annotation);
      Annotation anno = new Annotation(this,annotation);
      annotations.add(anno);
      return anno;
    }


    public Type getReturnType() {
      return returnType;
    }


  }
  
  public static class Parameter {
    
    private String name;
    private Type type;
    private boolean varargs;
    
    public Parameter(String name, Type type) {
      this.name = name;
      this.type = type;
      this.varargs = false;
    }
    
    public Parameter(String name, Type type, boolean varargs) {
      this.name = name;
      this.type = type;
      this.varargs = varargs;
    }
    
    public <E extends Enum<E>> Parameter(Enum<E> enumValue) {
      this.name = enumValue.getClass().getSimpleName()+"."+enumValue.name();
      this.type = enumValue.getClass();
    }
    
    public void appendTo(StringBuilder sb) {
      TypeUtils.appendTo(sb, type );
      if( varargs ) {
        sb.append(" ...");
      }
      sb.append(" ").append(name);
    }
    public void rename(String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
    public Class<?> getTypeClass() {
      return TypeUtils.getClass(type);
    }
    
  }

  
  public abstract class CodePart {

    protected CodePart parent;
    
    protected CodePart(CodePart parent) {
      this.parent = parent;
    }
    
    protected String indentation() {
      return "";
    }
    
    protected abstract void appendTo(StringBuilder sb);

    public abstract Statement call(MethodDeclaration method);
    
    public CodeBlock catching(Class<?> throwable, String name) {
      while ( ! ( parent instanceof FlowControl) ) {
        parent = parent.parent;
      }
      return ((FlowControl)parent).catches(throwable,name);
    }
  }
  
  public class CodeBlock extends CodePart {
    
    private final String name;
    
    protected CodeBlock(String name, CodePart parent) {
      super(parent);
      this.name = name;
      indentation = parent.indentation() +"  ";
    }

    private List<CodePart> parts = new ArrayList<CodePart>();
    private String indentation;
    
    public String toString() {
      return "CodeBlock("+name+")";
    }
    
    protected void appendTo(StringBuilder sb) {
      for( CodePart part : parts ) {
        sb.append(indentation);
        part.appendTo(sb);
      }
    }

    public Statement returning(String value) {
      return newStatement("return "+value);
    }
    
    public Statement returning() {
      return newStatement("return ");
    }
    
    public Statement throwing(String value) {
      return newStatement("throw ").code(value);
    }
    
    
    private Statement newStatement(String code) {
      Statement s = new Statement(this, code);
      parts.add(s);
      return s;
    }

    public Assignment assign(Type type, String name, Object to) {
      addImport(type);
      Assignment a = new Assignment(this, type,name);
      parts.add(a);
      a.value(to); 
      return a;
    }
    
    public Assignment assign(Type type, String name) {
      addImport(type);
      Assignment a = new Assignment(this, type,name);
      parts.add(a);
      return a;
    }

    public Assignment assignField(Type type, String name) {
      addFieldIfNew(type, name);
      Assignment a = new Assignment(this, null,name);
      parts.add(a);
      return a;
    }

    public Statement call(MethodDeclaration md) {
      return newStatement("").call(md);
    }
    
    public Statement on(String on) {
      return newStatement(on+".");
    }

    public Statement code(String code) {
      return newStatement(code);
    }

    @Override
    protected String indentation() {
      return indentation;
    }
    
    public CodeBlock trying() {
      FlowControl fc = new FlowControl(this, "try {");
      parts.add( fc );
      return fc.codeBlock("try");
    }

    public Comment comment(String comment) {
      Comment c = new Comment(this, comment);
      parts.add(c);
      return c;
    }

  }
  
  private class Comment extends CodePart {

    private String comment;

    protected Comment(CodePart parent, String comment) {
      super(parent);
      this.comment = comment;
    }

    @Override
    protected void appendTo(StringBuilder sb) {
      sb.append("//").append(comment).append("\n");
    }

    @Override
    public Statement call(MethodDeclaration method) {
      // FIXME raus
      return null;
    }
    
  }
  
  private class Simple extends CodePart {

    private String code;
    
    public Simple(CodePart parent, String code) {
      super(parent);
      this.code = code;
    }

    @Override
    protected void appendTo(StringBuilder sb) {
      sb.append(code);
    }

    @Override
    public Statement call(MethodDeclaration method) {
      return null;//FIXME CodePart auftrennen in sichtbar/unsichtbar?
    }
    
  }
  
  private class FlowControl extends CodePart {

    private List<CodePart> parts;
    
    
    public FlowControl(CodePart parent, String code) {
      super(parent);
      this.parts = new ArrayList<CodePart>();
      parts.add( new Simple(this, code+"\n") );
    }

    @Override
    protected String indentation() {
      return parent.indentation();
    }
    
    public CodeBlock catches(Class<?> throwable, String name) {
      parts.add( new Simple(this, indentation()+"} catch( "+throwable.getSimpleName()+" "+name+" ) {\n") );
      return codeBlock("catch");
    }

    public CodeBlock codeBlock(String name) {
      CodeBlock cb = new CodeBlock(name, this);
      parts.add( cb);
      return cb;
    }

    @Override
    protected void appendTo(StringBuilder sb) {
      for( CodePart part : parts ) {
        part.appendTo(sb);
      }
      sb.append(indentation()).append("}\n");
    }

    @Override
    public Statement call(MethodDeclaration method) {
      //FIXME CodePart auftrennen in sichtbar/unsichtbar?
       return null;
    }
    
  }
  
  public class Annotation extends CodePart {

    private Class<?> annotation;

    protected Annotation(CodePart parent, Class<?> annotation) {
      super(parent);
      this.annotation = annotation;
    }

    @Override
    protected void appendTo(StringBuilder sb) {
      sb.append(parent.indentation()).append("@");
      sb.append(annotation.getSimpleName());
      sb.append("\n");
    }

    @Override
    public Statement call(MethodDeclaration method) {
      //FIXME raus
      return null;
    }
    
  }
  
  public class Statement extends CodePart {

    protected StringBuilder statement;
    private String indentation;
    
    public Statement(CodePart parent, String code) {
      super(parent);
      this.statement = new StringBuilder(code);
    }

    @Override
    protected String indentation() {
      if( indentation == null ) {
        indentation = parent.indentation()+"    ";
      }
      return indentation;
    }
    
    public Statement code(String code) {
      statement.append(code);
      return this; 
    }

    public String toString() {
      return "Statement("+statement+")";
    }
    
    protected void appendTo(StringBuilder sb) {
      sb.append(statement.toString()).append(";\n");
    }
    
    public Statement on( String on) {
      statement.append(on).append(".");
      return this;
    }

    public Statement call(Call call) {
      call.appendTo(statement);
      return this;
    }
    
    public Statement call(MethodDeclaration md) {
      statement.append(md.name).append("(");
      String sep ="";
      for( Parameter param : md.parameter ) {
        statement.append(sep).append(param.getName());
        sep =", ";
      }
      statement.append(")");
      return this;
    }
    
    public Statement call(MethodDeclaration md, Object ... parameter) {
      Call c = new Call(md.name).parameter(parameter);
      c.appendTo(statement);
      return this;
    }
    
    public Statement call(MethodDeclaration md, List<Parameter> parameter) {
      //TODO Call verwenden
      statement.append(md.name).append("(");
      String sep ="";
      for( Parameter param : parameter ) {
        statement.append(sep).append(param.getName());
        sep =", ";
      }
      statement.append(")");
      return this;
    }

    public Statement call(String method, Object ... parameter) {
      Call c = new Call(method).parameter(parameter);
      c.appendTo(statement);
      return this;
    }
    
    public Statement chain() {
      statement.append(".");
      return this;
    }

    public Statement chainNextLine() {
      statement.append(".\n");
      statement.append(indentation());
      return this;
    }


  }

  public class Assignment extends Statement {

    private Call call;

    public Assignment(CodePart parent, Type type, String name) {
      this(parent, type, name, true);
    }
    
    public Assignment(CodePart parent, Type type, String name, boolean to) {
      super(parent, "");
      if( type != null ) {
        TypeUtils.appendTo(statement, type);
        statement.append(" ");
      }
      statement.append(name);
      if( to ) {
        statement.append(" = ");
      }
    }
    
    public String toString() {
      return "Assignment("+statement+")";
    }
    
    public void value(Object value) {
      if( value instanceof String ) {
        statement.append("\"").append(value).append("\"");
      } else if( value == null ) {
        statement.append("null");
      } else {
        statement.append(value);
      }
    }
    
    public Call newInstance(Type type, Constructor<?> constructor, Object ... params) {
      addImport(type); 
      //TODO type.raw == constructor.getDeclaringClass()
      statement.append("new ");
      StringBuilder sb = new StringBuilder();
      TypeUtils.appendTo(sb, type);
      this.call = new Call(sb.toString() );
      call.parameter(params);
      return call;
    }
    
    public Call newInstance(Constructor<?> constructor, Object ... params) {
      Class<?> cl = constructor.getDeclaringClass();
      addImport(cl);
      statement.append("new ");
      this.call = new Call(cl.getSimpleName());
      call.parameter(params);
      return call;
    }
    
    protected void appendTo(StringBuilder sb) {
      sb.append(statement.toString());
      if( call != null ) {
        call.appendTo(sb);
      } 
      sb.append(";\n");
    }

    public Assignment cast(Type type) {
      addImport(type);
      statement.append("(").append(TypeUtils.getName(type)).append(")");
      return this;
    }

  }

  public static class Call {

    private String on;
    private String name;
    private List<CallParameter> parameter = new ArrayList<CallParameter>();
    
    public Call(String name) {
      this.name = name;
    }
    public Call(String on, String name) {
      this.on = on;
      this.name = name;
    }
    
    public Call parameter(Object ... params) {
      for( Object param : params ) {
        if( param instanceof String ) {
          add( new StringCallParameter((String)param) );
        } else {
          add( new ObjectCallParameter(param) );
        }
      }
      return this;
    }

    private void add(CallParameter callParameter) {
      parameter.add(callParameter);
    }

    protected void appendTo(StringBuilder sb) {
      if( on != null ) {
        sb.append(on).append(".");
      }
      sb.append(name).append("(");
      String sep = "";
      for( CallParameter cp : parameter ) {
        sb.append(sep);
        cp.appendTo(sb);
        sep = ", ";
      }
      sb.append(")");
    }

    public Call parameterCode(String code) {
      add( new ObjectCallParameter(code) ); //TODO CodeCallParameter?
      return this;
    }

    public Call parameterCall(Call call) {
      add( new CallCallParameter(call) );
      return this;
    }
    public Call parameterCall(MethodDeclaration method) {
      return parameterCall(call(method));
    }
    
    private static interface CallParameter {
      public void appendTo(StringBuilder sb);
    }
    private static class StringCallParameter implements CallParameter {
      private String param;
      public StringCallParameter(String param) {
        this.param = param;
      }
      public void appendTo(StringBuilder sb) {
        sb.append("\"").append(toStringLiteral(param)).append("\"");
      }
    }
    private static class ObjectCallParameter implements CallParameter {
      private Object param;
      public ObjectCallParameter(Object param) {
        this.param = param;
      }
      public void appendTo(StringBuilder sb) {
        sb.append(param);
      }
    }
    private static class CallCallParameter implements CallParameter {
      private Call call;
      public CallCallParameter(Call call) {
        this.call = call;
      }
      public void appendTo(StringBuilder sb) {
        call.appendTo(sb);
      }
    }
    public static Call staticCall(CodeBuilder cb, Method method) {
      cb.addImport(method.getDeclaringClass());
      return new Call( TypeUtils.getName(method.getDeclaringClass()), method.getName() );
    }
    public static Call call(MethodDeclaration method) {
      return new Call(method.getName() );
    }
    public static Call call(String method) {
      return new Call(method);
    }
    
    
    
  }
  
  public static class GenericType implements Type {

    private final Class<?> raw;
    private final Type[] typeArguments;
    private final String path;
    private final String name;

    public GenericType(Class<?> raw, Type ... typeArguments) {
      this.raw = raw;
      this.typeArguments = typeArguments;
      this.path = raw.getPackage().getName();
      this.name = raw.getSimpleName();
    }
    
    public GenericType(String path, String name, Type ... typeArguments) {
      this.raw = tryGetClass(path+"."+name);
      this.typeArguments = typeArguments;
      this.path = path;
      this.name = name;
    }

    private Class<?> tryGetClass(String string) {
      // TODO Auto-generated method stub
      return null;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      TypeUtils.appendTo(sb, this);
      return sb.toString();
    }
    
    public Type[] getTypeArguments() {
      return typeArguments;
    }

    public Class<?> getRawType() {
      return raw;
    }

    public String getSimpleName() {
      return name;
    }

    public String getCanonicalName() {
      if( raw != null ) {
        return raw.getCanonicalName();
      } else {
        return path+"."+name;
      }
    }
    
  }
  
  public class Field {

    private int modifiers = Modifier.PRIVATE;
    private Assignment assigment;
    
    public Field(Type type, String name) {
      this.assigment = new Assignment(null, type, name, false);
    }

    public void appendTo(StringBuilder sb) {
      sb.append("  ");
      ModifierUtils.appendTo(sb, modifiers);
      assigment.appendTo(sb);
    }

    public Field modifier(int ... mods ) {
      modifiers = ModifierUtils.add(modifiers, mods);
      return this;
    }

    public Assignment assign() {
      assigment.code(" = ");
      return assigment;
    }
    
  }
  
  private static class ModifierUtils {

    private static final int METHOD_MODIFIERS = //ab Java7 Modifier.methodModifiers();
        Modifier.PUBLIC         | Modifier.PROTECTED    | Modifier.PRIVATE |
        Modifier.ABSTRACT       | Modifier.STATIC       | Modifier.FINAL   |
        Modifier.SYNCHRONIZED   | Modifier.NATIVE       | Modifier.STRICT;

    
    public static int add(int modifiers, int[] mods) {
      for( int m : mods ) {
        modifiers |= m;
      }
      return modifiers;
    }

    public static int methodModifier(int modifiers) {
      
      return modifiers & METHOD_MODIFIERS; //ab Java7 Modifier.methodModifiers();
    }

    public static void appendTo(StringBuilder sb, int modifiers) {
      String mod = Modifier.toString(modifiers);
      if( ! mod.isEmpty() ) {
        sb.append(mod).append(" ");
      }
    }
    
  }

  private static class TypeUtils {
    
    public static Transformation<Type, String> importFilter = new Transformation<Type, String>() {

      @Override
      public String transform(Type from) {
        if( from instanceof Class ) {
          Class<?> cls = (Class<?>)from;
          if( cls.isPrimitive() ) {
            return null;
          }
          if( cls.getPackage().getName().equals("java.lang") ) {
            return null;
          }
          return cls.getCanonicalName();
        } else if( from instanceof GenericType ) {
          return ((GenericType)from).getCanonicalName();
        }
        return null;
      }
    };
    
    public static Class<?> getClass(Type type) {
      if( type instanceof Class ) {
        return (Class<?>)type;
      } else if( type instanceof ParameterizedType ) {
        return getClass( ((ParameterizedType)type).getRawType() );
      } else if( type instanceof GenericType ) {
        return ((GenericType)type).getRawType();
      } else {
        return null; //TODO
      }
    }

    public static Type removeArray(Type type) {
      if( type instanceof Class ) {
        Class<?> c = (Class<?>)type;
        if( c.isArray() ) {
          return c.getComponentType();
        } else {
          return c;
        }
      } else {
        throw new UnsupportedOperationException();
      }
    }

    public static void appendGenericTypeVariableTo(StringBuilder sb, Type type) {
      HashSet<TypeVariable<?>> typeVars = new HashSet<TypeVariable<?>>();
      getTypeVarsRecursive( typeVars, type );
      if( ! typeVars.isEmpty() ) {
        String sep = "<";
        for( TypeVariable<?> t : typeVars ) {
          sb.append(sep);
          sb.append( t.getName() );
          String sep2 = " extends ";
          for( Type b : t.getBounds() ) {
            sb.append(sep2);
            appendTo(sb, b);
            sep2 = " & ";
          }
          sep = ", ";
        }
        sb.append("> ");
      }
    }

    private static void getTypeVarsRecursive(HashSet<TypeVariable<?>> typeVars, Type type) {
      if( type instanceof ParameterizedType ) {
        ParameterizedType pt = (ParameterizedType)type;
        for( Type t : pt.getActualTypeArguments() ) {
          getTypeVarsRecursive(typeVars, t);
        }
      } else if( type instanceof TypeVariable ) {
        typeVars.add( (TypeVariable<?>)type );
      }
    }

    public static String getName(Type type) {
      if( type instanceof Class ) {
        return ((Class<?>)type).getSimpleName();
      } else if( type instanceof ParameterizedType ) {
        return getClass( ((ParameterizedType)type).getRawType() ).getSimpleName();
      } else if( type instanceof GenericType ) {
        return ((GenericType)type).getSimpleName();
      } else {
        return null; //TODO
      }
    }

    public static void appendTo(StringBuilder sb, Type type) {
      if( type instanceof Class ) {
        sb.append( ((Class<?>)type).getSimpleName() );
      } else if( type instanceof ParameterizedType ) {
        ParameterizedType pt = (ParameterizedType)type;
        appendTo(sb, pt.getRawType() );
        String sep = "<";
        for( Type t : pt.getActualTypeArguments() ) {
          sb.append(sep);
          appendTo(sb, t);
          sep = ", ";
        }
        sb.append(">");
      } else if( type instanceof GenericType ) {
        GenericType gt = (GenericType)type;
        sb.append( gt.getSimpleName() );
        String sep = "<";
        for( Type t : gt.getTypeArguments() ) {
          sb.append(sep);
          appendTo(sb, t);
          sep = ", ";
        }
        sb.append(">");
      } else if( type instanceof WildcardType ) {
        sb.append(type);
      } else if( type instanceof TypeVariable ) {
        sb.append(type);
      } else {
        sb.append("???" + type.getClass() +" -> " + type); //TODO
      }
    }
    
    public static Collection<? extends Type> getAllUsedTypes(Type type) {
      //als Type ist nur Class und GenericType erlaubt
      List<Type> list = new ArrayList<Type>();
      if( type instanceof Class ) {
        Class<?> c = (Class<?>)type;
        if( c.isArray() ) {
          list.add(c.getComponentType());
        } else {
          list.add(c);
        }
      } else {
        getAllUsedTypesRecursively( list, new HashSet<String>(), type);
      }
      return list;
    }
    
    private static void getAllUsedTypesRecursively(List<Type> list, HashSet<String> alreadyFound, Type type) {
      String name = type.toString();
      if( alreadyFound.contains(name) ) {
        return;
      }
      alreadyFound.add(name);
      if( type instanceof Class ) {
        Class<?> c = (Class<?>)type;
        if( c.isArray() ) {
          list.add(c.getComponentType());
        } else {
          list.add(c);
        }
      } else if( type instanceof ParameterizedType ) {
        ParameterizedType pt = (ParameterizedType) type;
        getAllUsedTypesRecursively(list, alreadyFound, pt.getRawType() );
        for( Type t: pt.getActualTypeArguments() ) {
          getAllUsedTypesRecursively(list, alreadyFound, t);
        }
      } else if( type instanceof GenericType ) {
        GenericType gt = (GenericType) type;
        if( gt.getRawType() != null ) {
          list.add( gt.getRawType() );
        } else {
          list.add( gt );
        }
        for( Type t: gt.getTypeArguments() ) {
          getAllUsedTypesRecursively(list, alreadyFound, t);
        }
      } else if( type instanceof WildcardType ) {
        //WildcardType wt = (WildcardType)type;
      } else if( type instanceof TypeVariable ) {
        TypeVariable<?> tv = (TypeVariable<?>)type;
        for( Type b : tv.getBounds() ) {
          getAllUsedTypesRecursively(list, alreadyFound, b);
        }
      } else {
        throw new UnsupportedOperationException(type.getClass() +" "+ type +" is not supported");
      }
    }

  }

  
  public static final StringReplacer TO_LITERAL = StringReplacer.
      replace('\\', "\\\\").
      replace('\t', "\\t").
      replace('\b', "\\b").
      replace('\n', "\\n").
      replace('\r', "\\r").
      replace('\f', "\\f").
      replace('"', "\\\"").
      build();
   
  public static String toStringLiteral(String string) {
    return TO_LITERAL.replace(string); 
  }


}
