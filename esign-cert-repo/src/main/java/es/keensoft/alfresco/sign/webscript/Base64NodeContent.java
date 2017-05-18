package es.keensoft.alfresco.sign.webscript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.google.gson.Gson;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import es.alfatec.iText.WaterMark;
import es.keensoft.alfresco.sign.webscript.bean.Base64NodeContentResponse;

public class Base64NodeContent extends AbstractWebScript {
	
	private static Log log = LogFactory.getLog(Base64NodeContent.class);

	public static final String RESPONSE_CODE_OK = "OK";

	private ContentService contentService;	
	private WaterMark waterMark;
	private AuthenticationService authenticationService;
	private PersonService personService;
	private NodeService nodeService;
	
	private Boolean csv;
	private Boolean messageSigns;
	
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		Gson gson = new Gson();
		Base64NodeContentResponse response = new Base64NodeContentResponse();

		try {
			
			//nodeRef parameter
			String docNodeRefStr = req.getParameter("nodeRef");
			NodeRef nodeRef = new NodeRef(docNodeRefStr);	
			
			//All pages parameter
			Boolean allPages = Boolean.valueOf(req.getParameter("allPages"));
			
			//Mimetype parameter
			String mimetype = req.getParameter("mimetype");
			
			//
			
			byte[] nodeContent;
			
			if((!mimetype.equals("pdf")||(!allPages && !csv))&&(!messageSigns)){
				nodeContent = getNodeContent(nodeRef);
				
			}
			
			else{
				
				//Tmp file
				File tmpDir = TempFileProvider.getTempDir();
				String name = UUID.randomUUID().toString();
				File tmpFile = new File(tmpDir,name);
				
				PdfReader pdfReader = obtainPdfReader(nodeRef);
				FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
				
				PdfStamper pdfStamper = new PdfStamper(pdfReader, fileOutputStream);
				pdfReader.close();
				pdfStamper.close();
				
				
				
				if(allPages){
					
					Integer position = Integer.valueOf(req.getParameter("position"));
					
					String userName = authenticationService.getCurrentUserName();
					NodeRef user = personService.getPerson(userName);
					String signer = nodeService.getProperty(user, ContentModel.PROP_FIRSTNAME)+" "+nodeService.getProperty(user, ContentModel.PROP_LASTNAME);
					
					waterMark.printSign(tmpFile, signer, position);	

				}
				
				if(csv){
					
					String uuid = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_UUID);
					waterMark.printCSV(tmpFile, uuid, allPages);
				}
				
				if(messageSigns){
					
					String pageSelect = String.valueOf(req.getParameter("pageSelect"));
					waterMark.printMessageSigns(tmpFile, pageSelect, csv);
				}
				
				InputStream inputStream = new FileInputStream(tmpFile);
				nodeContent = IOUtils.toByteArray(inputStream);
				inputStream.close();
				tmpFile.delete();
				
			}
			
			response.setBase64NodeContent(Base64.encodeBase64String(nodeContent));
			response.setNodeRef(nodeRef.getId());
			response.setCode(RESPONSE_CODE_OK);
			
			
			
			
		} catch (Exception e) {
			
			log.error(ExceptionUtils.getFullStackTrace(e));
			throw new WebScriptException(e.getMessage(), e);
			
		}

		res.setContentType(MimetypeMap.MIMETYPE_JSON);
		res.getWriter().write(gson.toJson(response));
		
	}

	private byte[] getNodeContent(NodeRef nodeRef) throws ContentIOException, IOException {
		ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		return IOUtils.toByteArray(reader.getContentInputStream());
	}
	
	private PdfReader obtainPdfReader(NodeRef nodeRef) throws IOException
	    {
			//Comprobamos si existe el nodo
			if (nodeService.exists(nodeRef) == false)
	        {
	            //Si no existe, lanzamos error
	            throw new AlfrescoRuntimeException("NodeRef: " + nodeRef + " does not exist");
	        }
					
	        //Comprobamos si tiene contenido
	        QName typeQName = nodeService.getType(nodeRef);
	        if (!typeQName.equals(ContentModel.TYPE_CONTENT))
	        {
	            //Si no tiene contenido, lanzamos error
	            throw new AlfrescoRuntimeException("The selected node is not a content node");
	        }
	
	        // Obtenemos el contenido, pero si obtenemos null, lanzamos error
	        ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
	
	        if(contentReader == null)
	        {
	        	throw new AlfrescoRuntimeException("The content reader for NodeRef: " + nodeRef + "is null");
	        }
	        
	        InputStream inputStream = contentReader.getContentInputStream();
			PdfReader pdfReader = new PdfReader(inputStream);
	        
	        return pdfReader;
	    }
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public WaterMark getWaterMark() {
		return waterMark;
	}

	public void setWaterMark(WaterMark waterMark) {
		this.waterMark = waterMark;
	}

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public PersonService getPersonService() {
		return personService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public Boolean getCsv() {
		return csv;
	}

	public void setCsv(Boolean csv) {
		this.csv = csv;
	}

	public Boolean getMessageSigns() {
		return messageSigns;
	}

	public void setMessageSigns(Boolean messageSigns) {
		this.messageSigns = messageSigns;
	}

}
