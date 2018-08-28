package es.alfatec.alfresco.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;

import es.alfatec.alfresco.extractor.bean.DataSignature;
import es.alfatec.alfresco.extractor.bean.ErrorSignature;
import es.alfatec.alfresco.extractor.bean.ExternalSignature;
import es.alfatec.alfresco.extractor.bean.Signature;
import es.keensoft.alfresco.model.SignModel;

public class ExternalDigitalSignaturesExtractor implements DigitalSignaturesExtractor{
	
	private Log logger = LogFactory.getLog(ExternalDigitalSignaturesExtractor.class);
	
	private String signatureExtractorAppURL;
	private String signatureExtractorUsername;
	private String signatureExtractorPass;
	
	@Override
	public List<Signature> getDigitalSignatures(ContentReader contentReader) {
		Gson gson = new Gson();
        List<Signature> aspects = new ArrayList<Signature>();

        logger.info("esign-cert -- > ExternalDigitalSignaturesExtractor");
        
		try {
			
			byte[] nodeContent = IOUtils.toByteArray(contentReader.getContentInputStream());
			String documentContentBase64 = Base64.encodeBase64String(nodeContent);

			String encoded = Base64.encodeBase64String((signatureExtractorUsername+":"+signatureExtractorPass).getBytes(StandardCharsets.UTF_8));  //Java 8
			
			URL url = new URL(signatureExtractorAppURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Basic "+encoded);

			OutputStream os = conn.getOutputStream();
			os.write(documentContentBase64.getBytes()); 
			os.flush();

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				logger.error("esign-cert -- > ExternalDigitalSignaturesExtractor --> Failed : HTTP error code : "+ conn.getResponseCode());
				return new ArrayList<Signature>();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			while ((output = br.readLine()) != null) {
				ExternalSignature[] signaures = gson.fromJson(output, ExternalSignature[].class);
				aspects.addAll(mapProperties(signaures));
			}

			conn.disconnect();

		  } catch (MalformedURLException e) {
			  logger.error("esign-cert -- > ExternalDigitalSignaturesExtractor --> Exception: "+ e.getMessage());
			  throw new AlfrescoRuntimeException("esign-cert -- > ExternalDigitalSignaturesExtractor --> Exception: "+ e.getMessage());
		  } catch (IOException e) {
			  logger.error("esign-cert -- > ExternalDigitalSignaturesExtractor --> Exception: "+ e.getMessage());
			  throw new AlfrescoRuntimeException("esign-cert -- > ExternalDigitalSignaturesExtractor --> Exception: "+ e.getMessage());
		  }
		
		return aspects;
	}

	/**
	 * This method converts response of external signatures extractor application in esign-cert plugin structure. 
	 * @param signaures Data extracted from external application
	 * @return List of esign-cert data of signatures.
	 */
	private List<Signature> mapProperties(ExternalSignature[] signaures) {
		List<Signature> aspects = new ArrayList<>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz YYYY",Locale.ENGLISH);
		for(ExternalSignature signature:signaures){
		    Map<QName, Serializable> aspectSignatureProperties = new HashMap<QName, Serializable>(); 
		    if (!signature.getSign_error()){
		    	try{
		    		DataSignature dataSignature = new DataSignature();
		    		
			    	aspectSignatureProperties.put(SignModel.PROP_DATE, dateFormat.parse(signature.getSign_date()));
			    	aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_PRINCIPAL, signature.getSign_subject());
			    	aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_SERIAL_NUMBER, signature.getSign_serial_number());
			    	aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_NOT_AFTER, dateFormat.parse(signature.getSign_not_after()));
			    	aspectSignatureProperties.put(SignModel.PROP_CERTIFICATE_ISSUER, signature.getSign_not_after());
			    	
			    	dataSignature.setSignatureInfo(aspectSignatureProperties);
			    	aspects.add(dataSignature);
		    	}catch(ParseException e){
		    		aspects.add(new ErrorSignature()); 
		    	}
		    }else{
		    	aspects.add(new ErrorSignature());
		    }
		}
		return aspects;
	}

	public void setSignatureExtractorAppURL(String signatureExtractorAppURL) {
		this.signatureExtractorAppURL = signatureExtractorAppURL;
	}

	public void setSignatureExtractorUsername(String signatureExtractorUsername) {
		this.signatureExtractorUsername = signatureExtractorUsername;
	}

	public void setSignatureExtractorPass(String signatureExtractorPass) {
		this.signatureExtractorPass = signatureExtractorPass;
	}
}