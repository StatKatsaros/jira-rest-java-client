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

package com.atlassian.jira.restjavaclient.json;

import com.atlassian.jira.restjavaclient.domain.BasicComponent;
import com.atlassian.jira.restjavaclient.domain.BasicUser;
import com.atlassian.jira.restjavaclient.domain.Project;
import com.atlassian.jira.restjavaclient.domain.Version;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * TODO: Document this class / interface here
 *
 * @since v0.1
 */
public class ProjectJsonParser implements JsonParser<Project> {
	private final VersionJsonParser versionJsonParser = new VersionJsonParser();
	private final JsonParser<BasicComponent> componentJsonParser = ComponentJsonParser.createBasicComponentParser();
	@Override
	public Project parse(JSONObject json) throws JSONException {
		URI self = JsonParseUtil.getSelfUri(json);
		final BasicUser lead = JsonParseUtil.parseBasicUser(json.getJSONObject("lead"));
		final String key = json.getString("key");
		final String urlStr = json.getString("url");
		URI uri;
		try {
			 uri = "".equals(urlStr) ? null : new URI(urlStr);
		} catch (URISyntaxException e) {
			uri = null;
		}
		final String description = json.getString("description");
		final Collection<Version> versions = JsonParseUtil.parseJsonArray(json.getJSONArray("versions"), versionJsonParser);
		final Collection<BasicComponent> components = JsonParseUtil.parseJsonArray(json.getJSONArray("components"), componentJsonParser);
		return new Project(self, key, description, lead, uri, versions, components);

	}
}
