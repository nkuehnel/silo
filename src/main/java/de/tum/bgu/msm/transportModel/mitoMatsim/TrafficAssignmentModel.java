package de.tum.bgu.msm.transportModel.mitoMatsim;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import java.util.Map;
import java.util.ResourceBundle;

public class TrafficAssignmentModel {
    private ResourceBundle rb;
    private String trafficAssignmentDirectory;
    private TrafficAssignmentUtil trafficAssignmentUtil;
    private Config matsimConfig;
    private MutableScenario matsimScenario;
    private Population matsimPopulation;
    private Map<Integer, Zone> zones;
    private TravelTimes autoTravelTimes;
    private Map<Integer, MitoHousehold> mitoHouseholds;
    private double scalingFactor;


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
        this.scalingFactor = scalingFactor;
        populationFromMito.setup(scalingFactor, trafficAssignmentUtil, trafficAssignmentDirectory);
        configMatsim(numberOfIterations, numberOfThreads);
    }

    public void feedDataToMatsim(Map<Integer, Zone> zones, Map<Integer, MitoHousehold> mitoHouseholds, TravelTimes travelTimes){
        //feeds data from silo matsim, it is year-specific - I suggest to combine with the next method load
        this.zones = zones;
        this.mitoHouseholds = mitoHouseholds;
        this.autoTravelTimes = travelTimes;


    }

    public void load(int year){
        //configure year-specific parameters

        String networkFile = trafficAssignmentDirectory + rb.getString("matsim.network");
        matsimConfig.network().setInputFile(networkFile);

        //creates a matsim scenario and the population from mito households/persons/trips
        matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(matsimConfig);
        matsimConfig.controler().setOutputDirectory(trafficAssignmentDirectory + "output/" + year);
        matsimConfig.controler().setRunId("mitoMatsim" + year);
        matsimPopulation = populationFromMito.createPopulationFromMito(mitoHouseholds,  autoTravelTimes, zones, year);
        matsimScenario.setPopulation(matsimPopulation);

    }

    public TravelTimes runTrafficAssignment(){
        //run matsim for the selected year
        final Controler controler = new Controler(matsimScenario);
        controler.run();
        //get travel times object
        TravelTime travelTime = controler.getLinkTravelTimes();
        TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
        LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);
        return new MitoMatsimTravelTimes(leastCoastPathTree, matsimScenario.getNetwork(), trafficAssignmentUtil, zones);
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

    }

}
