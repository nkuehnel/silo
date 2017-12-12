package de.tum.bgu.msm.transportModel.mitoMatsim;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloModel;
import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.data.Accessibility;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.properties.Properties;
import org.matsim.api.core.v01.TransportMode;
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
import java.util.Set;
import java.util.TreeSet;

public class TrafficAssignmentModel {

    private String trafficAssignmentDirectory;
    private TrafficAssignmentUtil trafficAssignmentUtil;
    private Config matsimConfig;
    private MutableScenario matsimScenario;
    private Map<Integer, Zone> zones;
    private double scalingFactor;
    private SiloModelContainer modelContainer;


    private PopulationFromMito populationFromMito;

    public TrafficAssignmentModel(SiloModelContainer modelContainer) {

        this.modelContainer = modelContainer;

        trafficAssignmentUtil = new TrafficAssignmentUtil();
        populationFromMito = new PopulationFromMito();
    }

    public void setup(double scalingFactor, int numberOfIterations, int numberOfThreads){
        trafficAssignmentDirectory = Properties.get().transportModel.matsimDirectory;
        //create common configuration parameters for every year
        trafficAssignmentUtil.setup(trafficAssignmentDirectory);
        trafficAssignmentUtil.readCoordinateData();
        this.scalingFactor = scalingFactor;
        populationFromMito.setup(scalingFactor, trafficAssignmentUtil, trafficAssignmentDirectory);
        configMatsim(numberOfIterations, numberOfThreads);
    }

    public void feedDataToMatsim(Map<Integer, Zone> zones, Map<Integer, MitoHousehold> mitoHouseholds, Accessibility acc, int year){
        String networkFile = trafficAssignmentDirectory + Properties.get().transportModel.matsimNetworkFile;
        matsimConfig.network().setInputFile(networkFile);
        matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(matsimConfig);
        matsimConfig.controler().setOutputDirectory(trafficAssignmentDirectory + "output/" + year);
        matsimConfig.controler().setRunId("mitoMatsim" + year);
        Population matsimPopulation = populationFromMito.createPopulationFromMito(mitoHouseholds, acc, zones, year);
        matsimScenario.setPopulation(matsimPopulation);
        this.zones = zones;

    }

    public void runTrafficAssignment(){
        //run matsim for the selected year
        final Controler controler = new Controler(matsimScenario);
        controler.run();
        //get travel times object
        TravelTime travelTime = controler.getLinkTravelTimes();
        TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
        LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);
        MitoMatsimTravelTimes travelTimes = new  MitoMatsimTravelTimes(leastCoastPathTree, matsimScenario.getNetwork(), trafficAssignmentUtil, zones);
        modelContainer.getAcc().addTravelTimeForMode(TransportMode.car, travelTimes);
    }



    public void configMatsim(int numberOfIterations, int numberOfThreads){

        matsimConfig = ConfigUtils.createConfig();
        matsimConfig.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        matsimConfig.qsim().setFlowCapFactor(scalingFactor);
        matsimConfig.qsim().setStorageCapFactor(Math.pow(scalingFactor,0.75));
        matsimConfig.qsim().setRemoveStuckVehicles(false);

        matsimConfig.qsim().setStartTime(0);
        matsimConfig.qsim().setEndTime(24*60*60);

        matsimConfig.transit().setUseTransit(Properties.get().transportModel.runTransitInMatsim);
        Set<String> transitModes = new TreeSet<>();
        transitModes.add("pt");
        matsimConfig.transit().setTransitModes(transitModes);

        matsimConfig.transit().setTransitScheduleFile(trafficAssignmentDirectory + Properties.get().transportModel.matsimScheduleFile);
        matsimConfig.transit().setVehiclesFile(trafficAssignmentDirectory + Properties.get().transportModel.matsimVehicleFile);



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
        strategySettings1.setWeight(0.8); //originally 0.8
        matsimConfig.strategy().addStrategySettings(strategySettings1);

        StrategyConfigGroup.StrategySettings strategySettings2 = new StrategyConfigGroup.StrategySettings();
        strategySettings2.setStrategyName("ReRoute");
        strategySettings2.setWeight(0.2);//originally 0.2
        strategySettings2.setDisableAfter((int) (numberOfIterations * 0.7));
        matsimConfig.strategy().addStrategySettings(strategySettings2);

        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(0.1); //originally 0
        strategySettings3.setDisableAfter((int) (numberOfIterations * 0.7));
        matsimConfig.strategy().addStrategySettings(strategySettings3);

        //mode choice internal to matsim
        if (Properties.get().transportModel.runTransitInMatsim) {
            StrategyConfigGroup.StrategySettings strategySettings4 = new StrategyConfigGroup.StrategySettings();
            strategySettings4.setStrategyName("ChangeTripMode");
            strategySettings4.setWeight(0.2); //originally 0
            //strategySettings4.setDisableAfter((int) (numberOfIterations * 0.7));
            matsimConfig.strategy().addStrategySettings(strategySettings4);
        }

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
