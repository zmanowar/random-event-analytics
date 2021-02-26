package com.randomEventAnalytics;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("random-event-analytics")
public interface RandomEventAnalyticsConfig extends Config
{
	@ConfigItem(
		keyName = "enableOverlay",
		name = "Enable Overlay",
		description = "Show the Random Events overlay."
	)
	default boolean enableOverlay()
	{
		return true;
	}

}
