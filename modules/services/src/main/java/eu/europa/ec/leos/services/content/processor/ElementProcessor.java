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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ElementProcessor<T extends XmlDocument> {
    /**
     * Retrieves the element from the given document
     * @param document The document containing the article
     * @param elementId The  id of element
     * @return the xml string representation of the element
     */
    String getElement(T document, String elementName, String elementId);

    /**
     * Saves the new elemenContent of an existing element to the given document
     * or deletes the element if the given elementContent is null
     * @param document The document to update
     * @param elementContent The new article content, or null to delete the element
     * @param elementName The element Tag Name
     * @param elementId The id of the element
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    @PreAuthorize("hasPermission(#document, 'CAN_UPDATE')")
    byte[] updateElement(T document, String elementContent, String elementName, String elementId);
    /**
     * Deletes an element with the given id and saves the document.
     * @param document The document to update
     * @param elementId The id of the element which is to be deleted.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    @PreAuthorize("hasPermission(#document, 'CAN_UPDATE')")
    byte[] deleteElement(T document, String elementId, String elementType);
}
