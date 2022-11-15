package com.randomEventAnalytics;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RandomEventAnalyticsConfig.CONFIG_GROUP)
public interface RandomEventAnalyticsConfig extends Config
{
	String CONFIG_GROUP = "randomeventanalytics";
	String SECONDS_SINCE_LAST_RANDOM = "secondsSinceLastRandom";
	String TICKS_SINCE_LAST_RANDOM = "ticksSinceLastRandom";
	String SECONDS_IN_INSTANCE = "secondsInInstance";
	String LAST_RANDOM_SPAWN_INSTANT = "lastRandomSpawnInstant";

	@ConfigItem(
		keyName = "enableEstimation",
		name = "Enable Estimation",
		description = "Show the Random Events estimation."
	)
	default boolean enableEstimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableOverlay",
		name = "Enable Overlay",
		description = "Show the Random Events overlay. Enable Estimation must be enabled."
	)
	default boolean enableOverlay()
	{
		return false;
	}

	@ConfigSection(
		name = "Experimental",
		description = "Experimental features. May have a negative impact on performance.",
		position = 0,
		closedByDefault = true
	)
	String experimentalSection = "experimental";

	@ConfigItem(
		keyName = "showEventTimeWindow",
		name = "Show Event Time Window",
		description = "Shows a 5 minute sliding timer for events.",
		section = experimentalSection
	)
	default boolean showEventTimeWindow()
	{
		return false;
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
