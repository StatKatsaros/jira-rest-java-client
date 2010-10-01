/*
 * Copyright (C) 2010 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.jira.restjavaclient.jersey;

import com.atlassian.jira.restjavaclient.ProgressMonitor;
import com.atlassian.jira.restjavaclient.RestClientException;
import com.atlassian.jira.restjavaclient.json.JsonParseUtil;
import com.atlassian.jira.restjavaclient.json.JsonParser;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * TODO: Document this class / interface here
 *
 * @since v0.1
 */
public class AbstractJerseyRestClient {
	protected final ApacheHttpClient client;
	protected final URI baseUri;

	public AbstractJerseyRestClient(URI baseUri, ApacheHttpClient client) {
		this.baseUri = baseUri;
		this.client = client;
	}

	protected <T> T invoke(Callable<T> callable) {
		try {
			return callable.call();
		} catch (UniformInterfaceException e) {
			try {
				final String body = e.getResponse().getEntity(String.class);
				final Collection<String> errorMessages = extractErrors(body);
				throw new RestClientException(errorMessages, e);
			} catch (JSONException e1) {
				throw new RestClientException(e);
			}
		} catch (RestClientException e) {
			throw e;
		} catch (Exception e) {
			throw new RestClientException(e);
		}
	}

	protected <T> T getAndParse(final URI uri, final JsonParser<T> parser, ProgressMonitor progressMonitor) {
		return invoke(new Callable<T>() {
			@Override
			public T call() throws Exception {
				final WebResource webResource = client.resource(uri);
				final JSONObject s = webResource.get(JSONObject.class);
				return parser.parse(s);
			}
		});

	}
	
	static Collection<String> extractErrors(String body) throws JSONException {
		JSONObject jsonObject = new JSONObject(body);
		final Collection<String> errorMessages = new ArrayList<String>();
		final JSONArray errorMessagesJsonArray = jsonObject.optJSONArray("errorMessages");
		if (errorMessagesJsonArray != null) {
			errorMessages.addAll(JsonParseUtil.toStringCollection(errorMessagesJsonArray));
		}
		final JSONObject errorJsonObject = jsonObject.optJSONObject("errors");
		if (errorJsonObject != null) {
			final JSONArray valuesJsonArray = errorJsonObject.toJSONArray(errorJsonObject.names());
			if (valuesJsonArray != null) {
				errorMessages.addAll(JsonParseUtil.toStringCollection(valuesJsonArray));
			}
		}
		return errorMessages;
	}


}