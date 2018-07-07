/*
 * Copyright 2015 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
﻿; // jshint ignore:line
define(function leosCommentsPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");

    var dialogName = "insertCommentsDialog";
    var widgetName = "leosCommentsWidget";
    var pluginName = "leosComments";

    var leosCommentsWidgetDefinition = require("./leosCommentsWidget");

    var pluginDefinition = {
        icons: 'leosComment',
        requires: "widget,dialog",
        init: function(editor) {
            pluginTools.addDialog(dialogName, initializeDialog);
            editor.widgets.add(widgetName, leosCommentsWidgetDefinition);
            addButton('LeosComment', 'back', 'Insert Comments', 20, widgetName);

            function addButton(name, type, title, order, cmdName) {
                editor.ui.add(name, CKEDITOR.UI_BUTTON, {
                    label: title,
                    title: title,
                    toolbar: 'insert,' + order,
                    command: cmdName
                });
            };
        }
    };

    function initializeDialog(editor) {
        var dialogDefinition = {
            title: "Insert Custom Comments",
            minWidth: 400,
            minHeight: 100,
            contents: [{
                id: "info",
                elements: [{
                    id: "comment",
                    type: "text",
                    label: "Enter your comments here:",

                    setup: function setup(widget) {
                        // set the dialog value to the value from widget comment attribute
                        this.setValue(widget.data.commentdata);
                    },
                    commit: function commit(widget) {
                        // update comment value by data introduce by user in dialog
                        widget.setData("commentdata", this.getValue().trim());
                    }
                }]
            }]
        };
        return dialogDefinition;
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn: "popup",
        html: 'span[data-akn-name=popup]',
        attr: [{
            akn: "id",
            html: "id"
        }, {
            html: "class=leoscomments hint--top hint--bounce hint--rounded"
        }, {
            html: "data-akn-name=popup"
        }, {
            akn: "refersto=~leosComment",
        }, {
            akn: "leos:userid",
            html: "leos:userid"
        }, {
            akn: "leos:username",
            html: "leos:username"
        }, {
            akn: "leos:datetime",
            html: "leos:datetime"
        }],
        sub: {
            akn: "mp",
            html: "span/img",
            sub: [{
                akn: 'text',
                html: 'span/img[commentdata]'
            }]
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };
    return pluginModule;

});
