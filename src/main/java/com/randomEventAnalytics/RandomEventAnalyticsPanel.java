package com.randomEventAnalytics;

import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
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
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomEventAnalyticsPanel extends PluginPanel
{
	private static final Logger log = LoggerFactory.getLogger(RandomEventAnalyticsPanel.class);
	private final ArrayList<RandomEventRecordBox> infoBoxes = new ArrayList<RandomEventRecordBox>();

	private final JPanel estimationPanel = new JPanel();
	private final JComponent eventPanel = new JPanel();
	private final RandomEventAnalyticsConfig config;
	private final Client client;
	private final JLabel estimationUntilNext = new JLabel(RandomEventAnalyticsUtil.htmlLabel("Next Event: ", "--:--"));
	private final JLabel inInstanceIcon = new JLabel("\u26A0");
	RandomEventAnalyticsPlugin plugin;

	RandomEventAnalyticsPanel(RandomEventAnalyticsPlugin plugin, RandomEventAnalyticsConfig config, Client client)
	{
		super();
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
		estimationInfo.setLayout(new GridLayout(2, 1));
		estimationInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

		estimationUntilNext.setFont(FontManager.getRunescapeSmallFont());

		estimationInfo.add(new JLabel("Random Event Estimation"));
		estimationInfo.add(estimationUntilNext);


		estimationPanel.add(
			new JLabel(new ImageIcon(ImageUtil.loadImageResource(getClass(), "estimation_icon.png"))),
			BorderLayout.WEST
		);

		estimationPanel.add(estimationInfo);
		setupInInstanceIcon();
		estimationPanel.add(inInstanceIcon, BorderLayout.EAST);
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
		log.debug("Adding " + record.npcInfoRecord.npcName);
		SwingUtilities.invokeLater(() -> {
			RandomEventRecordBox recordBox = new RandomEventRecordBox(this, record);
			eventPanel.add(recordBox, 0);
			infoBoxes.add(recordBox);
		});
	}

	public void addUnconfirmedRandom(RandomEventRecord record)
	{
		log.debug("Adding " + record.npcInfoRecord.npcName + " (unconfirmed)");
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

	public void updateEstimation(int estimatedSeconds)
	{
		SwingUtilities.invokeLater(() -> updateEstimatedSecondsAsync(estimatedSeconds));
	}

	private void updateEstimatedSecondsAsync(int estimatedSeconds)
	{
		inInstanceIcon.setVisible(client.isInInstancedRegion());
		String label = estimatedSeconds >= 0 ? "Next Event: " : "Overestimate: ";
		estimationUntilNext.setText(RandomEventAnalyticsUtil.htmlLabel(label, RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds))));
	}
}
