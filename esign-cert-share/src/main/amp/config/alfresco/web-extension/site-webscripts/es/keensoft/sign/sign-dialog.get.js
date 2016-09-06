function main() {
	
	//Get Base64 content
	var base64NodeContentResponse = jsonConnection("/keensoft/sign/base64-node-content?nodeRef=" + args.nodeRef);
	if(base64NodeContentResponse == null) {
		model.jsonError = true;
		return;
	}
	
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
	
	//Get properties node
	var nodeProperties = jsonConnection("/slingshot/doclib2/node/" + args.nodeRef.replace(":/", ""));
	model.signLastPage = true;
	model.signFirstPage = true;
	if(nodeProperties.item.node.properties["sign:signsPage"] != null){
		model.signLastPage = (nodeProperties.item.node.properties["sign:signsPage"].indexOf("first") == -1);
		model.signFirstPage = (nodeProperties.item.node.properties["sign:signsPage"].indexOf("last") == -1);
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
	model.base64NodeContent = base64NodeContentResponse.base64NodeContent;
	model.mimeType = args.mimeType;
	model.nodeRef = args.nodeRef;
	model.jsonError = false;
}

main();

function jsonConnection(url) {
	
	var connector = remote.connect("alfresco"),
		result = connector.get(url);

	if (result.status == 200) {		
		return eval('(' + result + ')')
	} else {
		return null;
	}
}