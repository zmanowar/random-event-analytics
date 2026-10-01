package com.randomEventAnalytics;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBox;

public class RandomEventAnalyticsInfoBox extends InfoBox
{
	private final RandomEventAnalyticsConfig config;
	private final TimeTracking timeTracking;
	private final Client client;

	RandomEventAnalyticsInfoBox(BufferedImage image, RandomEventAnalyticsPlugin plugin,
								RandomEventAnalyticsConfig config, TimeTracking timeTracking, Client client)
	{
		super(image, plugin);
		this.config = config;
		this.timeTracking = timeTracking;
		this.client = client;
	}

	@Override
	public boolean render()
	{
		return config.enableInfoBox();
	}

	@Override
	public String getText()
	{
		final WindowState state = computeWindowState();
		setTooltip(buildTooltip());
		return formatText(state);
	}

	@Override
	public Color getTextColor()
	{
		if (!config.enableInfoBoxColor())
		{
			return Color.WHITE;
		}
		return computeWindowState().badgeColor;
	}

	WindowState computeWindowState()
	{
		final boolean hasAnchor = timeTracking.getWindowAnchor() != null;
		final boolean windowExpired = timeTracking.isWindowExpired();
		final boolean windowOpen = timeTracking.isWindowOpen();
		final boolean inInstance = client.isInInstancedRegion();
		final boolean noEventsYet = timeTracking.getLastRandomSpawnInstant() == null;
		return WindowState.from(hasAnchor, windowExpired, windowOpen, inInstance, noEventsYet);
	}

	private String formatText(WindowState state)
	{
		switch (state)
		{
			case OVERDUE:
				return "OVER";
			case ELIGIBLE:
			case IN_INSTANCE:
				return RandomEventAnalyticsUtil.formatSeconds(Math.abs(timeTracking.getNextRandomEventEstimation()));
			// WAITING. TODO: Checkout NO_DATA case.
			default:
				return RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilEarliest());
		}
	}

	private String buildTooltip()
	{
		final int nextTickSeconds = timeTracking.getNextRandomEventEstimation();
		final boolean windowExpired = timeTracking.isWindowExpired();
		final boolean windowOpen = timeTracking.isWindowOpen();

		final String windowLabel;
		final String windowText;
		if (windowExpired)
		{
			windowLabel = "Earliest";
			windowText = "overdue";
		}
		else if (windowOpen)
		{
			windowLabel = "Latest";
			windowText = RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilLatest());
		}
		else
		{
			windowLabel = "Earliest";
			windowText = RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilEarliest());
		}

		return "Next Check: " + RandomEventAnalyticsUtil.formatSeconds(Math.abs(nextTickSeconds))
			+ "</br>" + windowLabel + ": " + windowText;
	}
}
