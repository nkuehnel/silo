package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.SiloUtil;

//temporary departure time model, it might be omited if mito trips have a time of departure already seted up

public class TempTimeOfDay {
    private String trafficAssignmentDirectoty;
    private int[] timeClasses;
    private double[] departure2WProb;
    private double[] wDurationProb;

    public TempTimeOfDay(String trafficAssignmentDirectoty) {
        this.trafficAssignmentDirectoty = trafficAssignmentDirectoty;
    }


    public void setup(){

        TableDataSet timeOfDayDistributions = SiloUtil.readCSVfile(trafficAssignmentDirectoty + "input/midTimeDistributions.csv");
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
