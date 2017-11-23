package de.tum.bgu.msm.transportModel.mitoMatsim;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.properties.Properties;

import java.util.ResourceBundle;

//temporary departure time model, it might be omited if mito trips have a time of departure already seted up

public class TempTimeOfDay {


    private int[] timeClasses;
    private double[] departure2WProb;
    private double[] wDurationProb;

    public TempTimeOfDay() {


    }


    public void setup(String trafficAssignmentDirectoty){

        TableDataSet timeOfDayDistributions = SiloUtil.readCSVfile(trafficAssignmentDirectoty + Properties.get().transportModel.matsimDepartureTimeFile);
        timeClasses = timeOfDayDistributions.getColumnAsInt("classes");
        departure2WProb = timeOfDayDistributions.getColumnAsDouble("H2W_departure");
        wDurationProb = timeOfDayDistributions.getColumnAsDouble("W_duration");
    }





    public double selectDepartureTimeToWork(){
        return (SiloUtil.select(departure2WProb, timeClasses) + (SiloUtil.getRandomNumberAsDouble()-0.5)*30)*60;
    }


    public double selectWorkDuration(){
        return (SiloUtil.select(wDurationProb, timeClasses) + (SiloUtil.getRandomNumberAsDouble()-0.5)*30)*60;
    }
}
