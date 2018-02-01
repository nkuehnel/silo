package de.tum.bgu.msm.transportModel.mitoMatsim;

import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.transportModel.matsim.MatsimTravelTimes;
import de.tum.bgu.msm.transportModel.matsim.SiloMatsimUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;

import org.matsim.utils.leastcostpathtree.LeastCostPathTree;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MitoMatsimTravelTimes implements TravelTimes {

    private final static Logger logger = Logger.getLogger(MatsimTravelTimes.class);

    private final LeastCostPathTree leastCoastPathTree;
    private final Network network;
    private final Map<Integer, List<Node>> zoneCalculationNodesMap = new HashMap<>();
    private final static int NUMBER_OF_CALC_POINTS = 1;
    private final static double TIME_OF_DAY = 8. * 60 * 60.; // TODO
    private final Map<Id<Node>, Map<Id<Node>, LeastCostPathTree.NodeData>> treesForNode = new HashMap<>();
    private TrafficAssignmentUtil trafficAssignmentUtil;

    public MitoMatsimTravelTimes(LeastCostPathTree leastCoastPathTree, Network network, TrafficAssignmentUtil trafficAssignmentUtil, Map<Integer, MitoZone> zones) {
        this.leastCoastPathTree = leastCoastPathTree;

        NetworkCleaner nc = new NetworkCleaner();
        nc.run(network);


        this.network = network;
        this.trafficAssignmentUtil = trafficAssignmentUtil;
        initialize(zones);
    }

    private void initialize(Map<Integer, MitoZone> zones) {
        for (int zoneId : zones.keySet()) {

            for (int i = 0; i < NUMBER_OF_CALC_POINTS; i++) { // Several points in a given origin zone

                Coord originCoord = trafficAssignmentUtil.getRandomZoneCoordinates(zoneId);
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getToNode();

                if (!zoneCalculationNodesMap.containsKey(zoneId)) {
                    zoneCalculationNodesMap.put(zoneId, new LinkedList<Node>());
                }
                zoneCalculationNodesMap.get(zoneId).add(originNode);
            }
        }
    }

    @Override
    public double getTravelTime(int origin, int destination, double timeOfDay) {
        logger.trace("There are " + zoneCalculationNodesMap.keySet().size() + " origin zones.");
        double sumTravelTime_min = 0.;

        for (Node originNode : zoneCalculationNodesMap.get(origin)) { // Several points in a given origin zone
            Map<Id<Node>, LeastCostPathTree.NodeData> tree;
            if(treesForNode.containsKey(originNode.getId())) {
                tree = treesForNode.get(originNode.getId());
            } else {
                leastCoastPathTree.calculate(network, originNode, timeOfDay);
                tree = leastCoastPathTree.getTree();
                treesForNode.put(originNode.getId(), tree);
            }


            for (Node destinationNode : zoneCalculationNodesMap.get(destination)) {// several points in a given destination zone

                double arrivalTime = tree.get(destinationNode.getId()).getTime();
                sumTravelTime_min += ((arrivalTime - TIME_OF_DAY) / 60.);
            }
        }
        return sumTravelTime_min / NUMBER_OF_CALC_POINTS / NUMBER_OF_CALC_POINTS;
    }




}
