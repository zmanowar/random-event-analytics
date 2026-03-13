package com.randomEventAnalytics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class RandomEventAnalyticsOverlay extends Overlay
{
	private static final String EARLIEST_LABEL = "Earliest possible: ";
	private static final String LATEST_LABEL = "Latest possible: ";
	private static final String TITLE_LABEL = "Random Event";
	private final RandomEventAnalyticsConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private final TimeTracking timeTracking;
	private final RandomEventAnalyticsPlugin plugin;

	@Inject
	private RandomEventAnalyticsOverlay(RandomEventAnalyticsConfig config,
										TimeTracking timeTracking, RandomEventAnalyticsPlugin plugin)
	{
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		this.config = config;
		this.timeTracking = timeTracking;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableOverlay())
		{
			return null;
		}
		panelComponent.getChildren().clear();

		if (config.enableEstimation())
		{
			// 5-minute eligibility window countdown (existing behaviour).
			int closestSpawnTimer = timeTracking.getNextRandomEventEstimation();
			panelComponent.getChildren().add(LineComponent.builder()
				.left(timeTracking.hasLoggedInLongEnoughForSpawn() ? "Eligible check in" : "Initial login...")
				.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(closestSpawnTimer)))
				.build());
		}

		if (config.enableConfigCountdown())
		{
			boolean windowExpired = timeTracking.isWindowExpired();
			boolean windowOpen = timeTracking.isWindowOpen();

			if (windowExpired)
			{
				String status = timeTracking.isOfflineExtensionLikely() ? "Offline ext. possible" : "Window passed";
				panelComponent.getChildren().add(LineComponent.builder()
					.left(EARLIEST_LABEL)
					.right("Overdue \u2014 " + status)
					.build());
			}
			else if (windowOpen)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(EARLIEST_LABEL)
					.right("Now")
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left(LATEST_LABEL)
					.right(RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilLatest()))
					.build());
			}
			else
			{
				boolean noEventsYet = timeTracking.getLastRandomSpawnInstant() == null;
				panelComponent.getChildren().add(LineComponent.builder()
					.left(noEventsYet ? "Earliest (est.): " : EARLIEST_LABEL)
					.right(RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilEarliest()))
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left(noEventsYet ? "Latest (est.): " : LATEST_LABEL)
					.right(RandomEventAnalyticsUtil.formatSeconds((int) timeTracking.getSecondsUntilLatest()))
					.build());
			}
		}

		if (config.showDebug())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ticks: ")
				.right(String.valueOf(timeTracking.getTicksSinceLastRandomEvent()))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("# of Events Logged: ")
				.right(String.valueOf(plugin.getNumberOfEventsLogged()))
				.build());
		}

		return panelComponent.render(graphics);
	}
}
