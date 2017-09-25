package de.tum.bgu.msm.transportModel;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.MitoModel;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.io.input.InputFeed;
import de.tum.bgu.msm.transportModel.trafficAssignment.TrafficAssignmentModel;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * Implementation of Transport Model Interface for MITO
 * @author Rolf Moeckel
 * Created on February 18, 2017 in Munich, Germany
 *
 */

public class MitoTransportModel implements TransportModelI {
    private static final Logger logger = Logger.getLogger( MitoTransportModel.class );
    private MitoModel mito;
    private TrafficAssignmentModel trafficAssignmentModel;
    private Matrix outputAutoTravelTime;


    public MitoTransportModel(ResourceBundle rb, String baseDirectory) {
        this.mito = new MitoModel(rb);
        mito.setRandomNumberGenerator(SiloUtil.getRandomObject());
        setBaseDirectory(baseDirectory);

        //trafficAssignmentModel = new TrafficAssignmentModel();
        //trafficAssignmentModel.setup(0.5, 10, 16);

    }


    public void feedData(int[] zones, Matrix autoTravelTimes, Matrix transitTravelTimes, Map<Integer, MitoHousehold> mitoHouseholds,
                         int[] retailEmplByZone, int[] officeEmplByZone, int[] otherEmplByZone, int[] totalEmplByZone,
                         float[] sizeOfZonesInAcre) {
        logger.info("  SILO data being sent to MITO");
        InputFeed feed = new InputFeed(zones, autoTravelTimes, transitTravelTimes, mitoHouseholds, retailEmplByZone,
                officeEmplByZone, otherEmplByZone, totalEmplByZone, sizeOfZonesInAcre);
        mito.feedData(feed);
        //check whether feed data is done every run year or only once
        //trafficAssignmentModel.feedDataToMatsim(zones, autoTravelTimes, transitTravelTimes, mitoHouseholds);

    }


    public void setScenarioName (String scenarioName) {
        mito.setScenarioName (scenarioName);
    }


    private void setBaseDirectory (String baseDirectory) {
        mito.setBaseDirectory(baseDirectory);
    }


    @Override
    public void runTransportModel(int year) {

        logger.info("  Running travel demand model MITO for the year " + year);
        mito.runModel();

        //logger.info("  Running traffic assignment for the year " + year);
        //trafficAssignmentModel.load(year);
        //outputAutoTravelTime = trafficAssignmentModel.runTrafficAssignmentToGetTravelTimeMatrix();

    }

    @Override
    public void writeOutSocioEconomicDataForMstm(int year) {
        // not doing anything.
    }
    @Override
    public void tripGeneration() {
        // not doing anything.
    }

}
