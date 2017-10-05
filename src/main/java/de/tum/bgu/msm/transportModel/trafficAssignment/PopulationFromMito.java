package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoPerson;
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
    private Matrix autoTravelTime;
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

    public Population createPopulationFromMito(Map<Integer, MitoHousehold> households, Matrix autoTravelTime, Matrix transitTravelTime, int[] zones, int year){

        this.autoTravelTime = autoTravelTime;

        Config matsimConfig = ConfigUtils.createConfig();
        Scenario matsimScenario = ScenarioUtils.createScenario(matsimConfig);

        Network matsimNetwork = matsimScenario.getNetwork();

        Population matsimPopulation = matsimScenario.getPopulation();
        matsimPopulationFactory = matsimPopulation.getFactory();

        for (MitoHousehold mitoHousehold : households.values()){
            //with master at 22.9
            for (MitoPerson mitoPerson : mitoHousehold.getPersons().values()){

                //start with HBW trips
                int homeZone = mitoHousehold.getHomeZone().getZoneId();
                int workZone = zones[SiloUtil.select(zones.length)];

                int mode = tempModeChoice.selectMode(homeZone, workZone);

                if (SiloUtil.getRandomNumberAsDouble() < scalingFactor && mode == 0){

                    org.matsim.api.core.v01.population.Person matsimPerson = matsimPopulationFactory.createPerson(Id.create(mitoPerson.getId(), org.matsim.api.core.v01.population.Person.class));
                    matsimPlan = matsimPopulationFactory.createPlan();
                    addTripsToWork(homeZone, workZone);
                    //logger.info("person: " + mitoPerson.getId() + " home at " + homeZone + " work at " + workZone);
                    matsimPerson.addPlan(matsimPlan);
                    matsimPopulation.addPerson(matsimPerson);

                }
            }
            //with tripDist at 22.9
           /* for (MitoPerson mitoPerson : mitoHousehold.getPersons().values()){

                //start with HBW trips
                int homeZone = mitoHousehold.getHomeZone().getZoneId();
                int workZone = zones[SiloUtil.select(zones.length)];

                int mode = tempModeChoice.selectMode(homeZone, workZone);

                if (SiloUtil.getRandomNumberAsDouble() < scalingFactor && mode == 0){

                    org.matsim.api.core.v01.population.Person matsimPerson = matsimPopulationFactory.createPerson(Id.create(mitoPerson.getId(), org.matsim.api.core.v01.population.Person.class));
                    matsimPlan = matsimPopulationFactory.createPlan();
                    addTripsToWork(homeZone, workZone);
                    //logger.info("person: " + mitoPerson.getId() + " home at " + homeZone + " work at " + workZone);
                    matsimPerson.addPlan(matsimPlan);
                    matsimPopulation.addPerson(matsimPerson);

                }
            }*/
        }

        boolean writePopulation = true;
        if (writePopulation){
            new File(trafficAssignmentDirectory + "output/").mkdir();
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write(trafficAssignmentDirectory + "output/population" + year  + ".xml");

        }

        return matsimPopulation;
    }


    public void addTripsToWork(int homeZone, int workZone){

        double time = 0;

        time = tempTimeOfDay.selectDepartureTimeToWork();

        Coord homeCoord = trafficAssignmentUtil.getZoneCoordinates(homeZone);
        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        activity1.setEndTime(time);
        matsimPlan.addActivity(activity1);
        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

        time += tempTimeOfDay.selectWorkDuration() + autoTravelTime.getValueAt(homeZone, workZone);

        Coord workCoord = trafficAssignmentUtil.getZoneCoordinates(workZone);
        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoord);
        activity2.setEndTime(time);
        matsimPlan.addActivity(activity2);
        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

        Activity activity100 = matsimPopulationFactory.createActivityFromCoord("home", homeCoord);
        matsimPlan.addActivity(activity100);

    }


}
