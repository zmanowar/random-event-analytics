package com.randomEventAnalytics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class RandomEventAnalyticsOverlay extends Overlay
{
	private static final String TIME_UNTIL_LABEL = "Estimate: ";
	private static final String OVERESTIMATE_LABEL = "Overestimate: ";
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
			int closestSpawnTimer = timeTracking.getNextRandomEventEstimation();
			panelComponent.getChildren().add(LineComponent.builder()
				.left(timeTracking.hasLoggedInLongEnoughForSpawn() ? "Random Event Eligible In" : "Initial login " +
					"countdown." +
					"..")
				.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(closestSpawnTimer)))
				.build());
		}

		if (config.enableConfigCountdown())
		{
			int estimatedSeconds = timeTracking.getCountdownSeconds(config.countdownMinutes());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(estimatedSeconds >= 0 ? TIME_UNTIL_LABEL : OVERESTIMATE_LABEL)
				.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds)))
				.build());
		}


		if (config.showDebug())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ticks: ")
				.right(String.valueOf(timeTracking.getTicksSinceLastRandomEvent()))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Intervals: ")
				.right(String.valueOf(timeTracking.getIntervalsSinceLastRandom()))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("# of Events Logged: ")
				.right(String.valueOf(plugin.getNumberOfEventsLogged()))
				.build());
		}

		return panelComponent.render(graphics);
	}
}
