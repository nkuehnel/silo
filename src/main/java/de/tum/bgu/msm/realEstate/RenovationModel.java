package de.tum.bgu.msm.realEstate;

import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.events.EventRules;
import de.tum.bgu.msm.events.EventManager;
import de.tum.bgu.msm.events.EventTypes;
import de.tum.bgu.msm.data.Dwelling;
import de.tum.bgu.msm.data.RealEstateDataManager;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.UtilityExpressionCalculator;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ResourceBundle;
import java.io.File;

import org.apache.log4j.Logger;

import javax.script.ScriptException;

/**
 * Simulates renovation and deterioration of dwellings
 * Author: Rolf Moeckel, PB Albuquerque
 * Created on 7 January 2010 in Rhede
 **/

public class RenovationModel {

    // properties
	private double[][] renovationProbability;
    private RenovationJSCalculator renovationCalculator;

    public RenovationModel() {
        setupRenovationModel();
	}


	private void setupRenovationModel() {

		// read properties
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("RenovationCalc"));
        renovationCalculator = new RenovationJSCalculator(reader, false);

        //set renovation probabilities
        renovationProbability = new double[SiloUtil.numberOfQualityLevels][5];
        for (int oldQual = 0; oldQual < SiloUtil.numberOfQualityLevels; oldQual++) {
            renovationCalculator.setQuality(oldQual + 1);
            for (int alternative = 0; alternative < 5; alternative++){
                renovationCalculator.setAlternative(alternative + 1);
                try {
                    renovationProbability[oldQual][alternative] = renovationCalculator.calculate();
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
            }
        }
	}


    public void checkRenovation(int dwellingId) {
        //check if dwelling is renovated or deteriorates
        Dwelling dd = Dwelling.getDwellingFromId(dwellingId);
        if (!EventRules.ruleChangeDwellingQuality(dd)) return;  // Dwelling not available for renovation
        int currentQuality = dd.getQuality();
        int selected = SiloUtil.select(getProbabilities(currentQuality));

        if (selected != 2) {
            EventManager.countEvent(EventTypes.ddChangeQual);
            RealEstateDataManager.dwellingsByQuality[currentQuality - 1] -= 1;
        }
        switch (selected) {
            case (0): {
                RealEstateDataManager.dwellingsByQuality[currentQuality - 1 - 2] += 1;
                dd.setQuality(currentQuality - 2);
                break;
            }
            case (1): {
                RealEstateDataManager.dwellingsByQuality[currentQuality - 1 - 1] += 1;
                dd.setQuality(currentQuality - 1);
                break;
            }
            case (3): {
                RealEstateDataManager.dwellingsByQuality[currentQuality - 1 + 1] += 1;
                dd.setQuality(currentQuality + 1);
                break;
            }
            case (4): {
                RealEstateDataManager.dwellingsByQuality[currentQuality - 1 + 2] += 1;
                dd.setQuality(currentQuality + 2);
                break;
            }
        }
    }


    private double[] getProbabilities (int currentQual) {
        // return probabilities to upgrade or deteriorate based on current quality of dwelling and average
        // quality of all dwellings
        double[] currentShare = RealEstateDataManager.getCurrentQualShares();
        // if share of certain quality level is currently 0, set it to very small number to ensure model keeps working
        for (int i = 0; i < currentShare.length; i++) if (currentShare[i] == 0) currentShare[i] = 0.01d;
        double[] initialShare = RealEstateDataManager.getInitialQualShares();
        for (int i = 0; i < initialShare.length; i++) if (initialShare[i] == 0) initialShare[i] = 0.01d;
        double[] probs = new double[5];
        for (int i = 0; i < probs.length; i++) {
            int potentialNewQual = currentQual + i - 2;  // translate into new quality level this alternative would generate
            double ratio;
            if (potentialNewQual >= 1 && potentialNewQual <= 4) ratio = initialShare[potentialNewQual - 1] / currentShare[potentialNewQual - 1];
            else ratio = 0.;
            if (i <= 1) {
                probs[i] = renovationProbability[currentQual - 1][i] * ratio;
            } else if (i == 2) {
                probs[i] = renovationProbability[currentQual - 1][i];
            } else if (i >= 3) probs[i] = renovationProbability[currentQual - 1][i] * ratio;
        }
        return probs;
    }
}

