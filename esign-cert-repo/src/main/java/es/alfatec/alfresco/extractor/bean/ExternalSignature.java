package es.alfatec.alfresco.extractor.bean;

/**
 * Class for map data from digital signatures external application.
 *
 */
public class ExternalSignature {
	
	public String sign_date;
	public String sign_subject;
	public String sign_serial_number;
	public String sign_not_after;
	public String sign_issuer;
	public boolean sign_error;
	public String sign_error_message;
	
	public String getSign_date() {
		return sign_date;
	}
	public void setSign_date(String sign_date) {
		this.sign_date = sign_date;
	}
	public String getSign_subject() {
		return sign_subject;
	}
	public void setSign_subject(String sign_subject) {
		this.sign_subject = sign_subject;
	}
	public String getSign_serial_number() {
		return sign_serial_number;
	}
	public void setSign_serial_number(String sign_serial_number) {
		this.sign_serial_number = sign_serial_number;
	}
	public String getSign_not_after() {
		return sign_not_after;
	}
	public void setSign_not_after(String sign_not_after) {
		this.sign_not_after = sign_not_after;
	}
	public String getSign_issuer() {
		return sign_issuer;
	}
	public void setSign_issuer(String sign_issuer) {
		this.sign_issuer = sign_issuer;
	}
	public boolean getSign_error() {
		return sign_error;
	}
	public void setSign_error(boolean sign_error) {
		this.sign_error = sign_error;
	}
	public String getSign_error_message() {
		return sign_error_message;
	}
	public void setSign_error_message(String sign_error_message) {
		this.sign_error_message = sign_error_message;
	}
}