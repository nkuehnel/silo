package de.tum.bgu.msm.autoOwnership.munich;

import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Household;
import de.tum.bgu.msm.data.SummarizeData;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

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


    public int switchToAV(Map<Integer, int[]> conventionalCarsHouseholds, int year) {

        int counter = 0;
        for (Map.Entry<Integer, int[]> pair : conventionalCarsHouseholds.entrySet()) {
            Household hh = Household.getHouseholdFromId(pair.getKey());
            if (hh != null) {
                int income = hh.getHhIncome()/12 ; //uses monthly income

                double[] prob = calculator.calculate(income, year);

                int action = SiloUtil.select(prob);

                if (action == 1){
                    hh.setAutonomous(hh.getAutonomous() + 1);
                    counter++;
                }
            }
        }
        return counter;
    }

}
