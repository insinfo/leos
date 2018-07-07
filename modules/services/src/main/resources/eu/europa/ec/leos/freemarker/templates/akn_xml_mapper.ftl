<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://docs.oasis-open.org/legaldocml/ns/akn/3.0",
                   "leos":"urn:eu:europa:ec:leos"}>

<#--
    Copyright 2017 European Commission

    Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->

<#-- Hash of mapped Akoma Ntoso XML elements where:
       Key = Akoma Ntoso element name
     Value = Mapped element name
-->
<#assign aknMapped={
    'body':'aknBody',
    'title':'aknTitle',
    'p':'aknP',
    'GUID':'id'
}>

<#assign authorialNoteList = []>

<#-- Sequence of ignored Akoma Ntoso XML elements -->
<#assign aknIgnored=[
    'meta'
]>


<#macro akomaNtoso>
<akomaNtoso id="akomaNtoso">
    <#recurse/>
</akomaNtoso>
</#macro>
<#-----------------------------------------------------------------------------
 AKN xml tags. Necessary while using 'fallback' in other templates
------------------------------------------------------------------------------>
<#macro blockContainer>
    <@@element/>
</#macro>

<#macro recitals>
    <@@element/>
</#macro>

<#macro recital>
    <@@element/>
</#macro>

<#macro citations>
    <@@element/>
</#macro>

<#macro article>
    <@@element/>
</#macro>

<#macro clause>
    <@@element/>
</#macro>

<#-----------------------------------------------------------------------------
 Below handlers decide how pages are generated
------------------------------------------------------------------------------>
<#macro bill>
    <@generateWithPages .node/>
</#macro>

<#macro doc>
    <@generateWithPages .node/>
</#macro>

<#macro generateWithPages node>
<${.node?node_name}${xmlFtl.handleAttributes(.node.@@)?no_esc}><#t>
    <#if .node.coverPage??>
        <#visit .node.coverPage><#t>
    </#if>
    <div class="page" id="contentPageWrapperId"><#t>
        <#list .node?children as child>
            <#if (child?node_name != 'coverPage')>
                <#visit child><#t>
            </#if>
        </#list>
        <@printAuthorialNotes/>
    </div><#t>
</${.node?node_name}><#t>
</#macro>

<#-----------------------------------------------------------------------------
 AKN cover page specific handlers
------------------------------------------------------------------------------>
<#macro coverPage>
<div class="page" id="coverPageWrapperId">
    <coverPage${handleAttributes(.node.@@)?no_esc}>
        <#recurse>
    </coverPage>
</div>
</#macro>

<#macro container>
    <#local language = (.node["@name"][0]!'') == 'language'>
    <#if (language)>
    <container id="${.node.@GUID[0]!}" name="language" data-lang="${.node.p}"/>
    <#else>
        <@@element/>
    </#if>
</#macro>
<#-----------------------------------------------------------------------------
 AKN authorial note handler 
------------------------------------------------------------------------------>
<#macro authorialNote>
	<#assign authorialNoteList = authorialNoteList + [.node]>
    <#local marker=.node.@marker[0]!'*'>
    <#local noteText=.node.@@text?trim>
    <#local noteId=.node.@GUID[0]!''>
    <#if (noteId?length gt 0)>
        <authorialNote id="${noteId}" data-tooltip="${noteText}" onClick="LEOS.scrollTo('endNote_${noteId}')">${marker}</authorialNote><#t>
    <#else>
    	<authorialNote data-tooltip="${noteText}">${marker}</authorialNote><#t>
    </#if>
</#macro>

<#-----------------------------------------------------------------------------
Cross Reference handler
------------------------------------------------------------------------------>
<#macro ref>
    <#local refId=.node.@href[0]!''>
    <ref ${.node.@@attributes_markup?no_esc} onClick="LEOS.scrollTo('${refId}')"><#recurse></ref><#t>
</#macro>

<#-- AKN end-of-line handler -->
<#macro eol>
<br/>
</#macro>

<#-- print the footnotes in document -->
<#macro printAuthorialNotes>
    <#list authorialNoteList>
        <span id="leos-authnote-table-id" class="leos-authnote-table">
            <hr size="2"/>
        <#items as authNote>
            <#local noteMarker=authNote.@marker[0]!'*'>
            <#local noteText=authNote.@@text?trim>
            <#local noteId=authNote.@GUID[0]!''>
            <#if (noteId?length gt 0)>
                <span id="endNote_${noteId}" class="leos-authnote" onClick="LEOS.scrollTo('${noteId}')">
                    <marker id="marker_${noteId}">${noteMarker}</marker>
                    <text id="text_${noteId}">${noteText}</text>
                </span>
            <#else>
                <span class="leos-authnote">
                    <marker>${noteMarker}</marker>
                    <text>${noteText}</text>
                </span>
            </#if>
        </#items>
        </span>
    </#list>
</#macro>

<#-----------------------------------------------------------------------------
    Default handlers for XML nodes
------------------------------------------------------------------------------>
<#-- default handler for element nodes -->
<#macro @element>
    <#local nodeName=.node?node_name>
    <#if (!aknIgnored?seq_contains(nodeName))>
        <#local nodeTag=aknMapped[nodeName]!nodeName>
        <${nodeTag}${handleAttributes(.node.@@)?no_esc}><#recurse></${nodeTag}><#t>
    </#if>
</#macro>

<#-- default handler for text nodes -->
<#macro @text>
    <#if .node?trim?length gt 0>
        ${.node}<#t>
    </#if>
</#macro>

<#-----------------------------------------------------------------------------
    Common function to generate updated attributes for XML nodes
------------------------------------------------------------------------------>
<#function handleAttributes attrList auto_esc=false>
    <#assign str = ''>
    <#if (attrList?size gt 0)>
        <#list attrList as attr>
            <#local attrName=aknMapped[attr.@@qname]!attr.@@qname>
            <#assign str += ' ${attrName}="${attr}"'>
        </#list>
    </#if>
    <#return str>
</#function>