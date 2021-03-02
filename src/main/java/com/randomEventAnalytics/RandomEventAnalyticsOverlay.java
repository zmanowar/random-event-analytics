package com.randomEventAnalytics;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

/**
 * The InventoryValueOverlay class is used to display the value of the users inventory as an overlay
 * on the RuneLite client gameplay panel.
 */
public class RandomEventAnalyticsOverlay extends Overlay {
    private int timeUntilRandomEvent;
    private int ticksSinceLastRandomEvent;
    private int lastRandomTimeDiff;
    private final RandomEventAnalyticsConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    private static String TIME_UNTIL_LABEL = "Estimate: ";
    private static String OVERESTIMATE_LABEL = "Overestimate: ";
    private static String TITLE_LABEL = "Random Event";

    @Inject
    private RandomEventAnalyticsOverlay(RandomEventAnalyticsConfig config)
    {
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        timeUntilRandomEvent = 0;
        this.config = config;
    }

    /**
     * Render the item value overlay.
     * @param graphics the 2D graphics
     * @return the value of {@link PanelComponent#render(Graphics2D)} from this panel implementation.
     */
    @Override
    public Dimension render(Graphics2D graphics) {
        if(!config.enableOverlay() || !config.enableEstimation() ) {
            return null;
        }
        panelComponent.getChildren().clear();

        // Build overlay title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(TITLE_LABEL)
                .color(timeUntilRandomEvent >= 0 ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left(timeUntilRandomEvent >= 0 ? TIME_UNTIL_LABEL : OVERESTIMATE_LABEL)
                .right(RandomEventAnalyticsUtil.formatSeconds(Math.abs(timeUntilRandomEvent)))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Ticks: ")
                .right(String.valueOf(ticksSinceLastRandomEvent))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Previous: ")
                .right(String.valueOf(RandomEventAnalyticsUtil.formatSeconds(lastRandomTimeDiff)))
                .build());

        return panelComponent.render(graphics);
    }

    /**
     * Updates inventory value display
     * @param timeUntilRandomEvent the value to update the InventoryValue's {{@link #panelComponent}} with.
     */
    public void updateTimeUntilRandomEvent(final int timeUntilRandomEvent) {
        SwingUtilities.invokeLater(() -> {
            this.timeUntilRandomEvent = timeUntilRandomEvent;
        });
    }

    public void updateLastRandomTime(final int lastRandomTimeDiff) {
        SwingUtilities.invokeLater(() -> {
            this.lastRandomTimeDiff = lastRandomTimeDiff;
        });
    }

    public void updateTicksSinceLastRandomEvent(final int ticksSinceLastRandomEvent) {
        SwingUtilities.invokeLater(() -> {
            this.ticksSinceLastRandomEvent = ticksSinceLastRandomEvent;
        });
    }

}
