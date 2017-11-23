package de.tum.bgu.msm.transportModel.mitoMatsim;

import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.properties.Properties;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class TrafficAssignmentUtil {

    static Logger logger = Logger.getLogger(TrafficAssignmentUtil.class);

    private Map<Integer, Coord> coordinateMap = new HashMap<>();
    private Map<Integer, Double> sizeMap = new HashMap<>();
    private String trafficAssignmentDirectory;

    public TrafficAssignmentUtil() {

    }

    public void setup(String trafficAssignmentDirectory){
        this.trafficAssignmentDirectory = trafficAssignmentDirectory;
    }

    public Coord getRandomZoneCoordinates(int zoneId){

        if (!coordinateMap.keySet().contains(zoneId)){
            logger.info("Zonal coordinates not found for zone: " + zoneId);
            return null;
        } else {
            //returns a random coordinate in square
            //todo improve with shp
            return new Coord (coordinateMap.get(zoneId).getX() + (Math.random() - 0.5) * sizeMap.get(zoneId),
                    coordinateMap.get(zoneId).getY() + (Math.random() - 0.5) * sizeMap.get(zoneId));
        }

    }

    public void readCoordinateData() {
        // read dwelling micro data from ascii file

        logger.info("Reading zone coordinates");

        String fileName = trafficAssignmentDirectory + Properties.get().transportModel.matsimZoneCoordinates;


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
            int posSize      = SiloUtil.findPositionInArray("size",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id        = Integer.parseInt(lineElements[posId]);
                double x      = Double.parseDouble(lineElements[posX]);
                double y      = Double.parseDouble(lineElements[posY]);
                double size    = Double.parseDouble(lineElements[posSize]);

                coordinateMap.put(id,new Coord(x,y));
                sizeMap.put(id, size);

            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading zone coordinate file: " + fileName);

        }
        logger.info("Finished reading " + recCount + " zone coordinates.");
    }


}
