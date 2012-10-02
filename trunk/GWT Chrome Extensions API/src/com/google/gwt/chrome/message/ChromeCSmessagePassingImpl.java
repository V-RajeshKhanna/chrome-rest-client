package com.google.gwt.chrome.message;

import java.util.HashMap;

import com.google.gwt.core.client.Callback;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * @todo this class must be replaced in production mode to class that can handle
 *       message passing via Chrome API
 * @author jarrod
 * 
 */
public class ChromeCSmessagePassingImpl implements ChromeMessagePassing {

	private static HashMap<String, Callback<String, Throwable>> messageCallbackList = new HashMap<String, Callback<String, Throwable>>();

	public ChromeCSmessagePassingImpl() {
		handleContentScriptMessages(new ChromeMessageReceiver() {
			@Override
			public void onMessage(String payload, String message) {
				handleExternalMessage(payload, message);
			}
		});
		
		postMessage("setEnvironment", "{\"dev\":true}");
	}

	@Override
	public void postMessage(String payload, String data) {
		final JSONObject respObj = preparePostMessage();
		if (data != null) {
			respObj.put("data", new JSONString(data));
		}
		respObj.put("payload", new JSONString(payload));
		sendExtensionMessage(respObj.toString());
	}

	@Override
	public final void postMessage(String payload, String data,
			Callback<String, Throwable> callback) {
		messageCallbackList.put(payload, callback);
		postMessage(payload, data);
	}

	/**
	 * 
	 * @param handler
	 */
	private final native void handleContentScriptMessages(
			ChromeMessageReceiver handler)/*-{
		var rec = $entry(function(e) {
			if (e.origin != location.origin) {
				return;
			};
			if (!(e.data && e.data.source && e.data.source == "arc:cs"))
				return;
			if (!(e.data && e.data.payload))
				return;
			handler.@com.google.gwt.chrome.message.ChromeMessageReceiver::onMessage(Ljava/lang/String;Ljava/lang/String;)(e.data.payload,e.data.data+"");
		});

		$wnd.addEventListener("message", rec, false);
		var e = $doc.createEvent('Events');
		e.initEvent('ARC:READY');
		$wnd.dispatchEvent(e);

	}-*/;

	private JSONObject preparePostMessage() {
		JSONObject respObj = new JSONObject();
		respObj.put("source", new JSONString("arc:gwt"));
		return respObj;
	}

	private final native void sendExtensionMessage(String respObj)/*-{
		$wnd.postMessage(respObj, $wnd.location.href);
	}-*/;

	private final void handleExternalMessage(final String payload,
			final String message) {
		JSONObject respObj = null;
		if (payload.equals("ping")) {
			respObj = preparePostMessage();
			respObj.put("data", new JSONString("ok"));
			respObj.put("payload", new JSONString("ping"));
			sendExtensionMessage(respObj.toString());
		} else {
			if (messageCallbackList.containsKey(payload)) {
				messageCallbackList.get(payload).onSuccess(message);
				messageCallbackList.remove(payload);
			}
		}
	}
}
