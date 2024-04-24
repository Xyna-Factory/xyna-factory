<#import "/util/java.ftl" as java/>


<#-- variable declaration for the target member

  result:
    private [var]; <inline>

  example:
    public Text asText()
-->
<#macro target_var>
private <@java.variable var=targetVariable resolveTypes=true/>;
</#macro>


<#-- member variables of the datatype excluding the target member, which is handled separately

    result:
      [memberVar]

      [memberVar]

      ...

      [memberVar] <eol>
-->
<#macro non_target_vars>
<#list domOrException.memberVars as var>
<#if var!=targetVariable>
<@java.member_var var=var resolveTypes=true/>
<#--  linebreak  -->

</#if>
</#list>
</#macro>