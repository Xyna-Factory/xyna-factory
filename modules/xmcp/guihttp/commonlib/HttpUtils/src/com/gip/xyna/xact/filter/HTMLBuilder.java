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
package com.gip.xyna.xact.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.filter.HTMLBuilder.CodeBuilder.Line;

/**
 * HTMLBuilder vereinfacht das Bauen einfacher HTML-Seiten. 
 * 
 * Der Aufruf ist wie folgt:
 * <pre>
 *   HTMLBuilder html = new HTMLBuilder();
 *   html.title("Titel");
 *   HTMLPart body = html.body();
 *   
 *   body.heading(3, "�berschrift);
 *   body.paragraph().append("eine Zeile).lineBreak();
 *   
 *   TableBuilder tb = body.table().border(1);
 *   tb.header("Spalte1", "Spalte2");
 *   tb.row("a", "b");
 *   tb.row("c", "d");
 *   
 *   FormBuilder fb = body.form("Formular").method("get").action("form");
 *     fb.hidden("type", "TYPE" );
 *     fb.submit("absenden");
 *   }
 *   
 *   return html.toHTML();
 * </pre>
 *
 */
public class HTMLBuilder {
  
  private HTMLPart body;
  private HTMLPart head;
  
  public HTMLBuilder() {
    body = new HTMLPart("body");
    head = new HTMLPart("head");
    head.closingTag("meta").attribute("charset", "utf-8");
  }
  
  public HTMLBuilder(String title) {
    this();
    head.simpleTag("title", title);
  }

  public void title(String title) {
    head.simpleTag("title", title);
  }

  public HTMLPart body() {
    return body;
  }
  
  public HTMLPart head() {
    return head;
  }
  
  public String toHTML() {
    StringBuilder sb = new StringBuilder();
    CodeBuilder cb = new CodeBuilder(sb,"");
    cb.line("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
    cb.line("<html>");
    head.appendTo(cb, "  ");
    body.appendTo(cb, "  ");
    cb.line("</html>");
    
    return sb.toString();
  }
  
  public static class HTMLPart {
    
    private String tag;
    private StringBuilder attributes;
    private StringBuilder content;
    private List<HTMLPart> parts;
    private boolean noEnd= false;
    
    
    public HTMLPart() {
    }
    
    
 

    public HTMLPart(String tag) {
      this.tag = tag;
    }
    
    public HTMLPart(String tag, boolean noEnd) {
      this.tag = tag;
      this.noEnd = noEnd;
    }
    
    public void add(HTMLPart part) {
      if( parts == null ) {
        parts = new ArrayList<HTMLPart>();
      }
      parts.add(part);
    }
    
    public void appendTo(CodeBuilder cb, String indent) {
      if( tag == null ) {
        cb.appendLine(indent).append(content.toString()).end();
        return;
      }
      Line line = cb.appendLine(indent).append("<").append(tag);
      if( attributes != null ) {
        line.append(attributes.toString());
      }
      if( noEnd ) {
        line.append("/>");
        line.end();
        return;
      }

      line.append(">");
      if( content != null ) {
        line.append(content.toString());
      }

      if( parts != null ) {
        line.end();
        String nextIndent = indent+"  ";
        for( HTMLPart part : parts ) {
          part.appendTo(cb, nextIndent);
        }
        line = cb.appendLine(indent);
      }
      if( !noEnd ) {
        line.append("</").append(tag).append(">");
      }
      line.end();
    }
    
    public HTMLPart append(Object o) {
      if( content == null ) {
        content = new StringBuilder();
      }
      content.append(o);
      return this;
    }
    
    public HTMLPart append(String string) {
      if( content == null ) {
        content = new StringBuilder();
      }
      content.append(string);
      return this;
    }
    
    public HTMLPart attribute(String name, String value) {
      if( attributes == null ) {
        attributes = new StringBuilder();
      }
      attributes.append(" ").append(name).append("=\"").append(value).append("\"");
      return this;
    }

    public HTMLPart paragraph() {
      HTMLPart p = new HTMLPart("p");
      add(p);
      return p;
    }
    
    public HTMLPart paragraph(String style) {
      HTMLPart p = new HTMLPart("p");
      add(p);
      HTMLPart s = new HTMLPart(style);
      p.add(s);
      return s;
    }


    public FormBuilder form(String name) { //method, String name, String action) {
      FormBuilder form = new FormBuilder(name);
      add(form);
      return form;
    }
   
    public TableBuilder table() {
      TableBuilder table = new TableBuilder();
      add(table);
      return table;
    }
    
    public ScriptBuilder script(String type) {
      ScriptBuilder script = new ScriptBuilder(type);
      add(script);
      return script;
    }

    public void heading(int level, String heading) {
      simpleTag("h"+level, heading);
    }

    public HTMLPart line(String line) {
      HTMLPart l = new HTMLPart();
      l.append(line);
      add(l);
      return l;
    }

    public HTMLPart link(String target, String name) {
      return simpleTag("a", name).attribute("href", target );
    }

    public void lineBreak() {
      line("<br />\n");
    }

    public HTMLPart simpleTag(String tag, String content) {
      HTMLPart p = new HTMLPart(tag);
      add(p);
      if( content != null ) {
        p.append(content);
      }
      return p;
    }
    
    public HTMLPart closingTag(String tag) {
      HTMLPart p = new HTMLPart(tag, true);
      add(p);
      return p;
    }

    public HTMLPart css(String css) {
      HTMLPart p = new HTMLPart("link", true);
      p.attribute("rel", "stylesheet");
      p.attribute("href", css);
      add(p);
      return p;
    }

  }
  
  public static class FormBuilder extends HTMLPart {
    
    public FormBuilder(String name) {
      super("form");
      attribute("name",name);
    }
    
    public FormBuilder method(String method) {
      attribute("method",method);
      return this;
    }
    
    public FormBuilder action(String action) {
      attribute("action",action);
      return this;
    }

    public InputBuilder hidden(String name, String value) {
      InputBuilder input = new InputBuilder(value).type("hidden").name(name);
      add( input );
      return input;
    }
    
    public InputBuilder submit(String value) {
      InputBuilder input = new InputBuilder(value).type("submit");
      add(input);
      return input;
    }
    
    public InputBuilder button(String value) {
      InputBuilder input = new InputBuilder(value).type("button");
      add(input);
      return input;
    }

 }
  
  public static class InputBuilder extends HTMLPart {
    
    public InputBuilder(String value) {
      super("input", true);
       attribute("value",value);
    }
    
    public InputBuilder type(String type) {
      attribute("type", type);
      return this;
    }

    public InputBuilder name(String name) {
      attribute("name", name);
      return this;
    }

    public InputBuilder onClick(String onClick) {
      attribute("onClick", onClick);
      return this;
    }

    public InputBuilder title(String title) {
      attribute("title", title);
      return this;
    }
    
  }
  
  public static class ScriptBuilder extends HTMLPart {
    
    public ScriptBuilder(String type) {
      super("script");
      attribute("type",type);
    }
    
  }
  
  public static class TableBuilder extends HTMLPart {

    
    private HTMLPart currentRow;

    public TableBuilder() {
      super("table");
    }
    
    public TableBuilder border(int border) {
      attribute("border", String.valueOf(border));
      return this;
    }

    public TableBuilder header(String ... header) {
      HTMLPart tr = new HTMLPart("tr");
      add(tr);
      for( String h : header ) {
        tr.simpleTag("th", h);
      }
      return this;
    }

    public TableBuilder row(String ... cols) {
      currentRow = new HTMLPart("tr");
      add(currentRow);
      for( String h : cols ) {
        currentRow.simpleTag("td", h == null ? "" : h );
      }
      return this;
    }

    public TableBuilder column(String col) {
      currentRow.simpleTag("td", col );
      return this;
    }
    
    public HTMLPart column() {
      HTMLPart col = new HTMLPart("td");
      currentRow.add(col);
      return col;
    }
    
  }
  public static class CodeBuilder {

    private StringBuilder sb;
    private String indentation;
    private Line line;

    public CodeBuilder(StringBuilder sb, String indentation) {
      this.sb = sb;
      this.indentation = indentation;
      this.line = new Line(sb);
    }
    
    public void line(String line) {
      sb.append(indentation).append(line).append("\n");
    }

    public void line() {
      sb.append(indentation).append("\n");
    }
    

    public Line appendLine(String string) {
      sb.append(indentation).append(string);
      return line;
    }

    public static class Line {
      
      private StringBuilder sb;

      public Line(StringBuilder sb) {
        this.sb = sb;
      }

      public Line append(String string) {
        sb.append(string);
        return this;
      }
      
      public Line appendOptional(boolean append, String string) {
        if( append ) {
          sb.append(string);
        }
        return this;
      }

      public void end() {
        sb.append("\n");
      }
      
    }
  }
  public static void append(StringBuilder sb, HTMLPart part, String indentation) {
    CodeBuilder cb = new CodeBuilder(sb, "");
    for( HTMLPart p : part.parts ) {
      p.appendTo(cb, indentation + "  " );
    }
    
    //part.appendTo(cb, indentation);
  }
  
  
  @SuppressWarnings("deprecation")
  public static String urlencode(String string) {
    try {
      return URLEncoder.encode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return URLEncoder.encode(string);
    }
  }
  
  @SuppressWarnings("deprecation")
  public static String urldecode(String string ) {
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return URLDecoder.decode(string);
    }
   
  }

}

