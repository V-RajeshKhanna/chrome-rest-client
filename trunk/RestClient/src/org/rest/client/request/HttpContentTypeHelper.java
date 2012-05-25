/*******************************************************************************
 * Copyright 2012 Paweł Psztyć
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.rest.client.request;

/**
 * Helper class to provide access to default and user saved Content-Type values.
 * @author jarrod
 *
 */
public class HttpContentTypeHelper {
	
	private static String[] defaultValues = {
			"application/atom+xml",
			"application/json",
			"application/x-www-form-urlencoded",
			"application/xml",
			"multipart/form-data",
			"text/html",
			"text/plain"
	};
	
	/**
	 * Get all Content-Type values.
	 * It's provide values for default Content-Type headers and user defined values.
	 */
	public static String[] getAllValues(){
		
		return defaultValues;
	}
	
	/**
	 * Get default selected Content-Type value
	 * @return
	 */
	public static String getDefaulSelected(){
		return "application/x-www-form-urlencoded";
	}
}
