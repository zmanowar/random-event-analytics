package com.randomEventAnalytics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class RandomEventAnalyticsOverlay extends Overlay
{
	private static String TIME_UNTIL_LABEL = "Estimate: ";
	private static String OVERESTIMATE_LABEL = "Overestimate: ";
	private static String TITLE_LABEL = "Random Event";
	private final RandomEventAnalyticsConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private int estimatedSeconds;
	private int secondsInInstance;
	private int ticksSinceLastRandomEvent;

	@Inject
	private RandomEventAnalyticsOverlay(RandomEventAnalyticsConfig config)
	{
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		estimatedSeconds = 0;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableOverlay() || !config.enableEstimation())
		{
			return null;
		}
		panelComponent.getChildren().clear();

		// Build overlay title
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(TITLE_LABEL)
			.color(estimatedSeconds >= 0 ? Color.GREEN : Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(estimatedSeconds >= 0 ? TIME_UNTIL_LABEL : OVERESTIMATE_LABEL)
			.right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds)))
			.build());

		if (config.showDebug())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ticks: ")
				.right(String.valueOf(ticksSinceLastRandomEvent))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Instance: ")
				.right(String.valueOf(secondsInInstance))
				.build());
		}

		return panelComponent.render(graphics);
	}

	public void updateEstimation(final int estimatedSeconds)
	{
		SwingUtilities.invokeLater(() -> {
			this.estimatedSeconds = estimatedSeconds;
		});
	}

	public void updateTicksSinceLastRandomEvent(final int ticksSinceLastRandomEvent)
	{
		SwingUtilities.invokeLater(() -> {
			this.ticksSinceLastRandomEvent = ticksSinceLastRandomEvent;
		});
	}

	public void updateSecondsInInstance(final int secondsInInstance) {
		SwingUtilities.invokeLater(() -> {
			this.secondsInInstance = secondsInInstance;
		});
	}

}
