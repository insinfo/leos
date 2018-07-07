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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.extension.*;
import eu.europa.ec.leos.ui.model.VersionType;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@StyleSheet({"vaadin://../assets/css/bill.css" + LeosCacheToken.TOKEN})
//css not present in VAADIN folder so not published by server. Anything not published need to be served  by static servlet.
public class LegalTextComponent extends CustomComponent implements ContentPane {
    private static final long serialVersionUID = -7268025691934327898L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextComponent.class);
    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private LeosDisplayField docContent;
    private Label versionLabel;
    private Button textRefreshNote;
    private Registration shortcutSearchReplace;
    
    @SuppressWarnings("serial")
    public LegalTextComponent(final EventBus eventBus, final MessageHelper messageHelper) {

        this.eventBus = eventBus;
        this.messageHelper = messageHelper;

        setSizeFull();
        VerticalLayout legalTextlayout = new VerticalLayout();
        legalTextlayout.setSizeFull();
        // create toolbar
        legalTextlayout.addComponent(buildLegalTextToolbar());
        legalTextlayout.setSpacing(false);
        legalTextlayout.setMargin(false);
        // create content
        final Component textContent = buildLegalTextContent();
        legalTextlayout.addComponent(textContent);
        legalTextlayout.setExpandRatio(textContent, 1.0f);
        setCompositionRoot(legalTextlayout);
    }

    @Override
    public void attach() {
        super.attach();
    }
    
    @Override
    public void detach() {
        super.detach();
        removeReplaceAllShortcutListener();
    }
    
    private Component buildLegalTextToolbar() {
        LOG.debug("Building Legal Text toolbar...");

        // create text toolbar layout
        final HorizontalLayout toolsLayout = new HorizontalLayout();
        toolsLayout.setId("legalTextToolbar");
        toolsLayout.setStyleName("leos-viewdoc-docbar");
        toolsLayout.setSpacing(true);

        // set toolbar style
        toolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);
        toolsLayout.addStyleName("leos-viewdoc-padbar-both");

        // create legal text slider button
        final Button legalTextRightSlideButton = legalTextRightSliderButton();
        toolsLayout.addComponent(legalTextRightSlideButton);
        toolsLayout.setComponentAlignment(legalTextRightSlideButton, Alignment.MIDDLE_LEFT);

        final Button timeLineButton = buildTimeLineButton();
        toolsLayout.addComponent(timeLineButton);
        toolsLayout.setComponentAlignment(timeLineButton, Alignment.MIDDLE_LEFT);

        //create legal text major version button
        final Button legalTextMajorVersionButton = buildLegalTextMajorVersionButton();
        toolsLayout.addComponent(legalTextMajorVersionButton);
        toolsLayout.setComponentAlignment(legalTextMajorVersionButton, Alignment.MIDDLE_LEFT);
                
        // create oj importer button        
        final Button legalTextImporterButton = buildLegalTextImporterButton();
        toolsLayout.addComponent(legalTextImporterButton);
        toolsLayout.setComponentAlignment(legalTextImporterButton, Alignment.MIDDLE_LEFT);
        
        // create legal text slider button
        final Button legalTextLeftSlideButton = legalTextLeftSliderButton();

        // create toolbar spacer label to push components to the sides
        versionLabel = new Label();
        versionLabel.setSizeUndefined();
        versionLabel.setContentMode(ContentMode.HTML);
        toolsLayout.addComponent(versionLabel);
        toolsLayout.setComponentAlignment(versionLabel, Alignment.MIDDLE_CENTER);
        // toolbar spacer label will use all available space
        toolsLayout.setExpandRatio(versionLabel, 1.0f);

        // create blank Note Button
        textRefreshNote = legalTextNote();
        toolsLayout.addComponent(textRefreshNote);
        toolsLayout.setComponentAlignment(textRefreshNote, Alignment.MIDDLE_RIGHT);

        // create guidance button
        final Button guidanceButton = userGuidanceButton();
        toolsLayout.addComponent(guidanceButton);
        toolsLayout.setComponentAlignment(guidanceButton, Alignment.MIDDLE_RIGHT);

        // create comments button
        final Button commentsButton = legalTextCommentsButton();
        toolsLayout.addComponent(commentsButton);
        toolsLayout.setComponentAlignment(commentsButton, Alignment.MIDDLE_RIGHT);

        //add search and replace popup view
        toolsLayout.addComponent(buildSearchAndReplacePopup());
        
        // create text refresh button
        final Button textRefreshButton = legalTextRefreshButton();
        toolsLayout.addComponent(textRefreshButton);
        toolsLayout.setComponentAlignment(textRefreshButton, Alignment.MIDDLE_RIGHT);

        // add toc expand button last
        toolsLayout.addComponent(legalTextLeftSlideButton);
        toolsLayout.setComponentAlignment(legalTextLeftSlideButton, Alignment.MIDDLE_RIGHT);

        return toolsLayout;
    }

    // create legal text slider button
    private Button legalTextLeftSliderButton() {
        VaadinIcons legalTextSliderIcon = VaadinIcons.CARET_SQUARE_LEFT_O;

        final Button legalTextSliderButton = new Button();
        legalTextSliderButton.setIcon(legalTextSliderIcon);
        legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.LEFT);
        legalTextSliderButton.setStyleName("link");
        legalTextSliderButton.addStyleName("leos-toolbar-button");
        legalTextSliderButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), LegalTextComponent.this));
            }
        });

        return legalTextSliderButton;
    }

    // create legal text slider button
    private Button legalTextRightSliderButton() {
        VaadinIcons legalTextSliderIcon = VaadinIcons.CARET_SQUARE_RIGHT_O;

        final Button legalTextSliderButton = new Button();
        legalTextSliderButton.setIcon(legalTextSliderIcon);
        legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.RIGHT);
        legalTextSliderButton.setStyleName("link");
        legalTextSliderButton.addStyleName("leos-toolbar-button");
        legalTextSliderButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), LegalTextComponent.this));
            }
        });

        return legalTextSliderButton;
    }

    // create text refresh button
    private Button legalTextRefreshButton() {
        final Button textRefreshButton = new Button();
        textRefreshButton.setId("refreshDocument");
        textRefreshButton.setCaptionAsHtml(true);
        textRefreshButton.setIcon(VaadinIcons.REFRESH);
        textRefreshButton.setStyleName("link");
        textRefreshButton.addStyleName("leos-dimmed-button");
        textRefreshButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 3714441703159576377L;

            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
            }
        });

        return textRefreshButton;
    }

    // create comments button
    private Button userGuidanceButton() {
        Button userGuidanceButton = new Button();
        userGuidanceButton.setData(false); // user Guidance is true in start
        userGuidanceButton.setDescription(messageHelper.getMessage("leos.button.tooltip.show.guidance"));
        userGuidanceButton.addStyleName("link leos-toolbar-button");
        userGuidanceButton.setIcon(VaadinIcons.QUESTION_CIRCLE_O);

        userGuidanceButton.addClickListener(event -> {
            Button button = event.getButton();
            boolean targetState = !(boolean) button.getData();
            eventBus.post(new UserGuidanceRequest(targetState));
            button.setData(targetState);
            button.setIcon(targetState
                    ? VaadinIcons.QUESTION_CIRCLE // enabled
                    : VaadinIcons.QUESTION_CIRCLE_O); // disabled
            button.setDescription(targetState
                    ? messageHelper.getMessage("leos.button.tooltip.hide.guidance") // enabled
                    : messageHelper.getMessage("leos.button.tooltip.show.guidance")); // disabled
        });
        return userGuidanceButton;
    }

    // create comments button
    private Button legalTextCommentsButton() {
        final Button commentsButton = new Button();
        commentsButton.setId("documentComments");
        commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.show"));
        commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS_O);
        commentsButton.setStyleName("link");
        commentsButton.setData(false);
        commentsButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                commentsButton.setData(! (Boolean)commentsButton.getData());
                if((Boolean)commentsButton.getData()) {
                    commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.hide"));
                    commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS);
                }
                else{
                    commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.show"));
                    commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS_O);
                }
                eventBus.post(new LegalTextCommentToggleEvent());
            }
        });

        return commentsButton;
    }

    // create text refresh button
    private Button legalTextNote() {
        final Button textRefreshNote = new Button();
        textRefreshNote.setId("refreshDocumentNote");
        textRefreshNote.setCaptionAsHtml(true);
        textRefreshNote.setStyleName("link");
        textRefreshNote.addStyleName("leos-toolbar-button");
        textRefreshNote.setCaption(messageHelper.getMessage("document.request.refresh.msg"));
        textRefreshNote.setIcon(LeosTheme.LEOS_INFO_YELLOW_16);
        textRefreshNote.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 3714441703159576377L;

            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
            }
        });
        return textRefreshNote;
    }

    private Button buildTimeLineButton() {
        // create version diff button
        final Button diffButton = new Button();
        diffButton.setId("compareDocVersionButton");
        diffButton.setDescription(messageHelper.getMessage("document.tab.legal.button.timeline.tooltip"));
        diffButton.setIcon(VaadinIcons.COPY_O);
        diffButton.setStyleName("link");
        diffButton.addStyleName("leos-toolbar-button");
        diffButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent clickEvent) {
                LOG.debug("Time line Button clicked...");
                eventBus.post(new ShowTimeLineWindowEvent());
            }
        });

        return diffButton;
    }

    private Button buildLegalTextMajorVersionButton() {
        final Button majorVersionButton = new Button();
        majorVersionButton.setId("majorVersion");
        majorVersionButton.setDescription(messageHelper.getMessage("document.major.version.button.tooltip"));
        majorVersionButton.setCaptionAsHtml(true);
        majorVersionButton.setIcon(VaadinIcons.BOOKMARK);
        majorVersionButton.setStyleName("link");
        majorVersionButton.addStyleName("leos-dimmed-button");
        majorVersionButton.addClickListener(event -> eventBus.post(new ShowMajorVersionWindowEvent()));
        return majorVersionButton;
    }

    private Button buildLegalTextImporterButton() {
        final Button importerButton = new Button();
        importerButton.setDescription(messageHelper.getMessage("document.tab.legal.button.oj.importer.tooltip"));
        importerButton.setCaptionAsHtml(true);
        importerButton.setIcon(VaadinIcons.ARROW_CIRCLE_DOWN);
        importerButton.setStyleName("link");
        importerButton.addStyleName("leos-dimmed-button");
        importerButton.addClickListener(event -> eventBus.post(new ShowImportWindowEvent()));
        return importerButton;
    }

    private SearchAndReplaceComponent buildSearchAndReplacePopup() {
        SearchAndReplaceComponent searchAndReplaceComponent = new SearchAndReplaceComponent(messageHelper, eventBus);
        shortcutSearchReplace = searchAndReplaceComponent.addShortcutListener(new ShortcutListener("ReplaceAll", KeyCode.R, new int[]{ShortcutAction.ModifierKey.CTRL}) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                searchAndReplaceComponent.setPopupVisible(true);
            }
        });
        return searchAndReplaceComponent;
    }
    
    private void removeReplaceAllShortcutListener() {
        LOG.debug("Removing shortcutListener from search and replace popup...");
        shortcutSearchReplace.remove();
    }
    
    private Component buildLegalTextContent() {
        LOG.debug("Building Legal Text content...");

        // create placeholder to display Legal Text content
        docContent = new LeosDisplayField();
        docContent.setSizeFull();
        docContent.setStyleName("leos-doc-content");

        // create content extensions
        new MathJaxExtension<>(docContent);
        new SliderPinsExtension<>(docContent, getSelectorStyleMap());
        new LeosEditorExtension<>(docContent, eventBus);
        new LegalTextCommentsExtension<>(docContent, eventBus);
        new ActionManagerExtension<>(docContent);
        new UserGuidanceExtension<>(docContent, eventBus);
        new RefToLinkExtension<>(docContent);
        return docContent;
    }
    private Map<String, String> getSelectorStyleMap(){
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put("popup","pin-popup-side-bar");
        return selectorStyleMap;
    }
    
    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        String versionType = versionInfoVO.isMajor() ? VersionType.MAJOR.getVersionType() : 
                                    VersionType.MINOR.getVersionType();
        this.versionLabel.setValue(messageHelper.getMessage("document.version.caption", versionInfoVO.getDocumentVersion(), versionType, versionInfoVO.getLastModifiedBy(), versionInfoVO.getDg(), versionInfoVO.getLastModificationInstant()));
    }

    public void populateContent(String docContentText) {
        /* KLUGE: In order to force the update of the docContent on the client side
         * the unique seed is added on every docContent update, please note markDirty
         * method did not work, this was the only solution worked.*/
        String seed = "<div style='display:none' >" +
                new Date().getTime() +
                "</div>";
        docContent.setValue(docContentText + seed);

        docContent.addStyleName(LeosCategory.BILL.name().toLowerCase());

        textRefreshNote.setVisible(false);
    }

    @Override
    public float getDefaultPaneWidth(int numberOfFeatures) {
        float featureWidth=0f;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            default:
                featureWidth = 50f;
                break;
        }//end switch
        return featureWidth;
    }
}
