package de.tum.bgu.msm.autoOwnership.munich;

import de.tum.bgu.msm.util.js.JavaScriptCalculator;

import java.io.Reader;

/**
 * Created by matthewokrah on 12/12/2017.
 */
public class SwitchToAutonomousVehicleJSCalculator extends JavaScriptCalculator<double[]> {

    public SwitchToAutonomousVehicleJSCalculator (Reader reader){
        super(reader);
    }

    public double[] calculate(int income, double ratio) {
        return super.calculate("calculateSwitchToAutonomousVehicleProbabilities", income, ratio);
    }
}
