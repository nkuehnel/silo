package de.tum.bgu.msm.transportModel;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.container.SiloModelContainer;
import de.tum.bgu.msm.data.*;
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



	public MitoTransportModel(ResourceBundle siloRb, ResourceBundle mitoRb, String baseDirectory, GeoData geoData, SiloModelContainer modelContainer) {
		this.mito = new MitoModel(mitoRb);
		this.geoData = geoData;
		this.modelContainer = modelContainer;
		mito.setRandomNumberGenerator(SiloUtil.getRandomObject());
		setBaseDirectory(baseDirectory);

        trafficAssignmentModel = new TrafficAssignmentModel(siloRb, modelContainer);
        trafficAssignmentModel.setup(ResourceUtil.getDoubleProperty(siloRb, "matsim.scaling.factor"),
                ResourceUtil.getIntegerProperty(siloRb, "matsim.iterations"),
                ResourceUtil.getIntegerProperty(siloRb, "matsim.threads"));

    }

    @Override
    public void runTransportModel(int year) {
		logger.info("  Update MITO data for the year " + year);
    	//MitoModel.setScenarioName (SiloUtil.scenarioName);
    	updateData(year);
    	logger.info("  Running travel demand model MITO for the year " + year);
    	//mito.runModel();
		logger.info("  Running traffic assignment for the year " + year);


        trafficAssignmentModel.runTrafficAssignment();

        logger.info("travel times by car updated to year " + year);

    }

    private void updateData(int year) {
    	Map<Integer, Zone> zones = new HashMap<>();
		for (int i = 0; i < geoData.getZones().length; i++) {
			AreaType areaType = AreaType.RURAL; //TODO: put real area type in here
			Zone zone = new Zone(geoData.getZones()[i], geoData.getSizeOfZonesInAcres()[i], areaType);
			//comment because it takes too long
			//zone.setRetailEmpl(summarizeData.getRetailEmploymentByZone(geoData)[i]);
//			zone.setOfficeEmpl(summarizeData.getOfficeEmploymentByZone(geoData)[i]);
//			zone.setOtherEmpl(summarizeData.getOtherEmploymentByZone(geoData)[i]);
//			zone.setTotalEmpl(summarizeData.getTotalEmploymentByZone(geoData)[i]);
			zones.put(zone.getZoneId(), zone);
		}

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

        logger.info("  SILO data being sent to MITO");
        //InputFeed feed = new InputFeed(zones, travelTimes, households);
        //mito.feedData(feed);
		logger.info("  MITO and SILO data being sent to MATSim");
        trafficAssignmentModel.feedDataToMatsim(zones, households, modelContainer.getAcc(), year);

    }

    private void setBaseDirectory (String baseDirectory) {
        mito.setBaseDirectory(baseDirectory);
    }









}