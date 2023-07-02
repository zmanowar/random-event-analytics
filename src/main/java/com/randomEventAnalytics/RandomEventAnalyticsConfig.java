package com.randomEventAnalytics;

import java.awt.Color;
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
		name = "Logging Panel",
		description = "Settings related to the logging panel",
		position = 2,
		closedByDefault = false
	)
	String loggingPanelSection = "loggingPanelSection";
	@ConfigSection(
		name = "Experimental",
		description = "Experimental features. May have a negative impact on performance.",
		position = 3,
		closedByDefault = true
	)
	String experimentalSection = "experimental";

	@ConfigSection(
		name = "Random Event Helpers",
		description = "Configuration items for random event helpers",
		position = 0,
		closedByDefault = true
	)
	String helperSection = "helpers";


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
		keyName = "enableHelpers",
		name = "Enable Random Event Helper Overlay",
		description = "Highlights the correct answers for some random events",
		section = helperSection
	)
	default boolean enableHelpers() {return false;}

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

	@ConfigItem(
		keyName = "logTimeFormat",
		name = "Time Format",
		description = "Configures time between 12 or 24 hour time format",
		section = loggingPanelSection
	)
	default TimeFormat timeFormatMode()
	{
		return TimeFormat.TIME_24H;
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
