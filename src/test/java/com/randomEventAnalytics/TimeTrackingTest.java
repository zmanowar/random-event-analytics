package com.randomEventAnalytics;


import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimeTrackingTest
{

	@Test
	public void testSetsIntervalsForOldProfiles()
	{
		int minutesSinceLastSpawn = 60;
		TimeTracking tracking = new TimeTracking();
		tracking.init(
			Instant.now(),
			minutesSinceLastSpawn * 60,
			0,
			0,
			null,
			-1
		);

		Assert.assertEquals(minutesSinceLastSpawn / TimeTracking.SPAWN_INTERVAL_SECONDS, tracking.getIntervalsSinceLastRandom());
	}

	@Test
	public void testResetsIntervalsAndSecondsWhenSpawned()
	{
		int minutesSinceLastSpawn = 60;
		TimeTracking tracking = new TimeTracking();
		tracking.init(
			Instant.now(),
			minutesSinceLastSpawn * 60,
			0,
			365,
			null,
			minutesSinceLastSpawn / 5
		);

		tracking.setRandomEventSpawned();

		Assert.assertEquals(0, tracking.getIntervalsSinceLastRandom());
		Assert.assertEquals(0, tracking.getTotalSecondsSinceLastRandomEvent());
	}
}
