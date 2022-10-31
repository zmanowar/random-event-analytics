package com.randomEventAnalytics;

import java.awt.Color;
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
		name = "Enable Timing Overlay",
		description = "Show the Random Events overlay. Enable Estimation must be enabled."
	)
	default boolean enableOverlay()
	{
		return false;
	}

	@ConfigSection(
		name = "Random Event Helpers",
		description = "Configuration items for random event helpers",
		position = 0,
		closedByDefault = false
	)
	String helperSection = "helpers";

	@ConfigItem(
		keyName = "enableHelpers",
		name = "Enable Random Event Helper Overlay",
		description = "Highlights the correct answers for some random events",
		section = helperSection
	)
	default boolean enableHelpers() {return true;}

	@ConfigItem(
		keyName = "helperHighlightColor",
		name = "Helper Highlight Color",
		description = "Color for helper highlighter overlay",
		section = helperSection
	)
	default Color helperHighlightColor() {return Color.GREEN;}

	@ConfigItem(
		keyName = "helperHighlightTextColor",
		name = "Helper Highlight Text Color",
		description = "Color for helper text overlay",
		section = helperSection
	)
	default Color helperTextColor() {return Color.WHITE;}


	@ConfigSection(
		name = "Experimental",
		description = "Experimental features. May have a negative impact on performance.",
		position = 1,
		closedByDefault = true
	)
	String experimentalSection = "experimental";


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
