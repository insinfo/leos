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
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.MenuBar;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.PaneAddEvent;
import eu.europa.ec.leos.web.event.view.PaneEnableEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MenuBarComponent extends MenuBar {

    private static final long serialVersionUID = -1111730147519706141L;

    private static final Logger LOG = LoggerFactory.getLogger(MenuBarComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;

    private MenuItem settingsItem;
    private MenuBar.MenuItem tocOffItem;
    private MenuBar.MenuItem changesOffItem;

    @Autowired
    public MenuBarComponent(final MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        buildViewMenubar();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    private void buildViewMenubar() {
        LOG.debug("Building view menubar...");

        addStyleName("leos-document-layout");

        // create default not implemented action for menu items
        final Command notImplementedCmd = new Command() {
            private static final long serialVersionUID = 3775227587732968858L;

            @Override
            public void menuSelected(MenuBar.MenuItem menuItem) {
                LOG.debug("Menu item clicked ({})...", menuItem.getText());
                eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "leos.not.implemented", "Menu action: " + menuItem.getText()));
            }
        };

        // create menu items
        MenuItem closeDocumentItem = addItem(messageHelper.getMessage("menu.close.caption"),
                                    LeosTheme.LEOS_DOCVIEW_CLOSE,
                                    new CloseDocumentCommand());
        closeDocumentItem.setDescription(messageHelper.getMessage("menu.close.tooltip"));

        settingsItem = addItem("", LeosTheme.LEOS_MENU_SETTINGS_ICON_16, null);
        settingsItem.setDescription(messageHelper.getMessage("menu.settings.tooltip"));
    }


    @Subscribe
    void addItemToMenu(PaneAddEvent event) {
        LOG.debug("Add meuItem to Menu Bar for ({})...", event.getPaneClass().getTypeName());
        if (event.getPaneClass() == TableOfContentComponent.class) {
            tocOffItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.toc.on.caption"),
                    LeosTheme.LEOS_TOC_ON_ICON_16,
                    new TocLayoutMenuCommand(ColumnPosition.OFF));
            tocOffItem.setVisible(true);
            tocOffItem.setCheckable(true);
        }
        else if (event.getPaneClass() == MarkedTextComponent.class) {
            changesOffItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.changes.on.caption"),
                    LeosTheme.LEOS_CHANGES_ON_ICON_16,
                    new MarkedPaneLayoutMenuCommand(ColumnPosition.OFF));
            changesOffItem.setCheckable(true);
            changesOffItem.setVisible(true);
        }
    }

    @Subscribe
    void changePaneStatus(PaneEnableEvent event) {
        LOG.debug("Changing pane status in Menu Bar ({})...", event.getPaneClass().getTypeName());
        if (tocOffItem != null && event.getPaneClass() == TableOfContentComponent.class) {
            tocOffItem.setChecked(event.isEnabled());
        }
        if (changesOffItem != null && event.getPaneClass() == MarkedTextComponent.class) {
            changesOffItem.setChecked(event.isEnabled());
        }
    }

    // Create Layout for select menu commands for TOC
    private class TocLayoutMenuCommand implements MenuBar.Command {
        private static final long serialVersionUID = -4455740778411909392L;

        private ColumnPosition position;

        public TocLayoutMenuCommand(ColumnPosition position) {
            this.position = position;
        }

        @Override
        public void menuSelected(MenuBar.MenuItem menuItem) {
            LOG.debug("Toc Layout menu item clicked ({}:{})...", position, menuItem.isChecked());
            if (menuItem.isEnabled() && menuItem.isCheckable()) {
                position = menuItem.isChecked() ? ColumnPosition.LAST : ColumnPosition.OFF;
                LOG.debug("Changing layout settings (TOC={})...", position);
                eventBus.post(new LayoutChangeRequestEvent(position, TableOfContentComponent.class));
            }
        }
    }

    // Create Layout for select menu commands for TOC
    private class MarkedPaneLayoutMenuCommand implements MenuBar.Command {

        private ColumnPosition position;

        public MarkedPaneLayoutMenuCommand(ColumnPosition position) {
            this.position = position;
        }

        @Override
        public void menuSelected(MenuBar.MenuItem menuItem) {
            LOG.debug("Chanegs Layout menu item clicked ({}:{})...", position, menuItem.isChecked());
            if (menuItem.isEnabled() && menuItem.isCheckable()) {
                position = menuItem.isChecked() ? ColumnPosition.FIRST: ColumnPosition.OFF;//New Position
                LOG.debug("Changing layout settings (ChangedPane={})...", position);
                eventBus.post(new LayoutChangeRequestEvent(position,MarkedTextComponent.class ));
            }
        }
    }

    private class CloseDocumentCommand implements Command {

        private static final long serialVersionUID = -2486576157838091413L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.trace("Close document menu clicked...");
            eventBus.post(new CloseScreenRequestEvent());
        }
    }
}
