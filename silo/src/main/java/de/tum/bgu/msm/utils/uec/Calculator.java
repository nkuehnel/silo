package de.tum.bgu.msm.utils.uec;

import com.pb.common.calculator2.UtilityExpressionCalculator;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.properties.Properties;
import org.apache.log4j.Logger;

import java.io.File;

public class Calculator<T> {

    private static final Logger logger = Logger.getLogger(Calculator.class);

    private final UtilityExpressionCalculator calculator;
    protected final DMU dmu;
    private final int[] altAvailable;

    public Calculator(String uecPath, int dataSheetNumber,
                      int sheetNumber, DMU dmu) {
        this.calculator =  new UtilityExpressionCalculator(new File(uecPath), sheetNumber,
                dataSheetNumber, ResourceUtil.changeResourceBundleIntoHashMap(Properties.get().bundle), dmu);

        this.dmu = dmu;
        int numAlts= calculator.getNumberOfAlternatives();
        this.altAvailable = new int[numAlts + 1];
        for (int i = 1; i < altAvailable.length; i++) {
            altAvailable[i] = 1;
        }
    }

    public double[] calculate(boolean log) {
        double util[] = calculator.solve(dmu.getDmuIndexValues(), dmu, altAvailable);
        if (log) {
            calculator.logAnswersArray(logger, " Results using dmu: " + dmu.toString());
        }
        return util;
    }

    public int getNumberOfAlternatives() {
        return calculator.getNumberOfAlternatives();
    }
}