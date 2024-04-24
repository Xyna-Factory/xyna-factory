<#import "/util/java.ftl" as java/>

<#-- methods of the dom

  result:
    [memberMethod]

    [memberMethod]

    ...

    [memberMethod] <eol>
-->
<#macro member_methods>
<#list dom.operations as operation>
<@java.member_method method=operation resolveTypes=true/>

</#list>
</#macro>


<#-- method declarations of the dom

  result:
    [memberMethodDecl]

    [memberMethodDecl]

    ...

    [memberMethodDecl] <eol>
-->
<#macro member_methods_decl>
<#list dom.operations as operation>
<@java.member_method_decl method=operation resolveTypes=true/>

</#list>
</#macro>