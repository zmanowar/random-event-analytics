package com.randomEventAnalytics;


import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimeTrackingTest
{

	@Inject
	private TimeTracking timeTracking;

	@Mock
	@Bind
	ConfigManager configManager;

	@Bind
	Clock clock = Clock.systemUTC();

	@Before
	public void before() {
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
		when(configManager.getRSProfileKey()).thenReturn("ABCDEF01");
	}

	@Test
	public void testSetsIntervalsForOldProfiles()
	{
		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, -1);

		int secondsSinceLastSpawn = 60 * 60 * 10;
		mockConfigGet(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, secondsSinceLastSpawn);

		timeTracking.init(
			Instant.now(),
			null
		);

		Assert.assertEquals(secondsSinceLastSpawn / TimeTracking.SPAWN_INTERVAL_SECONDS, timeTracking.getIntervalsSinceLastRandom());
	}

	@Test
	public void testResetsIntervalsAndSecondsWhenSpawned()
	{
		int secondsSinceLastSpawn = 60 * 60;

		Instant now = Instant.now();
		timeTracking.init(
			Instant.now(),
			now.minus(secondsSinceLastSpawn, ChronoUnit.SECONDS)
		);

		timeTracking.setRandomEventSpawned();

		// After a spawn, LAST_RANDOM_SPAWN_INSTANT should be persisted.
		verify(configManager, org.mockito.Mockito.atLeastOnce())
			.setRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, RandomEventAnalyticsConfig.LAST_RANDOM_SPAWN_INSTANT, timeTracking.getLastRandomSpawnInstant());

		// Window anchor is now the fresh spawn; earliest is 1 hour away so window is not yet open.
		Assert.assertEquals(timeTracking.getLastRandomSpawnInstant(), timeTracking.getWindowAnchor());
		Assert.assertFalse(timeTracking.isWindowOpen());
		Assert.assertTrue(timeTracking.getSecondsUntilEarliest() > 0);
	}

	@Test
	public void testResetsIntervalsAndSecondsWhenStrangePlantAccepted()
	{
		int secondsSinceLastSpawn = 60 * 60;
		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, secondsSinceLastSpawn / TimeTracking.SPAWN_INTERVAL_SECONDS);

		Instant loginTime = Instant.now().minus(25, ChronoUnit.MINUTES); // logged in 25 minutes ago.

		timeTracking.init(
			loginTime,
			loginTime.minus(secondsSinceLastSpawn, ChronoUnit.SECONDS)
		);

		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM,0);

		Instant spawnedInstant = loginTime.plus(10 * 60, ChronoUnit.SECONDS); // 15 minutes ago (10min after login)
		timeTracking.setStrangePlantSpawned(new RandomEventRecord(spawnedInstant.toEpochMilli(), timeTracking, null, null, null));

		Assert.assertEquals(3, timeTracking.getIntervalsSinceLastRandom());
		int newEventSpawnedSeconds = (int) Duration.between(timeTracking.getLastRandomSpawnInstant(), Instant.now()).getSeconds();
		Assert.assertEquals(900, newEventSpawnedSeconds);
		Assert.assertEquals(3, timeTracking.getIntervalsSinceLastRandom());

		Assert.assertEquals(900, timeTracking.getTotalSecondsSinceLastRandomEvent());
	}

	private void reinjectWithFixedClock(Instant fixedNow)
	{
		clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	// -------------------------------------------------------------------------
	// Window model tests (deterministic via fixed Clock)
	// -------------------------------------------------------------------------

	@Test
	public void testWindowNotYetOpen()
	{
		// "now" is 30 minutes after last event — window opens at 1h, so not yet.
		Instant anchor = Instant.parse("2026-01-01T12:00:00Z");
		Instant fixedNow = anchor.plus(30, ChronoUnit.MINUTES);
		reinjectWithFixedClock(fixedNow);

		timeTracking.init(fixedNow, anchor);

		Assert.assertFalse("Window should not be open yet", timeTracking.isWindowOpen());
		Assert.assertFalse("Window should not be expired", timeTracking.isWindowExpired());
		// 30 min elapsed of 60 needed → 30 min remaining until earliest
		Assert.assertEquals(30 * 60, timeTracking.getSecondsUntilEarliest());
		// 30 min elapsed of 120 total → 90 min remaining until latest
		Assert.assertEquals(90 * 60, timeTracking.getSecondsUntilLatest());
	}

	@Test
	public void testWindowOpen()
	{
		// "now" is 90 minutes after last event — inside the [1h, 2h] window.
		Instant anchor = Instant.parse("2026-01-01T12:00:00Z");
		Instant fixedNow = anchor.plus(90, ChronoUnit.MINUTES);
		reinjectWithFixedClock(fixedNow);

		timeTracking.init(fixedNow, anchor);

		Assert.assertTrue("Window should be open", timeTracking.isWindowOpen());
		Assert.assertFalse("Window should not be expired yet", timeTracking.isWindowExpired());
		Assert.assertEquals(0, timeTracking.getSecondsUntilEarliest());
		// 90 min elapsed of 120 total → 30 min remaining until latest
		Assert.assertEquals(30 * 60, timeTracking.getSecondsUntilLatest());
	}

	@Test
	public void testWindowExpiredAndOfflineExtensionLikely()
	{
		// "now" is 3 hours after last event — past the 2h window.
		// Plugin loaded while already past the window → offline extension likely.
		Instant anchor = Instant.parse("2026-01-01T12:00:00Z");
		Instant fixedNow = anchor.plus(3, ChronoUnit.HOURS);
		reinjectWithFixedClock(fixedNow);

		timeTracking.init(fixedNow, anchor);

		Assert.assertTrue("Window should be expired", timeTracking.isWindowExpired());
		Assert.assertTrue("Offline extension should be flagged", timeTracking.isOfflineExtensionLikely());
		Assert.assertEquals(0, timeTracking.getSecondsUntilEarliest());
		Assert.assertEquals(0, timeTracking.getSecondsUntilLatest());
	}

	@Test
	public void testNoEventsYetUsesFirstLoginAsAnchor()
	{
		// No prior random events recorded — firstLoginInstant should become the window anchor.
		Instant firstLogin = Instant.parse("2026-01-01T12:00:00Z");
		Instant fixedNow = firstLogin.plus(30, ChronoUnit.MINUTES);
		reinjectWithFixedClock(fixedNow);

		// init() with null lastRandomSpawnInstant — no FIRST_LOGIN_INSTANT in config yet.
		timeTracking.init(firstLogin, null);

		Assert.assertNull(timeTracking.getLastRandomSpawnInstant());
		Assert.assertEquals(firstLogin, timeTracking.getWindowAnchor());
		Assert.assertEquals(
			firstLogin.plusSeconds(TimeTracking.SECONDS_IN_AN_HOUR),
			timeTracking.getEarliestSpawnInstant());
		// 30 min after first login, window (1h) not yet open.
		Assert.assertFalse(timeTracking.isWindowOpen());
	}

	@Test
	public void testAccountSwitchRestoresIndependentWindow()
	{
		// Account 1 last event 3h ago → window expired.
		// Account 2 last event 30min ago → window not yet open.
		// Switching accounts via init() should give each account its own independent window.
		Instant fixedNow = Instant.parse("2026-01-01T12:00:00Z");
		Instant account1LastSpawn = Instant.parse("2026-01-01T09:00:00Z"); // 3h ago → expired
		Instant account2LastSpawn = Instant.parse("2026-01-01T11:30:00Z"); // 30min ago → not open
		reinjectWithFixedClock(fixedNow);

		// Simulate account 1 logged in.
		timeTracking.init(fixedNow, account1LastSpawn);
		Assert.assertTrue("Account 1 window should be expired", timeTracking.isWindowExpired());

		// Simulate switch to account 2 (plugin calls init() with the new account's persisted spawn time).
		timeTracking.init(fixedNow, account2LastSpawn);
		Assert.assertFalse("Account 2 window should not be open", timeTracking.isWindowOpen());
		Assert.assertFalse("Account 2 window should not be expired", timeTracking.isWindowExpired());
		Assert.assertEquals(
			account2LastSpawn.plusSeconds(TimeTracking.SECONDS_IN_AN_HOUR),
			timeTracking.getEarliestSpawnInstant());
	}

	private <T> void mockConfigGet(String key, T returnValue) {
		when(configManager.getRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, int.class)).thenReturn(returnValue);
	}

	private <T> void assertConfigSet(String key, T value) {
		verify(configManager).setRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, value);
	}
}
