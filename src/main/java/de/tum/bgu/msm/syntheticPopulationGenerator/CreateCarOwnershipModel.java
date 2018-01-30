package de.tum.bgu.msm.syntheticPopulationGenerator;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.data.munich.GeoDataMuc;
import de.tum.bgu.msm.properties.Properties;
import omx.OmxFile;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Implements car ownership of initial synthetic population (base year) for the Munich Metropolitan Area
 *
 * @author Matthew Okrah
 *         Created on 28/04/2017 in Munich, Germany.
 */

public class CreateCarOwnershipModel {

    static Logger logger = Logger.getLogger(CreateCarOwnershipModel.class);

    //private TableDataSet zonalData;
    private final CreateCarOwnershipJSCalculator calculator;
    private GeoDataMuc geoDataMuc;

    public CreateCarOwnershipModel(GeoDataMuc geoDataMuc) {
        logger.info(" Setting up probabilities for car ownership model");
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("CreateCarOwnershipCalc"));
        calculator = new CreateCarOwnershipJSCalculator(reader);
        this.geoDataMuc = geoDataMuc;
    }

    public void run() {
        for (Household hh : Household.getHouseholds()) {
            simulateCarOwnership(hh);
        }
        //SummarizeData.summarizeCarOwnershipByMunicipality(zonalData);
    }

    public void simulateCarOwnership(Household hh) {
        // simulate number of autos for household hh
        // Note: This method can only be executed after all households have been generated and allocated to zones,
        // as distance to transit and areaType is dependent on where households are living
        int license = hh.getHHLicenseHolders();
        int workers = hh.getNumberOfWorkers();
        int income = hh.getHhIncome()/12;  // convert yearly into monthly income
        //add 1 to the value of distance to transit before taking log to avoid situations of log 0
        double logDistanceToTransit = Math.log(geoDataMuc.getDistanceToTransitOfZone(hh.getHomeZone()) + 1) ;
        int areaType = geoDataMuc.getAreaTypeOfZone(hh.getHomeZone());

        double[] prob = calculator.calculate(license, workers, income, logDistanceToTransit, areaType);
        hh.setAutos(SiloUtil.select(prob));
    }
/*
    public void update DistanceToTransit() {
        //method to update the distance to transit after MATSim updates transit Access times
        zonalData.buildIndex(zonalData.getColumnPosition("Zone"));

        //add a column to store distance to transit pre-populated with 0s
        float[] minDistance = SiloUtil.createArrayWithValue(zonalData.getRowCount(), 0f);
        zonalData.appendColumn(minDistance, "logDistanceToTransit");

        //convert transit access time matrix from omx to java matrix
        String omxFileName = Properties.get().main.baseDirectory + Properties.get().geo.transitAccessTime;
        OmxFile travelTimeOmx = new OmxFile(omxFileName);
        travelTimeOmx.openReadOnly();
        Matrix accessTimeMatrix = SiloUtil.convertOmxToMatrix(travelTimeOmx.getMatrix("mat1"));

        //get minimum time to transit, convert to distance and append to zonal data
        for (int i = 1; i <= zonalData.getRowCount(); i++) {
            float minDist = 9999; // upper limit for distance (in meters) to transit, default for places with no access to transit
            int origin = (int) zonalData.getValueAt(i, "Zone");
            for (int j = 1; j <= zonalData.getRowCount(); j++) {
                int dest = (int) zonalData.getValueAt(j, "Zone");
                // convert time in minutes to distance in meters using a speed of 5 km/h
                float distance = accessTimeMatrix.getValueAt(origin, dest) * 83.33f;
                if (distance > 0 & distance < minDist) {
                    minDist = distance;
                }
            }
            // add 1 to the value of distance to transit before taking log to avoid situations of log 0
            float logMinDist = (float) Math.log(minDist + 1);
            zonalData.setIndexedValueAt(origin, "logDistanceToTransit", logMinDist);
        }
    }
*/
}


