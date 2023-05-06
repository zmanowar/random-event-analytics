package com.randomEventAnalytics;


import com.google.inject.testing.fieldbinder.Bind;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

	@Mock
	@Bind
	ConfigManager configManager;

	@Before
	public void before() {
		when(configManager.getRSProfileKey()).thenReturn("ABCDEF01");
	}

	@Test
	public void testSetsIntervalsForOldProfiles()
	{
		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, -1);
		mockConfigGet(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, 0);
		mockConfigGet(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, 0);

		int secondsSinceLastSpawn = 60 * 60 * 10;
		mockConfigGet(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, secondsSinceLastSpawn);

		TimeTracking tracking = new TimeTracking();
		tracking.init(
			Instant.now(),
			null
		);

		Assert.assertEquals(secondsSinceLastSpawn / TimeTracking.SPAWN_INTERVAL_SECONDS, tracking.getIntervalsSinceLastRandom());
	}

	@Test
	public void testResetsIntervalsAndSecondsWhenSpawned()
	{
		int secondsSinceLastSpawn = 60 * 60;

		Instant now = Instant.now();
		TimeTracking tracking = new TimeTracking();
		tracking.init(
			Instant.now(),
			now.minus(secondsSinceLastSpawn, ChronoUnit.SECONDS)
		);

		tracking.setRandomEventSpawned();

		assertConfigSet(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, 0);
		assertConfigSet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, 0);
		assertConfigSet(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, 0);
		assertConfigSet(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, 0);


		Assert.assertEquals(0, tracking.getIntervalsSinceLastRandom());
		Assert.assertEquals(0, tracking.getTotalSecondsSinceLastRandomEvent());
	}

	@Test
	public void testResetsIntervalsAndSecondsWhenStrangePlantAccepted()
	{
		int secondsSinceLastSpawn = 60 * 60;
		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, secondsSinceLastSpawn / TimeTracking.SPAWN_INTERVAL_SECONDS);
		mockConfigGet(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, (int) (secondsSinceLastSpawn * 0.6));
		mockConfigGet(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, 0);
		mockConfigGet(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, secondsSinceLastSpawn);

		Instant loginTime = Instant.now().minus(25, ChronoUnit.MINUTES ); // logged in 25 minutes ago.
		TimeTracking tracking = new TimeTracking();
		tracking.init(
			loginTime,
			loginTime.minus(secondsSinceLastSpawn, ChronoUnit.SECONDS)
		);

		mockConfigGet(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM,0);

		Instant spawnedInstant = loginTime.plus(10 * 60, ChronoUnit.SECONDS); // 15 minutes ago (10min after login)
		tracking.setStrangePlantSpawned(new RandomEventRecord(spawnedInstant.toEpochMilli(), tracking, null, null, null));

		Assert.assertEquals(3, tracking.getIntervalsSinceLastRandom());
		int newEventSpawnedSeconds = (int) Duration.between(tracking.getLastRandomSpawnInstant(), Instant.now()).getSeconds();
		Assert.assertEquals(900, newEventSpawnedSeconds);
		Assert.assertEquals(3, tracking.getIntervalsSinceLastRandom());

		Assert.assertEquals(900, tracking.getTotalSecondsSinceLastRandomEvent());
	}

	private <T> void mockConfigGet(String key, T returnValue) {
		when(configManager.getRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, int.class)).thenReturn(returnValue);
	}

	private <T> void assertConfigSet(String key, T value) {
		verify(configManager).setRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, value);
	}
}
