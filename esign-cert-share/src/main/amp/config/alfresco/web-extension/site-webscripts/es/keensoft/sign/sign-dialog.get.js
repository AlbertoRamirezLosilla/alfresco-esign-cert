function main() {
			
	//Get signature parameters
	var signatureParams = jsonConnection("/keensoft/sign/signature-params");
	model.paramsPades = signatureParams.paramsPades;
	model.paramsCades = signatureParams.paramsCades;
	model.signatureAlg = signatureParams.signatureAlg;
	model.firstSignaturePosition = signatureParams.firstSignaturePosition;
	model.secondSignaturePosition = signatureParams.secondSignaturePosition;
	model.thirdSignaturePosition = signatureParams.thirdSignaturePosition;
	model.fourthSignaturePosition = signatureParams.fourthSignaturePosition;
	model.fifthSignaturePosition = signatureParams.fifthSignaturePosition;
	model.sixthSignaturePosition = signatureParams.sixthSignaturePosition;
	model.signaturePurposeEnabled = signatureParams.signaturePurposeEnabled;
	
	//Get properties node
	var nodeProperties = jsonConnection("/slingshot/doclib2/node/" + args.nodeRef.replace(":/", ""));
	
	//Get allPages parameters
	var allPagesProperty = jsonConnection("/alfatec/alfresco-global/allPages");
	model.allPages = allPagesProperty.allPages;
	
	model.signLastPage = true;
	model.signFirstPage = true;
	model.signAllPages = true;
	
	if(nodeProperties.item.node.properties["sign:signsPage"] != null){
		model.signFirstPage = (nodeProperties.item.node.properties["sign:signsPage"] == "first");
		model.signLastPage = (nodeProperties.item.node.properties["sign:signsPage"] == "last");
		model.signAllPages = (nodeProperties.item.node.properties["sign:signsPage"] == "all");
	}
	
	//Get available signature places
	model.showOptionFirstSignature = true;
	model.showOptionSecondSignature = true;
	model.showOptionThirdSignature = true;
	model.showOptionFourthSignature = true;
	model.showOptionFifthSignature = true;
	model.showOptionSixthSignature = true;
	if(nodeProperties.item.node.properties["sign:signsPositions"] != null){
		model.showOptionFirstSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("1") == -1);
		model.showOptionSecondSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("2") == -1);
		model.showOptionThirdSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("3") == -1);
		model.showOptionFourthSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("4") == -1);
		model.showOptionFifthSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("5") == -1);
		model.showOptionSixthSignature = (nodeProperties.item.node.properties["sign:signsPositions"].indexOf("6") == -1);
	}
	
	//Fill up the model with data
	model.mimeType = args.mimeType;
	model.nodeRef = args.nodeRef;
	model.jsonError = false;
}

function jsonConnection(url) {
	
	var connector = remote.connect("alfresco"),
		result = connector.get(url);

	if (result.status == 200) {		
		return eval('(' + result + ')')
	} else {
		return null;
	}

}

main();
