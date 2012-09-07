package org.rest.client.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rest.client.ClientFactory;
import org.rest.client.RestClient;
import org.rest.client.request.AssetRequest;
import org.rest.client.request.AssetStringCallback;
import org.rest.client.storage.StoreResultCallback;
import org.rest.client.storage.store.HeadersStoreWebSql;
import org.rest.client.storage.store.LocalStore;
import org.rest.client.storage.store.StatusesStoreWebSql;
import org.rest.client.storage.store.objects.RequestObject;
import org.rest.client.storage.websql.AppDatabase;
import org.rest.client.storage.websql.HeaderRow;
import org.rest.client.storage.websql.StatusCodeRow;
import org.rest.client.task.ui.LoaderWidget;
import org.rest.client.util.JSONHeadersUtils;

import com.allen_sauer.gwt.log.client.Log;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

/**
 * Task will execute only if it is first run. 
 * It downloads and insert to database headers and status codes definitions. 
 * @author jarrod
 *
 * Upgrade options:
 * - From previous version: 
 * 		- update database structure
 * 		- rename local store keys
 * 		- change latestRequest data structure
 * 		- insert old history data to new history table
 * - Fresh install
 * 		- insert default data to view (method and URL)
 * 		- insert default JSON headers
 * 		- download status codes definitions
 */
public class FirstRunTask implements LoadTask {

	private final static String FIRST_RUN_FLAG = "firstrun";
	TasksCallback callback;
	LoaderWidget loaderWidget;
	boolean lastRun;
	ClientFactory factory = RestClient.getClientFactory();
	Storage store = Storage.getLocalStorageIfSupported();
	
	interface SimpleCallback {
		void onResult();
	}
	
	
	@Override
	public void run(final TasksCallback callback, final boolean lastRun) {
		upgradeAppliction();
		Storage storage = Storage.getLocalStorageIfSupported();
		String firstRunFlag = storage.getItem(FIRST_RUN_FLAG);
		if(firstRunFlag != null){
			callback.onInnerTaskFinished(getTasksCount());
			callback.onSuccess();
			return;
		}
		this.callback = callback;
		this.lastRun = lastRun;
		callback.onInnerTaskFinished(1);
		// this is first run. lest's get to work! :)
		// Download headers and status codes definitions and insert to DB
		
		String oldDbSet = store.getItem("databaseSet");
		if(oldDbSet == null || oldDbSet.isEmpty()){
			//fresh install
			createAppliction();
		} else {
			//upgrade
			upgradeAppliction();
		}
	}
	/**
	 * From previous version: 
	 * 	- update database structure -OK
	 * 	- move data from old rest_forms table to request_data table -OK
	 *  - insert old history data to new history table
	 * 	- rename local store keys
	 * 	- change latestRequest data structure
	 */
	private void upgradeAppliction(){
		//check if DB need upgrade
		AppDatabase service = GWT.create(AppDatabase.class);
		if(service.getDatabase().getVersion().equals("")){
			service.getDatabase().changeVersion("", "1.0", new TransactionCallback() {
				@Override
				public void onTransactionSuccess() {
					
				}
				
				@Override
				public void onTransactionStart(SQLTransaction transaction) {
					upgradeRequestsData(transaction);
				}
				
				@Override
				public void onTransactionFailure(SQLError error) {
					
				}
			});
		}
	}
	
	
	private final native void upgradeRequestsData(SQLTransaction tx) /*-{
		console.log('start upgrade');
		tx.executeSql("SELECT * FROM rest_forms",[],function(tx, result){
			var cnt = result.rows.length;
			for(var i=0; i<cnt; i++){
				var item = result.rows.item(i);
				try{
					var data = JSON.parse(item.data);
					var headers = "";
					for(var headerNumber in data['headers']){ 
						var header = data['headers'][headerNumber]; 
						for(var key in header){
							headers += key + ": " + header[key] + "\n";
						}
					}
					var sql = "INSERT INTO request_data (name,url,method,encoding,headers,payload,time) VALUES ";
					sql += "(";
					sql += "'"+item['name'].replace(/"/gim,'\\"')+"',";
					sql += "'"+data['url'].replace(/"/gim,'\\"')+"',";
					sql += "'"+data['method'].replace(/"/gim,'\\"')+"',";
					sql += "'"+data['formEncoding'].replace(/"/gim,'\\"')+"',";
					sql += "'"+headers.replace(/"/gim,'\\"')+"',";
					sql += "'"+data['post'].replace(/"/gim,'\\"')+"',";
					sql += item['time'];
					sql += ")";
					tx.executeSql(sql,[],null,function(tx, error){console.error(error.message);});
				} catch(e){
					console.error(e)
				}
			}
		}, function(tx, error){console.log(error.message);});
	}-*/;
	
	
	
	
	/**
	 * Fresh install
	 * 	- insert default data to view (method and URL)
 	 * 	- insert default JSON headers
 	 * 	- download status codes definitions
	 */
	private void createAppliction(){
		RequestObject ro = RequestObject.createRequest();
		ro.setMethod("GET");
		ro.setURL("http://gdata.youtube.com/feeds/api/playlists/56D792A831D0C362/?v=2&alt=json&feature=plcp");
		store.setItem(LocalStore.LATEST_REQUEST_KEY, ro.toJSON());
	
		String[] jsonArray = new String[]{"application/json", "text/json", "text/x-json"};
		JSONHeadersUtils.store(jsonArray, new Callback<Boolean, Throwable>() {
			
			@Override
			public void onSuccess(Boolean result) {
				callback.onInnerTaskFinished(1);
				downloadDefinitionsTask();
			}
			
			@Override
			public void onFailure(Throwable reason) {
				if(lastRun){
					callback.onInnerTaskFinished(1);
					downloadDefinitionsTask();
				} else {
					callback.onFailure(1);
				}
			}
		});
	}
	
	
	
	
	
	
	private void downloadDefinitionsTask(){
		loaderWidget.setText("Downloading definitions...");
		
		AssetRequest.getAssetString("definitions.json", new AssetStringCallback() {
			@Override
			public void onSuccess(String response) {
				callback.onInnerTaskFinished(1);
				parseAssetResponse(response);
			}
			
			@Override
			public void onFailure(String message, Throwable exception) {
				if(lastRun){
					if(RestClient.isDebug()){
						Log.error("Error download application data file. Will try next time.");
					}
					Window.alert("Unable to download application data. Will try again next time.");
					loaderWidget.setText("Loading...");
					callback.onInnerTaskFinished(getTasksCount()-1);
					callback.onSuccess();
					return;
				}
				loaderWidget.setText("Unable to download definitions. Retrying...");
				callback.onFailure(1);
			}
		});
	}
	
	private void parseAssetResponse(String json){
		loaderWidget.setText("Creatintg databases...");
		JSONValue data = JSONParser.parseLenient(json);
		final JSONObject obj = data.isObject();
		if (obj == null) {
			if(RestClient.isDebug()){
				Log.error("Error download application data file. Will try next time.");
			}
			Window.alert("Unable to parse downloaded application data. Will try again next time.");
			loaderWidget.setText("Loading...");
			callback.onInnerTaskFinished(getTasksCount()-2);
			callback.onSuccess();
			return;
		}
		List<HeaderRow> headers = prepareHeadersData(obj);
		saveHeadersTask(headers, new SimpleCallback(){
			@Override
			public void onResult() {
				callback.onInnerTaskFinished(1);
				prepareAndSaveStauses(obj);
			}});
	}
	
	private List<HeaderRow> prepareHeadersData(JSONObject obj){
		List<HeaderRow> toSave = new ArrayList<HeaderRow>();
		JSONArray reqArray = obj.get("requests").isArray();
		JSONArray resArray = obj.get("responses").isArray();
		parseHeadersArray(toSave, reqArray, "request");
		parseHeadersArray(toSave, resArray, "response");
		return toSave;
	}
	
	private void parseHeadersArray(List<HeaderRow> toSave, JSONArray arr, String type){
		int cnt = arr.size();
		for (int i = 0; i < cnt; i++) {
			JSONObject obj = arr.get(i).isObject();
			if (obj == null)
				continue;
			JSONString keyJson = obj.get("key").isString();
			JSONString descJson = obj.get("desc").isString();
			JSONString exampleJson = obj.get("example").isString();
			if (keyJson == null || descJson == null || exampleJson == null) {
				continue;
			}
			HeaderRow row = HeaderRow.create();
			row.setName(keyJson.stringValue());
			row.setType(type);
			row.setDesc(descJson.stringValue());
			row.setExample(exampleJson.stringValue());
			toSave.add(row);
		}
	}
	
	private void saveHeadersTask(List<HeaderRow> headers, final SimpleCallback callback){
		
		HeadersStoreWebSql headersStore = factory.getHeadersStore();
		headersStore.putAll(headers, new StoreResultCallback<List<Integer>>() {
			@Override
			public void onSuccess(List<Integer> result) {
				callback.onResult();
			}
			@Override
			public void onError(Throwable e) {
				callback.onResult();
			}
		});
	}
	
	private void prepareAndSaveStauses(JSONObject _obj){
		JSONArray array = _obj.get("codes").isArray();
		ArrayList<StatusCodeRow> insert = new ArrayList<StatusCodeRow>();
		int cnt = array.size();
		for (int i = 0; i < cnt; i++) {
			JSONObject obj = array.get(i).isObject();
			if (obj == null)
				continue;
			JSONNumber codeJson = obj.get("key").isNumber();
			JSONString descJson = obj.get("desc").isString();
			JSONString labelJson = obj.get("label").isString();

			if (codeJson == null || descJson == null || labelJson == null) {
				continue;
			}
			
			StatusCodeRow row = StatusCodeRow.create();
			row.setCode(Integer.parseInt(codeJson.toString()));
			row.setDesc(descJson.stringValue());
			row.setLabel(labelJson.stringValue());
			insert.add(row);
		}
		saveStatusCodesTask(insert);
	}
	
	private void saveStatusCodesTask(ArrayList<StatusCodeRow> insert){
		StatusesStoreWebSql statusesStore = factory.getStatusesStore();
		statusesStore.putAll(insert, new StoreResultCallback<List<Integer>>() {
			@Override
			public void onSuccess(List<Integer> result) {
				postConfigTask();
			}
			
			@Override
			public void onError(Throwable e) {
				postConfigTask();
			}
		});
	}
	
	private void postConfigTask(){
		callback.onInnerTaskFinished(1);
		Storage storage = Storage.getLocalStorageIfSupported();
		Date d = new Date();
		String time = String.valueOf(d.getTime());
		storage.setItem(FIRST_RUN_FLAG, time);
		loaderWidget.setText("Update complete. Thank you.");
		new Timer() {
			@Override
			public void run() {
				loaderWidget.setText("Loading...");
				callback.onSuccess();
			}
		}.schedule(1500);
	}
	
	@Override
	public int getTasksCount() {
		return 6;
	}

	@Override
	public void setLoader(LoaderWidget loaderWidget) {
		this.loaderWidget = loaderWidget;
	}

}
