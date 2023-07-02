package com.randomEventAnalytics.helpers;

import com.google.inject.Inject;
import com.randomEventAnalytics.RandomEventAnalyticsConfig;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

public class SolverItemOverlay extends WidgetItemOverlay
{

	private final Client client;
	private final RandomEventAnalyticsConfig config;
	private final GravediggerSolver gravediggerSolver;

	@Inject
	SolverItemOverlay(Client client, RandomEventAnalyticsConfig config, GravediggerSolver gravediggerSolver) {
		this.client = client;
		this.config = config;
		this.gravediggerSolver = gravediggerSolver;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.enableHelpers()) {
			return;
		}
		if (gravediggerSolver.isEnabled()) {
			GravediggerSolver.Coffin coffin = gravediggerSolver.findCoffin(itemId);
			if (coffin != null) {
				renderItemOverlay(graphics, coffin.getItem(), widgetItem, coffin.getName());
			}
		}
	}

	private void renderItemOverlay(Graphics2D graphics, Item item, WidgetItem widgetItem, String text) {
		if (item == null) return;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		final Rectangle bounds = widgetItem.getCanvasBounds();
		final TextComponent textComponent = new TextComponent();
		textComponent.setPosition(new Point(bounds.x - 1, bounds.y + 15));
		textComponent.setText(text);
		textComponent.setColor(config.helperTextColor());
		textComponent.render(graphics);
	}
}
