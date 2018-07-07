/*
 * Copyright 2017 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
; // jshint ignore:line
define(function aknInlineArticleProfileModule(require) {
    "use strict";
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var plugins = [];
    plugins.push(require("plugins/leosInlineSave/leosInlineSavePlugin"));
    plugins.push(require("plugins/leosInlineCancel/leosInlineCancelPlugin"));
    plugins.push(require("plugins/leosInlineEditor/leosInlineEditorPlugin"));
    plugins.push(require("plugins/leosTable/leosTablePlugin"));
    plugins.push(require("plugins/aknHtmlAnchor/aknHtmlAnchorPlugin"));
    plugins.push(require("plugins/aknArticle/aknArticlePlugin"));
    plugins.push(require("plugins/aknParagraph/aknParagraphPlugin"));
    plugins.push(require("plugins/aknHtmlBold/aknHtmlBoldPlugin"));
    plugins.push(require("plugins/aknHtmlItalic/aknHtmlItalicPlugin"));
    plugins.push(require("plugins/aknHtmlUnderline/aknHtmlUnderlinePlugin"));
    plugins.push(require("plugins/aknOrderedList/aknOrderedListPlugin"));
    plugins.push(require("plugins/aknUnorderedList/aknUnorderedListPlugin"));
    plugins.push(require("plugins/aknNumberedParagraph/aknNumberedParagraphPlugin"));
    plugins.push(require("plugins/leosShowblocks/leosShowblocksPlugin"));
    plugins.push(require("plugins/aknAuthorialNote/aknAuthorialNotePlugin"));
    plugins.push(require("plugins/leosTransformer/leosTransformerPlugin"));
    plugins.push(require("plugins/leosFixNestedPs/leosFixNestedPsPlugin"));
    plugins.push(require("plugins/leosMathematicalFormula/leosMathematicalFormulaPlugin"));
    plugins.push(require("plugins/aknHtmlSuperScript/aknHtmlSuperScriptPlugin"));
    plugins.push(require("plugins/aknHtmlSubScript/aknHtmlSubScriptPlugin"));
    plugins.push(require("plugins/leosWidget/leosWidgetPlugin"));
    plugins.push(require("plugins/leosAttrHandler/leosAttrHandlerPlugin"));
    plugins.push(require("plugins/leosArticleList/leosArticleListPlugin"));
    plugins.push(require("plugins/leosArticleIndentlist/leosArticleIndentlistPlugin"));
    plugins.push(require("plugins/leosHighlight/leosHighlightPlugin"));
    plugins.push(require("plugins/leosPaste/leosPastePlugin"));
    plugins.push(require("plugins/leosComments/leosCommentsPlugin"));
    plugins.push(require("plugins/leosCommentAction/leosCommentActionPlugin"));
    plugins.push(require("plugins/leosCrossReference/leosCrossReferencePlugin"));
    plugins.push(require("plugins/leosHierarchicalElementShiftEnterHandler/leosHierarchicalElementShiftEnterHandler"));
    plugins.push(require("plugins/leosFloatingSpace/leosFloatingSpacePlugin"));
    plugins.push(require("plugins/leosMessageBus/leosMessageBusPlugin"));
    plugins.push(require("plugins/leosDropHandler/leosDropHandlerPlugin"));
    plugins.push(require("plugins/leosXmlEntities/leosXmlEntitiesPlugin"));

    var pluginNames=[];
    var specificConfig={};
    $.each(plugins, function( index, value ) {
        pluginNames.push(value.name);
        specificConfig= $.extend( specificConfig,  value.specificConfig);
    });
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    var leosPasteFilter = pluginTools.createFilterList(transformationConfigResolver);
    // holds ckEditor external plugins names
    var externalPluginsNames = [];
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");
    var leosEditorCss = pluginTools.toUrl("css/leosEditor.css");
    
    var customContentsCss = [leosEditorCss];

    var profileName = "Inline AKN Article";
    // create profile configuration
    var profileConfig = {
        // user interface language localization
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath,clipboard,undo,pastefromword,enterkey,button,dialog,dialogui,"
                + "widget,lineutils,basicstyles," + "indent,"
                + "fakeobjects,find,specialchar,table,tableresize,tabletools,tableselection,contextmenu,menubutton,mathjax,pastetext,colorbutton",
        // comma-separated list of plugins that must not be loaded
        removePlugins: "",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //only allow elements configured in transformer while paste
        pasteFilter:leosPasteFilter,
        defaultPasteElement:'paragraph/content/mp/text',
        // custom style sheet
        contentsCss: customContentsCss,
        // force Paste as plain text
        forcePasteAsPlainText: false,
        //Use native spellchecker
        disableNativeSpellChecker: false,
        // toolbar groups arrangement, optimized for a single toolbar row
        toolbarGroups : [ {
            name : "save"
        }, {
            name : "document",
            groups : [ "document", "doctools" ]
        }, {
            name : "clipboard",
            groups : [ "clipboard", "undo" ]
        }, {
            name : "editing",
            groups : [ "find", "selection", "spellchecker" ]
        }, {
            name : "forms"
        }, {
            name : "basicstyles",
            groups : [ "basicstyles", "cleanup" ]
        }, {
            name : "paragraphmode"
        }, {
            name : "paragraph",
            groups : [ "indent", "blocks", "align", "bidi" ]
        }, '/', {
            name : "insert"
        }, {
            name : "styles"
        }, {
            name : "colors"
        }, {
            name : "tools"
        }, {
            name : "others"
        }, {
            name : "mode"
        }, {
            name : "about"
        } ],
        //show toolbar on startup
        startupFocus: true,
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike,Anchor,TextColor",
        // semicolon-separated list of dialog elements that must not be rendered
        // element is a string concatenation of dialog name + colon + tab name
        removeDialogTabs: "",
        // height of the editing area
        height: 515,
        //MathJax plugin configuration - Sets the path to the MathJax library
        mathJaxLib: './webjars/MathJax/2.7.0/MathJax.js?config=default'
    };
    // adding the specific configs coming from the plugins.
    profileConfig = $.extend( profileConfig,  specificConfig);
    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver: transformationConfigResolver
    };

    return profileDefinition;
});
