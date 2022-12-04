package com.randomEventAnalytics.localstorage;

import com.randomEventAnalytics.RandomEventAnalyticsTimeTracking;
import lombok.Data;

@Data
public class RandomEventRecord
{
	// Random event spawned  time
	public final long spawnedTime;
	// Seconds (logged in) since last random event occurred
	public final int secondsSinceLastRandomEvent;
	// Ticks (logged in) since last random event occurred
	public final int ticksSinceLastRandomEvent;
	// Total 5-minute intervals that have passed.
	public final int intervalsSinceLastRandom;
	public final NpcInfoRecord npcInfoRecord;
	public final PlayerInfoRecord playerInfoRecord;
	public final XpInfoRecord xpInfoRecord;
	private final int secondsInInstance;

	public RandomEventRecord(long spawnedTime, RandomEventAnalyticsTimeTracking timeTracking,
							 NpcInfoRecord npcInfoRecord, PlayerInfoRecord playerInfoRecord, XpInfoRecord xpInfoRecord)
	{
		this.spawnedTime = spawnedTime;
		this.secondsSinceLastRandomEvent = timeTracking.getTotalSecondsSinceLastRandomEvent();
		this.ticksSinceLastRandomEvent = timeTracking.getTicksSinceLastRandomEvent();
		this.secondsInInstance = timeTracking.getSecondsInInstance();
		this.intervalsSinceLastRandom = timeTracking.getIntervalsSinceLastRandom();
		this.xpInfoRecord = xpInfoRecord;
		this.playerInfoRecord = playerInfoRecord;
		this.npcInfoRecord = npcInfoRecord;
	}
}