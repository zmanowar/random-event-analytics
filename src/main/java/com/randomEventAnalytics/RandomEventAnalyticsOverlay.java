package com.randomEventAnalytics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class RandomEventAnalyticsOverlay extends Overlay
{
	private static final String TIME_UNTIL_LABEL = "Estimate: ";
	private static final String OVERESTIMATE_LABEL = "Overestimate: ";
	private static final String TITLE_LABEL = "Random Event";
	private final RandomEventAnalyticsConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private final RandomEventAnalyticsTimeTracking timeTracking;

	@Inject
	private RandomEventAnalyticsOverlay(RandomEventAnalyticsConfig config,
										RandomEventAnalyticsTimeTracking timeTracking)
	{
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		this.config = config;
		this.timeTracking = timeTracking;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableOverlay() || !config.enableEstimation())
		{
			return null;
		}
		panelComponent.getChildren().clear();

		int estimatedSeconds = timeTracking.getNextRandomEventEstimation();

		// Build overlay title
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(TITLE_LABEL)
			.color(estimatedSeconds >= 0 ? Color.GREEN : Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(estimatedSeconds >= 0 ? TIME_UNTIL_LABEL : OVERESTIMATE_LABEL)
			.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds)))
			.build());

		if (config.showEventTimeWindow()) {
			int closestSpawnTimer = timeTracking.getClosestSpawnTimer();
			panelComponent.getChildren().add(LineComponent.builder()
				.left(timeTracking.hasLoggedInLongEnoughForSpawn() ? "Event Spawn Window" : "Waiting for login timer...")
				.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(closestSpawnTimer)))
				.build());
		}

		if (config.showDebug())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ticks: ")
				.right(String.valueOf(timeTracking.getTicksSinceLastRandomEvent()))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Instance: ")
				.right(String.valueOf(timeTracking.getSecondsInInstance()))
				.build());
		}

		return panelComponent.render(graphics);
	}
}
