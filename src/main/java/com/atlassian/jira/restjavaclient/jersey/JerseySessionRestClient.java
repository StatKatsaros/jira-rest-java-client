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
import com.atlassian.jira.restjavaclient.SessionRestClient;
import com.atlassian.jira.restjavaclient.domain.Session;
import com.atlassian.jira.restjavaclient.json.SessionJsonParser;
import com.sun.jersey.client.apache.ApacheHttpClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * TODO: Document this class / interface here
 *
 * @since v0.1
 */
public class JerseySessionRestClient implements SessionRestClient {
	private final ApacheHttpClient client;
	private final URI serverUri;
	private final SessionJsonParser sessionJsonParser = new SessionJsonParser();

	public JerseySessionRestClient(ApacheHttpClient client, URI serverUri) {
		this.client = client;
		this.serverUri = serverUri;
	}

	@Override
	public Session getCurrentSession(ProgressMonitor progressMonitor) {
		final JSONObject jsonObject = client.resource(UriBuilder.fromUri(serverUri).path("rest/auth/latest/session").build()).get(JSONObject.class);
		try {
			return sessionJsonParser.parse(jsonObject);
		} catch (JSONException e) {
			throw new RestClientException(e);
		}

	}
}
