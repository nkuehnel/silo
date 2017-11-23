package de.tum.bgu.msm.properties.modules;

import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;

public class TransportModelPropertiesModule {

    public final int[] modelYears;
    public final int[] skimYears;

    public final boolean runTravelDemandModel;
    public final String demandModelPropertiesPath;

    public final boolean runMatsim;
    public final String matsimZoneShapeFile;
    public final String matsimZoneCRS;
    public final String idOfZonesInShapefile;

    public final double matsimScaleFactor;
    public final int matsimIterations;
    public final int matsimThreads;
    public final String matsimDirectory;
    public final String matsimNetworkFile;
    public final String matsimZoneCoordinates;
    public final String matsimDistanceSkimFile;
    public final String matsimDepartureTimeFile;

    public TransportModelPropertiesModule(ResourceBundle bundle) {
        modelYears = ResourceUtil.getIntegerArray(bundle, "transport.model.years");
        skimYears = ResourceUtil.getIntegerArray(bundle, "skim.years");
        runTravelDemandModel = ResourceUtil.getBooleanProperty(bundle, "mito.run.travel.model", false);
        demandModelPropertiesPath = ResourceUtil.getProperty(bundle, "mito.properties.file");
        runMatsim = ResourceUtil.getBooleanProperty(bundle, "matsim.run.travel.model", false);
        matsimZoneShapeFile = ResourceUtil.getProperty(bundle, "matsim.zones.shapefile");
        matsimZoneCRS = ResourceUtil.getProperty(bundle, "matsim.zones.crs");
        idOfZonesInShapefile = ResourceUtil.getProperty(bundle, "matsim.zones.id.attribute");
        matsimScaleFactor = ResourceUtil.getDoubleProperty(bundle,"matsim.scaling.factor");
        matsimIterations = ResourceUtil.getIntegerProperty(bundle, "matsim.iterations");
        matsimThreads = ResourceUtil.getIntegerProperty(bundle, "matsim.threads");
        matsimDirectory = ResourceUtil.getProperty(bundle, "matsim.directory");
        matsimNetworkFile = ResourceUtil.getProperty(bundle, "matsim.network");
        matsimZoneCoordinates = ResourceUtil.getProperty(bundle, "matsim.zone.coordinates");
        matsimDistanceSkimFile = ResourceUtil.getProperty(bundle, "matsim.distance.skim.file");
        matsimDepartureTimeFile = ResourceUtil.getProperty(bundle, "matsim.departure.times");
    }
}
