package com.randomEventAnalytics;

import com.randomEventAnalytics.localstorage.RandomEventRecord;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class RandomEventRecordBox extends JPanel
{
	private final RandomEventAnalyticsPanel panel;
	private final RandomEventRecord randomEvent;

	private final JLabel spawnedTimeLabel = new JLabel();

	RandomEventRecordBox(RandomEventAnalyticsPanel panel, RandomEventRecord randomEvent)
	{
		this(panel, randomEvent, true);
	}

	RandomEventRecordBox(RandomEventAnalyticsPanel panel, RandomEventRecord randomEvent, boolean isConfirmed)
	{
		this.randomEvent = randomEvent;
		this.panel = panel;
		buildRandomEventPanel(randomEvent, isConfirmed);
	}

	private String buildToolTip(RandomEventRecord record)
	{
		String spawnedTime = panel.longTimeFormat.format(record.spawnedTime);
		String data =
			"<html> " + "NPC ID: " + record.npcInfoRecord.npcId + "<br>\u0394 Event Time: " + RandomEventAnalyticsUtil.formatSeconds(record.secondsSinceLastRandomEvent) + "<br>Spawned Time: " + spawnedTime;
		data += "<br>PlayerLocal: <p style=\"font-size: 0.95em\">" + "(X: " + record.playerInfoRecord.localX + " , Y: "
			+ record.playerInfoRecord.localY + ")</p>";
		data += "Intervals: ";
		if (record.intervalsSinceLastRandom > 0)
		{
			data += record.intervalsSinceLastRandom + "<p style=\"font-size: 0.95em\">(" + record.intervalsSinceLastRandom * 5 + " minutes)</p>";
		} else {
			data += "Record has no data<br>";
		}
		data += "World: <p style=\"font-size: 0.95em\">" + "(X: " + record.playerInfoRecord.worldX + " , Y: " + record.playerInfoRecord.worldY + " , P: " + record.playerInfoRecord.worldPlane + ")</p>";
		data += "</html>";
		return data;

	}

	public RandomEventRecord getRandomEvent()
	{
		return randomEvent;
	}

	private JPanel buildRandomEventPanel(RandomEventRecord record, boolean isConfirmed)
	{
		JLabel randomName = new JLabel(record.npcInfoRecord.npcName);
		spawnedTimeLabel.setText(panel.shortTimeFormat.format(record.spawnedTime));

		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		this.setLayout(new BorderLayout());

		JPanel randomInfo = new JPanel();
		randomInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		randomInfo.setLayout(new GridLayout(2, 1));
		randomInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

		randomName.setFont(FontManager.getRunescapeBoldFont());
		spawnedTimeLabel.setFont(FontManager.getRunescapeSmallFont());
		randomInfo.add(randomName);
		randomInfo.add(spawnedTimeLabel);

		this.add(new JLabel(RandomEventAnalyticsUtil.getNPCIcon(record.npcInfoRecord.npcId)), BorderLayout.WEST);
		this.add(randomInfo);
		this.setToolTipText(buildToolTip(record));
		if (!isConfirmed)
		{
			this.add(buildConfirmationPanel(record));
		}
		update();
		return this;
	}

	private JPanel buildConfirmationPanel(RandomEventRecord record)
	{
		JPanel confirmationPanel = new JPanel();
		confirmationPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		confirmationPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		confirmationPanel.setLayout(new BorderLayout());
		JLabel confirmRandomLabel = new JLabel("Is this your random event?");
		JButton confirm = new JButton("\u2713");
		confirm.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		confirm.setForeground(Color.GREEN);
		confirm.addActionListener((e) -> {
			SwingUtilities.invokeLater(() -> {
				panel.eventRecordBoxUpdated(this, true);
			});
		});
		JButton cancel = new JButton("\u2717");
		cancel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		cancel.setForeground(Color.RED);
		cancel.addActionListener((e) -> {
			panel.eventRecordBoxUpdated(this, false);
		});
		confirmationPanel.add(confirmRandomLabel, BorderLayout.NORTH);
		confirmationPanel.add(confirm, BorderLayout.WEST);
		confirmationPanel.add(cancel, BorderLayout.EAST);
		return confirmationPanel;
	}

	void update() {
		// TODO: We'll eventually want to update the rest of the values, but
		//		for now just the spawned time (when config changed)
		spawnedTimeLabel.setText(panel.shortTimeFormat.format(randomEvent.spawnedTime));
		this.setToolTipText(buildToolTip(randomEvent));
	}
}
