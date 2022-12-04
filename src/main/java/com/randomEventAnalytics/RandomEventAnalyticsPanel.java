package com.randomEventAnalytics;

import com.google.inject.Inject;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.SkillColor;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.ImageUtil;

public class RandomEventAnalyticsPanel extends PluginPanel
{
	private final ArrayList<RandomEventRecordBox> infoBoxes = new ArrayList<RandomEventRecordBox>();
	private final ProgressBar spawnTimeProgressBar = new ProgressBar();
	private final JPanel estimationPanel = new JPanel();
	private final JComponent eventPanel = new JPanel();
	private final RandomEventAnalyticsConfig config;
	private final Client client;
	private final JLabel estimationUntilNext = new JLabel(RandomEventAnalyticsUtil.htmlLabel("Next Event: ", "--:--"));
	private final JLabel numIntervals = new JLabel();
	private final JLabel inInstanceIcon = new JLabel("\u26A0");
	RandomEventAnalyticsTimeTracking timeTracking;
	RandomEventAnalyticsPlugin plugin;

	@Inject
	RandomEventAnalyticsPanel(RandomEventAnalyticsPlugin plugin, RandomEventAnalyticsConfig config,
							  RandomEventAnalyticsTimeTracking timeTracking, Client client)
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
		estimationPanel.setLayout(new BorderLayout());

		final JPanel estimationInfo = new JPanel();
		estimationInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		estimationInfo.setLayout(new GridLayout(3, 1));
		estimationInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

		estimationUntilNext.setFont(FontManager.getRunescapeSmallFont());

		estimationInfo.add(new JLabel("Random Event Estimation"));
		estimationInfo.add(estimationUntilNext);
		estimationInfo.add(numIntervals);

		estimationPanel.add(new JLabel(new ImageIcon(ImageUtil.loadImageResource(getClass(), "estimation_icon.png"))),
			BorderLayout.WEST);

		estimationPanel.add(estimationInfo);
		setupInInstanceIcon();
		estimationPanel.add(inInstanceIcon, BorderLayout.EAST);

		JPanel progressWrapper = new JPanel();
		progressWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		// https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/xptracker/XpInfoBox.java#L277
		progressWrapper.setBorder(new EmptyBorder(10, 0, 0, 0));
		progressWrapper.setLayout(new BorderLayout());

		spawnTimeProgressBar.setMaximumValue(RandomEventAnalyticsTimeTracking.SPAWN_INTERVAL_SECONDS);
		spawnTimeProgressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		spawnTimeProgressBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		progressWrapper.add(spawnTimeProgressBar);

		estimationPanel.add(progressWrapper, BorderLayout.SOUTH);

		layoutPanel.add(estimationPanel);

		eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
		eventPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
		layoutPanel.add(eventPanel, BorderLayout.SOUTH);
	}

	private void setupInInstanceIcon()
	{
		inInstanceIcon.setVisible(false);
		inInstanceIcon.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		inInstanceIcon.setForeground(Color.RED);
		inInstanceIcon.setToolTipText("You are currently in an instance where random events cannot spawn.");
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
			eventPanel.add(recordBox, 0);
			infoBoxes.add(recordBox);
		});
	}

	public void updateConfig()
	{
		estimationPanel.setVisible(config.enableEstimation());
	}

	public void updateEstimation()
	{
		if (!config.enableEstimation())
		{
			return;
		}

		spawnTimeProgressBar.setValue(Math.abs(RandomEventAnalyticsTimeTracking.SPAWN_INTERVAL_SECONDS - timeTracking.getNextRandomEventEstimation()));
		numIntervals.setText(String.valueOf(timeTracking.getIntervalsSinceLastRandom()));

		SwingUtilities.invokeLater(() -> {
			int estimatedSeconds = timeTracking.getNextRandomEventEstimation();
			inInstanceIcon.setVisible(client.isInInstancedRegion());
			estimationUntilNext.setText(RandomEventAnalyticsUtil.htmlLabel("Next Event: ",
				RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds))));
			estimationUntilNext.setToolTipText("Intervals: " + timeTracking.getIntervalsSinceLastRandom());
		});
	}
}
