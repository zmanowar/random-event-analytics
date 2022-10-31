package com.randomEventAnalytics.localstorage;

import com.randomEventAnalytics.RandomEventAnalyticsTimeTracking;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RandomEventRecord
{
	public final long spawnedTime;
	public final int secondsSinceLastRandomEvent;
	public final int ticksSinceLastRandomEvent;
	public final NpcInfoRecord npcInfoRecord;
	public final PlayerInfoRecord playerInfoRecord;
	public final XpInfoRecord xpInfoRecord;
	private final int secondsInInstance;

	public RandomEventRecord(long spawnedTime, RandomEventAnalyticsTimeTracking timeTracking,
							 NpcInfoRecord npcInfoRecord, PlayerInfoRecord playerInfoRecord, XpInfoRecord xpInfoRecord)
	{
		this.spawnedTime = spawnedTime;
		this.secondsSinceLastRandomEvent = timeTracking.getSecondsSinceLastRandomEvent();
		this.ticksSinceLastRandomEvent = timeTracking.getTicksSinceLastRandomEvent();
		this.secondsInInstance = timeTracking.getSecondsInInstance();
		this.xpInfoRecord = xpInfoRecord;
		this.playerInfoRecord = playerInfoRecord;
		this.npcInfoRecord = npcInfoRecord;
	}
}