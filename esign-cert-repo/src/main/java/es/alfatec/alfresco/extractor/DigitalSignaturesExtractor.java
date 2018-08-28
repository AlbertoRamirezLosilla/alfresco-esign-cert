package es.alfatec.alfresco.extractor;

import java.util.List;

import org.alfresco.service.cmr.repository.ContentReader;

import es.alfatec.alfresco.extractor.bean.Signature;

public interface DigitalSignaturesExtractor {
	public List<Signature> getDigitalSignatures(ContentReader contentReader);
}