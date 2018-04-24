/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package de.tum.bgu.msm;

import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.data.SummarizeData;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.events.EventManager;
import de.tum.bgu.msm.events.EventTypes;
import de.tum.bgu.msm.events.IssueCounter;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SkimUtil;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Greg Erhardt
 * Created on Dec 2, 2009
 */
public final class SiloModel {
	private final static Logger LOGGER = Logger.getLogger(SiloModel.class);

	private final Set<Integer> tdmYears = new HashSet<>();
	private final Set<Integer> skimYears = new HashSet<>();
    private final Set<Integer> scalingYears = new HashSet<>();

    private boolean trackTime;
    private long[][] timeCounter;
    private long startTime;

	private SiloModelContainer modelContainer;
	private SiloDataContainer dataContainer;
	private final Config matsimConfig;

	public SiloModel() {
		this(null) ;
	}

	public SiloModel(Config matsimConfig) {
		IssueCounter.setUpCounter();
		SiloUtil.modelStopper("initialize");
		this.matsimConfig = matsimConfig ;
	}

	public void runModel() {
		if (!Properties.get().main.runSilo) {
			return;
		}
		setupModel();
		runYearByYear();
		endSimulation();
	}

	private void setupModel() {
		LOGGER.info("Setting up SILO Model (Implementation " + Properties.get().main.implementation + ")");

        setupContainer();
        setupYears();
        setupAccessibility();
		setupTimeTracker();

        if (Properties.get().main.writeSmallSynpop) {
            dataContainer.getHouseholdData().writeOutSmallSynPop();
        }
		if (Properties.get().main.createPrestoSummary) {
			SummarizeData.preparePrestoSummary(dataContainer.getGeoData());
		}
	}

    private void setupContainer() {
        dataContainer = SiloDataContainer.loadSiloDataContainer(Properties.get());
		IssueCounter.regionSpecificCounters(dataContainer.getGeoData());
		dataContainer.getHouseholdData().setTypeOfAllHouseholds();
		dataContainer.getHouseholdData().setHighestHouseholdAndPersonId();
		dataContainer.getHouseholdData().calculateInitialSettings();
		dataContainer.getJobData().calculateEmploymentForecast();
		dataContainer.getJobData().identifyVacantJobs();
		dataContainer.getJobData().calculateJobDensityByZone();
		dataContainer.getRealEstateData().fillQualityDistribution();
		dataContainer.getRealEstateData().setHighestVariables();
		dataContainer.getRealEstateData().identifyVacantDwellings();

        modelContainer = SiloModelContainer.createSiloModelContainer(dataContainer, matsimConfig);
    }

    private void setupAccessibility() {

	    if(Properties.get().transportModel.runMatsim) {
	        modelContainer.getTransportModel().runTransportModel(Properties.get().main.startYear);
        } else {
            SkimUtil.updateCarSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                    Properties.get().main.startYear, Properties.get());
        }
        SkimUtil.updateTransitSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                Properties.get().main.startYear, Properties.get());
        modelContainer.getAcc().initialize();
        modelContainer.getAcc().calculateHansenAccessibilities(Properties.get().main.startYear);
    }



    private void setupTimeTracker() {
		trackTime = Properties.get().main.trackTime;
		timeCounter = new long[EventTypes.values().length + 12][Properties.get().main.endYear + 1];
		startTime = 0;
		IssueCounter.logIssues(dataContainer.getGeoData());
	}

	private void setupYears() {
		scalingYears.addAll(Properties.get().main.scalingYears);
		if (!scalingYears.isEmpty()) {
			SummarizeData.readScalingYearControlTotals();
		}
		tdmYears.addAll(Properties.get().transportModel.modelYears);
		skimYears.addAll(Properties.get().transportModel.skimYears);
	}

	private void runYearByYear() {
		for (int year = Properties.get().main.startYear; year < Properties.get().main.endYear; year ++) {
			if (scalingYears.contains(year)) {
				SummarizeData.scaleMicroDataToExogenousForecast(year, dataContainer);
			}
			LOGGER.info("Simulating changes from year " + year + " to year " + (year + 1));
			IssueCounter.setUpCounter();    // setup issue counter for this simulation period
			SiloUtil.trackingFile("Simulating changes from year " + year + " to year " + (year + 1));
			EventManager em = new EventManager(dataContainer);

			if (trackTime) startTime = System.currentTimeMillis();
			modelContainer.getIomig().setupInOutMigration(year);
			if (trackTime) timeCounter[EventTypes.values().length][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			modelContainer.getCons().planNewDwellingsForThisComingYear(year, modelContainer, dataContainer);
			if (trackTime) timeCounter[EventTypes.values().length + 1][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			if (year != Properties.get().main.implementation.BASE_YEAR) {
				modelContainer.getUpdateJobs().updateJobInventoryMultiThreadedThisYear(year);
				dataContainer.getJobData().identifyVacantJobs();
			}
			if (trackTime) timeCounter[EventTypes.values().length + 2][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			dataContainer.getHouseholdData().setUpChangeOfJob(year);   // has to run after updateJobInventoryThisYear, as updateJobInventoryThisYear may remove jobs
			if (trackTime) timeCounter[EventTypes.values().length + 3][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
            List<int[]> couples = modelContainer.getMardiv().selectCouplesToGetMarriedThisYear();
			if (trackTime) timeCounter[EventTypes.values().length + 5][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			em.createListOfEvents(couples);
			if (trackTime) timeCounter[EventTypes.values().length + 4][year] += System.currentTimeMillis() - startTime;

			if (skimYears.contains(year) && !tdmYears.contains(year) &&
					!Properties.get().transportModel.runTravelDemandModel &&
					year != Properties.get().main.startYear) {
				// skims are (in non-Matsim case) always read in start year and in every year the transportation model ran. Additional
				// years to read skims may be provided in skimYears
				if (!Properties.get().transportModel.runMatsim) {
                    SkimUtil.updateCarSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                            Properties.get().main.startYear, Properties.get());
				}
                SkimUtil.updateTransitSkim((SkimTravelTimes) modelContainer.getAcc().getTravelTimes(),
                        Properties.get().main.startYear, Properties.get());
                modelContainer.getAcc().calculateHansenAccessibilities(year);
            }


			if (trackTime) startTime = System.currentTimeMillis();
			modelContainer.getDdOverwrite().addDwellings(year);
			if (trackTime) timeCounter[EventTypes.values().length + 10][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			modelContainer.getMove().calculateRegionalUtilities();
			modelContainer.getMove().calculateAverageHousingSatisfaction();
			if (trackTime) timeCounter[EventTypes.values().length + 6][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			if (year != Properties.get().main.implementation.BASE_YEAR) dataContainer.getHouseholdData().adjustIncome();
			if (trackTime) timeCounter[EventTypes.values().length + 9][year] += System.currentTimeMillis() - startTime;

			if (trackTime) startTime = System.currentTimeMillis();
			if (year == Properties.get().main.implementation.BASE_YEAR || year != Properties.get().main.startYear)
				SiloUtil.summarizeMicroData(year, modelContainer, dataContainer);
			if (trackTime) timeCounter[EventTypes.values().length + 7][year] += System.currentTimeMillis() - startTime;

			LOGGER.info("  Simulating events");
			// walk through all events
			for (int i = 1; i <= em.getNumberOfEvents(); i++) {
				int[] event = em.selectNextEvent();
				if (event[1] == SiloUtil.trackPp || event[1] == SiloUtil.trackHh || event[1] == SiloUtil.trackDd)
					SiloUtil.trackWriter.println ("Check event " + EventTypes.values()[event[0]] +  " for pp/hh/dd " +
							event[1]);
				if (event[0] == EventTypes.BIRTHDAY.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getBirth().checkBirthday(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_DEATH.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getDeath().chooseDeath(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_BIRTH.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getBirth().chooseBirth(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_LEAVE_PARENT_HH.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getLph().chooseLeaveParentHh(event[1], modelContainer);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_MARRIAGE.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
                    int[] couple = Arrays.copyOfRange(event, 1,3);
					modelContainer.getMardiv().marryCouple(couple);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_DIVORCE.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getMardiv().chooseDivorce(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_SCHOOL_UNIV.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getChangeSchoolUniv().updateSchoolUniv(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.CHECK_DRIVERS_LICENSE.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getDriversLicense().checkLicenseChange(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.FIND_NEW_JOB.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getEmployment().lookForJob(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.QUIT_JOB.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getEmployment().quitJob(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.HOUSEHOLD_MOVE.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getMove().chooseMove(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.INMIGRATION.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getIomig().inmigrateHh(event[1], modelContainer);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.OUT_MIGRATION.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getIomig().outMigrateHh(event[1], false);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.DD_CHANGE_QUAL.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getRenov().checkRenovation(event[1]);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.DD_DEMOLITION.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getDemol().checkDemolition(event[1], modelContainer, year);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else if (event[0] == EventTypes.DD_CONSTRUCTION.ordinal()) {
					if (trackTime) startTime = System.currentTimeMillis();
					modelContainer.getCons().buildDwelling(event[1], year, modelContainer, dataContainer);
					if (trackTime) timeCounter[event[0]][year] += System.currentTimeMillis() - startTime;
				} else {
					LOGGER.warn("Unknown event type: " + event[0]);
				}
			}

			if (trackTime) startTime = System.currentTimeMillis();
			int[] carChangeCounter = modelContainer.getCarOwnershipModel().updateCarOwnership(dataContainer.getHouseholdData().getUpdatedHouseholds());
			dataContainer.getHouseholdData().clearUpdatedHouseholds();
			if (trackTime) timeCounter[EventTypes.values().length + 11][year] += System.currentTimeMillis() - startTime;

			if ( Properties.get().transportModel.runMatsim || Properties.get().transportModel.runTravelDemandModel
                    || Properties.get().main.createMstmOutput) {
                if (tdmYears.contains(year + 1)) {
                    modelContainer.getTransportModel().runTransportModel(year + 1);
                    modelContainer.getAcc().calculateHansenAccessibilities(year + 1);
                }
            }

			if (trackTime) startTime = System.currentTimeMillis();
			modelContainer.getPrm().updatedRealEstatePrices();
			if (trackTime) timeCounter[EventTypes.values().length + 8][year] += System.currentTimeMillis() - startTime;

			EventManager.logEvents(carChangeCounter, dataContainer);
			IssueCounter.logIssues(dataContainer.getGeoData());           // log any issues that arose during this simulation period

			LOGGER.info("  Finished this simulation period with " + dataContainer.getHouseholdData().getPersonCount() +
					" persons, " + dataContainer.getHouseholdData().getHouseholds().size() + " households and "  +
					dataContainer.getRealEstateData().getDwellings().size() + " dwellings.");
			if (SiloUtil.modelStopper("check")) break;
		}
	}

	private void endSimulation() {
		if (scalingYears.contains(Properties.get().main.endYear)) {
            SummarizeData.scaleMicroDataToExogenousForecast(Properties.get().main.endYear, dataContainer);
        }

		dataContainer.getHouseholdData().summarizeHouseholdsNearMetroStations(modelContainer);

		if (Properties.get().main.endYear != 2040) {
			SummarizeData.writeOutSyntheticPopulation(Properties.get().main.endYear, dataContainer);
			dataContainer.getGeoData().writeOutDevelopmentCapacityFile(dataContainer);
		}

		SiloUtil.summarizeMicroData(Properties.get().main.endYear, modelContainer, dataContainer);
		SiloUtil.finish(modelContainer);
		SiloUtil.modelStopper("removeFile");
		if (trackTime) {
		    SiloUtil.writeOutTimeTracker(timeCounter);
        }
		LOGGER.info("Scenario results can be found in the directory scenOutput/" + Properties.get().main.scenarioName + ".");
	}

	//*************************************************************************
	//*** Special implementation of same SILO Model for integration with    ***
	//*** CBLCM Framework (http://csdms.colorado.edu/wiki/Main_Page). Used  ***
	//*** in Maryland implementation only. Functions:                       ***
	//*** initialize(), runYear (double dt) and finishModel()               ***
	//*************************************************************************

	@Deprecated // use SiloModelCBLCM directly
	private SiloModelCBLCM cblcm ;
	@Deprecated // use SiloModelCBLCM directly
	public void initialize() {
		cblcm = new SiloModelCBLCM() ;
		cblcm.initialize();
	}
	@Deprecated // use SiloModelCBLCM directly
	public void runYear (double dt) {
		cblcm.runYear(dt);
	}
	@Deprecated // use SiloModelCBLCM directly
	public void finishModel() {
		cblcm.finishModel();
	}
}