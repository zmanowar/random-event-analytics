package com.randomEventAnalytics;

import com.google.inject.Singleton;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

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

	private int sessionTicks;

	@Getter
	private Instant lastRandomSpawnInstant;

	@Getter
	private Instant loginTime;

	@Inject
	private ConfigManager configManager;

	public void init(Instant loginTime, Instant lastRandomSpawnInstant)
	{
		this.loginTime = loginTime;
		this.lastRandomSpawnInstant = lastRandomSpawnInstant;
	}

	public int getTotalSecondsSinceLastRandomEvent()
	{
		if (this.loginTime == null)
		{
			return getConfigSecondsSinceLastRandom();
		}

		if (this.lastRandomSpawnInstant != null && this.lastRandomSpawnInstant.isAfter(this.loginTime))
		{
			return (int) Duration.between(this.lastRandomSpawnInstant, Instant.now()).toMillis() / 1000;
		}

		Duration duration = Duration.between(this.loginTime, Instant.now());
		return getConfigSecondsSinceLastRandom() + (int) duration.getSeconds();
	}

	public void onTick()
	{
		sessionTicks += 1;

	}

	public boolean hasLoggedInLongEnoughForSpawn()
	{
		return getSecondsSinceLogin() > SPAWN_INTERVAL_SECONDS;
	}

	public int getSecondsSinceLogin()
	{
		if (loginTime == null)
		{
			return -1;
		}
		return (int) Duration.between(loginTime, Instant.now()).toMillis() / 1000;
	}

	public int getNextRandomEventEstimation()
	{
		int loginTime = getSecondsSinceLogin();

		if (loginTime < 0)
		{
			return SPAWN_INTERVAL_SECONDS;
		}

		if (!hasLoggedInLongEnoughForSpawn())
		{
			// Initial login, must wait 5 minutes
			return SPAWN_INTERVAL_SECONDS - loginTime;
		}

		int secondsMod = loginTime % SPAWN_INTERVAL_SECONDS;
		if (secondsMod <= SPAWN_INTERVAL_MARGIN_SECONDS)
		{
			// Event should spawn within a 15-second window of 5 minutes.
			return -secondsMod;
		}

		// The event will spawn around the next 5-minute period.
		return SPAWN_INTERVAL_SECONDS - secondsMod;
	}

	public void setRandomEventSpawned()
	{
		lastRandomSpawnInstant = Instant.now();
		sessionTicks = 0;
		setConfigAfterSpawn();
	}

	public void setStrangePlantSpawned(RandomEventRecord record)
	{
		lastRandomSpawnInstant = Instant.ofEpochMilli(record.spawnedTime);
		sessionTicks = 0;
		setConfigAfterSpawn();
	}

	public void setLoginTime(Instant instant)
	{
		this.loginTime = instant;
	}


	private Instant getMostRecentInstant()
	{
		if (loginTime == null)
		{
			return null;
		}
		if (lastRandomSpawnInstant == null)
		{
			return loginTime;
		}
		if (lastRandomSpawnInstant.isAfter(loginTime))
		{
			return lastRandomSpawnInstant;
		}
		return loginTime;
	}

	public int getIntervalsSinceLastRandom()
	{
		if (getMostRecentInstant() == null)
		{
			return getConfigIntervalsSinceLastRandom();
		}
		else
		{
			int sessionDuration = (int) Duration.between(getMostRecentInstant(), Instant.now()).getSeconds();
			return getConfigIntervalsSinceLastRandom() + (sessionDuration / SPAWN_INTERVAL_SECONDS);
		}
	}

	public int getTicksSinceLastRandomEvent()
	{
		return this.getConfigTicksSinceLastRandom() + this.sessionTicks;
	}

	// TODO: Rework how instances are calculated
	public int getSecondsInInstance()
	{
		return this.getConfigSecondsInInstance();
	}

	public int getCountdownSeconds(int numMinutesPerRandom)
	{
		return (numMinutesPerRandom * 60) - this.getTotalSecondsSinceLastRandomEvent();
	}

	public double getProbabilityForNextWindow() {
		int intervalsAfterHour = this.getIntervalsSinceLastRandom() - (60 / 5);
		if (intervalsAfterHour < 0) {
			return 0;
		}
		return ((double) intervalsAfterHour)/12;
	}


	private int getConfigIntervalsSinceLastRandom()
	{
		int configIntervals = getIntFromConfig(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, -1);
		if (configIntervals == -1)
		{
			// One-time Update: Should only need to be set once per profile config.
			configIntervals = getTotalSecondsSinceLastRandomEvent() / SPAWN_INTERVAL_SECONDS;
		}
		return configIntervals;
	}

	private int getConfigTicksSinceLastRandom()
	{
		return getIntFromConfig(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, 0);
	}

	private int getConfigSecondsSinceLastRandom()
	{
		return getIntFromConfig(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, 0);
	}

	private int getConfigSecondsInInstance()
	{
		return getIntFromConfig(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, -1);
	}

	public void persistConfig()
	{
		saveConfig(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, getTotalSecondsSinceLastRandomEvent());
		saveConfig(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, getIntervalsSinceLastRandom());
		saveConfig(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, getTicksSinceLastRandomEvent());
		saveConfig(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, getSecondsInInstance());
		if (lastRandomSpawnInstant != null)
		{
			saveConfig(RandomEventAnalyticsConfig.LAST_RANDOM_SPAWN_INSTANT, lastRandomSpawnInstant);
		}
	}

	public void setConfigAfterSpawn()
	{
		saveConfig(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, 0);
		saveConfig(RandomEventAnalyticsConfig.INTERVALS_SINCE_LAST_RANDOM, 0);
		saveConfig(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, 0);
		saveConfig(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, 0);
		if (lastRandomSpawnInstant != null)
		{
			saveConfig(RandomEventAnalyticsConfig.LAST_RANDOM_SPAWN_INSTANT, lastRandomSpawnInstant);
		}
	}

	private <T> void saveConfig(String key, T value)
	{
		try
		{
			configManager.setRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP,
				key, value);
		} catch (NullPointerException e) {
			log.debug("Error setting config {}: {} | ConfigManager most likely not set", key, value);
		}
	}

	private int getIntFromConfig(String key, int _default)
	{
		try
		{
			return configManager.getRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, int.class);
		}
		catch (NullPointerException e)
		{
			try
			{
				log.debug("No config loaded for: {}@{}", key, configManager.getRSProfileKey());
				return _default;
			} catch (NullPointerException configError) {
				log.debug("Error loading configManager");
				return _default;
			}
		}
	}
}
