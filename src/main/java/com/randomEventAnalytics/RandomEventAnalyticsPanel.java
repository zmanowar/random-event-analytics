package com.randomEventAnalytics;

import com.google.inject.Inject;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

public class RandomEventAnalyticsPanel extends PluginPanel
{
	private final ArrayList<RandomEventRecordBox> infoBoxes = new ArrayList<RandomEventRecordBox>();
	private final ProgressBar spawnTimeProgressBar = new ProgressBar();
	private final JPanel estimationPanel = new JPanel();
	@Getter
	private final JComponent eventPanel = new JPanel();
	private final RandomEventAnalyticsConfig config;
	private final Client client;
	private final SpawnWindowBar spawnWindowBar = new SpawnWindowBar();
	private final JLabel statusBadgeLabel = new JLabel();
	private final JLabel earliestSpawnLabel = new JLabel("--:--");
	private final JLabel latestSpawnLabel = new JLabel("--:--");
	private final JLabel nextTickTimeLabel = new JLabel("--:--:--");
	private final JLabel tickCountdownLabel = new JLabel("");
	private final JLabel anchorTimeLabel = new JLabel("\u2014");
	private final JLabel sessionLabel = new JLabel("\u2014");
	private final JLabel statusMessageLabel = new JLabel();
	public SimpleDateFormat shortTimeFormat = new SimpleDateFormat("MMM dd, h:mm a");
	public SimpleDateFormat longTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	/** 24-hour clock formatter used for anchor and next-tick labels. Only accessed from the @Schedule thread. */
	private final SimpleDateFormat hmsFormat = new SimpleDateFormat("HH:mm:ss");
	TimeTracking timeTracking;
	RandomEventAnalyticsPlugin plugin;
	private RandomEventRecordBox unconfirmedRandomEventRecordBox;

	// Last-displayed state, read and written only from the @Schedule thread.
	// Used to skip invokeLater when nothing visible has actually changed.
	private long lastSecondsUntilEarliest = Long.MIN_VALUE;
	private long lastSecondsUntilLatest = Long.MIN_VALUE;
	private boolean lastWindowOpen;
	private boolean lastWindowExpired;
	private boolean lastInInstance;
	private boolean lastOfflineExt;
	private boolean lastNoEventsYet;
	private boolean lastNullAnchor = true;
	private WindowState lastWindowState;
	private int lastSecondsUntilTick = Integer.MIN_VALUE;

	@Inject
	RandomEventAnalyticsPanel(RandomEventAnalyticsPlugin plugin, RandomEventAnalyticsConfig config,
							  TimeTracking timeTracking, Client client)
	{
		super();
		this.timeTracking = timeTracking;
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		final JPanel layoutPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(layoutPanel, BoxLayout.Y_AXIS);
		layoutPanel.setLayout(boxLayout);
		add(layoutPanel, BorderLayout.NORTH);

		estimationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		estimationPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		estimationPanel.setLayout(new BoxLayout(estimationPanel, BoxLayout.Y_AXIS));

		// ── Title row ──────────────────────────────────────────────────────────
		final JPanel titleRow = buildRow();
		final JLabel titleLabel = new JLabel("RANDOM EVENTS");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		setupStatusBadge();
		titleRow.add(titleLabel, BorderLayout.WEST);
		titleRow.add(statusBadgeLabel, BorderLayout.EAST);
		estimationPanel.add(titleRow);

		// ── Spawn window bar ───────────────────────────────────────────────────
		estimationPanel.add(Box.createVerticalStrut(8));
		estimationPanel.add(spawnWindowBar);

		// ── Earliest / Latest spawn rows ───────────────────────────────────────
		estimationPanel.add(Box.createVerticalStrut(8));

		final JPanel earliestRow = buildRow();
		earliestRow.add(buildKeyLabel("Earliest spawn"), BorderLayout.WEST);
		earliestSpawnLabel.setFont(FontManager.getRunescapeSmallFont());
		earliestSpawnLabel.setForeground(Color.WHITE);
		earliestRow.add(earliestSpawnLabel, BorderLayout.EAST);
		estimationPanel.add(earliestRow);

		final JPanel latestRow = buildRow();
		latestRow.add(buildKeyLabel("Latest spawn"), BorderLayout.WEST);
		latestSpawnLabel.setFont(FontManager.getRunescapeSmallFont());
		latestSpawnLabel.setForeground(Color.WHITE);
		latestRow.add(latestSpawnLabel, BorderLayout.EAST);
		estimationPanel.add(latestRow);

		// ── Next 5-min tick row ────────────────────────────────────────────────
		estimationPanel.add(Box.createVerticalStrut(4));

		final JPanel tickHeaderRow = buildRow();
		tickHeaderRow.add(buildKeyLabel("Next 5-min tick"), BorderLayout.WEST);
		nextTickTimeLabel.setFont(FontManager.getRunescapeSmallFont());
		nextTickTimeLabel.setForeground(Color.WHITE);
		tickHeaderRow.add(nextTickTimeLabel, BorderLayout.EAST);
		estimationPanel.add(tickHeaderRow);

		// Progress bar counts down to the next 5-min tick
		final JPanel progressWrapper = new JPanel(new BorderLayout());
		progressWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		progressWrapper.setBorder(new EmptyBorder(3, 0, 0, 0));
		spawnTimeProgressBar.setMaximumValue(TimeTracking.SPAWN_INTERVAL_SECONDS);
		spawnTimeProgressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		spawnTimeProgressBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progressWrapper.add(spawnTimeProgressBar, BorderLayout.CENTER);
		estimationPanel.add(progressWrapper);

		// Tick countdown label ("In Xm Ys") — right-aligned
		final JPanel tickCountdownRow = buildRow();
		tickCountdownLabel.setFont(FontManager.getRunescapeSmallFont());
		tickCountdownLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		tickCountdownRow.add(tickCountdownLabel, BorderLayout.EAST);
		estimationPanel.add(tickCountdownRow);

		// ── Anchor & Session footer ────────────────────────────────────────────
		estimationPanel.add(Box.createVerticalStrut(4));

		final JPanel anchorFooterRow = buildRow();
		anchorFooterRow.add(buildKeyLabel("Anchor"), BorderLayout.WEST);
		anchorTimeLabel.setFont(FontManager.getRunescapeSmallFont());
		anchorTimeLabel.setForeground(Color.WHITE);
		anchorFooterRow.add(anchorTimeLabel, BorderLayout.EAST);
		estimationPanel.add(anchorFooterRow);

		final JPanel sessionFooterRow = buildRow();
		sessionFooterRow.add(buildKeyLabel("Session"), BorderLayout.WEST);
		sessionLabel.setFont(FontManager.getRunescapeSmallFont());
		sessionLabel.setForeground(Color.WHITE);
		sessionFooterRow.add(sessionLabel, BorderLayout.EAST);
		estimationPanel.add(sessionFooterRow);

		// ── Status message ─────────────────────────────────────────────────────
		estimationPanel.add(Box.createVerticalStrut(6));
		statusMessageLabel.setFont(FontManager.getRunescapeSmallFont());
		statusMessageLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		estimationPanel.add(statusMessageLabel);

		layoutPanel.add(estimationPanel);

		layoutPanel.add(buildSimulatePanel());

		eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
		eventPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
		layoutPanel.add(eventPanel, BorderLayout.SOUTH);
		updateConfig();
	}

	private JPanel buildSimulatePanel()
	{
		JPanel simulatePanel = new JPanel(new BorderLayout());
		simulatePanel.setBorder(new EmptyBorder(4, 0, 0, 0));
		simulatePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton simulateButton = new JButton("Simulate Spawn");
		simulateButton.setToolTipText("Resets the spawn window as if a random event just occurred. Does not log an event.");
		simulateButton.setFocusPainted(false);
		simulateButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		simulateButton.setForeground(Color.LIGHT_GRAY);
		simulateButton.setFont(FontManager.getRunescapeSmallFont());
		simulateButton.addActionListener(e -> {
			timeTracking.simulateRandomEventSpawned();
			updateEstimation();
		});

		simulatePanel.add(simulateButton, BorderLayout.CENTER);
		return simulatePanel;
	}

	/** Initialises the status badge with the default NO_DATA appearance. */
	private void setupStatusBadge()
	{
		statusBadgeLabel.setOpaque(true);
		statusBadgeLabel.setBackground(WindowState.NO_DATA.badgeColor);
		statusBadgeLabel.setForeground(Color.WHITE);
		statusBadgeLabel.setFont(FontManager.getRunescapeSmallFont());
		statusBadgeLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
		statusBadgeLabel.setText(WindowState.NO_DATA.iconPrefix + WindowState.NO_DATA.label);
	}

	/** Creates a full-width row panel (BorderLayout, dark background) for use in a BoxLayout Y_AXIS container. */
	private JPanel buildRow()
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		return row;
	}

	/** Creates a dimmed key label using the RuneScape small font. */
	private JLabel buildKeyLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}


	public void eventRecordBoxUpdated(RandomEventRecordBox randomEventRecordBox, boolean isConfirmed)
	{
		eventPanel.remove(randomEventRecordBox);
		if (isConfirmed)
		{
			plugin.addRandomEvent(randomEventRecordBox.getRandomEvent());
		}
		infoBoxes.remove(randomEventRecordBox);
		eventPanel.revalidate();
		eventPanel.repaint();
	}

	public void removeUnconfirmedRandom() {
		eventPanel.remove(unconfirmedRandomEventRecordBox);
	}

	public void clearAllRandomsView()
	{
		eventPanel.removeAll();
		infoBoxes.clear();
		eventPanel.repaint();
	}

	public void addRandom(RandomEventRecord record)
	{
		SwingUtilities.invokeLater(() -> {
			RandomEventRecordBox recordBox = new RandomEventRecordBox(this, record);
			eventPanel.add(recordBox, 0);
			infoBoxes.add(recordBox);
		});
	}

	public void addUnconfirmedRandom(RandomEventRecord record)
	{
		SwingUtilities.invokeLater(() -> {
			RandomEventRecordBox recordBox = new RandomEventRecordBox(this, record, false);
			// TODO: Refactor this to store the unconfirmed record box for removal later without reloading.
			unconfirmedRandomEventRecordBox = recordBox;
			eventPanel.add(recordBox, 0);
			infoBoxes.add(recordBox);
		});
	}

	public void updateConfig()
	{
		if (config.timeFormatMode() == TimeFormat.TIME_12H) {
			shortTimeFormat = new SimpleDateFormat("MMM dd, h:mm a");
			longTimeFormat = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a");
		} else {
			shortTimeFormat = new SimpleDateFormat("MMM dd, HH:mm");
			longTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}

	}

	public void updateAllRandomEventBoxes() {
		infoBoxes.forEach(RandomEventRecordBox::update);
	}

	public void updateEstimation()
	{
		// No account data loaded yet (pre-first-login) — show placeholders and bail.
		if (timeTracking.getWindowAnchor() == null)
		{
			if (!lastNullAnchor)
			{
				lastNullAnchor = true;
				SwingUtilities.invokeLater(() -> {
					updateBadge(WindowState.NO_DATA);
					spawnWindowBar.update(0, WindowState.NO_DATA);
					earliestSpawnLabel.setText("--:--");
					latestSpawnLabel.setText("--:--");
					nextTickTimeLabel.setText("--:--:--");
					tickCountdownLabel.setText("");
					anchorTimeLabel.setText("\u2014");
					sessionLabel.setText("\u2014");
					statusMessageLabel.setText("");
				});
			}
			return;
		}

		// Compute all display values on the scheduler thread.
		final int nextTickSeconds = timeTracking.getNextRandomEventEstimation();
		final int progressValue = Math.abs(TimeTracking.SPAWN_INTERVAL_SECONDS - nextTickSeconds);
		final boolean inInstance = client.isInInstancedRegion();
		final boolean windowOpen = timeTracking.isWindowOpen();
		final boolean windowExpired = timeTracking.isWindowExpired();
		final boolean noEventsYet = timeTracking.getLastRandomSpawnInstant() == null;
		final boolean offlineExt = timeTracking.isOfflineExtensionLikely();
		final long secondsUntilEarliest = timeTracking.getSecondsUntilEarliest();
		final long secondsUntilLatest = timeTracking.getSecondsUntilLatest();
		final WindowState windowState = WindowState.from(true, windowExpired, windowOpen, inInstance, noEventsYet);

		// Derive seconds elapsed since the window anchor for the spawn window bar.
		final long secondsSinceAnchor;
		if (!windowOpen && !windowExpired)
		{
			// Before the +1h mark: anchor + 1h - now = secondsUntilEarliest
			secondsSinceAnchor = TimeTracking.SECONDS_IN_AN_HOUR - secondsUntilEarliest;
		}
		else if (!windowExpired)
		{
			// Inside the window: anchor + 2h - now = secondsUntilLatest
			secondsSinceAnchor = 2L * TimeTracking.SECONDS_IN_AN_HOUR - secondsUntilLatest;
		}
		else
		{
			// Past the +2h mark — signal a fully-elapsed bar.
			secondsSinceAnchor = 2L * TimeTracking.SECONDS_IN_AN_HOUR + 1;
		}

		// Skip the EDT dispatch entirely when nothing visible has changed.
		if (!lastNullAnchor
			&& secondsUntilEarliest == lastSecondsUntilEarliest
			&& secondsUntilLatest == lastSecondsUntilLatest
			&& nextTickSeconds == lastSecondsUntilTick
			&& windowState == lastWindowState
			&& windowOpen == lastWindowOpen
			&& windowExpired == lastWindowExpired
			&& inInstance == lastInInstance
			&& offlineExt == lastOfflineExt
			&& noEventsYet == lastNoEventsYet)
		{
			return;
		}

		lastNullAnchor = false;
		lastSecondsUntilEarliest = secondsUntilEarliest;
		lastSecondsUntilLatest = secondsUntilLatest;
		lastSecondsUntilTick = nextTickSeconds;
		lastWindowState = windowState;
		lastWindowOpen = windowOpen;
		lastWindowExpired = windowExpired;
		lastInInstance = inInstance;
		lastOfflineExt = offlineExt;
		lastNoEventsYet = noEventsYet;

		// ── Format all display strings on the scheduler thread ─────────────────

		// Earliest / Latest spawn labels
		final String earliestText;
		final String latestText;
		if (windowExpired)
		{
			earliestText = "\u2014\u2014 : \u2014\u2014";
			latestText = "\u2014\u2014 : \u2014\u2014";
		}
		else if (windowOpen)
		{
			earliestText = "Now";
			latestText = secondsUntilLatest > 0
				? RandomEventAnalyticsUtil.formatSeconds((int) secondsUntilLatest)
				: "\u2014\u2014 : \u2014\u2014";
		}
		else
		{
			earliestText = RandomEventAnalyticsUtil.formatSeconds((int) secondsUntilEarliest);
			latestText = RandomEventAnalyticsUtil.formatSeconds((int) secondsUntilLatest);
		}

		// Absolute time of the next 5-min tick (e.g. "14:35:00")
		final String nextTickTimeText = hmsFormat.format(
			new Date(Instant.now().plusSeconds(Math.max(0, nextTickSeconds)).toEpochMilli()));

		// Tick countdown ("In Xm Ys")
		final int tickSecs = Math.max(0, nextTickSeconds);
		final String tickCountdownText = String.format("In %dm %02ds", tickSecs / 60, tickSecs % 60);

		// Anchor time (annotated with "(login)" when estimated from first login)
		final Instant anchor = timeTracking.getWindowAnchor();
		final String anchorText = hmsFormat.format(new Date(anchor.toEpochMilli()))
			+ (noEventsYet ? " (login)" : "");

		// Session duration
		final String sessionText;
		final Instant loginTime = timeTracking.getLoginTime();
		if (loginTime == null)
		{
			sessionText = "\u2014";
		}
		else
		{
			long sessionSecs = Math.max(0, Duration.between(loginTime, Instant.now()).getSeconds());
			sessionText = String.format("%dh %02dm", sessionSecs / 3600, (sessionSecs % 3600) / 60);
		}

		// Status message
		final String statusMessage;
		if (windowExpired)
		{
			statusMessage = offlineExt
				? "Window has passed. If you were logged out, an offline extension of +10\u201370 min is possible."
				: "Window has passed.";
		}
		else if (windowOpen)
		{
			if (inInstance)
			{
				statusMessage = "Window is open, but you are in an instance. The timer may be paused or reset.";
			}
			else
			{
				int openMinutes = (int) Math.max(0, (TimeTracking.SECONDS_IN_AN_HOUR - secondsUntilLatest) / 60);
				statusMessage = "Window open for " + openMinutes + "m. You are eligible for a random event now.";
			}
		}
		else
		{
			statusMessage = noEventsYet
				? "No events recorded yet. Estimating from first login time."
				: "No spawn possible yet. The earliest window opens at the +1h mark.";
		}

		SwingUtilities.invokeLater(() -> {
			spawnTimeProgressBar.setValue(progressValue);
			updateBadge(windowState);
			spawnWindowBar.update(secondsSinceAnchor, windowState);
			earliestSpawnLabel.setText(earliestText);
			latestSpawnLabel.setText(latestText);
			nextTickTimeLabel.setText(nextTickTimeText);
			tickCountdownLabel.setText(tickCountdownText);
			anchorTimeLabel.setText(anchorText);
			sessionLabel.setText(sessionText);
			statusMessageLabel.setText(
				"<html><body style='width:180px'>" + statusMessage + "</body></html>");
		});
	}

	/** Updates the status badge colour and label text. Must be called from the EDT. */
	private void updateBadge(WindowState state)
	{
		statusBadgeLabel.setBackground(state.badgeColor);
		statusBadgeLabel.setText(state.iconPrefix + state.label);
	}
}
