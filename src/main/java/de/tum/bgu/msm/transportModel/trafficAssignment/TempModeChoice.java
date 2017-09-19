package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.SiloUtil;
import omx.OmxFile;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import java.util.Arrays;

public class TempModeChoice {

    private Matrix autoTravelDistance;

    //hard coded as a temporary solution
    private float b_auto = 0.0f;
    private float b_walk = -51.9f;
    private float b_bicycle = -6.0f;
    private float b_transit = -1.3f;
    private float alpha_auto = -23.4f;
    private float alpha_walk = 89.5f;
    private float alpha_bicycle = -18.4f;
    private float alpha_transit = -22.9f;
    private float beta_auto = -0.05f;
    private float beta_walk = -0.2f;
    private float beta_bicycle = -0.07f;
    private float beta_transit = -0.05f;

    public TempModeChoice() {



    }

    public void readInputData(){

        OmxFile file = new OmxFile("C:/models/siloMitoMatsim/input/tdTest.omx");
        file.openReadOnly();
        OmxMatrix matrix = file.getMatrix("mat1");
        autoTravelDistance = SiloUtil.convertOmxToMatrix(matrix);
        //todo check for lookup vectors
    }

    public int selectMode(int orig, int dest){
        //0: car, 1: walk, 2: bicycle: 3: transit
        int[] alternatives = new int[]{0, 1, 2, 3};
        float travelDistance = autoTravelDistance.getValueAt(orig, dest);
        double[] expUtilities = calculateUtilities(travelDistance, alternatives);
        double[] probabilities = expUtilities;
        int chosen = SiloUtil.select(probabilities, alternatives);
        return chosen;
    }

    private double[] calculateUtilities(float travelDistance, int[] alternatives) {
        double[] utilities = new double[alternatives.length];
        utilities[0] = Math.exp(b_auto + alpha_auto* Math.exp(beta_auto* travelDistance / 1000));
        utilities[1] = Math.exp(b_walk + alpha_walk* Math.exp(beta_walk* travelDistance / 1000));
        utilities[2] = Math.exp(b_bicycle + alpha_bicycle* Math.exp(beta_bicycle* travelDistance / 1000));
        utilities[3] = Math.exp(b_transit + alpha_transit* Math.exp(beta_transit* travelDistance / 1000));
        return utilities;

    }


}
