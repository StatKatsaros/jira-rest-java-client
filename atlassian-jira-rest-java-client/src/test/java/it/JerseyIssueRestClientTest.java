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

package it;

import com.atlassian.jira.restjavaclient.IntegrationTestUtil;
import com.atlassian.jira.restjavaclient.IssueRestClient;
import com.atlassian.jira.restjavaclient.IterableMatcher;
import com.atlassian.jira.restjavaclient.NullProgressMonitor;
import com.atlassian.jira.restjavaclient.TestUtil;
import com.atlassian.jira.restjavaclient.domain.Attachment;
import com.atlassian.jira.restjavaclient.domain.BasicUser;
import com.atlassian.jira.restjavaclient.domain.Comment;
import com.atlassian.jira.restjavaclient.domain.FieldInput;
import com.atlassian.jira.restjavaclient.domain.Issue;
import com.atlassian.jira.restjavaclient.domain.Transition;
import com.atlassian.jira.restjavaclient.domain.TransitionInput;
import com.atlassian.jira.restjavaclient.domain.Votes;
import com.atlassian.jira.restjavaclient.domain.Watchers;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static com.atlassian.jira.restjavaclient.IntegrationTestUtil.*;
import static com.atlassian.jira.restjavaclient.json.TestConstants.USER1_PASSWORD;
import static com.atlassian.jira.restjavaclient.json.TestConstants.USER1_USERNAME;
import static org.junit.Assert.assertThat;


/**
 * TODO: Document this class / interface here
 *
 * @since v0.1
 */
public class JerseyIssueRestClientTest extends AbstractRestoringJiraStateJerseyRestClientTest {

	// no timezone here, as JIRA does not store timezone information in its dump file
	private final DateTime dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2010-08-04T17:46:45.454");

	@Test
	public void testGetWatchers() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		final Watchers watchers = client.getIssueClient().getWatchers(issue, new NullProgressMonitor());
		assertEquals(1, watchers.getNumWatchers());
		assertFalse(watchers.isWatching());
		assertThat(watchers.getUsers(), IterableMatcher.hasOnlyElements(USER1));
	}

	public void testGetWatcherForAnonymouslyAccessibleIssue() {
		setAnonymousMode();
		final Issue issue = client.getIssueClient().getIssue("ANNON-1", new NullProgressMonitor());
		final Watchers watchers = client.getIssueClient().getWatchers(issue, pm);
		assertEquals(1, watchers.getNumWatchers());
		assertFalse(watchers.isWatching());
		assertTrue("JRADEV-3594 bug!!!", Iterables.isEmpty(watchers.getUsers()));
	}


	public URI jiraRestUri(String path) {
		return UriBuilder.fromUri(jiraRestRootUri).path(path).build();
	}

	@Test
	public void testGetIssue() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		assertEquals("TST-1", issue.getKey());
		assertTrue(issue.getSelf().toString().startsWith(jiraUri.toString()));

		assertEquals(3, Iterables.size(issue.getComments()));
		assertThat(issue.getExpandos(), IterableMatcher.hasOnlyElements("html"));

		assertEquals(IntegrationTestUtil.START_PROGRESS_TRANSITION_ID, Iterables.size(issue.getAttachments()));
		final Iterable<Attachment> items = issue.getAttachments();
		assertNotNull(items);
		Attachment attachment1 = new Attachment(IntegrationTestUtil.concat(jiraRestRootUri, "/attachment/10040"),
				"dla Paw\u0142a.txt", IntegrationTestUtil.USER_ADMIN, dateTime, 643, "text/plain",
				IntegrationTestUtil.concat(jiraUri, "/secure/attachment/10040/dla+Paw%C5%82a.txt"), null);

		assertEquals(attachment1, items.iterator().next());

	}

	public void testGetIssueWithNoViewWatchersPermission() {
		setUser1();
		assertTrue(client.getIssueClient().getIssue("TST-1", pm).getWatchers().isWatching());

		setUser2();
		final Issue issue = client.getIssueClient().getIssue("TST-1", pm);
		assertFalse(issue.getWatchers().isWatching());
		client.getIssueClient().watch(issue, pm);
		final Issue watchedIssue = client.getIssueClient().getIssue("TST-1", pm);
		assertTrue(watchedIssue.getWatchers().isWatching());
		assertEquals(2, watchedIssue.getWatchers().getNumWatchers());

		// although there are 2 watchers, only one is listed with details - the caller itself, as the caller does not
		// have view watchers and voters permission 
		assertThat(client.getIssueClient().getWatchers(watchedIssue, pm).getUsers(), IterableMatcher.hasOnlyElements(USER2));
	}

	@Test
	public void testGetVoter() {
		final Issue issue = client.getIssueClient().getIssue("TST-1", pm);
		final Votes votes = client.getIssueClient().getVotes(issue, pm);
		assertFalse(votes.hasVoted());
		assertThat(votes.getUsers(), IterableMatcher.hasOnlyElements(USER1));
	}


	@Test
	public void testGetVotersWithoutViewVotersPermission() {
		setUser2();
		assertNumVotesAndNoVotersDetails("TST-1", 1);
	}

	@Test
	public void testGetVotersAnonymously() {
		setAnonymousMode();
		assertNumVotesAndNoVotersDetails("ANNON-1", 0);
	}


	private void assertNumVotesAndNoVotersDetails(final String issueKey, final int numVotes) {
		final Issue issue = client.getIssueClient().getIssue(issueKey, pm);
		assertEquals(numVotes, issue.getVotes().getVotes());
		assertFalse(issue.getVotes().hasVoted());
		final Votes votes = client.getIssueClient().getVotes(issue, pm);
		assertFalse(votes.hasVoted());
		assertEquals(numVotes, votes.getVotes());
		assertTrue(Iterables.isEmpty(votes.getUsers()));
	}


	@Test
	public void testGetTransitions() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, new NullProgressMonitor());
		assertEquals(4, Iterables.size(transitions));
		assertTrue(Iterables.contains(transitions, new Transition("Start Progress", IntegrationTestUtil.START_PROGRESS_TRANSITION_ID, Collections.<Transition.Field>emptyList())));
	}

	@Test
	public void testTransition() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, new NullProgressMonitor());
		assertEquals(4, Iterables.size(transitions));
		final Transition startProgressTransition = new Transition("Start Progress", IntegrationTestUtil.START_PROGRESS_TRANSITION_ID, Collections.<Transition.Field>emptyList());
		assertTrue(Iterables.contains(transitions, startProgressTransition));

		client.getIssueClient().transition(issue, new TransitionInput(IntegrationTestUtil.START_PROGRESS_TRANSITION_ID,
				Collections.<FieldInput>emptyList(), Comment.valueOf("My test comment")), new NullProgressMonitor()) ;
		final Issue transitionedIssue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		assertEquals("In Progress", transitionedIssue.getStatus().getName());
		final Iterable<Transition> transitionsAfterTransition = client.getIssueClient().getTransitions(issue, new NullProgressMonitor());
		assertFalse(Iterables.contains(transitionsAfterTransition, startProgressTransition));
		final Transition stopProgressTransition = new Transition("Stop Progress", IntegrationTestUtil.STOP_PROGRESS_TRANSITION_ID, Collections.<Transition.Field>emptyList());
		assertTrue(Iterables.contains(transitionsAfterTransition, stopProgressTransition));
	}


	@Test
	public void testTransitionWithNumericCustomFieldPolishLocale() throws Exception {
		final double newValue = 123.45;
		final FieldInput fieldInput = new FieldInput(NUMERIC_CUSTOMFIELD_ID,
				NumberFormat.getNumberInstance(new Locale("pl")).format(newValue));
		assertTransitionWithNumericCustomField(fieldInput, newValue);
	}

	@Test
	public void testTransitionWithNumericCustomFieldEnglishLocale() throws Exception {
		setClient(USER1_USERNAME, USER1_PASSWORD);
		final double newValue = 123.45;
		final FieldInput fieldInput = new FieldInput(NUMERIC_CUSTOMFIELD_ID,
				NumberFormat.getNumberInstance(new Locale("pl")).format(newValue));

		TestUtil.assertErrorCode(Response.Status.BAD_REQUEST, "'" + fieldInput.getValue() + "' is an invalid number", new Runnable() {
			@Override
			public void run() {
				assertTransitionWithNumericCustomField(fieldInput, newValue);
			}
		});

		final FieldInput fieldInput2 = new FieldInput(NUMERIC_CUSTOMFIELD_ID, newValue); // this will be serialized always with "." according to JSL
		assertTransitionWithNumericCustomField(fieldInput2, newValue);

	}


	private void assertTransitionWithNumericCustomField(FieldInput fieldInput, Double expectedValue) {
		final Issue issue = client.getIssueClient().getIssue("TST-1", new NullProgressMonitor());
		assertNull(issue.getField(NUMERIC_CUSTOMFIELD_ID).getValue());
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, new NullProgressMonitor());

		final Transition transitionFound = getTransitionByName(transitions, "Estimate");
		assertNotNull(transitionFound);
		assertTrue(Iterables.contains(transitionFound.getFields(),
				new Transition.Field(NUMERIC_CUSTOMFIELD_ID, false, NUMERIC_CUSTOMFIELD_TYPE)));
		client.getIssueClient().transition(issue, new TransitionInput(transitionFound.getId(), Arrays.asList(fieldInput),
				Comment.valueOf("My test comment")), new NullProgressMonitor());
		final Issue changedIssue = client.getIssueClient().getIssue("TST-1", pm);
		assertTrue(changedIssue.getField(NUMERIC_CUSTOMFIELD_ID).getValue().equals(expectedValue));
	}

	@Test
	public void testTransitionWithNumericCustomFieldAndInteger() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", pm);
		assertNull(issue.getField(NUMERIC_CUSTOMFIELD_ID).getValue());
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, pm);
		Transition transitionFound = getTransitionByName(transitions, "Estimate");

		assertNotNull(transitionFound);
		assertTrue(Iterables.contains(transitionFound.getFields(),
				new Transition.Field(NUMERIC_CUSTOMFIELD_ID, false, NUMERIC_CUSTOMFIELD_TYPE)));
		final double newValue = 123;
		final FieldInput fieldInput = new FieldInput(NUMERIC_CUSTOMFIELD_ID, newValue);
		client.getIssueClient().transition(issue, new TransitionInput(transitionFound.getId(), Arrays.asList(fieldInput),
				Comment.valueOf("My test comment")), pm);
		final Issue changedIssue = client.getIssueClient().getIssue("TST-1", pm);
		assertEquals(newValue, changedIssue.getField(NUMERIC_CUSTOMFIELD_ID).getValue());
	}

	@Test
	public void testTransitionWithInvalidNumericField() throws Exception {
		final Issue issue = client.getIssueClient().getIssue("TST-1", pm);
		assertNull(issue.getField(NUMERIC_CUSTOMFIELD_ID).getValue());
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, pm);
		final Transition transitionFound = getTransitionByName(transitions, "Estimate");

		assertNotNull(transitionFound);
		assertTrue(Iterables.contains(transitionFound.getFields(),
				new Transition.Field(NUMERIC_CUSTOMFIELD_ID, false, NUMERIC_CUSTOMFIELD_TYPE)));
		final FieldInput fieldInput = new FieldInput(NUMERIC_CUSTOMFIELD_ID, "]432jl");
		// warning: Polish language here - I am asserting if the messages are indeed localized
		TestUtil.assertErrorCode(Response.Status.BAD_REQUEST, "']432jl' nie jest prawid\u0142ow\u0105 liczb\u0105", new Runnable() {
			@Override
			public void run() {
				client.getIssueClient().transition(issue, new TransitionInput(transitionFound.getId(), Arrays.asList(fieldInput),
						Comment.valueOf("My test comment")), pm);
			}
		});
	}


	public void testTransitionWithNoRoleOrGroup() {
		Comment comment = Comment.valueOf("My text which I am just adding " + new DateTime());
		testTransitionImpl(comment);
	}

	@Test
	public void testTransitionWithRoleLevel() {
		Comment comment = Comment.createWithRoleLevel("My text which I am just adding " + new DateTime(), "Users");
		testTransitionImpl(comment);
	}

	@Test
	public void testTransitionWithGroupLevel() {
		Comment comment = Comment.createWithGroupLevel("My text which I am just adding " + new DateTime(), "jira-users");
		testTransitionImpl(comment);
	}

	private void testTransitionImpl(Comment comment) {
		final Issue issue = client.getIssueClient().getIssue("TST-1", pm);
		final Iterable<Transition> transitions = client.getIssueClient().getTransitions(issue, pm);
		Transition transitionFound = getTransitionByName(transitions, "Estimate");
		DateTime now = new DateTime();
		client.getIssueClient().transition(issue, new TransitionInput(transitionFound.getId(), comment), pm);

		final Issue changedIssue = client.getIssueClient().getIssue("TST-1", pm);
		final Comment lastComment = Iterables.getLast(changedIssue.getComments());
		assertEquals(comment.getBody(), lastComment.getBody());
		assertEquals(USER_ADMIN, lastComment.getAuthor());
		assertEquals(USER_ADMIN, lastComment.getUpdateAuthor());
		assertEquals(lastComment.getCreationDate(), lastComment.getUpdateDate());
		assertTrue(lastComment.getCreationDate().isAfter(now) || lastComment.getCreationDate().isEqual(now));
		assertEquals(comment.getGroupLevel(), lastComment.getGroupLevel());
		// @todo restore it when JIRA REST is fixed
//		assertEquals(comment.getRoleLevel(), lastComment.getRoleLevel());
	}

	@Test
	public void testVoteUnvote() {
		final Issue issue1 = client.getIssueClient().getIssue("TST-1", pm);
		assertFalse(issue1.getVotes().hasVoted());
		assertEquals(1, issue1.getVotes().getVotes()); // the other user has voted

		// I hope that such Polish special characters (for better testing local specific behaviour of REST
		TestUtil.assertErrorCode(Response.Status.NOT_FOUND, "Nie mo\u017cesz g\u0142osowa\u0107 na zadanie kt\u00f3re utworzy\u0142e\u015b.", new Runnable() {
			@Override
			public void run() {
				client.getIssueClient().vote(issue1, pm);
			}
		});


		final String issueKey = "TST-7";
		Issue issue = client.getIssueClient().getIssue(issueKey, pm);
		assertFalse(issue.getVotes().hasVoted());
		assertEquals(0, issue.getVotes().getVotes());

		client.getIssueClient().vote(issue, pm);
		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertTrue(issue.getVotes().hasVoted());
		assertEquals(1, issue.getVotes().getVotes());

		client.getIssueClient().unvote(issue, pm);
		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertFalse(issue.getVotes().hasVoted());
		assertEquals(0, issue.getVotes().getVotes());

		setUser2();
		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertFalse(issue.getVotes().hasVoted());
		assertEquals(0, issue.getVotes().getVotes());
		final Issue finalIssue = issue;
		TestUtil.assertErrorCode(Response.Status.NOT_FOUND, "Cannot remove a vote for an issue that the user has not already voted for.", new Runnable() {
			@Override
			public void run() {
				client.getIssueClient().unvote(finalIssue, pm);
			}
		});


		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertFalse(issue.getVotes().hasVoted());
		assertEquals(0, issue.getVotes().getVotes());
		client.getIssueClient().vote(issue, pm);
		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertTrue(issue.getVotes().hasVoted());
		assertEquals(1, issue.getVotes().getVotes());

		setClient(ADMIN_USERNAME, ADMIN_PASSWORD);
		client.getIssueClient().vote(issue, pm);
		issue = client.getIssueClient().getIssue(issueKey, pm);
		assertTrue(issue.getVotes().hasVoted());
		assertEquals(2, issue.getVotes().getVotes());
	}

	@Test
	public void testWatchUnwatch() {
		final IssueRestClient issueClient = client.getIssueClient();
		final Issue issue1 = issueClient.getIssue("TST-1", pm);

		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(),
				Matchers.not(IterableMatcher.contains(USER_ADMIN)));

		issueClient.watch(issue1, pm);
		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(), IterableMatcher.contains(USER_ADMIN));

		issueClient.unwatch(issue1, pm);
		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(), Matchers.not(IterableMatcher.contains(USER_ADMIN)));

		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(), IterableMatcher.contains(USER1));
		issueClient.removeWatcher(issue1, USER1.getName(), pm);
		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(), Matchers.not(IterableMatcher.contains(USER1)));
		issueClient.addWatcher(issue1, USER1.getName(), pm);
		Assert.assertThat(issueClient.getWatchers(issue1, pm).getUsers(), IterableMatcher.contains(USER1));
	}

	@Test
	public void testRemoveWatcherUnauthorized() {
		final IssueRestClient issueClient = client.getIssueClient();
		final Issue issue1 = issueClient.getIssue("TST-1", pm);
		issueClient.watch(issue1, pm);

		setUser1();
		final IssueRestClient issueClient2 = client.getIssueClient();
		TestUtil.assertErrorCode(Response.Status.UNAUTHORIZED,
				"User 'wseliga' is not allowed to remove watchers from issue 'TST-1'", new Runnable() {
			@Override
			public void run() {
				issueClient2.removeWatcher(issue1, ADMIN_USERNAME, pm);
			}
		});
	}


	@Test
	public void testWatchAlreadyWatched() {
		setUser1();
		final IssueRestClient issueClient = client.getIssueClient();
		final Issue issue = issueClient.getIssue("TST-1", pm);
		Assert.assertThat(client.getIssueClient().getWatchers(issue, pm).getUsers(), IterableMatcher.contains(USER1));
		// JIRA allows to watch already watched issue by you - such action effectively has no effect
		issueClient.watch(issue, pm);
		Assert.assertThat(client.getIssueClient().getWatchers(issue, pm).getUsers(), IterableMatcher.contains(USER1));
	}

	@Test
	public void testAddWatcherUnauthorized() {
		final IssueRestClient issueClient = client.getIssueClient();
		final Issue issue1 = issueClient.getIssue("TST-1", pm);
		issueClient.addWatcher(issue1, USER1_USERNAME, pm);
		assertThat(client.getIssueClient().getWatchers(issue1, pm).getUsers(), IterableMatcher.contains(USER1));

		setUser1();
		assertTrue(client.getIssueClient().getIssue("TST-1", pm).getWatchers().isWatching());
	}


	private Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
		Transition transitionFound = null;
		for (Transition transition : transitions) {
			if (transition.getName().equals(transitionName)) {
				transitionFound = transition;
				break;
			}
		}
		return transitionFound;
	}
}