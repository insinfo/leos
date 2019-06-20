/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.usecases.document;

import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal;
import eu.europa.ec.leos.domain.document.LeosMetadata.ProposalMetadata;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.document.ProposalService;
import io.atlassian.fugue.Option;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import java.util.*;

import static eu.europa.ec.leos.domain.document.LeosCategory.*;
import static eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.*;

@Component
@Scope("prototype")
public class ProposalContext {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalContext.class);

    private final TemplateService templateService;
    private final PackageService packageService;
    private final ProposalService proposalService;

    private final Provider<MemorandumContext> memorandumContextProvider;
    private final Provider<BillContext> billContextProvider;

    private final Map<LeosCategory, XmlDocument> categoryTemplateMap;

    private final Map<ContextAction, String> actionMsgMap;

    private Proposal proposal;
    private String purpose;

    private DocumentVO propDocument;
    private String propChildDocument;

    ProposalContext(TemplateService templateService,
                    PackageService packageService,
                    ProposalService proposalService,
                    Provider<MemorandumContext> memorandumContextProvider,
                    Provider<BillContext> billContextProvider) {
        this.templateService = templateService;
        this.packageService = packageService;
        this.proposalService = proposalService;
        this.memorandumContextProvider = memorandumContextProvider;
        this.billContextProvider = billContextProvider;
        this.categoryTemplateMap = new HashMap<>();
        this.actionMsgMap = new HashMap<>();
    }

    public void useTemplate(String name) {
        Validate.notNull(name, "Template name is required!");
        XmlDocument template = templateService.getTemplate(name);
        Validate.notNull(template, "Template not found! [name=%s]", name);

        LOG.trace("Using {} template... [id={}, name={}]", template.getCategory(), template.getId(), template.getName());
        categoryTemplateMap.put(template.getCategory(), template);
    }

    public void useActionMessage(ContextAction action, String actionMsg) {
        Validate.notNull(actionMsg, "Action message is required!");
        Validate.notNull(action, "Context Action not found! [name=%s]", action);

        LOG.trace("Using action message... [action={}, name={}]", action, actionMsg);
        actionMsgMap.put(action, actionMsg);
    }

    public void useProposal(String id) {
        Validate.notNull(id, "Proposal identifier is required!");
        LOG.trace("Using Proposal... [id={}]", id);
        proposal = proposalService.findProposal(id);
        Validate.notNull(proposal, "Proposal not found! [id=%s]", id);
    }

    public void usePurpose(String purpose) {
        Validate.notNull(purpose, "Proposal purpose is required!");
        LOG.trace("Using Proposal purpose... [purpose={}]", purpose);
        this.purpose = purpose;
    }

    public void useDocument(DocumentVO document) {
        Validate.notNull(document, "Proposal document is required!");
        propDocument = document;
    }

    public void useChildDocument(String documentId) {
        Validate.notNull(documentId, "Proposal child document is required!");
        propChildDocument = documentId;
    }

    public void executeImportProposal() {
        LOG.trace("Executing 'Import Proposal' use case...");
        MetadataVO propMeta = propDocument.getMetadata();
        Validate.notNull(propMeta, "Proposal metadata is required!");
        Validate.notNull(propDocument.getChildDocuments(), "Proposal must contain child documents to import!");
        // create package
        LeosPackage leosPackage = packageService.createPackage();
        // use template
        Proposal proposalTemplate = cast(categoryTemplateMap.get(PROPOSAL));
        Validate.notNull(proposalTemplate, "Proposal template is required!");

        // get metadata from template
        Option<ProposalMetadata> metadataOption = proposalTemplate.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Proposal metadata is required!");
        purpose = propMeta.getDocPurpose();
        Validate.notNull(purpose, "Proposal purpose is required!");
        // TODO right now it's only needed the docPurpose and docRef, we will have to add more in the future.
        ProposalMetadata metadata = metadataOption.get().withPurpose(purpose);

        Validate.notNull(propDocument.getSource(), "Proposal xml is required!");
        proposal = proposalService.createProposal(proposalTemplate.getId(), leosPackage.getPath(), metadata, propDocument.getSource());

        // create child element
        for (DocumentVO docChild : propDocument.getChildDocuments()) {
            if (docChild.getCategory() == MEMORANDUM) {
                MemorandumContext memorandumContext = memorandumContextProvider.get();
                memorandumContext.usePackage(leosPackage);
                // use template
                memorandumContext.useTemplate(cast(categoryTemplateMap.get(MEMORANDUM)));
                // We want to use the same purpose that was set in the wizzard for all the documents.
                memorandumContext.usePurpose(purpose);
                memorandumContext.useDocument(docChild);
                memorandumContext.useActionMessageMap(actionMsgMap);
                Memorandum memorandum = memorandumContext.executeImportMemorandum();
                proposal = proposalService.addComponentRef(proposal, memorandum.getName(), LeosCategory.MEMORANDUM);
            } else if (docChild.getCategory() == BILL) {
                BillContext billContext = billContextProvider.get();
                billContext.usePackage(leosPackage);
                // use template
                billContext.useTemplate(cast(categoryTemplateMap.get(BILL)));
                billContext.usePurpose(purpose);
                billContext.useDocument(docChild);
                billContext.useActionMessageMap(actionMsgMap);
                Bill bill = billContext.executeImportBill();
                proposal = proposalService.addComponentRef(proposal, bill.getName(), LeosCategory.BILL);
            }
        }
    }

    public void executeCreateProposal() {
        LOG.trace("Executing 'Create Proposal' use case...");

        LeosPackage leosPackage = packageService.createPackage();

        Proposal proposalTemplate = cast(categoryTemplateMap.get(PROPOSAL));
        Validate.notNull(proposalTemplate, "Proposal template is required!");

        Option<ProposalMetadata> metadataOption = proposalTemplate.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Proposal metadata is required!");

        Validate.notNull(purpose, "Proposal purpose is required!");
        ProposalMetadata metadata = metadataOption.get().withPurpose(purpose);

        Proposal proposal = proposalService.createProposal(proposalTemplate.getId(), leosPackage.getPath(), metadata, null);

        MemorandumContext memorandumContext = memorandumContextProvider.get();
        memorandumContext.usePackage(leosPackage);
        memorandumContext.useTemplate(cast(categoryTemplateMap.get(MEMORANDUM)));
        memorandumContext.usePurpose(purpose);
        memorandumContext.useActionMessageMap(actionMsgMap);
        Memorandum memorandum = memorandumContext.executeCreateMemorandum();
        proposal = proposalService.addComponentRef(proposal, memorandum.getName(), LeosCategory.MEMORANDUM);

        BillContext billContext = billContextProvider.get();
        billContext.usePackage(leosPackage);
        billContext.useTemplate(cast(categoryTemplateMap.get(BILL)));
        billContext.usePurpose(purpose);
        billContext.useActionMessageMap(actionMsgMap);
        Bill bill = billContext.executeCreateBill();
        proposalService.addComponentRef(proposal, bill.getName(), LeosCategory.BILL);
    }

    public void executeUpdateProposal() {
        LOG.trace("Executing 'Update Proposal' use case...");

        Validate.notNull(proposal, "Proposal is required!");

        Option<ProposalMetadata> metadataOption = proposal.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Proposal metadata is required!");

        Validate.notNull(purpose, "Proposal purpose is required!");
        ProposalMetadata metadata = metadataOption.get().withPurpose(purpose);

        proposal = proposalService.updateProposal(proposal, metadata);

        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposal.getId());

        MemorandumContext memorandumContext = memorandumContextProvider.get();
        memorandumContext.usePackage(leosPackage);
        memorandumContext.usePurpose(purpose);
        memorandumContext.useActionMessageMap(actionMsgMap);
        memorandumContext.executeUpdateMemorandum();

        BillContext billContext = billContextProvider.get();
        billContext.usePackage(leosPackage);
        billContext.usePurpose(purpose);
        billContext.useActionMessageMap(actionMsgMap);
        billContext.executeUpdateBill();
    }

    public void executeDeleteProposal() {
        LOG.trace("Executing 'Delete Proposal' use case...");

        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposal.getId());

        packageService.deletePackage(leosPackage);
    }

    public void executeUpdateProposalAsync() {
        proposalService.updateProposalAsync(propChildDocument);
    }


    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
    }
}
