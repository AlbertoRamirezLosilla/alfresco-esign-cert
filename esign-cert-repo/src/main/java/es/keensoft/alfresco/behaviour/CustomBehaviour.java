package es.keensoft.alfresco.behaviour;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;

import es.alfatec.alfresco.extractor.DigitalSignaturesExtractor;
import es.alfatec.alfresco.extractor.bean.Signature;
import es.keensoft.alfresco.model.SignModel;

public class CustomBehaviour implements 
    NodeServicePolicies.OnCreateNodePolicy,
    NodeServicePolicies.OnMoveNodePolicy,
    ContentServicePolicies.OnContentUpdatePolicy {
	
	private static Log logger = LogFactory.getLog(CustomBehaviour.class);
	
	private PolicyComponent policyComponent;
	private NodeService nodeService;
	private VersionService versionService;
	private ContentService contentService;
	private MessageService messageService;
	private DigitalSignaturesExtractor extractor;
	
	private static final String PADES = "PAdES";
	
	public void init() {
		policyComponent.bindClassBehaviour(
		        NodeServicePolicies.OnCreateNodePolicy.QNAME,
		        ContentModel.TYPE_CONTENT,
		        new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(
                NodeServicePolicies.OnMoveNodePolicy.QNAME,
                SignModel.ASPECT_SIGNED,
                new JavaBehaviour(this, "onMoveNode", NotificationFrequency.TRANSACTION_COMMIT));
		policyComponent.bindClassBehaviour(
		        ContentServicePolicies.OnContentUpdatePolicy.QNAME,
		        ContentModel.TYPE_CONTENT,
		        new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
	}	
	
	@Override
	public void onCreateNode(ChildAssociationRef childNodeRef) {

		NodeRef node = childNodeRef.getChildRef();
		
		if (!nodeService.exists(node)) {
			return; 
		}
		
		processSignatures(node);
	}



	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {

	    if (nodeService.exists(nodeRef) && !newContent) {
			processSignatures(nodeRef);
		}

	}
	
    @Override
    public void onMoveNode(ChildAssociationRef from, ChildAssociationRef to) {
        
        for (AssociationRef signatureAssoc : nodeService.getTargetAssocs(from.getChildRef(), SignModel.ASSOC_SIGNATURE)) {
            nodeService.moveNode(
                    signatureAssoc.getTargetRef(), 
                    to.getParentRef(), 
                    ContentModel.ASSOC_CONTAINS, 
                    QName.createQName(nodeService.getProperty(signatureAssoc.getTargetRef(), ContentModel.PROP_NAME).toString()));
        }

    }
	
    private void processSignatures(NodeRef node) {
        
        ContentData contentData = (ContentData) nodeService.getProperty(node, ContentModel.PROP_CONTENT);
        
        if (contentData != null && contentData.getMimetype().equalsIgnoreCase(MimetypeMap.MIMETYPE_PDF)) {
		    
			List<Signature> signatures = getDigitalSignatures(node);
			
			if (!signatures.isEmpty()) {
			    
			    // Remove signatures from previous version
			    removeSignatureMetadata(node);
			    
				// Create signatures from PDF source
			    for (Signature signature : signatures) {
				
					//Check if there are any signature incorrect.
			    	if(!signature.hasError()){
			    		
			    		Map<QName, Serializable> aspectProperties = signature.getSignatureInfo();
				    	String originalFileName = nodeService.getProperty(node, ContentModel.PROP_NAME).toString();
						String signatureFileName = FilenameUtils.getBaseName(originalFileName) + "-" 
							    + System.currentTimeMillis() + "-" + PADES;
						
						// Creating a node reference without type (no content and no folder): remains invisible for Share
						NodeRef signatureNodeRef = nodeService.createNode(
								nodeService.getPrimaryParent(node).getParentRef(),
								ContentModel.ASSOC_CONTAINS, 
								QName.createQName(signatureFileName), 
								ContentModel.TYPE_CMOBJECT).getChildRef();
						
						nodeService.createAssociation(node, signatureNodeRef, SignModel.ASSOC_SIGNATURE);
						nodeService.createAssociation(signatureNodeRef, node, SignModel.ASSOC_DOC);
						
					    aspectProperties.put(SignModel.PROP_FORMAT, PADES);
						nodeService.addAspect(signatureNodeRef, SignModel.ASPECT_SIGNATURE, aspectProperties);
			    	}else{
			    		//Set aspect errorSign properties
			            Map<QName, Serializable> aspectErrorSignatureProperties = new HashMap<QName, Serializable>(); 
			            SimpleDateFormat formatDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			            String date = formatDate.format(new Date());
			            aspectErrorSignatureProperties.put(SignModel.PROP_ERROR_SIGN, messageService.getMessage("sign.error") + " - " + date);    
			            nodeService.addAspect(node, SignModel.ASPECT_ERROR_SIGNATURE, aspectErrorSignatureProperties);
		        		
		    			logger.warn("Signature has an error or it's invalid!");
			    	}
				}

                // Implicit signature aspect
                Map<QName, Serializable> aspectSignedProperties = new HashMap<QName, Serializable>(); 
                aspectSignedProperties.put(SignModel.PROP_TYPE, I18NUtil.getMessage("signature.implicit"));
                nodeService.addAspect(node,  SignModel.ASPECT_SIGNED, aspectSignedProperties);
                
            } else {
                
                if (nodeService.hasAspect(node, SignModel.ASPECT_SIGNED)) {
                    removeSignatureMetadata(node);
                }
                
            }
        }
        
    }
    
    private void removeSignatureMetadata(NodeRef nodeRef) {
        
        if (nodeService.hasAspect(nodeRef, SignModel.ASPECT_SIGNED)) {
             List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(nodeRef, SignModel.ASSOC_SIGNATURE);
             for (AssociationRef targetAssoc : targetAssocs) {
                 nodeService.removeAssociation(targetAssoc.getSourceRef(), targetAssoc.getTargetRef(), targetAssoc.getTypeQName());
             }
             List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(nodeRef, SignModel.ASSOC_DOC);
             for (AssociationRef sourceAssoc : sourceAssocs) {
                 nodeService.removeAssociation(sourceAssoc.getSourceRef(), sourceAssoc.getTargetRef(), sourceAssoc.getTypeQName());
                 nodeService.deleteNode(sourceAssoc.getSourceRef());
             }
        }
        nodeService.removeAspect(nodeRef, SignModel.ASPECT_SIGNED);
    }
    
    public List<Signature> getDigitalSignatures(NodeRef node) {		
		ContentReader contentReader = contentService.getReader(node, ContentModel.PROP_CONTENT);
		
		//Extract digital signatures
		return extractor.getDigitalSignatures(contentReader);
	}
	
	public PolicyComponent getPolicyComponent() {
		return policyComponent;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public VersionService getVersionService() {
		return versionService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
	
	public ContentService getContentService() {
		return contentService;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}

	public void setExtractor(DigitalSignaturesExtractor extractor) {
		this.extractor = extractor;
	}
}