package de.tum.bgu.msm.transportModel.mitoMatsim;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Accessibility;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoPerson;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.Map;
import java.util.ResourceBundle;

public class PopulationFromMito {
    private ResourceBundle rb;
    static Logger logger = Logger.getLogger(PopulationFromMito.class);
    private String trafficAssignmentDirectory;
    private PopulationFactory matsimPopulationFactory;
    private TrafficAssignmentUtil trafficAssignmentUtil;
    private Plan matsimPlan;
    private double scalingFactor;

    private TempModeChoice tempModeChoice;
    private TempTimeOfDay tempTimeOfDay;


    public PopulationFromMito(ResourceBundle rb) {
        this.rb=rb;
        tempModeChoice = new TempModeChoice(rb);
        tempTimeOfDay = new TempTimeOfDay(rb);

    }

    public void setup(double scalingFactor,  TrafficAssignmentUtil trafficAssignmentUtil, String trafficAssignmentDirectory){

        this.trafficAssignmentDirectory = trafficAssignmentDirectory;
        this.scalingFactor = scalingFactor;
        this.trafficAssignmentUtil = trafficAssignmentUtil;
        tempModeChoice.setup(trafficAssignmentDirectory);
        tempTimeOfDay.setup(trafficAssignmentDirectory);
    }

    public Population createPopulationFromMito(Map<Integer, MitoHousehold> households, Accessibility acc, Map<Integer, Zone> zones, int year){

        logger.info("starting conversion of MITO households to MATSim hbw trips");

        Config matsimConfig = ConfigUtils.createConfig();
        Scenario matsimScenario = ScenarioUtils.createScenario(matsimConfig);

        Network matsimNetwork = matsimScenario.getNetwork();

        Population matsimPopulation = matsimScenario.getPopulation();
        matsimPopulationFactory = matsimPopulation.getFactory();

        for (MitoHousehold mitoHousehold : households.values()){
            //with master at 22.9
            for (MitoPerson mitoPerson : mitoHousehold.getPersons().values()){

                if (mitoPerson.getWorkplace() > 0 ) {
                    //start with HBW trips
                    int homeZone = mitoHousehold.getHomeZone().getZoneId();
                    int workZone = mitoPerson.getWorkplace();


                    int mode = tempModeChoice.selectMode(homeZone, workZone);

                    if (SiloUtil.getRandomNumberAsDouble() < scalingFactor && mode == 0) {

                        org.matsim.api.core.v01.population.Person matsimPerson = matsimPopulationFactory.createPerson(Id.create(mitoPerson.getId(), org.matsim.api.core.v01.population.Person.class));
                        matsimPlan = matsimPopulationFactory.createPlan();
                        addTripsToWork(homeZone, workZone, acc);
                        //logger.info("person: " + mitoPerson.getId() + " home at " + homeZone + " work at " + workZone);
                        matsimPerson.addPlan(matsimPlan);
                        matsimPopulation.addPerson(matsimPerson);

                    }
                }
            }
        }

        logger.info("MATSim population already created");
        boolean writePopulation = true;
        if (writePopulation){
            new File(trafficAssignmentDirectory + "output/").mkdir();
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write(trafficAssignmentDirectory + "output/population" + year  + ".xml");

        }

        return matsimPopulation;
    }


    public void addTripsToWork(int homeZone, int workZone, Accessibility acc){

        double time;

        time = tempTimeOfDay.selectDepartureTimeToWork();

        Coord homeCoord = trafficAssignmentUtil.getRandomZoneCoordinates(homeZone);
        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        activity1.setEndTime(time);
        matsimPlan.addActivity(activity1);
        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

        time += tempTimeOfDay.selectWorkDuration() + acc.getAutoTravelTime(homeZone, workZone);

        Coord workCoord = trafficAssignmentUtil.getRandomZoneCoordinates(workZone);
        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoord);
        activity2.setEndTime(time);
        matsimPlan.addActivity(activity2);
        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

        Activity activity100 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        matsimPlan.addActivity(activity100);

    }


}
