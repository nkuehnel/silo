package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.Dwelling;
import de.tum.bgu.msm.data.DwellingType;
import de.tum.bgu.msm.data.RealEstateDataManager;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TrafficAssignmentUtil {

    static Logger logger = Logger.getLogger(TrafficAssignmentUtil.class);

    private Map<Integer, Coord> coordinateMap = new HashMap<>();
    private String trafficAssignmentDirectory;

    public TrafficAssignmentUtil(String trafficAssignmentDirectory) {
        this.trafficAssignmentDirectory = trafficAssignmentDirectory;
    }

    public Coord getZoneCoordinates(int zoneId){

        if (!coordinateMap.keySet().contains(zoneId)){
            //this solves the issue of zones not included in the test list of zones
            return new Coord(4468000,5333000);
        } else {
            return coordinateMap.get(zoneId);
        }

    }

    public void readCoordinateData() {
        // read dwelling micro data from ascii file

        logger.info("Reading zone coordinates");

        String fileName = trafficAssignmentDirectory + "input/zoneCoordinates.csv";


        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            int posId      = SiloUtil.findPositionInArray("ID", header);
            int posX    = SiloUtil.findPositionInArray("x",header);
            int posY      = SiloUtil.findPositionInArray("y",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id        = Integer.parseInt(lineElements[posId]);
                double x      = Double.parseDouble(lineElements[posX]);
                double y      = Double.parseDouble(lineElements[posY]);

                coordinateMap.put(id,new Coord(x,y));
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading zone coordinate file: " + fileName);

        }
        logger.info("Finished reading " + recCount + " zone coordinates.");
    }


}
