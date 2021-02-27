package com.randomEventAnalytics;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.NpcID;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RandomEventAnalyticsUtil {
    public static final Set<Integer> EVENT_NPCS = ImmutableSet.of(
            NpcID.BEE_KEEPER_6747,
            NpcID.CAPT_ARNAV,
            NpcID.DR_JEKYLL, NpcID.DR_JEKYLL_314,
            NpcID.DRUNKEN_DWARF,
            NpcID.DUNCE_6749,
            NpcID.EVIL_BOB, NpcID.EVIL_BOB_6754,
            NpcID.FLIPPA_6744,
            NpcID.FREAKY_FORESTER_6748,
            NpcID.FROG_5429,
            NpcID.GENIE, NpcID.GENIE_327,
            NpcID.GILES, NpcID.GILES_5441,
            NpcID.LEO_6746,
            NpcID.MILES, NpcID.MILES_5440,
            NpcID.MYSTERIOUS_OLD_MAN_6750, NpcID.MYSTERIOUS_OLD_MAN_6751,
            NpcID.MYSTERIOUS_OLD_MAN_6752, NpcID.MYSTERIOUS_OLD_MAN_6753,
            NpcID.NILES, NpcID.NILES_5439,
            NpcID.PILLORY_GUARD,
            NpcID.POSTIE_PETE_6738,
            NpcID.QUIZ_MASTER_6755,
            NpcID.RICK_TURPENTINE, NpcID.RICK_TURPENTINE_376,
            NpcID.SANDWICH_LADY,
            NpcID.SERGEANT_DAMIEN_6743
            ////This is handled separately
            //,NpcID.STRANGE_PLANT
    );

    public static String calculateTimeFromTicks(int ticks) {
        long seconds = Math.round(ticks * 0.6);
        long durationDays = seconds / (24 * 60 * 60);
        long durationHours = (seconds % (24 * 60 * 60)) / (60 * 60);
        long durationHoursTotal = seconds / (60 * 60);
        long durationMinutes = (seconds % (60 * 60)) / 60;
        long durationSeconds = seconds % 60;
        if (durationHoursTotal > 0)
        {
            return String.format("%02d:%02d:%02d", durationHoursTotal, durationMinutes, durationSeconds);
        }

        // Minutes and seconds will always be present
        return String.format("%02d:%02d", durationMinutes, durationSeconds);
    }

    public static String formatSeconds(int seconds) {
        long durationDays = seconds / (24 * 60 * 60);
        long durationHours = (seconds % (24 * 60 * 60)) / (60 * 60);
        long durationHoursTotal = seconds / (60 * 60);
        long durationMinutes = (seconds % (60 * 60)) / 60;
        long durationSeconds = seconds % 60;
        // durationDays = 0 or durationHoursTotal = 0 or goalTimeType = SHORT if we got here.
        // return time remaining in hh:mm:ss or mm:ss format where hh can be > 24
        if (durationHoursTotal > 0)
        {
            return String.format("%02d:%02d:%02d", durationHoursTotal, durationMinutes, durationSeconds);
        }

        // Minutes and seconds will always be present
        return String.format("%02d:%02d", durationMinutes, durationSeconds);
    }

    public static String buildHTMLTable(HashMap<String, String> labelValueMap) {
        String table = "<table>";
        for(Map.Entry<String, String> entry : labelValueMap.entrySet()) {
            table += String.format("<tr><td>%s</td><td>%s</td></tr>", entry.getKey(), entry.getValue());
        }
        table += "</table>";
        return table;
    }

    public static String formatNumber(int value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public static String formatNumber(long value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public static ImageIcon getNPCIcon(int id)
    {
        switch (id)
        {
            case NpcID.BEE_KEEPER_6747:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/bee_keeper.png"));
            case NpcID.CAPT_ARNAV:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/capt_arnav.png"));
            case NpcID.DRUNKEN_DWARF:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/drunken_dwarf.png"));
            case NpcID.FLIPPA_6744:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/flippa.png"));
            case NpcID.GILES:
            case NpcID.GILES_5441:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/giles.png"));
            case NpcID.MILES:
            case NpcID.MILES_5440:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/miles.png"));
            case NpcID.NILES:
            case NpcID.NILES_5439:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/niles.png"));
            case NpcID.PILLORY_GUARD:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/pillory_guard.png"));
            case NpcID.POSTIE_PETE_6738:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/postie_pete.png"));
            case NpcID.RICK_TURPENTINE:
            case NpcID.RICK_TURPENTINE_376:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/rick_turpentine.png"));
            case NpcID.SERGEANT_DAMIEN_6743:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/sergeant_damien.png"));
            case NpcID.FREAKY_FORESTER_6748:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/freaky_forester.png"));
            case NpcID.FROG_5429:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/frog.png"));
            case NpcID.GENIE:
            case NpcID.GENIE_327:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/genie.png"));
            case NpcID.DR_JEKYLL:
            case NpcID.DR_JEKYLL_314:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/dr_jekyll.png"));
            case NpcID.EVIL_BOB:
            case NpcID.EVIL_BOB_6754:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/evil_bob.png"));
            case NpcID.LEO_6746:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/leo.png"));
            case NpcID.MYSTERIOUS_OLD_MAN_6750:
            case NpcID.MYSTERIOUS_OLD_MAN_6751:
            case NpcID.MYSTERIOUS_OLD_MAN_6752:
            case NpcID.MYSTERIOUS_OLD_MAN_6753:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/mysterious_old_man.png"));
            case NpcID.QUIZ_MASTER_6755:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/quiz_master.png"));
            case NpcID.DUNCE_6749:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/dunce.png"));
            case NpcID.SANDWICH_LADY:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/sandwich_lady.png"));
            case NpcID.STRANGE_PLANT:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/strange_plant.png"));
            default:
                return new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/question_mark.png"));
        }
    }
}
