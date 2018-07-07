/**
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
package eu.europa.ec.leos.web.presenter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.ArticleService;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.RulesService;
import eu.europa.ec.leos.services.content.WorkspaceService;
import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.web.UrlBuilder;
import eu.europa.ec.leos.support.xml.TransformationManager;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.EditTocRequestEvent;
import eu.europa.ec.leos.web.event.component.ReleaseAllLocksEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.DeleteArticleRequestEvent;
import eu.europa.ec.leos.web.event.view.document.DownloadXmlRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditArticleRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditMetadataRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EnterDocumentViewEvent;
import eu.europa.ec.leos.web.event.view.document.InsertArticleRequestEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.SaveArticleRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseArticleEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseMetadataEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseTocEditorEvent;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.event.window.SaveTocRequestEvent;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.RepositoryView;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class DocumentPresenter extends AbstractPresenter<DocumentView> implements LockUpdateBroadcastListener {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentPresenter.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DocumentView documentView;

    @Autowired
    private TransformationManager transformationManager;

    @Autowired
    private RulesService rulesService;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private UrlBuilder urlBuilder;

    @Autowired
    private LockHelper lockHelper;

    private String strLockId;

    private final static String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    //this is needed to reference the  object from different threads as springContext is not available from different thread.
    @PostConstruct
    private void setLocalVariables() {
        strLockId=getDocumentId();
    }

    @Subscribe
    public void enterDocumentView(EnterDocumentViewEvent event) throws IOException {

        String documentId = getDocumentId();
        if(documentId == null ){
            rejectView(RepositoryView.VIEW_ID, "document.id.missing");
            return;
        }

        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.lockDocument(documentId, user, session.getId(), LockLevel.READ_LOCK);

        if (lockActionInfo.sucesss()) {
            LeosDocument document = getDocument();
            populateViewWithDocumentDetails(document);
            lockingService.registerLockInfoBroadcastListener(this);
            documentView.updateLocks(lockActionInfo);
        } else {
            LockData lockingInfo = lockActionInfo.getCurrentLocks().get(0); 
            rejectView(RepositoryView.VIEW_ID, "document.locked", lockingInfo.getUserName(), lockingInfo.getUserLoginName(),
                    (new SimpleDateFormat(DATE_FORMAT)).format(new Date(lockingInfo.getLockingAcquiredOn())));
        }
    }

    @Override
    public void onViewLeave() {
        // cleanup 
        lockingService.unregisterLockInfoBroadcastListener(this);
        String documentId = getDocumentId();
        if(documentId!=null){
            lockingService.releaseLocksForSession(documentId, session.getId());
            session.removeAttribute(SessionAttribute.DOCUMENT_ID.name());
        }
        strLockId=null;
    }

    @Subscribe
    public void closeDocument(CloseDocumentEvent event) {
        eventBus.post(new NavigationRequestEvent(RepositoryView.VIEW_ID));
    }

    @Subscribe
    public void refreshDocument(RefreshDocumentEvent event) throws IOException {
        LeosDocument document = getDocument();
        populateViewWithDocumentDetails(document);
    }

    @Subscribe
    public void editArticle(EditArticleRequestEvent event) throws IOException {
        if (lockHelper.lockElement(event.getArticleId())) {
            LeosDocument document = getDocument();
            String article = articleService.getArticle(document, event.getArticleId());
            documentView.showArticleEditor(event.getArticleId(), article);
        }
    }

    @Subscribe
    public void saveArticle(SaveArticleRequestEvent event) throws IOException {

        if (lockHelper.isElementLockedFor(event.getArticleId())) {
            LeosDocument document = getDocument();
            document = articleService.saveArticle(document, getUserLogin(), event.getArticleContent(), event.getArticleId());
            if (document != null) {
                String articleContent  = articleService.getArticle(document, event.getArticleId());
                documentView.refreshArticleEditor(articleContent);
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void closeArticleEditor(CloseArticleEditorEvent event) throws IOException {
        if (lockHelper.isElementLockedFor(event.getArticleId())) {
            lockHelper.unlockElement(event.getArticleId());
        }
    }

    @Subscribe
    public void closeTocEditor(CloseTocEditorEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            lockHelper.unlockDocument();
        }
    }

    @Subscribe
    public void closeMetadataEditor(CloseMetadataEditorEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            lockHelper.unlockDocument();
        }
    }
    @Subscribe
    public void deleteArticle(DeleteArticleRequestEvent event) throws IOException {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            document = articleService.deleteArticle(document, getUserLogin(), event.getArticleId());

            if (document != null) {
                eventBus.post(new NotificationEvent(Type.INFO, "document.article.deleted"));
                eventBus.post(new RefreshDocumentEvent());
            }
            lockHelper.unlockDocument();
        }
    }

    @Subscribe
    public void insertArticle(InsertArticleRequestEvent event) throws IOException {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            document = articleService.insertNewArticle(document, getUserLogin(), event.getArticleId(),
                    InsertArticleRequestEvent.POSITION.BEFORE.equals(event.getPosition()));
            if (document != null) {
                eventBus.post(new NotificationEvent(Type.INFO, "document.article.inserted"));
                eventBus.post(new RefreshDocumentEvent());
            }
            lockHelper.unlockDocument();
        }
    }

    @Subscribe
    public void editToc(EditTocRequestEvent event) {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            documentView.showTocEditWindow(getTableOfContent(document), rulesService.getDefaultTableOfContentRules());
        }
    }

    @Subscribe
    public void releaseAllLocks(ReleaseAllLocksEvent event){
        //this method is invoked when force release of locks is done.
        //1. all locks for current doc are fetched from the lock service
        //2. one by one all locks are released.
        User user = leosSecurityContext.getUser();
        String documentId = getDocumentId();

        List<LockData> locks=lockingService.getLockingInfo(documentId);
        for(LockData lockData : locks){
            LockActionInfo lockActionInfo = null;

            if(lockData.getUserLoginName().equalsIgnoreCase(user.getLogin())){
                switch (lockData.getLockLevel()){
                    case READ_LOCK:
                    case DOCUMENT_LOCK:
                        lockActionInfo= lockingService.unlockDocument(documentId, user.getLogin(),lockData.getSessionId() , lockData.getLockLevel());
                        break;
                    case ELEMENT_LOCK:
                        lockActionInfo=lockingService.unlockDocument(documentId, user.getLogin(),lockData.getSessionId() , lockData.getLockLevel(), lockData.getElementId());
                        break;
                }//end switch
                if(!lockActionInfo.sucesss()){
                    lockHelper.handleLockFailure(lockActionInfo);
                }//end handle failure
            }//end   
        }//end for
    }

    @Subscribe
    public void editMetadata(EditMetadataRequestEvent event) throws IOException {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            documentView.showMetadataEditWindow(documentService.getMetaData(document));
        }
    }

    @Subscribe
    public void downloadXml(DownloadXmlRequestEvent event) {
        LeosDocument document = getDocument();
        if (document != null){
            documentView.showDownloadWindow(document, "xml.window.download.message");
        }
    }

    @Subscribe
    public void saveToc(SaveTocRequestEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            LeosDocument document = getDocument();
            document = documentService.saveTableOfContent(document, getUserLogin(), event.getTableOfContentItemVOs());

            List<TableOfContentItemVO> tableOfContent = getTableOfContent(document);
            documentView.setToc(tableOfContent);
            eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void saveMetaData(SaveMetaDataRequestEvent event) {
        if (lockHelper.isDocumentLockedFor()) {
            LeosDocument document = getDocument();
            document = documentService.updateMetaData(document, getUserLogin(), event.getMetaDataVO());

            if (document != null) {
                eventBus.post(new NotificationEvent(Type.INFO, "metadata.edit.saved"));
                eventBus.post(new RefreshDocumentEvent());
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }

    }

    @Override
    public DocumentView getView() {
        return documentView;
    }

    private String getDocumentContent(LeosDocument document) {
        return transformationManager.toEditableXml(document, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));
    }

    private LeosDocument getDocument() {
        String documentId=getDocumentId();
        LeosDocument document = null;
        if (documentId != null) {
            document =  documentService.getDocument(documentId);
        }
        return document;
    }

    private String getDocumentId() {
        String documentId =(String) session.getAttribute(SessionAttribute.DOCUMENT_ID.name());
        strLockId=documentId;//to set the id to receive lock updates
        return documentId;
    }

    private List<TableOfContentItemVO> getTableOfContent(LeosDocument leosDocument) {
        return documentService.getTableOfContent(leosDocument);
    }

    private void populateViewWithDocumentDetails(LeosDocument document) throws IOException {
        if (document != null) {
            documentView.setDocumentName(document.getName());
            documentView.refreshContent(getDocumentContent(document));
            documentView.setToc(getTableOfContent(document));
            String documentId = getDocumentId();
            documentView.setDocumentPreviewURLs(documentId,
                    urlBuilder.getDocumentPdfUrl(VaadinServletService.getCurrentServletRequest(), documentId),
                    urlBuilder.getDocumentHtmlUrl(VaadinServletService.getCurrentServletRequest(), documentId));
        }
    }

    private String getUserLogin(){
        return leosSecurityContext.getUser().getLogin();
    }

    @Override
    public void onLockUpdate(LockActionInfo lockActionInfo) {
        documentView.updateLocks(lockActionInfo);
    }

    /** to be used only for the lock update mechanism in the GUI 
     * as session is not available for async threads
     */
    @Override
    public String getLockId(){
        return strLockId;
    }
}
