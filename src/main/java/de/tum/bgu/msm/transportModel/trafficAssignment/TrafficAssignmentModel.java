package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoTrip;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.ArrayList;
import java.util.Map;

public class TrafficAssignmentModel {

    private TrafficAssignmentUtil trafficAssignmentUtil;

    private Config matsimConfig;
    private MutableScenario matsimScenario;
    private Population matsimPopulation;

    private String networkFile = "C:/models/siloMitoMatsim/input/studyNetworkLight.xml";

    private int[] zones;
    private Matrix autoTravelTimes;
    private Matrix transitTravelTimes;
    private Map<Integer, MitoHousehold> mitoHouseholds;

    private double scalingFactor;
    private int numberOfIterations;
    private int numberOfThreads;

    private PopulationFromMito populationFromMito;

    public TrafficAssignmentModel(double scalingFactor, int numberOfIterations, int numberOfThreads) {


        trafficAssignmentUtil = new TrafficAssignmentUtil();
        trafficAssignmentUtil.readCoordinateData();

        this.numberOfIterations = numberOfIterations;
        this.scalingFactor = scalingFactor;
        this.numberOfThreads = numberOfThreads;

        populationFromMito = new PopulationFromMito(scalingFactor,trafficAssignmentUtil);

    }

    public void setup(){
        //create config parameters for all the simulation years
        configMatsim(numberOfIterations, numberOfThreads);


    }

    public void load(int year){
        //configure specific parameters for the simulated years
        matsimConfig.network().setInputFile(networkFile);

        matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(matsimConfig);

        matsimConfig.controler().setOutputDirectory("C:/models/siloMitoMatsim/output/" + year);
        matsimConfig.controler().setRunId("trafficAssignment" + year);


        matsimPopulation = populationFromMito.createPopulationFromMito(mitoHouseholds,  autoTravelTimes, transitTravelTimes, zones, year);

        matsimScenario.setPopulation(matsimPopulation);




    }

    public Matrix runTrafficAssignmentToGetTravelTimeMatrix(){
        //run matsim for the selected year

        final Controler controler = new Controler(matsimScenario);

        Zone2ZoneTravelTime zone2ZoneTravelTime = new Zone2ZoneTravelTime(autoTravelTimes, controler, matsimScenario.getNetwork(),
                numberOfIterations, zones, 8*60*60, 1 , trafficAssignmentUtil );

        controler.addControlerListener(zone2ZoneTravelTime);
        controler.run();

        return zone2ZoneTravelTime.getAutoTravelTime();

    }


    public void feedDataToMatsim(int[] zones, Matrix autoTravelTimes, Matrix transitTravelTimes, Map<Integer, MitoHousehold> mitoHouseholds){

        this.zones = zones;
        this.mitoHouseholds = mitoHouseholds;
        this.autoTravelTimes = autoTravelTimes;
        this.transitTravelTimes = transitTravelTimes;

    }


    public void configMatsim(int numberOfIterations, int numberOfThreads){



        matsimConfig = ConfigUtils.createConfig();
        matsimConfig.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        matsimConfig.qsim().setFlowCapFactor(scalingFactor);
        matsimConfig.qsim().setStorageCapFactor(Math.pow(scalingFactor,0.75));
        matsimConfig.qsim().setRemoveStuckVehicles(false);

        matsimConfig.qsim().setStartTime(0);
        matsimConfig.qsim().setEndTime(24*60*60);

        matsimConfig.controler().setFirstIteration(1);
        matsimConfig.controler().setLastIteration(numberOfIterations);
        matsimConfig.controler().setWritePlansInterval(numberOfIterations);
        matsimConfig.controler().setWriteEventsInterval(numberOfIterations);
        matsimConfig.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);
        matsimConfig.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        matsimConfig.controler().setMobsim("qsim");

        matsimConfig.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        matsimConfig.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        //Strategy
        StrategyConfigGroup.StrategySettings strategySettings1 = new StrategyConfigGroup.StrategySettings();
        strategySettings1.setStrategyName("ChangeExpBeta");
        strategySettings1.setWeight(0.5); //originally 0.8
        matsimConfig.strategy().addStrategySettings(strategySettings1);

        StrategyConfigGroup.StrategySettings strategySettings2 = new StrategyConfigGroup.StrategySettings();
        strategySettings2.setStrategyName("ReRoute");
        strategySettings2.setWeight(1);//originally 0.2
        strategySettings2.setDisableAfter((int) (numberOfIterations * 0.7));
        matsimConfig.strategy().addStrategySettings(strategySettings2);

        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(1); //originally 0
        strategySettings3.setDisableAfter((int) (numberOfIterations * 0.7));
        matsimConfig.strategy().addStrategySettings(strategySettings3);

        //TODO this strategy is implemented to test the pt modes (in general do not include)
//        StrategyConfigGroup.StrategySettings strategySettings4 = new StrategyConfigGroup.StrategySettings();
//        strategySettings4.setStrategyName("ChangeTripMode");
//        strategySettings4.setWeight(0); //originally 0
//        strategySettings4.setDisableAfter((int) (numberOfIterations * 0.7));
//        config.strategy().addStrategySettings(strategySettings4);


        matsimConfig.strategy().setMaxAgentPlanMemorySize(4);

        // Plan Scoring (planCalcScore)
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        matsimConfig.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        matsimConfig.planCalcScore().addActivityParams(workActivity);


        matsimConfig.qsim().setNumberOfThreads(numberOfThreads);
        matsimConfig.global().setNumberOfThreads(numberOfThreads);
        matsimConfig.parallelEventHandling().setNumberOfThreads(numberOfThreads);
        matsimConfig.qsim().setUsingThreadpool(false);


        matsimConfig.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        // Scenario //chose between population file and population creator in java


    }

}
