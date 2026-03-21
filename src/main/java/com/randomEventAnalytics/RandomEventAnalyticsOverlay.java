package com.randomEventAnalytics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class RandomEventAnalyticsOverlay extends Overlay
{
	private static final int BACKGROUND_ALPHA = 126;

	private final RandomEventAnalyticsConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private final TimeTracking timeTracking;
	private final Client client;

	@Inject
	private RandomEventAnalyticsOverlay(RandomEventAnalyticsConfig config,
										TimeTracking timeTracking, Client client)
	{
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		this.config = config;
		this.timeTracking = timeTracking;
		this.client = client;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));

		final boolean hasAnchor = timeTracking.getWindowAnchor() != null;
		final boolean windowExpired = timeTracking.isWindowExpired();
		final boolean windowOpen = timeTracking.isWindowOpen();
		final boolean inInstance = client.isInInstancedRegion();
		final boolean noEventsYet = timeTracking.getLastRandomSpawnInstant() == null;
		final WindowState windowState = WindowState.from(hasAnchor, windowExpired, windowOpen, inInstance,
			noEventsYet);

		if (config.enableOverlayBackground())
		{
			final Color base = windowState.badgeColor;
			panelComponent.setBackgroundColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
				BACKGROUND_ALPHA));
		}
		else
		{
			panelComponent.setBackgroundColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		}

		// Tick line: 5-minute eligibility countdown.
		final int nextTickSeconds = timeTracking.getNextRandomEventEstimation();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Next Check:")
			.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(nextTickSeconds)))
			.build());

		// Earliest line: countdown to the spawn window opening.
		final String earliestText;
		if (windowExpired)
		{
			earliestText = "overdue";
		}
		else if (windowOpen)
		{
			earliestText = "now";
		}
		else
		{
			earliestText = RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilEarliest());
		}
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Earliest:")
			.right(earliestText)
			.build());

		return panelComponent.render(graphics);
	}
}
