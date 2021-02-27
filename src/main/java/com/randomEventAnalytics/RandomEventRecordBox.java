package com.randomEventAnalytics;

import com.randomEventAnalytics.localstorage.RandomEventRecord;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;

public class RandomEventRecordBox extends JPanel {
    private final JComponent parentPanel;
    private final RandomEventAnalyticsPlugin plugin;
    private final JPanel container;
    private JPanel randomEventPanel;
    private JPanel confirmationPanel = new JPanel();
    private RandomEventAnalyticsPanel analyticsPanel;
    private RandomEventRecord randomEvent;
    private boolean isConfirmed;

    RandomEventRecordBox(RandomEventAnalyticsPlugin plugin, RandomEventAnalyticsConfig config, Client client, RandomEventAnalyticsPanel analyticsPanel, JComponent parentPanel, RandomEventRecord randomEvent, boolean isConfirmed) {
        this.plugin = plugin;
        this.parentPanel = parentPanel;
        this.analyticsPanel = analyticsPanel;
        this.randomEvent = randomEvent;
        this.isConfirmed = isConfirmed;
        this.container = new JPanel();
        this.randomEventPanel = buildRandomPanel(randomEvent);
        this.parentPanel.add(randomEventPanel, 0);
    }

    public JPanel getRandomEventPanel() {
        return randomEventPanel;
    }

    private void updateConfirmed(boolean confirmed) {
        this.confirmationPanel.removeAll();
        this.container.remove(this.confirmationPanel);
        analyticsPanel.removeRandomRecordBox(this);
        if (confirmed) {
            plugin.addRandomEvent(randomEvent);
        }
        this.container.revalidate();
        this.container.repaint();
    }

    private JPanel buildRandomPanel(RandomEventRecord record) {
        JLabel randomName = new JLabel(record.npcInfoRecord.npcName);
        JLabel spawnTime = new JLabel(new SimpleDateFormat("MMM dd, h:mm").format(record.spawnedTime));

        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setLayout(new BorderLayout());

        JPanel randomInfo = new JPanel();
        randomInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomInfo.setLayout(new GridLayout(2, 1));
        randomInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

        randomName.setFont(FontManager.getRunescapeBoldFont());
        spawnTime.setFont(FontManager.getRunescapeSmallFont());
        randomInfo.add(randomName);
        randomInfo.add(spawnTime);

        container.add(
                new JLabel(RandomEventAnalyticsUtil.getNPCIcon(record.npcInfoRecord.npcId)),
                BorderLayout.WEST
        );
        container.add(randomInfo);
        if (!isConfirmed) {
            this.confirmationPanel = buildRandomEventConfirmation(record);
            container.add(this.confirmationPanel, BorderLayout.SOUTH);
        }
        container.setToolTipText(buildToolTip(record));
        return container;
    }

    private JPanel buildRandomEventConfirmation(RandomEventRecord record) {
        confirmationPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        confirmationPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        confirmationPanel.setLayout(new BorderLayout());
        JLabel confirmRandomLabel = new JLabel("Is this your random event?");
        JButton confirm = new JButton("\u2713");
        confirm.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        confirm.setForeground(Color.GREEN);
        confirm.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                this.updateConfirmed(true);
            });
        });
        JButton cancel = new JButton("\u2717");
        cancel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        cancel.setForeground(Color.RED);
        cancel.addActionListener((e) -> {
            this.updateConfirmed(false);
        });
        confirmationPanel.add(confirmRandomLabel, BorderLayout.NORTH);
        confirmationPanel.add(confirm, BorderLayout.WEST);
        confirmationPanel.add(cancel, BorderLayout.EAST);
        return confirmationPanel;
    }

    private static String buildToolTip(RandomEventRecord record) {
        String data = "<html> "
                + "NPC ID: " + record.npcInfoRecord.npcId
                + "<br>NPC Combat Level: " + record.npcInfoRecord.combatLevel
                + "<br>\u0394 Event Time: " + RandomEventAnalyticsUtil.formatSeconds(record.secondsSinceLastRandomEvent)
                + "<br>Spawned Time: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(record.spawnedTime)
                + "<br>XP/Hr: " + RandomEventAnalyticsUtil.formatNumber(record.xpInfoRecord.totalXpHr)
                + "<br>Total XP: " + RandomEventAnalyticsUtil.formatNumber(record.xpInfoRecord.overallExperience);
        data += "<br>PlayerLocal: <p style=\"font-size: 0.95em\">"
                + "(X: " + record.playerInfoRecord.localX
                + " , Y: " + record.playerInfoRecord.localY
                + ")</p>";
        data += "World: <p style=\"font-size: 0.95em\">"
                + "(X: " + record.playerInfoRecord.worldX
                + " , Y: " + record.playerInfoRecord.worldY
                + " , P: " + record.playerInfoRecord.worldPlane
                + ")</p>";
        data += "</html>";
        return data;

    }

    static String htmlLabel(String key, int value) {
        return htmlLabel(key,
                QuantityFormatter.quantityToRSDecimalStack(value, true)
        );
    }

    static String htmlLabel(String key, String value) {
        return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, value);
    }

    static final DecimalFormat TWO_DECIMAL_FORMAT = new DecimalFormat("0.00");

    static
    {
        TWO_DECIMAL_FORMAT.setRoundingMode(RoundingMode.DOWN);
    }


    private static final String HTML_TOOL_TIP_TEMPLATE =
            "<html>%s %s done<br/>"
                    + "%s %s/hr<br/>"
                    + "%s till goal lvl</html>";

    private static final String HTML_LABEL_TEMPLATE =
            "<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";
}
