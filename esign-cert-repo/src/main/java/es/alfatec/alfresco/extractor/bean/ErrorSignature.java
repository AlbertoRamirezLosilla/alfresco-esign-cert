package es.alfatec.alfresco.extractor.bean;


public class ErrorSignature extends Signature {

	@Override
	public boolean hasError() {
		return true;
	}
}