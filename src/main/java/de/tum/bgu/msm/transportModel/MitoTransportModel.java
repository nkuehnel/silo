package de.tum.bgu.msm.transportModel;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.input.InputFeed;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.resources.Implementation;
import de.tum.bgu.msm.transportModel.mitoMatsim.MitoMatsimTravelTimes;
import de.tum.bgu.msm.transportModel.mitoMatsim.TrafficAssignmentModel;
import org.apache.log4j.Logger;

import de.tum.bgu.msm.MitoModel;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.util.TravelTime;

/**
 * Implementation of Transport Model Interface for MITO
 * @author Rolf Moeckel
 * Created on February 18, 2017 in Munich, Germany
 */
public class MitoTransportModel implements TransportModelI {
    private static final Logger logger = Logger.getLogger( MitoTransportModel.class );
    private MitoModel mito;
	private final SiloModelContainer modelContainer;
    private TrafficAssignmentModel trafficAssignmentModel;
	private final GeoData geoData;
	private boolean runMatsimAfterMito;



	public MitoTransportModel(ResourceBundle rb, String baseDirectory, GeoData geoData, SiloModelContainer modelContainer) {
		this.mito = new MitoModel(rb, Implementation.MUNICH);
		this.geoData = geoData;
		this.modelContainer = modelContainer;
		mito.setRandomNumberGenerator(SiloUtil.getRandomObject());
		setBaseDirectory(baseDirectory);
		runMatsimAfterMito = Properties.get().transportModel.runMatsimAfterMito;

		if (runMatsimAfterMito) {
			trafficAssignmentModel = new TrafficAssignmentModel(modelContainer);
			trafficAssignmentModel.setup(Properties.get().transportModel.matsimScaleFactor,
					Properties.get().transportModel.matsimIterations,
					Properties.get().transportModel.matsimThreads);
		}
    }

    @Override
    public void runTransportModel(int year) {
    	MitoModel.setScenarioName (Properties.get().main.scenarioName);
    	updateData(year);
		logger.info("  Update MITO data for the year " + year);
    	//MitoModel.setScenarioName (SiloUtil.scenarioName);
    	updateData(year);
    	logger.info("  Running travel demand model MITO for the year " + year);
    	//mito.runModel();
		logger.info("  Running traffic assignment for the year " + year);

		if (runMatsimAfterMito) {
			trafficAssignmentModel.runTrafficAssignment();
		}

        logger.info("travel times by car updated to year " + year);

    }

    private void updateData(int year) {
    	Map<Integer, Zone> zones = new HashMap<>();
		for (int i = 0; i < geoData.getZones().length; i++) {
			AreaType areaType = AreaType.RURAL; //TODO: put real area type in here
			Zone zone = new Zone(geoData.getZones()[i], geoData.getSizeOfZonesInAcres()[i], areaType);
			zones.put(zone.getZoneId(), zone);
		}
		JobDataManager.fillMitoZoneEmployees(zones);

		Map<Integer, MitoHousehold> households = Household.convertHhs(zones);
		for(Person person: Person.getPersons()) {
			int hhId = person.getHhId();
			if(households.containsKey(hhId)) {
				MitoPerson mitoPerson = person.convertToMitoPp();
				households.get(hhId).addPerson(mitoPerson);
			} else {
				logger.warn("Person " + person.getId() + " refers to non-existing household " + hhId
						+ " and will thus NOT be considered in the transport model.");
			}
		}

        Map<String, TravelTimes> travelTimes = modelContainer.getAcc().getTravelTimes();
        logger.info("  SILO data being sent to MITO");
        //InputFeed feed = new InputFeed(zones, travelTimes, households);
        //mito.feedData(feed);


		if (runMatsimAfterMito) {
			logger.info("  MITO and SILO data being sent to MATSim");
			trafficAssignmentModel.feedDataToMatsim(zones, households, modelContainer.getAcc(), year);
		}
    }

    private void setBaseDirectory (String baseDirectory) {
        mito.setBaseDirectory(baseDirectory);
    }
}