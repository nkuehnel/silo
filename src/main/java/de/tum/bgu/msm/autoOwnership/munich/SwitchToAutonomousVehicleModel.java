package de.tum.bgu.msm.autoOwnership.munich;

import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Household;
import de.tum.bgu.msm.data.SummarizeData;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by matthewokrah on 12/12/2017.
 */
public class SwitchToAutonomousVehicleModel {

    static Logger logger = Logger.getLogger(SwitchToAutonomousVehicleModel.class);
    private final SwitchToAutonomousVehicleJSCalculator calculator;

    public SwitchToAutonomousVehicleModel() {
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("SwitchToAutonomousVehicleCalc"));
        calculator = new SwitchToAutonomousVehicleJSCalculator(reader);
    }

/*
    public void run() {
        for (Household hh : Household.getHouseholdArray()) {
            simulateCarOwnership(hh);
        }
        SummarizeData.summarizeCarOwnershipByMunicipality(zonalData);
    }

    public void simulateCarOwnership(Household hh) {
        // simulate number of autos for household hh
        // Note: This method can only be executed after all households have been generated and allocated to zones,
        // as distance to transit and areaType is dependent on where households are living
        int license = hh.getHHLicenseHolders();
        int workers = hh.getNumberOfWorkers();
        int income = hh.getHhIncome()/12;  // convert yearly into monthly income
        // add 1 to the value of distance to transit before taking log to avoid situations of log 0
        double logDistanceToTransit = Math.log(zonalData.getIndexedValueAt(hh.getHomeZone(), "distanceToTransit") + 1);
        int areaType = (int) zonalData.getIndexedValueAt(hh.getHomeZone(), "BBSR");

        double[] prob = calculator.calculate(license, workers, income, logDistanceToTransit, areaType);
        hh.setAutos(SiloUtil.select(prob));
    }*/
}
