package de.tum.bgu.msm.syntheticPopulationGenerator;


import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.data.Household;
import de.tum.bgu.msm.data.SummarizeData;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.data.munich.GeoDataMuc;
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

    private final CreateCarOwnershipJSCalculator calculator;
    private final GeoDataMuc geoDataMuc;
    private final Accessibility accessibility;

    public CreateCarOwnershipModel(GeoDataMuc geoDataMuc, Accessibility accessibility) {
        logger.info(" Setting up probabilities for car ownership model");
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("CreateCarOwnershipCalc"));
        calculator = new CreateCarOwnershipJSCalculator(reader);
        this.geoDataMuc = geoDataMuc;
        this.accessibility = accessibility;
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
        double logDistanceToTransit = Math.log(accessibility.getPtDistances().getDistanceToNearestPTStop(hh.getHomeZone()) + 1) ;
        int areaType = geoDataMuc.getAreaTypeOfZone(hh.getHomeZone());

        double[] prob = calculator.calculate(license, workers, income, logDistanceToTransit, areaType);
        hh.setAutos(SiloUtil.select(prob));
    }
}


