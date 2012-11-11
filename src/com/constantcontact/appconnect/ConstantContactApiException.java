package com.constantcontact.appconnect;

public class ConstantContactApiException extends Exception {
	private static final long serialVersionUID = 1L;

	public ConstantContactApiException() {
	}

	public ConstantContactApiException(String detailMessage) {
		super(detailMessage);
	}

	public ConstantContactApiException(Throwable throwable) {
		super(throwable);
	}

	public ConstantContactApiException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
