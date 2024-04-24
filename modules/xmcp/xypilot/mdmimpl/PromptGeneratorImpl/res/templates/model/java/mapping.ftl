<#import "/util/api-reference.ftl" as api/>
<#import "/util/functions.ftl" as fun/>
<#import "/util/java.ftl" as java/>


<#--  Documentation of the mapping including the label and documentation (if any)

  result:
    // [label]
    // [documentation]

  example:
    // To Subnet
    // Maps an IPv4 address and a prefix length to a subnet.
-->
<#macro doc>
<#if mapping.label?has_content>
// ${mapping.label}
</#if>
<#if mapping.documentation?has_content>
// ${mapping.documentation}
</#if>
</#macro>


<#-- Documentation block of the mapping, empty if no documentation is available

  result:
    /**
     * [documentation]
     */

  example:
    /**
     * Maps an IPv4 address and a prefix length to a subnet.
     */
-->
<#macro doc_block>
<@java.documentation_block mapping.documentation/>
</#macro>


<#-- Declares all variables in the specified list in a mapping context.
  The variables include the type and the name obta

  params:
    variables: List of variables to declare

  result:
    [type] [name];
    ...
    [type] [name]; <eol>

  example:
    Text ip_Address;
    Integer prefix_Length;
-->
<#macro vars variables>
<#list variables as var>
<@java.variable var=var allowCollections=false/>;
</#list>
</#macro>


<#-- Declares all input variables of the mapping

  result:
    [type] [name];
    ...
    [type] [name]; <eol>

  example:
    Text ip_Address;
    Integer prefix_Length;
-->
<#macro input_vars>
<@vars variables=mapping.inputVars/>
</#macro>


<#-- Declares all input parameters of a mapping function

  result:
    [type] [name], ..., [type] [name]

  example:
    Text ip_Address, Integer prefix_Length
-->
<#macro input_params>
<@java.parameters params=mapping.inputVars allowCollections=false/>
</#macro>


<#-- Declares all output variables of the mapping

  result:
    [type] [name];
    ...
    [type] [name]; <eol>

  example:
    IPv4Subnet iPv4_Subnet;
-->
<#macro output_vars>
<@vars variables=mapping.outputVars/>
</#macro>


<#-- Outputs an inital (partial) assignment for the mapping of the form "output.var[0]" if possible.
This is used if no expressions are available to help the ai model to generate at least one assignment.

  params:
    output: Prefix for the output variables, empty if no prefix is needed

  result:
    [output].[var0]

  example:
    out.text

-->
<#macro initial_assignment output="out">
<@compress single_line=true>
<#if mapping.outputVars?size != 0>
  <#if output?has_content>
    ${output}.${mapping.outputVars[0].varName}
  <#else>
    ${mapping.outputVars[0].varName}
  </#if>
</#if>
</@compress>
</#macro>


<#--  Mapping assignments, i.e. expressions of the mapping in java code
  If no expressions are available, returns an initial assignment of the form "output.var0" if possible.

  params:
    input: Prefix for the input variables, empty if no prefix is needed
    output: Prefix for the output variables, empty if no prefix is needed

  result:
    [expression];
    [expression];
    ...
    [expression] <eol>

  example:
    out.text = in.text.toText();
    out.prefixLength = in.prefixLength;
    out.address =
-->
<#macro assignments input="in" output="out">
<#if mapping.rawExpressions?size == 0>
  <@initial_assignment output/>
<#else>
<#list mapping.rawExpressions as expression>
${utils.turnMappingExpressionToJava(expression, mapping, input, output)}<#sep>;</#sep>
</#list>
</#if>
</#macro>


<#-- API reference for a single datatype (DomOrExceptionGenerationBase) in a mapping context.
  This is different to other api references, e.g. exposing public members instead of getters and setters

  params:
    datatype: Datatype to create API reference for
    static: Whether the class should be static, e.g. when the reference is packed into a class

  result:
    ([documentation])
    (static) [classHeader] {
      [variable];
      ...
      [variable];

      [methodHeader];
      ...
      [methodHeader];
    } <eol>

  example:
    // Represents a IPv4 address using a 32-bit integer.
    static class IPv4Address {
      int address;

      Text asText(); // Returns a textual representation of the address in the format "a.b.c.d"
    }
-->
<#macro datatype_ref datatype static=false>
<@api.class_header_ref datatype static/> {
<#list datatype.memberVars as memberVar>
  <@java.variable var=memberVar allowCollections=false/>;
</#list>
<#if datatype.operations?has_content>

<#list datatype.operations as operation>
  <@java.method_header_types_only method=operation allowCollections=false/>;
</#list>
</#if>
}
</#macro>


<#-- API reference for a single datatype (DomOrExceptionGenerationBase) in a mapping context.
  This is different to other api references, e.g. exposing public members instead of getters and setters
  The light version does not include the method headers.

  params:
    datatype: Datatype to create API reference for

  result:
    [classHeader] {
      [variable];
      ...
      [variable];
    } <eol>

  example:
    // Represents a IPv4 address using a 32-bit integer.
    static class IPv4Address {
      int address;

      Text asText(); // Returns a textual representation of the address in the format "a.b.c.d"
    }
-->
<#macro datatype_ref_light datatype>
<@java.class_header datatype resolveTypes/> {<#nt/>
<#list datatype.memberVars as memberVar>
  <@java.variable var=memberVar allowCollections=false/>;
</#list>
}
</#macro>


<#-- API reference for dependencies of a mapping.
This is different to the default API references, e.g. exposing public members instead of getters and setters
  params:
    static: true if the reference classes should be static

  result:
    [datatypeRef]
    ...
    [datatypeRef] <eol>
-->
<#macro api_ref static=false>
<#--  "dependencies" are part of the MappingModel and include the inputs and output types  -->
<#list dependencies as dependency>
<@datatype_ref dependency static/>
<#--  linebreak  -->

</#list>
</#macro>


<#-- API reference for dependencies of a mapping.
  This is different to the default API references, e.g. exposing public members instead of getters and setters
  The light version does not include the method headers or comments.


  result:
    [datatypeRef]
    ...
    [datatypeRef] <eol>
-->
<#macro api_ref_light>
<#--  "dependencies" are part of the MappingModel and include the inputs and output types  -->
<#list dependencies as dependency>
<@datatype_ref_light dependency/>
<#--  linebreak  -->

</#list>
</#macro>


<#-- Declaration of available builtin functions for the mapping to use -->
<#macro builtins_decl>
// Util-Methods:

// Appends the item to the given list
<T> T[] append(T[] arr, T item);

// Concatenates the given Strings
String concat(String ...strings);

// Concatenates the given lists
<T> T[] concatlists(T[] arr1, T[] arr2);

// Returns the index of the first occurrence of the query in the searchString
Integer indexof(String searchString, String query);

// Returns length of list
Integer length(Object[] arr);

// Returns length of String
Integer length(String str);

// Replaces all occurrences of search with replacement in the given string
String replaceall(String text, String search, String replacement);

// Checks whether a query is contained in a string
Boolean contains(String text, String query);

// Checks whether a text starts with a given prefix
Boolean startswith(String text, String prefix);

// Checks whether a text ends with a given suffix
Boolean endswith(String text, String suffix);

// Converts String to lowercase
String tolowercase(String str);

// Converts String to uppercase
String touppercase(String str);
</#macro>

<#-- API reference of available builtin functions for the mapping to use -->
<#macro builtins_ref>
// Util-Methods:

// Appends the item to the given list
// <T> T[] append(T[] arr, T item)

// Concatenates the given Strings
// String concat(String ...strings)

// Concatenates the given lists
// <T> T[] concatlists(T[] arr1, T[] arr2)

// Returns the index of the first occurrence of the query in the searchString
// Integer indexof(String searchString, String query)

// Returns length of list
// Integer length(Object[] arr)

// Returns length of String
// Integer length(String str)

// Replaces all occurrences of search with replacement in the given string
// String replaceall(String text, String search, String replacement)

// Checks whether a query is contained in a string
// Boolean contains(String text, String query)

// Checks whether a text starts with a given prefix
// Boolean startswith(String text, String prefix)

// Checks whether a text ends with a given suffix
// Boolean endswith(String text, String suffix)

// Converts String to lowercase
// String tolowercase(String str)

// Converts String to uppercase
// String touppercase(String str)
</#macro>