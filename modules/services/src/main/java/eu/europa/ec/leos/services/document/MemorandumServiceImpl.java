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
package eu.europa.ec.leos.services.document;

import com.google.common.base.Stopwatch;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosMetadata.MemorandumMetadata;
import eu.europa.ec.leos.repository.document.MemorandumRepository;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.MemorandumTocItemType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;

@Service
public class MemorandumServiceImpl implements MemorandumService {

    private static final Logger LOG = LoggerFactory.getLogger(MemorandumServiceImpl.class);

    private static final String MEMORANDUM_NAME_PREFIX = "memorandum_";

    private final MemorandumRepository memorandumRepository;
    private final PackageRepository packageRepository;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlContentProcessor xmlContentProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;

    MemorandumServiceImpl(MemorandumRepository memorandumRepository,
                          PackageRepository packageRepository,
                          XmlNodeProcessor xmlNodeProcessor,
                          XmlContentProcessor xmlContentProcessor,
                          XmlNodeConfigHelper xmlNodeConfigHelper) {
        this.memorandumRepository = memorandumRepository;
        this.packageRepository = packageRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
    }

    @Override
    public Memorandum createMemorandum(String templateId, String path, MemorandumMetadata metadata) {
        LOG.trace("Creating Memorandum... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String name = generateMemorandumName();
        Memorandum memorandum = memorandumRepository.createMemorandum(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml(memorandum, metadata);
        return memorandumRepository.updateMemorandum(memorandum.getId(), metadata, updatedBytes,false,"Metadata updated.");
    }

    @Override
    public Memorandum findMemorandum(String id) {
        LOG.trace("Finding Memorandum... [id={}]", id);
        return memorandumRepository.findMemorandumById(id, true);
    }

    @Override
    public Memorandum findMemorandumByPackagePath(String path) {
        LOG.trace("Finding Memorandum by package path... [path={}]", path);
        // FIXME temporary workaround
        List<Memorandum> docs = packageRepository.findDocumentsByPackagePath(path, Memorandum.class);
        Memorandum memorandum = findMemorandum(docs.get(0).getId());
        return memorandum;
    }

    @Override
    public Memorandum updateMemorandum(String memorandumId, byte[] updatedMemorandumContent, boolean major, String comment) {
        LOG.trace("Updating Memorandum Xml Content... [id={}]", memorandumId);
        Memorandum memorandum = memorandumRepository.updateMemorandum(memorandumId, updatedMemorandumContent, major, comment);
        return memorandum;
    }

    @Override
    public Memorandum updateMemorandum(Memorandum memorandum, MemorandumMetadata updatedMetadata, boolean major, String comment) {
        LOG.trace("Updating Memorandum... [id={}, metadata={}]", memorandum.getId(), updatedMetadata);
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] updatedBytes = updateDataInXml(memorandum, updatedMetadata); //FIXME: Do we need latest data again??
        memorandum = memorandumRepository.updateMemorandum(memorandum.getId(), updatedMetadata, updatedBytes, major, comment);
        LOG.trace("Updated Memorandum ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return memorandum;
    }

    @Override
    public List<TableOfContentItemVO> getTableOfContent(Memorandum memorandum) {
        Validate.notNull(memorandum, "Memorandum is required");
        final Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        final byte[] memorandumContent = content.getSource().getByteString().toByteArray();
        return xmlContentProcessor.buildTableOfContent("doc", MemorandumTocItemType::getTocItemTypeFromName, memorandumContent);
    }

    private byte[] updateDataInXml(Memorandum memorandum, MemorandumMetadata dataObject) {
        final Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        final byte[] memorandumContent = content.getSource().getByteString().toByteArray();
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(memorandumContent, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(memorandum.getCategory()));
        return xmlContentProcessor.doXMLPostProcessing(updatedBytes);
    }

    private String generateMemorandumName() {
        return MEMORANDUM_NAME_PREFIX + Cuid.createCuid();
    }

    @Override
    public List<Memorandum> findVersions(String id) {
        LOG.trace("Finding Memorandum versions... [id={}]", id);
        return memorandumRepository.findMemorandumVersions(id);
    }

    @Override
    public Memorandum createVersion(String id, boolean major, String comment) {
        LOG.trace("Creating Memorandum version... [id={}, major={}, comment={}]", id, major, comment);
        final Memorandum memorandum = findMemorandum(id);
        final MemorandumMetadata metadata = memorandum.getMetadata().getOrError(() -> "Memorandum metadata is required!");
        final Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        final byte[] contentBytes = content.getSource().getByteString().toByteArray();
        return memorandumRepository.updateMemorandum(id, metadata, contentBytes, major, comment);
    }
}
