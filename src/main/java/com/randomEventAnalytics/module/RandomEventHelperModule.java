package com.randomEventAnalytics.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.randomEventAnalytics.helpers.BeekeeperSolver;
import com.randomEventAnalytics.helpers.CaptArnavSolver;
import com.randomEventAnalytics.helpers.CerterSolver;
import com.randomEventAnalytics.helpers.DrillDemonSolver;
import com.randomEventAnalytics.helpers.EvilBobSolver;
import com.randomEventAnalytics.helpers.EvilTwinSolver;
import com.randomEventAnalytics.helpers.FreakyForesterSolver;
import com.randomEventAnalytics.helpers.GravediggerSolver;
import com.randomEventAnalytics.helpers.MazeSolver;
import com.randomEventAnalytics.helpers.MimeSolver;
import com.randomEventAnalytics.helpers.PinballSolver;
import com.randomEventAnalytics.helpers.PrisonPeteSolver;
import com.randomEventAnalytics.helpers.QuizMasterSolver;
import com.randomEventAnalytics.helpers.SandwichLadySolver;
import com.randomEventAnalytics.helpers.SolverOverlayManager;
import com.randomEventAnalytics.helpers.SurpriseExamSolver;

public class RandomEventHelperModule extends AbstractModule
{
	@Override
	protected void configure() {
		Multibinder<SolverModuleComponent> lifecycleComponents = Multibinder.newSetBinder(binder(), SolverModuleComponent.class);
		lifecycleComponents.addBinding().to(MimeSolver.class);
		lifecycleComponents.addBinding().to(EvilTwinSolver.class);
		lifecycleComponents.addBinding().to(PinballSolver.class);
		lifecycleComponents.addBinding().to(EvilBobSolver.class);
		lifecycleComponents.addBinding().to(SandwichLadySolver.class);
		lifecycleComponents.addBinding().to(FreakyForesterSolver.class);
		lifecycleComponents.addBinding().to(PrisonPeteSolver.class);
		lifecycleComponents.addBinding().to(QuizMasterSolver.class);
		lifecycleComponents.addBinding().to(MazeSolver.class);
		lifecycleComponents.addBinding().to(GravediggerSolver.class);
		lifecycleComponents.addBinding().to(DrillDemonSolver.class);
		lifecycleComponents.addBinding().to(SurpriseExamSolver.class);
		lifecycleComponents.addBinding().to(CaptArnavSolver.class);
		lifecycleComponents.addBinding().to(BeekeeperSolver.class);
		lifecycleComponents.addBinding().to(CerterSolver.class);
		lifecycleComponents.addBinding().to(SolverOverlayManager.class);

	}
}
