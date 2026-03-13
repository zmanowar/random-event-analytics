package com.randomEventAnalytics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A custom panel that renders the 2-hour random event spawn window as a pseudo-progress bar.
 *
 * <p>The bar spans from the window anchor (t = 0) to +2 hours, with a midpoint tick at +1h
 * indicating when the eligible window opens. A vertical cursor moves left-to-right as time
 * passes. The fill colour is driven by {@link WindowState}.
 *
 * <p>Must be updated from the EDT via {@link #update(long, WindowState)}.
 */
public class SpawnWindowBar extends JPanel
{
	/** Total window duration: 2 hours in seconds. */
	private static final int TOTAL_WINDOW_SECONDS = TimeTracking.SECONDS_IN_AN_HOUR * 2;

	private static final int TRACK_HEIGHT = 12;
	private static final int TICK_OVERHANG = 4; // extra pixels the midpoint tick extends below the track
	private static final int LABEL_GAP = 2;

	/** Dark background for the pre-window half of the track. */
	private static final Color TRACK_BG = new Color(35, 35, 35);
	/** Slightly lighter background for the +1h–+2h eligible zone. */
	private static final Color TRACK_WINDOW_BG = new Color(50, 50, 50);
	private static final Color TICK_COLOR = new Color(120, 120, 120);
	private static final Color LABEL_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color CURSOR_COLOR = Color.WHITE;

	/** Alpha applied to the bar fill when in NO_DATA state (estimated from first login). */
	private static final float NO_DATA_ALPHA = 0.55f;

	private long secondsSinceAnchor = 0;
	private WindowState state = WindowState.NO_DATA;

	public SpawnWindowBar()
	{
		setOpaque(false);
	}

	/**
	 * Updates the bar with new timing data and schedules a repaint.
	 * Must be called from the EDT.
	 *
	 * @param secondsSinceAnchor seconds elapsed since the window anchor instant
	 * @param state              the current {@link WindowState}
	 */
	public void update(long secondsSinceAnchor, WindowState state)
	{
		this.secondsSinceAnchor = secondsSinceAnchor;
		this.state = state;
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		FontMetrics fm = getFontMetrics(FontManager.getRunescapeSmallFont());
		int h = TRACK_HEIGHT + TICK_OVERHANG + LABEL_GAP + fm.getHeight() + 2;
		return new Dimension(super.getPreferredSize().width, h);
	}

	@Override
	public Dimension getMaximumSize()
	{
		// Allow the bar to stretch horizontally but never expand vertically beyond its preferred height.
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int w = getWidth();
			int trackY = 0;
			int midX = w / 2;

			// Cursor fraction in [0, 1]; clamped so it never exceeds right edge visually
			double fraction = Math.min(1.0, secondsSinceAnchor / (double) TOTAL_WINDOW_SECONDS);
			int cursorX = (int) (fraction * w);

			// ── Background track ─────────────────────────────────────────────
			g2.setColor(TRACK_BG);
			g2.fillRect(0, trackY, w, TRACK_HEIGHT);

			// Slightly lighter zone for the eligible window (+1h → +2h)
			g2.setColor(TRACK_WINDOW_BG);
			g2.fillRect(midX, trackY, w - midX, TRACK_HEIGHT);

			// ── Elapsed fill ──────────────────────────────────────────────────
			Composite originalComposite = g2.getComposite();
			if (state == WindowState.NO_DATA)
			{
				// Semi-transparent fill to signal estimation rather than confirmed data
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, NO_DATA_ALPHA));
			}

			if (state == WindowState.OVERDUE)
			{
				// Full bar coloured to signal the window has passed
				g2.setColor(state.barFillColor);
				g2.fillRect(0, trackY, w, TRACK_HEIGHT);
			}
			else if (cursorX > 0)
			{
				g2.setColor(state.barFillColor);
				g2.fillRect(0, trackY, cursorX, TRACK_HEIGHT);
			}

			g2.setComposite(originalComposite);

			// ── +1h midpoint tick ─────────────────────────────────────────────
			g2.setColor(TICK_COLOR);
			g2.setStroke(new BasicStroke(1f));
			g2.drawLine(midX, trackY, midX, trackY + TRACK_HEIGHT + TICK_OVERHANG);

			// ── Cursor line ───────────────────────────────────────────────────
			// Not drawn when overdue (cursor is off the right edge) or at position 0
			if (state != WindowState.OVERDUE && cursorX > 0 && cursorX < w)
			{
				g2.setColor(CURSOR_COLOR);
				g2.setStroke(new BasicStroke(2f));
				g2.drawLine(cursorX, trackY, cursorX, trackY + TRACK_HEIGHT);
			}

			// ── Labels ────────────────────────────────────────────────────────
			g2.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g2.getFontMetrics();
			int labelY = trackY + TRACK_HEIGHT + TICK_OVERHANG + LABEL_GAP + fm.getAscent();

			g2.setColor(LABEL_COLOR);

			g2.drawString("Anchor", 0, labelY);

			String midLabel = "+1h";
			int midLabelW = fm.stringWidth(midLabel);
			g2.drawString(midLabel, midX - midLabelW / 2, labelY);

			String endLabel = "+2h";
			int endLabelW = fm.stringWidth(endLabel);
			g2.drawString(endLabel, w - endLabelW, labelY);
		}
		finally
		{
			g2.dispose();
		}
	}
}
