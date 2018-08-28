package es.alfatec.alfresco.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.security.PdfPKCS7;

import es.alfatec.alfresco.extractor.bean.DataSignature;
import es.alfatec.alfresco.extractor.bean.ErrorSignature;
import es.alfatec.alfresco.extractor.bean.Signature;
import es.keensoft.alfresco.model.SignModel;

public class InternalDigitalSignaturesExtractor implements DigitalSignaturesExtractor{

	private static Log logger = LogFactory.getLog(InternalDigitalSignaturesExtractor.class);

	@Override
	public List<Signature> getDigitalSignatures(ContentReader contentReader) {
		InputStream is = null;
        List<Signature> aspects = new ArrayList<Signature>();

        logger.info("esign-cert -- > InternalDigitalSignaturesExtractor");
        
		try {	
			is = contentReader.getContentInputStream();
			
			// For SHA-256 and upper
			loadBCProvider();
			
			PdfReader reader = new PdfReader(is);
	        AcroFields af = reader.getAcroFields();
	        ArrayList<String> names = af.getSignatureNames();
	        if(names == null || names.isEmpty()) return new ArrayList<Signature>();
	        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
	        ks.load(null, null);
	        for (String name : names) {
	        	try{
	        		DataSignature dataSignature = new DataSignature();
		            PdfPKCS7 pk = af.verifySignature(name);
		            X509Certificate certificate = pk.getSigningCertificate();
		           
		            //Set aspect properties for each signature
		            Map<QName, Serializable> aspectSignatureProperties = new HashMap<QName, Serializable>(); 
		            if (pk.getSignDate() != null) aspectSignatureProperties.put(SignModel.PROP_DATE, pk.getSignDate().getTime());
		    		aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_PRINCIPAL, certificate.getSubjectX500Principal().toString());
		    	    aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_SERIAL_NUMBER, certificate.getSerialNumber().toString());
		    	    aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_NOT_AFTER, certificate.getNotAfter());
		    	    aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_ISSUER, certificate.getIssuerX500Principal().toString());   
		    	    
		    	    dataSignature.setSignatureInfo(aspectSignatureProperties);
		    	    aspects.add(dataSignature);
		    	    
	        	}catch(Exception e){
	        		
	        		//Set aspect errorSign properties
		            aspects.add(new ErrorSignature());
	        	}
	        }
	        
			return aspects;
			
		} catch (Exception e) {
			
			// Not every PDF has a signature inside
			logger.warn("No signature found!", e);
			return new ArrayList<Signature>();
			
			// WARN: Do not throw this exception up, as it will break WedDAV PDF files uploading 
		} finally {// As this verification can be included in a massive operation, closing files is required
			try {
			    if (is != null) is.close();
			} catch (IOException ioe) {}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void loadBCProvider() {
        try {
            Class c = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.insertProviderAt((Provider)c.newInstance(), 2000);
        } catch(Exception e) {
            // provider is not available
        }		
	}
}