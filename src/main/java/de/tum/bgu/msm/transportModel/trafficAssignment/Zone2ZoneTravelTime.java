package de.tum.bgu.msm.transportModel.trafficAssignment;

import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import java.util.HashMap;
import java.util.Map;

public class Zone2ZoneTravelTime implements IterationEndsListener {

    private final static Logger log = Logger.getLogger(Zone2ZoneTravelTime.class);

    private Controler controler;
    private Network network;
    private int finalIteration;
    private int[] zones;
    //private Map<Integer, SimpleFeature> zoneFeatureMap;
    private int departureTime;
    private int numberOfCalcPoints;
    //	private CoordinateTransformation ct;
    private Matrix autoTravelTime;
    private TrafficAssignmentUtil trafficAssignmentUtil;



    public Zone2ZoneTravelTime(Matrix autoTravelTime, Controler controler, Network network,
                                       int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                       int [] zones,
                                       int timeOfDay,
                                       int numberOfCalcPoints,
                               TrafficAssignmentUtil trafficAssignmentUtil //CoordinateTransformation ct,
    ) {
        this.autoTravelTime = autoTravelTime;
        this.controler = controler;
        this.network = network;
        this.finalIteration = finalIteration;
        //this.zoneFeatureMap = zoneFeatureMap;
        this.zones = zones;
        this.departureTime = timeOfDay;
        this.numberOfCalcPoints = numberOfCalcPoints;
        this.trafficAssignmentUtil = trafficAssignmentUtil;
//		this.ct = ct;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {

            log.info("Starting to calculate average zone-to-zone travel times based on MATSim.");
            TravelTime travelTime = controler.getLinkTravelTimes();
            TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
//            TravelDisutility travelTimeAsTravelDisutility = new MyTravelTimeDisutility(controler.getLinkTravelTimes());

            LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);

//            Dijkstra dijkstra = new Dijkstra(network, travelTimeAsTravelDisutility, travelTime);

            autoTravelTime = new Matrix(autoTravelTime.getRowCount(),autoTravelTime.getColumnCount());

            //Map to assign a node to each zone
            Map<Integer, Node> zoneCalculationNodesMap = new HashMap<>();

            for (int i : zones) {
                Coord originCoord = trafficAssignmentUtil.getZoneCoordinates(i);
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getFromNode();
                zoneCalculationNodesMap.put(i, originNode);
            }

            int counter = 0;

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < zones.length; i++) { // loop origin zones

                int orig = zones[i];
                Node originNode = zoneCalculationNodesMap.get(orig);
                leastCoastPathTree.calculate(network, originNode, departureTime);
                Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();

                for (int j = 0; j < zones.length; j++) { // going over all destination zones

                    int dest = zones[j];
                    //nex line to fill only half matrix and use half time
                    if (i <= j) {


                        Node destinationNode = zoneCalculationNodesMap.get(dest);

                        double arrivalTime = tree.get(destinationNode.getId()).getTime();
                        //congested car travel times in minutes
                        float congestedTravelTimeMin = (float) ((arrivalTime - departureTime) / 60.);
                        //float congestedTravelTimeMin = (float) (path.travelTime/60);
                        autoTravelTime.setValueAt(orig, dest, congestedTravelTimeMin);
                        //if only done half matrix need to add next line
                        autoTravelTime.setValueAt(dest, orig, congestedTravelTimeMin);

                    }
                }

            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Completed in: " + duration);
        }

    }

    public Matrix getAutoTravelTime() {
        return autoTravelTime;
    }


}
