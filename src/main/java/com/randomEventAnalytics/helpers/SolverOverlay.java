package com.randomEventAnalytics.helpers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.randomEventAnalytics.RandomEventAnalyticsConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SolverOverlay extends Overlay
{
	private final Client client;

	private final EvilBobSolver evilBobSolver;	private final RandomEventAnalyticsConfig config;
	private final EvilTwinSolver evilTwinSolver;
	private final MimeSolver mimeSolver;
	private final PinballSolver pinballSolver;
	private final SandwichLadySolver sandwichLadySolver;
	private final FreakyForesterSolver freakyForesterSolver;
	private final PrisonPeteSolver prisonPeteSolver;
	private final QuizMasterSolver quizMasterSolver;
	private final MazeSolver mazeSolver;
	private final GravediggerSolver gravediggerSolver;
	private final DrillDemonSolver drillDemonSolver;
	private final SurpriseExamSolver surpriseExamSolver;
	private final CaptArnavSolver captArnavSolver;
	private final BeekeeperSolver beekeeperSolver;

	@Inject
	SolverOverlay(Client client, RandomEventAnalyticsConfig config, EvilBobSolver evilBobSolver,
				  EvilTwinSolver evilTwinSolver, MimeSolver mimeSolver, PinballSolver pinballSolver,
				  SandwichLadySolver sandwichLadySolver, FreakyForesterSolver freakyForesterSolver,
				  PrisonPeteSolver prisonPeteSolver, QuizMasterSolver quizMasterSolver, MazeSolver mazeSolver,
				  GravediggerSolver gravediggerSolver, DrillDemonSolver drillDemonSolver, SurpriseExamSolver surpriseExamSolver, CaptArnavSolver captArnavSolver, BeekeeperSolver beekeeperSolver) {
		this.client = client;
		this.config = config;
		this.evilBobSolver = evilBobSolver;
		this.evilTwinSolver = evilTwinSolver;
		this.mimeSolver = mimeSolver;
		this.pinballSolver = pinballSolver;
		this.sandwichLadySolver = sandwichLadySolver;
		this.freakyForesterSolver = freakyForesterSolver;
		this.prisonPeteSolver = prisonPeteSolver;
		this.quizMasterSolver = quizMasterSolver;
		this.mazeSolver = mazeSolver;
		this.gravediggerSolver = gravediggerSolver;
		this.drillDemonSolver = drillDemonSolver;
		this.surpriseExamSolver = surpriseExamSolver;
		this.captArnavSolver = captArnavSolver;
		this.beekeeperSolver = beekeeperSolver;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableHelpers()) return null;
		/**
		 * TODO: Optimize this by not getting widgets or GameItems dynamically
		 *  every render cycle and instead rely on class attributes set onGameTick.
		 */
		if (mimeSolver.isEnabled()) {
			highlightWidget(graphics, mimeSolver.getMimeEmoteWidget());
		}
		if (evilTwinSolver.isEnabled()) {
			highlightNpc(graphics, evilTwinSolver.getEvilTwin());
		}
		if (pinballSolver.isEnabled()) {
			highlightGameObject(graphics, pinballSolver.getActivePinballGameObject());
		}
		if (evilBobSolver.isEnabled()) {
			highlightGameObject(graphics, evilBobSolver.getCorrectCatStatue());
		}
		if (sandwichLadySolver.isEnabled()) {
			highlightWidget(graphics, sandwichLadySolver.getFoodItemWidget());
		}
		if (freakyForesterSolver.isEnabled()) {
			for (NPC npc : freakyForesterSolver.getValidPheasants())
			{
				highlightNpc(graphics, npc);
			}
		}
		if (prisonPeteSolver.isEnabled()) {
			for (NPC npc : prisonPeteSolver.getValidBalloonAnimals()) {
				highlightNpc(graphics, npc);
			}
		}
		if(quizMasterSolver.isEnabled()) {
			highlightWidget(graphics, quizMasterSolver.getOddWidget());
		}
		if(mazeSolver.isEnabled()) {
			for(WallObject wallObject : mazeSolver.getDoors()) {
				Polygon poly = getPolygonFromTileObject(wallObject);
				if (poly == null) continue;

				OverlayUtil.renderPolygon(graphics, poly, config.helperHighlightColor());
			}
		}
		if (gravediggerSolver.isEnabled()) {
			for (GravediggerSolver.Headstone headstone : gravediggerSolver.getHeadstones()) {
				addTextToGameObject(graphics, headstone.getGameObject(), headstone.getName());
			}
		}
		if (drillDemonSolver.isEnabled()) {
			highlightTileObject(graphics, drillDemonSolver.getCurrentTileObject());
		}
		if (surpriseExamSolver.isEnabled()) {
			for (Widget widget : surpriseExamSolver.getMatchThreeAnswerWidgets()) {
				highlightWidget(graphics, widget);
			}
			highlightWidget(graphics, surpriseExamSolver.getWhatNextAnswerWidget());
		}
		if (captArnavSolver.isEnabled()) {
			for (Widget widget : captArnavSolver.getCorrectTreasureWidgets()) {
				highlightWidget(graphics, widget);
			}
		}
		if (beekeeperSolver.isEnabled())
		{
			for (Widget widget : beekeeperSolver.getAnswerWidgets())
			{
				highlightWidget(graphics, widget);
			}
		}
		return null;
	}

	private Polygon getPolygonFromTileObject(TileObject tileObject) {
		LocalPoint lp = LocalPoint.fromWorld(client, tileObject.getWorldLocation());
		if (lp == null) return null;

		return Perspective.getCanvasTilePoly(client, lp);
	}

	private void highlightGameObject(Graphics2D graphics, GameObject gameObject) {
		if (gameObject == null || gameObject.getConvexHull() == null) return;
		OverlayUtil.renderPolygon(graphics, gameObject.getConvexHull(), config.helperHighlightColor());
	}

	private void addTextToGameObject(Graphics2D graphics, GameObject gameObject, String text) {
		OverlayUtil.renderTextLocation(graphics, gameObject.getCanvasLocation(), text, config.helperTextColor());
	}

	private void highlightNpc(Graphics2D graphics, NPC npc) {
		if (npc == null) return;
		Polygon poly = npc.getCanvasTilePoly();
		if (poly == null) return;
		OverlayUtil.renderPolygon(graphics, poly, config.helperHighlightColor());
	}

	private void highlightWidget(Graphics2D graphics, Widget widget) {
		if (widget == null) return;
		OverlayUtil.renderPolygon(graphics, widget.getBounds(), config.helperHighlightColor());
	}


	private void highlightTileObject(Graphics2D graphics, TileObject tileObject)
	{
		if (tileObject == null) return;
		OverlayUtil.renderTileOverlay(graphics, tileObject, "", config.helperHighlightColor());
	}
}
