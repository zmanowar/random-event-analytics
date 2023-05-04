package com.randomEventAnalytics;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.NpcID;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

public class RandomEventAnalyticsUtil
{

	public static final HashMap<Integer, NpcIdWrapper> NPCS = new HashMap<Integer, NpcIdWrapper>()
	{
		{
			put(NpcID.BEE_KEEPER_6747,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/bee_keeper" +
						".png")),
					false,
					true)
			);
			put(NpcID.CAPT_ARNAV,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/capt_arnav" +
						".png")),
					true,
					true
				)
			);
			put(NpcID.DRUNKEN_DWARF,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads" +
						"/drunken_dwarf.png")),
					true,
					true
				)
			);
			put(NpcID.FLIPPA_6744,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/flippa" +
						".png")),
					false,
					true
				)
			);

			NpcIdWrapper giles = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/giles.png")),
				true,
				true
			);
			put(NpcID.GILES, giles);
			put(NpcID.GILES_5441, giles);

			NpcIdWrapper miles = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/miles.png")),
				true,
				true
			);
			put(NpcID.MILES, miles);
			put(NpcID.MILES_5440, miles);

			NpcIdWrapper niles = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/niles.png")),
				true,
				true
			);
			put(NpcID.NILES, niles);
			put(NpcID.NILES_5439, niles);

			put(NpcID.PILLORY_GUARD,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads" +
						"/pillory_guard.png")),
					false,
					true
				)
			);
			put(NpcID.POSTIE_PETE_6738,
				new NpcIdWrapper(
					new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/postie_pete" +
						".png")),
					false,
					true
				)
			);

			NpcIdWrapper rickTurpentine = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/rick_turpentine" +
					".png")),
				true,
				true
			);
			put(NpcID.RICK_TURPENTINE, rickTurpentine);
			put(NpcID.RICK_TURPENTINE_376, rickTurpentine);

			put(NpcID.SERGEANT_DAMIEN_6743, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/sergeant_damien" +
					".png")),
				false,
				true
			));
			put(NpcID.FREAKY_FORESTER_6748, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/freaky_forester" +
					".png")),
				false,
				true
			));
			put(NpcID.FROG_5429, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/frog.png")),
				true,
				true
			));

			NpcIdWrapper genie = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/genie.png")),
				true,
				true
			);
			put(NpcID.GENIE, genie);
			put(NpcID.GENIE_327, genie);

			NpcIdWrapper drJekyll = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/dr_jekyll" +
					".png")),
				true,
				false
			);
			put(NpcID.DR_JEKYLL, drJekyll);
			put(NpcID.DR_JEKYLL_314, drJekyll);

			NpcIdWrapper evilBob = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/evil_bob.png")),
				false,
				true
			);
			put(NpcID.EVIL_BOB, evilBob);
			put(NpcID.EVIL_BOB_6754, evilBob); // Pete

			put(NpcID.LEO_6746, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/leo.png")),
				false,
				true
			));

			NpcIdWrapper mysteriousOldMan = new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads" +
					"/mysterious_old_man.png")),
				true,
				true
			);
			// TODO: Determine which of these are maze/mime to better describe "isAvailableInWilderness"
			put(NpcID.MYSTERIOUS_OLD_MAN_6750, mysteriousOldMan); // Rick Turpentine style
			put(NpcID.MYSTERIOUS_OLD_MAN_6751, mysteriousOldMan);
			put(NpcID.MYSTERIOUS_OLD_MAN_6752, mysteriousOldMan); // Maze
			put(NpcID.MYSTERIOUS_OLD_MAN_6753, mysteriousOldMan); // Mime

			put(NpcID.QUIZ_MASTER_6755, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/quiz_master" +
					".png")),
				false,
				true
			));
			put(NpcID.DUNCE_6749, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/dunce.png")),
				false,
				true
			));
			put(NpcID.SANDWICH_LADY, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/sandwich_lady" +
					".png")),
				true,
				true
			));
			put(NpcID.STRANGE_PLANT, new NpcIdWrapper(
				new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/strange_plant" +
					".png")),
				true,
				false
			));
		}
	};
	static final DecimalFormat TWO_DECIMAL_FORMAT = new DecimalFormat("0.00");
	private static final String HTML_LABEL_TEMPLATE =
		"<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";
	private static final ImageIcon unknownNPC =
		new ImageIcon(ImageUtil.loadImageResource(RandomEventAnalyticsPlugin.class, "chatheads/question_mark.png"));

	static
	{
		TWO_DECIMAL_FORMAT.setRoundingMode(RoundingMode.DOWN);
	}

	public static Set<Integer> getEventNpcIds()
	{
		return NPCS.keySet();
	}

	public static Set<Integer> getWildernessEventNpcIds()
	{
		return NPCS.entrySet().stream()
			.filter(npcWrapper -> npcWrapper.getValue().isAvailableInWilderness)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	public static Set<Integer> getF2PEventNpcIds()
	{
		return NPCS.entrySet().stream()
			.filter(npcWrapper -> npcWrapper.getValue().isAvailableF2P)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	public static String calculateTimeFromTicks(int ticks)
	{
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

	public static String formatSeconds(int seconds)
	{
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

	static String htmlLabel(String key, int value)
	{
		return htmlLabel(key,
			QuantityFormatter.quantityToRSDecimalStack(value, true)
		);
	}

	static String htmlLabel(String key, String value)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, value);
	}

	public static String formatNumber(int value)
	{
		return NumberFormat.getNumberInstance(Locale.US).format(value);
	}

	public static String formatNumber(long value)
	{
		return NumberFormat.getNumberInstance(Locale.US).format(value);
	}

	public static ImageIcon getNPCIcon(int id)
	{
		NpcIdWrapper npc = NPCS.get(id);
		if (npc == null || npc.icon == null)
		{
			return unknownNPC;
		}
		return npc.icon;
	}

	@Data
	@AllArgsConstructor
	private static class NpcIdWrapper
	{
		ImageIcon icon;
		boolean isAvailableInWilderness;
		boolean isAvailableF2P;
	}
}
