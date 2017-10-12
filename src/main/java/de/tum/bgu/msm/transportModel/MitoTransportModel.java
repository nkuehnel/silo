package de.tum.bgu.msm.transportModel;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.MitoModel;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Accessibility;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.Zone;
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


    public MitoTransportModel(ResourceBundle mitoRb, ResourceBundle siloRb, String baseDirectory) {
        this.mito = new MitoModel(mitoRb);
        mito.setRandomNumberGenerator(SiloUtil.getRandomObject());
        setBaseDirectory(baseDirectory);

        trafficAssignmentModel = new TrafficAssignmentModel(siloRb);
        trafficAssignmentModel.setup(ResourceUtil.getDoubleProperty(siloRb, "matsim.scaling.factor"),
                ResourceUtil.getIntegerProperty(siloRb, "matsim.iterations"),
                ResourceUtil.getIntegerProperty(siloRb, "matsim.threads"));

    }

    @Override
    public void feedData(Map<Integer, Zone> zones, Matrix hwySkim, Matrix transitSkim, Map<Integer, MitoHousehold> households) {
        logger.info("  SILO data being sent to MITO");
        InputFeed feed = new InputFeed(zones, hwySkim, transitSkim, households);
        mito.feedData(feed);
        //check whether feed data is done every run year or only once
       trafficAssignmentModel.feedDataToMatsim(zones, hwySkim, transitSkim, households);

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
       // Matrix outputAutoTravelTime = trafficAssignmentModel.runTrafficAssignmentToGetTravelTimeMatrix();
       // Accessibility.updateHwySkim(outputAutoTravelTime);

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
