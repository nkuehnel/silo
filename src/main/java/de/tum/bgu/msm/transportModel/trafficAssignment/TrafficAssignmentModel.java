package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.data.MitoHousehold;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Map;
import java.util.ResourceBundle;

public class TrafficAssignmentModel {
    private ResourceBundle rb;
    private String trafficAssignmentDirectory;
    private TrafficAssignmentUtil trafficAssignmentUtil;
    private Config matsimConfig;
    private MutableScenario matsimScenario;
    private Population matsimPopulation;
    private int[] zones;
    private Matrix autoTravelTimes;
    private Matrix transitTravelTimes;
    private Map<Integer, MitoHousehold> mitoHouseholds;
    private double scalingFactor;
    private int numberOfIterations;
    private int numberOfThreads;

    private PopulationFromMito populationFromMito;

    public TrafficAssignmentModel(ResourceBundle rb) {
        this.rb = rb;

        trafficAssignmentUtil = new TrafficAssignmentUtil(rb);
        populationFromMito = new PopulationFromMito(rb);
    }

    public void setup(double scalingFactor, int numberOfIterations, int numberOfThreads){

        trafficAssignmentDirectory = rb.getString("matsim.directory");
        //create common configuration parameters for every year
        trafficAssignmentUtil.setup(trafficAssignmentDirectory);
        trafficAssignmentUtil.readCoordinateData();
        this.numberOfIterations = numberOfIterations;
        this.scalingFactor = scalingFactor;
        this.numberOfThreads = numberOfThreads;
        populationFromMito.setup(scalingFactor, trafficAssignmentUtil, trafficAssignmentDirectory);


        configMatsim(numberOfIterations, numberOfThreads);
    }

    public void feedDataToMatsim(int[] zones, Matrix autoTravelTimes, Matrix transitTravelTimes, Map<Integer, MitoHousehold> mitoHouseholds){
        //feeds data from silo matsim, it is year-specific - I suggest to combine with the next method load
        this.zones = zones;
        this.mitoHouseholds = mitoHouseholds;
        this.autoTravelTimes = autoTravelTimes;
        this.transitTravelTimes = transitTravelTimes;
    }

    public void load(int year){
        //configure year-specific parameters
        //todo only 2 networks before and after certain year - 2030 in the test
        String networkFile;
        if (year < 2030) {
            networkFile = trafficAssignmentDirectory + rb.getString("matsim.network.2000");
        } else {
            networkFile = trafficAssignmentDirectory + rb.getString("matsim.network.2030");
        }

        matsimConfig.network().setInputFile(networkFile);

        //creates a matsim scenario and the population from mito households/persons/trips
        matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(matsimConfig);
        matsimConfig.controler().setOutputDirectory(trafficAssignmentDirectory + "output/" + year);
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





    public void configMatsim(int numberOfIterations, int numberOfThreads){



        matsimConfig = ConfigUtils.createConfig();
        matsimConfig.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        matsimConfig.qsim().setFlowCapFactor(scalingFactor);
        matsimConfig.qsim().setStorageCapFactor(Math.pow(scalingFactor,0.75));
        matsimConfig.qsim().setRemoveStuckVehicles(false);

        matsimConfig.qsim().setStartTime(0);
        matsimConfig.qsim().setEndTime(12*60*60);

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
