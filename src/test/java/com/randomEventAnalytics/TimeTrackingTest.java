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
}