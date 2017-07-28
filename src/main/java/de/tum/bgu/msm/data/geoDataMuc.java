package de.tum.bgu.msm.data;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.container.SiloDataContainer;
import omx.OmxFile;
import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import de.tum.bgu.msm.SiloUtil;

/**
 * Interface to store zonal, county and regional data used by the SILO Model
 * @author Ana Moreno and Rolf Moeckel, Technical University of Munich
 * Created on 5 April 2017 in Munich
 **/

public class geoDataMuc implements geoDataI {
    private static final Logger logger = Logger.getLogger(geoDataMuc.class);

    protected static final String PROPERTIES_ZONAL_DATA_FILE  = "zonal.data.file";
    protected static final String PROPERTIES_REGION_DEF_FILE  = "region.definition.file";
    protected static final String PROPERTIES_LAND_USE_AREA    = "land.use.area.by.taz";
    protected static final String PROPERTIES_DEVELOPABLE      = "developable.lu.category";
    protected static final String PROPERTIES_DEVELOPM_RESTR   = "development.restrictions";
    protected static final String PROPERTIES_USE_CAPACITY     = "use.growth.capacity.data";
    protected static final String PROPERTIES_CAPACITY_FILE    = "growth.capacity.file";
    protected static final String PROPERTIES_TRANSIT_ACCEESS_TIME = "transit.access.time";

    private ResourceBundle rb;
    private static int[] zoneIndex;
    private static int highestZonalId;
    private static HashMap<Integer, int[]> regionDefinition;
    private static int[] regionList;
    private static int[] regionIndex;
    private static TableDataSet regDef;
    private static int[] counties;
    private static int[] countyIndex;
    private static TableDataSet landUse;
    private int[] developableLUtypes;
    private boolean useCapacityAsNumberOfDwellings;
    private TableDataSet developmentCapacity;
    private static TableDataSet developmentRestrictions;
    private static TableDataSet distanceToTransit;

    public geoDataMuc(ResourceBundle rb) {
        this.rb = rb;
    }
    protected Matrix accessDistanceMatrix;


    public void setInitialData () {
        SiloUtil.endYear = ResourceUtil.getIntegerProperty(rb, SiloUtil.PROPERTIES_END_YEAR);
        SiloUtil.simulationLength = ResourceUtil.getIntegerProperty(rb, SiloUtil.PROPERTIES_SIMULATION_PERIOD_LENGTH);
        SiloUtil.gregorianIterator = ResourceUtil.getIntegerProperty(rb, SiloUtil.PROPERTIES_GREGORIAN_ITERATOR);
        SiloUtil.incBrackets = ResourceUtil.getIntegerArray(rb, SiloUtil.PROPERTIES_INCOME_BRACKETS);
        SiloUtil.numberOfQualityLevels = ResourceUtil.getIntegerProperty(rb, SiloUtil.PROPERTIES_NUMBER_OF_DWELLING_QUALITY_LEVELS);
        readZones();
        setDistanceToTransit();
        //readLandUse();
    }


    private void readZones() {
        // read zonal data
        String fileName = SiloUtil.baseDirectory + ResourceUtil.getProperty(rb, PROPERTIES_ZONAL_DATA_FILE);
        SiloUtil.zonalData = SiloUtil.readCSVfile(fileName);
        highestZonalId = SiloUtil.getHighestVal(SiloUtil.zonalData.getColumnAsInt("Zone"));
        SiloUtil.zonalData.buildIndex(SiloUtil.zonalData.getColumnPosition("Zone"));

        int[] zones = getZones();
        zoneIndex = SiloUtil.createIndexArray(zones);

        // read region definition
        String regFileName = SiloUtil.baseDirectory + ResourceUtil.getProperty(rb, PROPERTIES_REGION_DEF_FILE);
        regDef = SiloUtil.readCSVfile(regFileName);
        regionDefinition = new HashMap<>();
        for (int row = 1; row <= regDef.getRowCount(); row++) {
            int taz = (int) regDef.getValueAt(row, "Zone");
            int reg = (int) regDef.getValueAt(row, "Region");
            if (regionDefinition.containsKey(reg)) {
                int[] zoneInThisRegion = regionDefinition.get(reg);
                int[] newZones = SiloUtil.expandArrayByOneElement(zoneInThisRegion, taz);
                regionDefinition.put(reg, newZones);
            } else {
                regionDefinition.put(reg, new int[]{taz});
            }
        }
        regionList = SiloUtil.idendifyUniqueValues(regDef.getColumnAsInt("Region"));
        regionIndex = SiloUtil.createIndexArray(regionList);
        regDef.buildIndex(regDef.getColumnPosition("Zone"));


        // create list of county FIPS codes
        counties = SiloUtil.idendifyUniqueValues(SiloUtil.zonalData.getColumnAsInt("ID_county"));
        countyIndex = SiloUtil.createIndexArray(counties);
    }


    private void readLandUse() {
        // read land use data
        logger.info("Reading land use data");
        String fileName;
        if (SiloUtil.startYear == SiloUtil.getBaseYear()) {  // start in year 2000
            fileName = SiloUtil.baseDirectory + "input/" + ResourceUtil.getProperty(rb, PROPERTIES_LAND_USE_AREA) + ".csv";
        } else {                                             // start in different year (continue previous run)
            fileName = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/" +
                    ResourceUtil.getProperty(rb, PROPERTIES_LAND_USE_AREA) + "_" + SiloUtil.startYear + ".csv";
        }
        landUse = SiloUtil.readCSVfile(fileName);
        landUse.buildIndex(landUse.getColumnPosition("Zone"));

        // read developers data
        developableLUtypes = ResourceUtil.getIntegerArray(rb, PROPERTIES_DEVELOPABLE);

        String restrictionsFileName = SiloUtil.baseDirectory + ResourceUtil.getProperty(rb, PROPERTIES_DEVELOPM_RESTR);
        developmentRestrictions = SiloUtil.readCSVfile(restrictionsFileName);
        developmentRestrictions.buildIndex(developmentRestrictions.getColumnPosition("Zone"));

        useCapacityAsNumberOfDwellings = ResourceUtil.getBooleanProperty(rb, PROPERTIES_USE_CAPACITY, false);
        if (useCapacityAsNumberOfDwellings) {
            String capacityFileName;
            if (SiloUtil.startYear == SiloUtil.getBaseYear()) {  // start in year 2000
                capacityFileName = SiloUtil.baseDirectory + "input/" + ResourceUtil.getProperty(rb, PROPERTIES_CAPACITY_FILE) + ".csv";
            } else {                                             // start in different year (continue previous run)
                capacityFileName = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/" +
                        ResourceUtil.getProperty(rb, PROPERTIES_CAPACITY_FILE) + "_" + SiloUtil.startYear + ".csv";
            }
            developmentCapacity = SiloUtil.readCSVfile(capacityFileName);
            developmentCapacity.buildIndex(developmentCapacity.getColumnPosition("Zone"));
        }
    }

    public boolean useNumberOfDwellingsAsCapacity () {
        return useCapacityAsNumberOfDwellings;
    }


    public float getDevelopmentCapacity (int zone) {
        return developmentCapacity.getIndexedValueAt(zone, "DevCapacity");
    }

    public void reduceDevelopmentCapacityByOneDwelling (int zone) {
        float capacity = Math.max(getDevelopmentCapacity(zone) - 1, 0);
        developmentCapacity.setIndexedValueAt(zone, "DevCapacity", capacity);
    }

    public void reduceDevelopmentCapacityByDevelopableAcres (int zone, float acres) {
        for (int type: getDevelopableLandUseTypes()) {
            String column = "LU" + type;
            float existing = landUse.getIndexedValueAt(zone, column);
            if (existing < acres) {
                landUse.setIndexedValueAt(zone, column, 0);
                acres = acres - existing;
            } else {
                float reduced = existing - acres;
                landUse.setIndexedValueAt(zone, column, reduced);
                acres = 0;
            }
            if (acres <= 0) return;
        }
    }

    public void writeOutDevelopmentCapacityFile (SiloDataContainer dataContainer) {
        // write out development capacity file to allow model run to be continued from this point later

        boolean useCapacityAsNumberOfDwellings = ResourceUtil.getBooleanProperty(rb, PROPERTIES_USE_CAPACITY, false);
        if(useCapacityAsNumberOfDwellings)	{
            String capacityFileName = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/" +
                    ResourceUtil.getProperty(rb, PROPERTIES_CAPACITY_FILE) + "_" + SiloUtil.getEndYear() + ".csv";
            PrintWriter pwc = SiloUtil.openFileForSequentialWriting(capacityFileName, false);
            pwc.println("Zone,DevCapacity");
            for (int zone: getZones()) pwc.println(zone + "," + getDevelopmentCapacity(zone));
            pwc.close();
        }

        String landUseFileName = SiloUtil.baseDirectory + "scenOutput/" + SiloUtil.scenarioName + "/" +
                ResourceUtil.getProperty(rb, PROPERTIES_LAND_USE_AREA) + "_" + SiloUtil.getEndYear() + ".csv";
        PrintWriter pwl = SiloUtil.openFileForSequentialWriting(landUseFileName, false);
        pwl.println("Zone,lu41");
        for (int zone: getZones()) pwl.println(zone + "," + dataContainer.getRealEstateData().getDevelopableLand(zone));
        pwl.close();
    }

    public float getAreaOfLandUse (String landUseType, int zone) {
        return landUse.getIndexedValueAt(zone, landUseType);
    }


    public int[] getDevelopableLandUseTypes() {
        return developableLUtypes;
    }

    public boolean isThisDwellingTypeAllowed (String dwellingType, int zone) {
        // return TRUE if this dwellingType can be built in this zone, otherwise return false

        int col = 0;
        try {
            col = developmentRestrictions.getColumnPosition(dwellingType);
        } catch (Exception e) {
            logger.error("Unknown DwellingType " + dwellingType + ". Cannot find it in developmentRestrictions.");
            System.exit(1);
        }
        return (developmentRestrictions.getIndexedValueAt(zone, col) == 1);
    }


    public int getHighestZonalId () {
        // return highest zone ID
        return highestZonalId;
    }

    public int[] getZones () {
        // return array with zone IDs
        return SiloUtil.zonalData.getColumnAsInt("Zone");
    }

    public float getSizeOfZoneInAcres(int zone) {
        return SiloUtil.zonalData.getIndexedValueAt(zone, "Area");
    }

    public float[] getSizeOfZonesInAcres() {
        float[] size = new float[getZones().length];
        for (int zone: getZones()) {
            size[getZoneIndex(zone)] = getSizeOfZoneInAcres(zone);
        }
        return size;
    }

    public static int getCountyOfZone(int zone) {
        return (int) SiloUtil.zonalData.getIndexedValueAt(zone, "COUNTYFIPS");
    }

    public int getZoneIndex(int zone) {
        return zoneIndex[zone];
    }

    public int[] getRegionList() {
        return regionList;
    }

    public int getRegionIndex(int region) {
        return regionIndex[region];
    }

    public int[] getZonesInRegion (int region) {
        return regionDefinition.get(region);
    }

    public int getRegionOfZone (int zone) {
        return (int) regDef.getIndexedValueAt(zone, "Region");
    }

    public static int getPUMAofZone (int taz) {
        // return PUMA in which taz is located
        return (int) SiloUtil.zonalData.getIndexedValueAt(taz, "PUMA");
    }

    public static int getMSAOfZone (int zone) {
        return (int) SiloUtil.zonalData.getIndexedValueAt(zone, "msa");
    }

    public static int getSimplifiedPUMAofZone (int taz) {
        // return PUMA in which taz is located (less geographic detail, last digit is rounded to 1)
        return (int) SiloUtil.zonalData.getIndexedValueAt(taz, "simplifiedPUMA");
    }


    public void setDistanceToTransit(){
        //convert access time matrix to access distance matrix considering a walking speed of 5km/h
        //set the minimum value of each row as the respective zone's distance to transit
        logger.info("   Starting to read OMX matrix");
        String omxFileName= SiloUtil.baseDirectory + ResourceUtil.getProperty(rb,PROPERTIES_TRANSIT_ACCEESS_TIME);
        OmxFile travelTimeOmx = new OmxFile(omxFileName);
        travelTimeOmx.openReadOnly();
        accessDistanceMatrix = SiloUtil.convertOmxToMatrix(travelTimeOmx.getMatrix("mat1"));

        distanceToTransit = new TableDataSet();
        int [] zoneNO = SiloUtil.createArrayWithValue(accessDistanceMatrix.getRowCount(),0);
        distanceToTransit.appendColumn(zoneNO, "zoneNO");
        for (int z = 1; z<=accessDistanceMatrix.getRowCount(); z++){
            distanceToTransit.setValueAt(z,"zoneNO",z);
        }
        float[] minDist = SiloUtil.createArrayWithValue(distanceToTransit.getRowCount(),0f);
        distanceToTransit.appendColumn(minDist, "minDist");

        for (int i = 1; i <= accessDistanceMatrix.getRowCount(); i++){
            float minDistance = 9999;
            for (int j = 1; j <= accessDistanceMatrix.getColumnCount(); j++){
                accessDistanceMatrix.setValueAt(i,j, accessDistanceMatrix.getValueAt(i,j)*83.33f);
                if (accessDistanceMatrix.getValueAt(i,j) > 0 & accessDistanceMatrix.getValueAt(i,j) < minDistance){
                    minDistance = accessDistanceMatrix.getValueAt(i,j);
                }
            }
            distanceToTransit.setValueAt(i,"minDist",minDistance+1);
        }
        distanceToTransit.writeFile("checkDistToTransit.csv",distanceToTransit);
    }

    public float getDistanceToTransit (int zone) {
        return (int) distanceToTransit.getValueAt((zone ),"minDist");
    }

    public int getAreaTypeOfZone (int zone) {
        return (int) SiloUtil.zonalData.getIndexedValueAt(zone, "BBSR");
    }

    public int getMunicipalityOfZone(int zone) {
        return (int) SiloUtil.zonalData.getIndexedValueAt(zone, "ID_city");
    }
}