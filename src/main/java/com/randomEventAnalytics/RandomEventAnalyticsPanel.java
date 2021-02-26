package com.randomEventAnalytics;

import com.randomEventAnalytics.localstorage.RandomEventRecord;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class RandomEventAnalyticsPanel extends PluginPanel {
    RandomEventAnalyticsPlugin plugin;


    private final ArrayList<RandomEventRecordBox> infoBoxes = new ArrayList<RandomEventRecordBox>();

    private final JPanel estimationPanel = new JPanel();
    private final JComponent eventPanel = new JPanel();
    private final JPanel eventLog = new JPanel();
    private final RandomEventAnalyticsConfig config;
    private final Client client;
    private final JLabel estimationUntilNext = new JLabel(RandomEventRecordBox.htmlLabel("Next Event: ", "--:--"));
    private final JLabel inInstanceIcon = new JLabel("\u26A0");
    private final String IN_INSTANCE_TOOLTIP = "You are currently in an instance where random events cannot spawn.";
    private static final Logger log = LoggerFactory.getLogger(RandomEventAnalyticsPanel.class);

    RandomEventAnalyticsPanel(RandomEventAnalyticsPlugin plugin, RandomEventAnalyticsConfig config, Client client) {
        super();
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setBorder(new EmptyBorder(6 ,6, 6, 6));
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
                new JLabel(new ImageIcon(ImageUtil.getResourceStreamFromClass(getClass(), "estimation_icon.png"))),
                BorderLayout.WEST
        );

        estimationPanel.add(estimationInfo);
        setupInInstanceIcon();
        estimationPanel.add(inInstanceIcon, BorderLayout.EAST);
        layoutPanel.add(estimationPanel);

        eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
        eventPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        eventLog.setBorder(new EmptyBorder(10, 10, 10, 10));
        eventLog.add(new JLabel("Login to view the event log"));
        //initialEventLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        layoutPanel.add(eventLog);
        layoutPanel.add(eventPanel, BorderLayout.SOUTH);
    }

    private void setupInInstanceIcon() {
        inInstanceIcon.setVisible(false);
        inInstanceIcon.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        inInstanceIcon.setForeground(Color.RED);
        inInstanceIcon.setToolTipText(IN_INSTANCE_TOOLTIP);
    }

    public void setEmptyLog() {
        this.setEventLog("<html style='text-align:center'>Event log will populate<br>once a random event occurs</html>");
    }

    private void setEventLog(String label) {
        SwingUtilities.invokeLater(() -> {
            eventLog.setVisible(true);
            eventLog.removeAll();
            eventLog.add(new JLabel("<html><div style='text-align:center'>" + label + "</div></html>"));
            eventLog.revalidate();
            eventLog.repaint();
        });
    }

    public void clearEventLog() {
        eventLog.setVisible(false);
        eventLog.removeAll();
        eventLog.revalidate();
        eventLog.repaint();
    }

    public void removeRandomRecordBox(RandomEventRecordBox recordBox) {
        eventPanel.remove(recordBox.getRandomEventPanel());
        infoBoxes.remove(recordBox);
        this.repaintAll();
    }

    void clearAllRandomsView() {
        eventPanel.removeAll();
        infoBoxes.clear();
        this.repaintAll();
    }

    void addRandom(RandomEventRecord record) {
        log.debug("Adding " + record.npcInfoRecord.npcName);
        SwingUtilities.invokeLater(() -> {
            RandomEventRecordBox recordBox = new RandomEventRecordBox(plugin, config, client, this, eventPanel, record, true);
            infoBoxes.add(recordBox);
        });
    }

    void addUnconfirmedRandom(RandomEventRecord record) {
        log.debug("Adding " + record.npcInfoRecord.npcName + " (unconfirmed)");
        SwingUtilities.invokeLater(() -> {
            RandomEventRecordBox recordBox = new RandomEventRecordBox(plugin, config, client, this, eventPanel, record, false);
            infoBoxes.add(recordBox);
        });
    }

    private void repaintAll() {
        eventPanel.revalidate();
        eventPanel.repaint();
        revalidate();
        repaint();
    }

    void updateSeconds(int estimatedSeconds) {
        SwingUtilities.invokeLater(() -> rebuildAsync(estimatedSeconds));
    }

    private void rebuildAsync(int estimatedSeconds) {
        inInstanceIcon.setVisible(client.isInInstancedRegion());
        String label = estimatedSeconds >= 0 ? "Next Event: " :  "Overestimate: ";
        estimationUntilNext.setText(RandomEventRecordBox.htmlLabel(label, RandomEventAnalyticsUtil.formatSeconds(Math.abs(estimatedSeconds))));
    }
}
