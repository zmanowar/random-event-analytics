package com.randomEventAnalytics;

import com.google.inject.Singleton;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Singleton
public class RandomEventAnalyticsTimeTracking
{
	public static final int SPAWN_INTERVAL_SECONDS = 60 * 5;
	private static final int SPAWN_INTERVAL_MARGIN_SECONDS = 15;

	@Setter
	private int secondsSinceLastRandomEvent;
	@Getter
	private int secondsInInstance;
	@Getter
	private int ticksSinceLastRandomEvent;

	@Getter
	private Instant lastRandomSpawnTime;

	@Getter
	private Instant loginTime;

	public void init(Instant loginTime, int secondsSinceLastRandomEvent, int secondsInInstance, int ticksSinceLastRandomEvent, Instant lastRandomSpawnTime)
	{
		this.loginTime = loginTime;
		this.secondsSinceLastRandomEvent = secondsSinceLastRandomEvent;
		this.secondsInInstance = secondsInInstance;
		this.ticksSinceLastRandomEvent = ticksSinceLastRandomEvent;
		this.lastRandomSpawnTime = lastRandomSpawnTime;
	}

	/**
	 * Calculates the time since the last random event based on the logged in time
	 * and the last random spawned time.
	 *
	 * Helpful to visualize a timeline:
	 * LI: Logged In, SP: Spawned Time, LO: Logged Out
	 *
	 * LI--SP--LO   LI----LO  LI---SP---LO
	 *
	 */
	public int getTotalSecondsSinceLastRandomEvent() {
		if (this.lastRandomSpawnTime != null && this.lastRandomSpawnTime.isAfter(this.loginTime)) {
			return (int) Duration.between(this.lastRandomSpawnTime, Instant.now()).toMillis() / 1000;
		}
		return this.secondsSinceLastRandomEvent + this.getSecondsSinceLogin();
	}

	public void incrementTotalLoggedInTicks()
	{
		ticksSinceLastRandomEvent += 1;
	}

	public boolean hasLoggedInLongEnoughForSpawn() {
		return getSecondsSinceLogin() > SPAWN_INTERVAL_SECONDS + SPAWN_INTERVAL_MARGIN_SECONDS;
	}

	public int getSecondsSinceLogin() {
		if (loginTime == null) return -1;
		return (int) Duration.between(loginTime, Instant.now()).toMillis() / 1000;
	}

	public int getClosestSpawnTimer() {
		int loginTime = getSecondsSinceLogin();
		if (!hasLoggedInLongEnoughForSpawn()) {
			// Initial spawn, must wait 5 minutes
			return SPAWN_INTERVAL_SECONDS - loginTime;
		}

		int secondsMod = loginTime % SPAWN_INTERVAL_SECONDS;
		if (secondsMod <= SPAWN_INTERVAL_MARGIN_SECONDS) {
			// Event should spawn within a 15-second window of 5 minutes.
			return -secondsMod;
		}

		// The event will spawn around the next 5-minute period.
		return SPAWN_INTERVAL_SECONDS - secondsMod;
	}

	public Integer getCurrentNumIntervals() {
		return getTotalSecondsSinceLastRandomEvent() / SPAWN_INTERVAL_SECONDS;
	}

	public int getNextRandomEventEstimation()
	{
		return SPAWN_INTERVAL_SECONDS - (getTotalSecondsSinceLastRandomEvent() % SPAWN_INTERVAL_SECONDS);
	}

	public void setRandomEventSpawned()
	{
		lastRandomSpawnTime = Instant.now();
		secondsInInstance = 0;
		ticksSinceLastRandomEvent = 0;
		secondsSinceLastRandomEvent = 0;
	}

	public void correctStrangePlantSpawn(RandomEventRecord record) {
		lastRandomSpawnTime = Instant.ofEpochMilli(record.spawnedTime);
		secondsSinceLastRandomEvent = getTotalSecondsSinceLastRandomEvent();
	}
}
