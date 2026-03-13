package com.randomEventAnalytics;

import com.google.inject.Singleton;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.time.Clock;
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
	private static final int SPAWN_INTERVAL_TIMEFRAME_SECONDS = 15;
	public static final int SECONDS_IN_AN_HOUR = 60 /*seconds in a minute*/ * 60 /*minutes in an hour*/;

	private int sessionTicks;

	@Getter
	private Instant lastRandomSpawnInstant;

	/** The first time this account logged in with the plugin active. Used as spawn window anchor when no random events have been recorded yet. */
	@Getter
	private Instant firstLoginInstant;

	@Getter
	private Instant loginTime;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Clock clock;

	public void init(Instant loginTime, Instant lastRandomSpawnInstant)
	{
		this.loginTime = loginTime;
		this.lastRandomSpawnInstant = lastRandomSpawnInstant;

		// Load persisted firstLoginInstant; if missing, this is the first ever login — record it now.
		Instant persistedFirstLogin = getInstantFromConfig(RandomEventAnalyticsConfig.FIRST_LOGIN_INSTANT);
		if (persistedFirstLogin != null)
		{
			this.firstLoginInstant = persistedFirstLogin;
		}
		else
		{
			this.firstLoginInstant = loginTime;
			saveConfig(RandomEventAnalyticsConfig.FIRST_LOGIN_INSTANT, this.firstLoginInstant);
		}

		// Detect if we're logging in after the spawn window has already expired.
		// If so, the game may have applied an offline extension of 10–70 minutes.
		this.offlineExtensionLikely = isWindowExpired();
	}

	public int getTotalSecondsSinceLastRandomEvent()
	{
		if (this.loginTime == null)
		{
			return getConfigSecondsSinceLastRandom();
		}

		if (this.lastRandomSpawnInstant != null && this.lastRandomSpawnInstant.isAfter(this.loginTime))
		{
			return (int) Duration.between(this.lastRandomSpawnInstant, clock.instant()).toMillis() / 1000;
		}

		Duration duration = Duration.between(this.loginTime, clock.instant());
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
		return (int) Duration.between(loginTime, clock.instant()).toMillis() / 1000;
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

	private boolean isInsideRandomEventWindow()
	{
		int loginTime = getSecondsSinceLogin();
		int secondsMod = loginTime % SPAWN_INTERVAL_SECONDS;
		// If we're within 15 seconds of an event window, it's considered a possible time.
		return secondsMod <= SPAWN_INTERVAL_TIMEFRAME_SECONDS || secondsMod >= SPAWN_INTERVAL_SECONDS - SPAWN_INTERVAL_TIMEFRAME_SECONDS;
	}

	public boolean isPossibleTimeForRandomEvent()
	{
		if (lastRandomSpawnInstant == null)
		{
			return isInsideRandomEventWindow();
		}

		if (!hasLoggedInLongEnoughForSpawn())
		{
			return false;
		}

		if (getTotalSecondsSinceLastRandomEvent() <  SECONDS_IN_AN_HOUR)
		{
			// There's a minimum of an hour between random events.
			return false;
		}

		return isInsideRandomEventWindow();
	}

	public void setRandomEventSpawned()
	{
		lastRandomSpawnInstant = clock.instant();
		sessionTicks = 0;
		setConfigAfterSpawn();
	}

	public void setStrangePlantSpawned(RandomEventRecord record)
	{
		lastRandomSpawnInstant = Instant.ofEpochMilli(record.spawnedTime);
		sessionTicks = 0;
		setConfigAfterSpawn();
	}

	/**
	 * Simulates a random event occurring right now, resetting the spawn window to [now+1h, now+2h].
	 * Updates in-memory state and persists the new anchor instant to config, but does NOT
	 * write a record to local storage — this is intended for testing/debugging only.
	 */
	public void simulateRandomEventSpawned()
	{
		lastRandomSpawnInstant = clock.instant();
		sessionTicks = 0;
		offlineExtensionLikely = false;
		setConfigAfterSpawn();
	}

	public void setLoginTime(Instant instant)
	{
		this.loginTime = instant;
	}

	/**
	 * The anchor for the 1–2 hour spawn window.
	 * Uses lastRandomSpawnInstant when a random event has been recorded;
	 * falls back to firstLoginInstant for accounts with no prior events.
	 */
	public Instant getWindowAnchor()
	{
		if (lastRandomSpawnInstant != null)
		{
			return lastRandomSpawnInstant;
		}
		return firstLoginInstant;
	}

	/**
	 * Earliest the game could schedule a random event after the anchor: anchor + 1 hour.
	 * May be null if no anchor is available yet (neither a recorded event nor a first-login instant).
	 */
	public Instant getEarliestSpawnInstant()
	{
		Instant anchor = getWindowAnchor();
		if (anchor == null)
		{
			return null;
		}
		return anchor.plusSeconds(SECONDS_IN_AN_HOUR);
	}

	/**
	 * Latest the game could initially schedule a random event after the anchor: anchor + 2 hours.
	 * Does not account for offline extensions; see {@link #isOfflineExtensionLikely()}.
	 */
	public Instant getLatestSpawnInstant()
	{
		Instant anchor = getWindowAnchor();
		if (anchor == null)
		{
			return null;
		}
		return anchor.plusSeconds(SECONDS_IN_AN_HOUR * 2L);
	}

	/**
	 * True when the current time is past the earliest possible spawn instant (anchor + 1h).
	 * A random event may have already occurred or could occur any time until {@link #getLatestSpawnInstant()}.
	 */
	public boolean isWindowOpen()
	{
		Instant earliest = getEarliestSpawnInstant();
		return earliest != null && clock.instant().isAfter(earliest);
	}

	/**
	 * True when the current time is past the latest initial spawn instant (anchor + 2h).
	 * The game will have either already sent the event or applied an offline extension.
	 */
	public boolean isWindowExpired()
	{
		Instant latest = getLatestSpawnInstant();
		return latest != null && clock.instant().isAfter(latest);
	}

	/**
	 * True when the plugin was loaded while the window is already expired.
	 * Indicates an offline extension of 10–70 minutes may have been applied by the game.
	 * Set once during {@link #init(Instant, Instant)} and not updated thereafter.
	 */
	@Getter
	private boolean offlineExtensionLikely;

	/** Seconds until the earliest possible spawn instant, or 0 if the window is already open. */
	public long getSecondsUntilEarliest()
	{
		Instant earliest = getEarliestSpawnInstant();
		if (earliest == null)
		{
			return 0;
		}
		long seconds = Duration.between(clock.instant(), earliest).getSeconds();
		return Math.max(0, seconds);
	}

	/** Seconds until the latest initial spawn instant, or 0 if the window has expired. */
	public long getSecondsUntilLatest()
	{
		Instant latest = getLatestSpawnInstant();
		if (latest == null)
		{
			return 0;
		}
		long seconds = Duration.between(clock.instant(), latest).getSeconds();
		return Math.max(0, seconds);
	}

	private Instant getInstantFromConfig(String key)
	{
		try
		{
			return configManager.getRSProfileConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, key, Instant.class);
		}
		catch (NullPointerException e)
		{
			log.debug("No config loaded for: {}@{}", key, configManager.getRSProfileKey());
			return null;
		}
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
			int sessionDuration = (int) Duration.between(getMostRecentInstant(), clock.instant()).getSeconds();
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
		if (lastRandomSpawnInstant != null)
		{
			saveConfig(RandomEventAnalyticsConfig.LAST_RANDOM_SPAWN_INSTANT, lastRandomSpawnInstant);
		}
		if (firstLoginInstant != null)
		{
			saveConfig(RandomEventAnalyticsConfig.FIRST_LOGIN_INSTANT, firstLoginInstant);
		}
	}

	public void setConfigAfterSpawn()
	{
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
