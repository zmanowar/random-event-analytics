package com.randomEventAnalytics;

import com.google.inject.Singleton;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/*
	Notes:
		Does everyone get a random 5 minutes after their first login after an
		update / server restart?
 */
@Slf4j
@Singleton
public class TimeTracking
{
	public static final int SPAWN_INTERVAL_SECONDS = 60 * 5;
	private static final int SPAWN_INTERVAL_MARGIN_SECONDS = 0;

	@Setter
	private int secondsSinceLastRandomEvent;
	@Getter
	private int secondsInInstance;
	@Getter
	private int ticksSinceLastRandomEvent;

	@Getter
	private Instant lastRandomSpawnTime;

	private int intervalsSinceLastRandom;

	@Getter
	private Instant loginTime;

	public void init(Instant loginTime, int secondsSinceLastRandomEvent, int secondsInInstance, int ticksSinceLastRandomEvent, Instant lastRandomSpawnTime, int intervalsSinceLastRandom)
	{
		this.loginTime = loginTime;
		this.secondsSinceLastRandomEvent = secondsSinceLastRandomEvent;
		this.secondsInInstance = secondsInInstance;
		this.ticksSinceLastRandomEvent = ticksSinceLastRandomEvent;
		this.lastRandomSpawnTime = lastRandomSpawnTime;
		setIntervalsSinceLastRandom(intervalsSinceLastRandom);
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
		if (this.loginTime == null) return -1;
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
		return getSecondsSinceLogin() > SPAWN_INTERVAL_SECONDS;
	}

	public int getSecondsSinceLogin() {
		if (loginTime == null) return -1;
		return (int) Duration.between(loginTime, Instant.now()).toMillis() / 1000;
	}

	public int getNextRandomEventEstimation() {
		int loginTime = getSecondsSinceLogin();

		if (loginTime < 0) {
			return SPAWN_INTERVAL_SECONDS;
		}

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

	public void setRandomEventSpawned()
	{
		lastRandomSpawnTime = Instant.now();
		secondsInInstance = 0;
		ticksSinceLastRandomEvent = 0;
		secondsSinceLastRandomEvent = 0;
		intervalsSinceLastRandom = 0;
		this.setIntervalsSinceLastRandom(0);
	}

	public void setLoginTime(Instant instant) {
		if (instant == null) {
			this.intervalsSinceLastRandom = getIntervalsSinceLastRandom();
		}
		this.loginTime = instant;
	}

	public void correctStrangePlantSpawn(RandomEventRecord record) {
		lastRandomSpawnTime = Instant.ofEpochMilli(record.spawnedTime);
		secondsSinceLastRandomEvent = getTotalSecondsSinceLastRandomEvent();
	}

	public int getIntervalsSinceLastRandom() {
		return this.intervalsSinceLastRandom + getCurrentSessionNumIntervals();
	}

	private int getCurrentSessionNumIntervals() {
		return getSecondsSinceLogin() / SPAWN_INTERVAL_SECONDS;
	}

	private void setIntervalsSinceLastRandom(int numIntervals) {
		if (numIntervals < 0) {
			// One-time Update: Should only need to be set once per profile config.
			this.intervalsSinceLastRandom = getTotalSecondsSinceLastRandomEvent() / SPAWN_INTERVAL_SECONDS;
		} else {
			this.intervalsSinceLastRandom = numIntervals;
		}
	}
}
