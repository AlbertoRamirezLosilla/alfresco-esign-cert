package es.alfatec.alfresco.extractor.bean;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

public abstract class Signature{
	public Map<QName, Serializable> signatureInfo;
	public abstract boolean hasError();
	
	public Map<QName, Serializable> getSignatureInfo() {
		return signatureInfo;
	}
	public void setSignatureInfo(Map<QName, Serializable> signatureInfo) {
		this.signatureInfo = signatureInfo;
	}
}