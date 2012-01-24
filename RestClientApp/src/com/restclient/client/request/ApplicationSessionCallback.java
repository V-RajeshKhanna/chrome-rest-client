package com.restclient.client.request;


public interface ApplicationSessionCallback {
	/**
	 * Called when error occurred.
	 * @param message Message to display to user
	 * @param exception can be null!
	 */
	void onFailure(String message, Throwable exception);
	/**
	 * Called when data has been successfully retrieved from server
	 */
	void onSuccess(ApplicationSession session);
}
