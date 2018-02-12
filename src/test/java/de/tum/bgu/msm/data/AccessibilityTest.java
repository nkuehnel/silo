package de.tum.bgu.msm.data;

import cern.colt.matrix.io.MatrixVectorWriter;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import de.tum.bgu.msm.Implementation;
import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.data.maryland.GeoDataMstm;
import de.tum.bgu.msm.properties.Properties;
import junitx.framework.FileAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AccessibilityTest {

    @Test
    public void testZoneToZoneAccessiblities() {
        DoubleMatrix1D population = DoubleFactory1D.dense.ascending(10);
        DoubleMatrix2D travelTimes = DoubleFactory2D.dense.ascending(10, 10);
        DoubleMatrix2D accessibilities = Accessibility.calculateZoneToZoneAccessibilities(population, travelTimes, 1.2, -0.3);
        Assert.assertEquals(12.799312461169375, accessibilities.zSum(), 0.);
    }

    @Test
    public void testScaleAccessibilities() {
        DoubleMatrix1D accessibility = DoubleFactory1D.dense.ascending(10);
        Accessibility.scaleAccessibility(accessibility);
        Assert.assertEquals(100., accessibility.getQuick(9), 0.);
        Assert.assertEquals(550, accessibility.zSum(), 0.);
    }

    @Test
    public void testAggregateAccessibilities() {
        List<Integer> keys = Arrays.asList(0,1,2,3,4,5,6,7,8,9);
        DoubleMatrix2D accessibilitiesAuto = DoubleFactory2D.dense.ascending(10, 10);
        DoubleMatrix1D accessibilityAuto = DoubleFactory1D.dense.make(10);

        DoubleMatrix2D accessibilitiesTransit = DoubleFactory2D.dense.descending(10, 10);
        DoubleMatrix1D accessibilityTransit = DoubleFactory1D.dense.make(10);

        Accessibility.aggregateAccessibilities(accessibilitiesAuto, accessibilitiesTransit, accessibilityAuto, accessibilityTransit, keys);

        Assert.assertEquals(5050., accessibilityAuto.zSum(), 0);
        Assert.assertEquals(4950., accessibilityTransit.zSum(), 0);
    }

    @Test
    public void testRegionalAccessibilities() {
        Region region1 = new RegionImpl(1);
        Region region2 = new RegionImpl(2);
        List<Region> regions = Arrays.asList(region1, region2);
        Zone zone1 = new ZoneImpl(1, 1, 1);
        Zone zone2 = new ZoneImpl(2, 1, 1);
        Zone zone3 = new ZoneImpl(3, 1 ,1);
        region1.addZone(zone1);
        region1.addZone(zone2);
        region2.addZone(zone3);
        DoubleMatrix1D accessibilityAuto = DoubleFactory1D.dense.make(4);
        accessibilityAuto.assign(10);

        DoubleMatrix1D regionalAccessibility = Accessibility.calculateRegionalAccessibility(regions, accessibilityAuto);
        Assert.assertEquals(3, regionalAccessibility.size());
        Assert.assertEquals(10, regionalAccessibility.getQuick(region1.getId()), 0.);
        Assert.assertEquals(10, regionalAccessibility.getQuick(region2.getId()), 0.);
    }

    @Test
    public void testIntegration() throws IOException {
        SiloUtil.siloInitialization("./test/scenarios/annapolis/javaFiles/siloMstm.properties", Implementation.MARYLAND);

        int smallSize = 0;
        boolean readSmallSynPop = Properties.get().main.readSmallSynpop;
        if (readSmallSynPop) {
            smallSize = Properties.get().main.smallSynPopSize;
        }

        GeoData geoData = new GeoDataMstm();
        geoData.setInitialData();

        RealEstateDataManager realEstateDataManager = new RealEstateDataManager(geoData);
        realEstateDataManager.readDwellings(readSmallSynPop, smallSize);

        HouseholdDataManager hhManager = new HouseholdDataManager(realEstateDataManager);
        hhManager.readPopulation(readSmallSynPop, smallSize);

        Accessibility accessibility = new Accessibility(geoData);
        accessibility.readCarSkim(2000);
        accessibility.readPtSkim(2000);
        accessibility.calculateAccessibilities(2000);


        double[] accCar = new double[geoData.getZones().keySet().stream().mapToInt(Integer::intValue).max().getAsInt()+1];
        double[] accTransit = new double[geoData.getZones().keySet().stream().mapToInt(Integer::intValue).max().getAsInt()+1];
        double[] accRegions = new double[geoData.getRegions().keySet().stream().mapToInt(Integer::intValue).max().getAsInt()+1];

        for(int zone: geoData.getZones().keySet()) {
            accCar[zone] = accessibility.getAutoAccessibilityForZone(zone);
            accTransit[zone] = accessibility.getTransitAccessibilityForZone(zone);
        }

        for(int region: geoData.getRegions().keySet()) {
            accRegions[region] = accessibility.getRegionalAccessibility(region);
        }

        MatrixVectorWriter writerCar = new MatrixVectorWriter(new FileWriter("./test/output/accessibilitiesCar.txt"));
        writerCar.printArray(accCar);
        writerCar.flush();
        writerCar.close();

        MatrixVectorWriter writerTransit = new MatrixVectorWriter(new FileWriter("./test/output/accessibilitiesTransit.txt"));
        writerTransit.printArray(accTransit);
        writerTransit.flush();
        writerTransit.close();

        MatrixVectorWriter writerRegion = new MatrixVectorWriter(new FileWriter("./test/output/accessibilitiesRegion.txt"));
        writerRegion.printArray(accRegions);
        writerRegion.flush();
        writerRegion.close();

        FileAssert.assertEquals("car accessibilities are different.", new File("./test/input/accessibilitiesCar.txt"), new File("./test/output/accessibilitiesCar.txt"));
        FileAssert.assertEquals("transit accessibilities are different.", new File("./test/input/accessibilitiesTransit.txt"), new File("./test/output/accessibilitiesTransit.txt"));
        FileAssert.assertEquals("region accessibilities are different.", new File("./test/input/accessibilitiesRegion.txt"), new File("./test/output/accessibilitiesRegion.txt"));
    }
}