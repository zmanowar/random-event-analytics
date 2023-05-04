package com.randomEventAnalytics;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(RandomEventAnalyticsConfig.CONFIG_GROUP)
public interface RandomEventAnalyticsConfig extends Config
{
	String CONFIG_GROUP = "randomeventanalytics";
	String SECONDS_SINCE_LAST_RANDOM = "secondsSinceLastRandom";
	String TICKS_SINCE_LAST_RANDOM = "ticksSinceLastRandom";
	String SECONDS_IN_INSTANCE = "secondsInInstance";
	String LAST_RANDOM_SPAWN_INSTANT = "lastRandomSpawnInstant";
	String INTERVALS_SINCE_LAST_RANDOM = "intervalsSinceLastRandom";
	@ConfigSection(
		name = "Countdown",
		description = "Logged-in countdown configuration",
		position = 1,
		closedByDefault = false
	)
	String countdownSection = "countdown";
	@ConfigSection(
		name = "Experimental",
		description = "Experimental features. May have a negative impact on performance.",
		position = 2,
		closedByDefault = true
	)
	String experimentalSection = "experimental";


//	@ConfigSection(
//		name = "Notifications",
//		description = "Notifications for Random Event timing & activity",
//		position = 0,
//		closedByDefault = true
//	)
//	String notificationSection = "notifications";

	@ConfigItem(
		keyName = "enableEstimation",
		name = "Enable Estimation",
		description = "Shows a 5 minute sliding timer for events."
	)
	default boolean enableEstimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableOverlay",
		name = "Enable Overlay",
		description = "Show the Random Events overlay."
	)
	default boolean enableOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableConfigCountdown",
		name = "Enable Countdown",
		description = "Enables the time since last random countdown",
		section = countdownSection
	)
	default boolean enableConfigCountdown()
	{
		return true;
	}

	@Range(min = 1)
	@ConfigItem(
		keyName = "countdownMinutes",
		name = "Countdown (Minutes)",
		description = "Number of minutes the countdown should last",
		section = countdownSection
	)
	default int countdownMinutes()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "showDebug",
		name = "Show Debug",
		description = "Shows debug information in the overlay.",
		section = experimentalSection
	)
	default boolean showDebug()
	{
		return false;
	}
}
