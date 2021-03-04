package com.randomEventAnalytics;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RandomEventAnalyticsConfig.CONFIG_GROUP)
public interface RandomEventAnalyticsConfig extends Config
{
	String CONFIG_GROUP = "randomeventanalytics";
	String SECONDS_SINCE_LAST_RANDOM = "secondsSinceLastRandom";
	String TICKS_SINCE_LAST_RANDOM = "ticksSinceLastRandom";

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
		return true;
	}
}
