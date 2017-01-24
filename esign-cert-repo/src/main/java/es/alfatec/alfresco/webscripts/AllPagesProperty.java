package es.alfatec.alfresco.webscripts;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.http.MediaType;


public class AllPagesProperty extends AbstractWebScript{

	private Boolean allPages;
	
	@Override
	public void execute(WebScriptRequest request, WebScriptResponse response)
			throws IOException {
		
		JSONObject json = new JSONObject();
        json.put("allPages", allPages);
		
		String jsonString = json.toString();
	    response.setContentType(MediaType.APPLICATION_JSON.toString());
	    response.setContentEncoding("UTF-8");
        response.getWriter().write(jsonString);
		
	}

	public Boolean getAllPages() {
		return allPages;
	}

	public void setAllPages(Boolean allPages) {
		this.allPages = allPages;
	}
	
	

}
