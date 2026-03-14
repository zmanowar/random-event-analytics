package com.randomEventAnalytics;

import java.awt.Color;

/**
 * Represents the current state of the random event spawn window.
 * Drives the status badge colour and label in the estimation panel.
 */
public enum WindowState
{
	/**
	 * No confirmed random events recorded yet — estimating the window from first login.
	 * Badge is grey; the bar still renders using the WAITING fill colour to show estimation.
	 */
	NO_DATA("NO DATA", new Color(80, 80, 80), "", new Color(41, 113, 170)),

	/**
	 * Anchor is set but the 1-hour earliest mark has not been reached.
	 */
	WAITING("WAITING", new Color(41, 113, 170), "\u23F1 ", new Color(41, 113, 170)),   // ⏱

	/**
	 * Past the 1-hour mark — a random event can spawn now.
	 */
	ELIGIBLE("ELIGIBLE", new Color(0, 153, 68), "\u2714 ", new Color(0, 153, 68)),   // ✔

	/**
	 * Eligible, but the player is inside an instanced region.
	 */
	IN_INSTANCE("IN INSTANCE", new Color(180, 140, 0), "\u26A0 ", new Color(180, 140, 0)), // ⚠

	/**
	 * Past the 2-hour latest mark — window has passed.
	 */
	OVERDUE("OVERDUE", new Color(200, 90, 0), "\uD83D\uDD25 ", new Color(200, 90, 0)); // 🔥

	/**
	 * Text shown inside the coloured badge.
	 */
	public final String label;

	/**
	 * Background colour for the status badge.
	 */
	public final Color badgeColor;

	/**
	 * Optional icon prefix rendered before the label text.
	 */
	public final String iconPrefix;

	/**
	 * Colour used for the elapsed fill in the {@link SpawnWindowBar}.
	 */
	public final Color barFillColor;

	WindowState(String label, Color badgeColor, String iconPrefix, Color barFillColor)
	{
		this.label = label;
		this.badgeColor = badgeColor;
		this.iconPrefix = iconPrefix;
		this.barFillColor = barFillColor;
	}

	/**
	 * Derives the current window state from a set of boolean flags computed by
	 * {@link TimeTracking}.
	 *
	 * @param hasAnchor     true when {@link TimeTracking#getWindowAnchor()} is non-null
	 * @param windowExpired true when past the 2-hour mark
	 * @param windowOpen    true when past the 1-hour mark
	 * @param inInstance    true when the player is in an instanced region
	 * @param noEventsYet   true when no confirmed random events have been recorded
	 *                      (window is estimated from first login time)
	 */
	public static WindowState from(boolean hasAnchor, boolean windowExpired, boolean windowOpen,
								   boolean inInstance, boolean noEventsYet)
	{
		if (!hasAnchor)
		{
			return NO_DATA;
		}
		if (noEventsYet)
		{
			return NO_DATA;
		}
		if (windowExpired)
		{
			return OVERDUE;
		}
		if (windowOpen)
		{
			return inInstance ? IN_INSTANCE : ELIGIBLE;
		}
		return WAITING;
	}
}
