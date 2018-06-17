package de.tum.bgu.msm.models.autoOwnership.maryland;

import de.tum.bgu.msm.util.js.JavaScriptCalculator;

import java.io.Reader;

public class MarlandCarOwnershipJSCalculator extends JavaScriptCalculator<double[]> {
    protected MarlandCarOwnershipJSCalculator(Reader reader) {
        super(reader);
    }


    public double[] calculateCarOwnerShipProbabilities(int hhSize, int wrk, int inc, int transitAcc, int dens) {
        return super.calculate("calculateCarOwnerShipProbabilities", hhSize, wrk, inc,
                transitAcc, dens);
    }
}
