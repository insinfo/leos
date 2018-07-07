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

import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.CommentService;
import eu.europa.ec.leos.services.content.DocumentService;
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
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.feedback.*;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.view.FeedbackView;
import eu.europa.ec.leos.web.view.RepositoryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class FeedbackPresenter extends AbstractPresenter<FeedbackView> implements LockUpdateBroadcastListener {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackPresenter.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private FeedbackView feedbackView;

    @Autowired
    private TransformationManager transformationManager;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private UrlBuilder urlBuilder;

    private String strLockId;

    private final static String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    //this is needed to reference the  object from different threads as springContext is not available from different thread.
    @PostConstruct
    private void setLocalVariables() {
        strLockId=getDocumentId();
    }

    @Subscribe
    public void enterFeedbackView(EnterFeedbackViewEvent event) throws IOException {
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
            feedbackView.updateLocks(lockActionInfo);
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
    public void getExistingComments(LoadExistingCommentsEvent event) throws IOException {
        feedbackView.setData(commentService.getAllComments(getDocument()));
    }

    @Subscribe
    public void insertComments(InsertCommentEvent event) throws IOException {
        commentService.insertNewComment(getDocument(),event.getElementId(),event.getCommentId(),event.getCommentContent(),true);
        eventBus.post(new NotificationEvent(Type.INFO, "operation.comment.inserted"));
        //No refresh required
    }
    
    @Subscribe
    public void updateComment(UpdateCommentEvent event) throws IOException {
        commentService.updateComment(getDocument(),event.getCommentId(),event.getCommentContent());
        eventBus.post(new NotificationEvent(Type.INFO, "operation.comment.updated"));
        //No refresh required
    }

    @Subscribe
    public void deleteComment(DeleteCommentEvent event) throws IOException {
        commentService.deleteComment(getDocument(),event.getCommentId());
        eventBus.post(new NotificationEvent(Type.INFO, "operation.comment.deleted"));
        //No refresh required
    }

    @Override
    public FeedbackView getView() {
        return feedbackView;
    }

    private String getDocumentContent(LeosDocument document) {
        return transformationManager.toNonEditableXml(document.getContentStream(), 
                urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));
    }

    private LeosDocument getDocument() {
            String documentId=getDocumentId();
            LeosDocument document = null;
            try{
                if (documentId != null) {
                    document =  documentService.getDocument(documentId);
                }
            } catch(IllegalArgumentException iae){
                LOG.debug("Document {} can not be retrieved due to exception {}, Rejecting view",documentId, iae);
                rejectView(RepositoryView.VIEW_ID, "document.not.found", documentId);
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
            feedbackView.setDocumentTitle(document.getTitle());
            feedbackView.setDocumentStage(document.getStage());
            feedbackView.refreshContent(getDocumentContent(document));
            feedbackView.setData(commentService.getAllComments(document));
            feedbackView.setToc(getTableOfContent(document));
        }
    }

    @Override
    public void onLockUpdate(LockActionInfo lockActionInfo) {
        feedbackView.updateLocks(lockActionInfo);
    }

    /** to be used only for the lock update mechanism in the GUI 
     * as session is not available for async threads
     */
    @Override
    public String getLockId(){
        return strLockId;
    }
    

}
