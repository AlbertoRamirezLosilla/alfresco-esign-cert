package es.alfatec.iText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

public class WaterMark {
	
	
	private NodeService nodeService;
	private ContentService contentService;
	private Boolean csv;
	private String url;
	
	//Fonts
	private static final float TEXT_SIZE = 7;
	private static final Font FONT_SIGNER = new Font(FontFamily.HELVETICA, TEXT_SIZE, Font.NORMAL, BaseColor.GRAY);
	
	//Sizes
	private static final float RECTANGLE_SIGN_HEIGHT=33;
	private static final float RECTANGLE_SIGN_MARGIN_X=10;
	private static final float RECTANGLE_SIGN_MARGIN_Y=10;
	private static final float RECTANGLE_SIGN_MARGIN_INTERLINE=2;
	private static final float RECTANGLE_SIGN_MARGIN_TEXT=4;	
		
	
	public File printSign(NodeRef nodeRef, String signer, Integer position) throws IOException, DocumentException{
		
		PdfReader pdfReader = obtainPdfReader(nodeRef);
		
		//Number of Pages
		int pages = pdfReader.getNumberOfPages();
		
		//Tmp file
		File tmpDir = TempFileProvider.getTempDir();
		String name = UUID.randomUUID().toString();
		File tmpFile = new File(tmpDir,name);
		FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
		PdfStamper pdfStamper = new PdfStamper(pdfReader, fileOutputStream);
		
		//PageSize Calculation
		Rectangle pageSize;
		if(pdfReader.getPageRotation(1)==90 || pdfReader.getPageRotation(1)==270){
			pageSize = pdfReader.getPageSize(1).rotate();
		}
		else{
			pageSize = pdfReader.getPageSize(1);
		}
		
		//Rectangle sing positions
		float xLowerLeft = 0 + RECTANGLE_SIGN_MARGIN_X ;
		float yLowerLeft = 0 + RECTANGLE_SIGN_MARGIN_Y;
		float xUppeRight  = pageSize.getWidth() - RECTANGLE_SIGN_MARGIN_X;
		float yUppeRight = yLowerLeft + RECTANGLE_SIGN_HEIGHT;
		
		//Rectangle sing properties
		Rectangle signerRectangle = new Rectangle(xLowerLeft, yLowerLeft, xUppeRight, yUppeRight);
		signerRectangle.setBorder(Rectangle.BOX);
		signerRectangle.setBorderColor(BaseColor.GRAY);
		signerRectangle.setBorderWidth(1);
		
		//Text with sign information
		String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
		if((pageSize.getHeight()>pageSize.getWidth()) && (signer.length()>39))
			signer = signer.substring(0, 39)+"...";
		else if(signer.length()>69)
				signer = signer.substring(0, 69)+"...";
		String text = "Firmado por: "+signer+"   Fecha: "+date+" CET";
		Chunk chunk = new Chunk(text,FONT_SIGNER); 
		
		// Calculation x  
		float xText;
		if(position%2!=0)
			xText= xLowerLeft + RECTANGLE_SIGN_MARGIN_TEXT;
		else
			xText= xLowerLeft + RECTANGLE_SIGN_MARGIN_TEXT + ((xUppeRight-xLowerLeft)/2);
		
		// Calculation y  
		float yText;
		if(position%2!=0)
			yText = yUppeRight - RECTANGLE_SIGN_MARGIN_TEXT - (RECTANGLE_SIGN_MARGIN_INTERLINE*(position/2)) - (TEXT_SIZE*(position-position/2));
		else
			yText = yUppeRight - RECTANGLE_SIGN_MARGIN_TEXT - (RECTANGLE_SIGN_MARGIN_INTERLINE*((position/2)-1)) - (TEXT_SIZE*(position/2));
		
		PdfContentByte canvas;
		
		//Add sing in all pages
		for(int pag=1; pag<=pages; pag++){
			
			//Add rectangle
			canvas = pdfStamper.getOverContent(pag);
			canvas.saveState();
			canvas.rectangle(signerRectangle);
			canvas.restoreState();
			
			//Add text
			ColumnText.showTextAligned(canvas, -1, new Phrase(chunk), xText, yText, 0);
		}
		
		//Close objects
		pdfStamper.close();
		pdfReader.close();
		
		return tmpFile;
		
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

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public Boolean getCsv() {
		return csv;
	}

	public void setCsv(Boolean csv) {
		this.csv = csv;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	

}
