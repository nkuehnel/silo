package de.tum.bgu.msm.transportModel.trafficAssignment;

import de.tum.bgu.msm.SiloUtil;

public class TempTimeOfDay {

    public double selectDepartureTimeToWork(){
        return 7*60*60 + SiloUtil.getRandomNumberAsDouble()*60*60*2;
    }


    public double selectWorkDuration(){
        return 6*60*60 + SiloUtil.getRandomNumberAsDouble()*60*60*4;
    }
}
