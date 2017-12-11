package de.tum.bgu.msm.transportModel.mitoMatsim;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Accessibility;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoPerson;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.properties.Properties;
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
    private boolean useTransit;

    private TempModeChoice tempModeChoice;
    private TempTimeOfDay tempTimeOfDay;


    public PopulationFromMito() {

        tempModeChoice = new TempModeChoice();
        tempTimeOfDay = new TempTimeOfDay();
        useTransit = Properties.get().transportModel.runMatsimAfterMito;

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

        int numberOfTripsByCar = 0;
        int numberOfTripsByTransit = 0;


        for (MitoHousehold mitoHousehold : households.values()){
            for (MitoPerson mitoPerson : mitoHousehold.getPersons().values()){

                if (mitoPerson.getWorkplace() > 0 ) {
                    //HBW trips
                    int homeZone = mitoHousehold.getHomeZone().getZoneId();
                    int workZone = mitoPerson.getWorkplace();


                    int mode = tempModeChoice.selectMode(homeZone, workZone);

                    if (SiloUtil.getRandomNumberAsDouble() < scalingFactor) {

                        if (mode ==0) {

                            org.matsim.api.core.v01.population.Person matsimPerson = matsimPopulationFactory.createPerson(Id.create(mitoPerson.getId(), org.matsim.api.core.v01.population.Person.class));
                            matsimPlan = matsimPopulationFactory.createPlan();
                            addTripsToWork(homeZone, workZone, acc, TransportMode.car);
                            //logger.info("person: " + mitoPerson.getId() + " home at " + homeZone + " work at " + workZone);
                            matsimPerson.addPlan(matsimPlan);
                            matsimPopulation.addPerson(matsimPerson);
                            numberOfTripsByCar++;

                        } else if (mode == 3 && useTransit){
                            org.matsim.api.core.v01.population.Person matsimPerson = matsimPopulationFactory.createPerson(Id.create(mitoPerson.getId(), org.matsim.api.core.v01.population.Person.class));
                            matsimPlan = matsimPopulationFactory.createPlan();
                            addTripsToWork(homeZone, workZone, acc, TransportMode.pt);
                            //logger.info("person: " + mitoPerson.getId() + " home at " + homeZone + " work at " + workZone);
                            matsimPerson.addPlan(matsimPlan);
                            matsimPopulation.addPerson(matsimPerson);
                            numberOfTripsByTransit++;
                        }

                    }
                }
            }
        }

        logger.info("Number of trips by car: " + numberOfTripsByCar);
        logger.info("Number of trips by transit: " + numberOfTripsByTransit);


        logger.info("MATSim population already created");
        boolean writePopulation = true;
        if (writePopulation){
            new File(trafficAssignmentDirectory + "output/").mkdir();
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write(trafficAssignmentDirectory + "output/population" + year  + ".xml");

        }

        return matsimPopulation;
    }


    public void addTripsToWork(int homeZone, int workZone, Accessibility acc, String mode){

        double time;

        time = tempTimeOfDay.selectDepartureTimeToWork();
        Coord homeCoord = trafficAssignmentUtil.getRandomZoneCoordinates(homeZone);
        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        activity1.setEndTime(time);
        matsimPlan.addActivity(activity1);

        matsimPlan.addLeg(matsimPopulationFactory.createLeg(mode));
        time += tempTimeOfDay.selectWorkDuration() + acc.getAutoTravelTime(homeZone, workZone);

        Coord workCoord = trafficAssignmentUtil.getRandomZoneCoordinates(workZone);
        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoord);
        activity2.setEndTime(time);
        matsimPlan.addActivity(activity2);

        matsimPlan.addLeg(matsimPopulationFactory.createLeg(mode));

        Activity activity100 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        matsimPlan.addActivity(activity100);

    }





}
