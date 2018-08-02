package org.jlab.calib.services.cnd; 

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorDecoderView;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.examples.RawEventViewer;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

/**
* CND Calibration suite
* Based on the work of Louise Clark thanks!
*
* @author  Gavin Murdoch
*/

public class CNDTDCconvEventListener extends CNDCalibrationEngine {

	// hists
//	public final int GEOMEAN = 0;
//	public final int LOGRATIO = 1;
	public final int TIMEL = 0;
	public final int TIMER = 1;

    
    public final int OVERRIDE_LEFT = 0;
    public final int OVERRIDE_RIGHT = 0;

    public final double EXPECTED_SLOPE = CNDPaddlePair.NS_PER_CH;
    public final double EXPECTED_OFFSET = 0.0;
    public final double ALLOWED_DIFF = 0.001;  //Check this
    
    //Height threshold for leading edge calculations
    public double FIT_HEIGHT_THRESHOLD = 0.05;
    
    
    
//    private String fitOption = "RNQ";
    private String fitOption = "";
	private String showPlotType = "TDC_conv";
	int backgroundSF = 2;
	boolean showSlices = false;
	private int FIT_METHOD_SF = 0;
	private int FIT_METHOD_MAX = 1;
        
        //GEMC IDEAL LIMITS
//        public int HIST_BINS = 600;
//        public double HIST_X_MIN = 0;
//        public double HIST_X_MAX = HIST_X_MIN + ((double) HIST_BINS * (CNDPaddlePair.NS_PER_CH));
        
        //CALIB CHALL SAMPLE
//        public int HIST_BINS = 640;
//        public double HIST_X_MIN = 0;
//        public double HIST_X_MAX = 640;
//        public int HIST_BINS = 160;
//        public double HIST_X_MIN = 123;
//        public double HIST_X_MAX = 139;
        public int HIST_BINS = 200;
//        public double HIST_X_MIN = 123;
//        public double HIST_X_MAX = 143;
        public double HIST_X_MIN = 100;
        public double HIST_X_MAX = 200;
	
	private final double[]        FIT_MIN = {0.0, 25500.0, 24800.0, 25500.0};
	private final double[]        FIT_MAX = {0.0, 26800.0, 26200.0, 26800.0};
	private final double[]        TDC_MIN = {0.0, 25000.0,  24000.0, 25000.0};
	private final double[]        TDC_MAX = {0.0, 28000.0, 27000.0, 28000.0};	

    public CNDTDCconvEventListener() {

        stepName = "TDC_conv";
        fileNamePrefix = "CND_CALIB_TDC_CONV_";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "slope_L/F:offset_L/F:slope_R/F:offset_R/F");        
                
        calib.setName("/calibration/cnd/TDC_conv");
        calib.setPrecision(5);

        // assign constraints to all paddles
        calib.addConstraint(3, EXPECTED_SLOPE*(1-ALLOWED_DIFF),
                EXPECTED_SLOPE*(1+ALLOWED_DIFF));
        calib.addConstraint(4, EXPECTED_OFFSET*(1-ALLOWED_DIFF),
                EXPECTED_OFFSET*(1+ALLOWED_DIFF));
        calib.addConstraint(5, EXPECTED_SLOPE*(1-ALLOWED_DIFF),
                EXPECTED_SLOPE*(1+ALLOWED_DIFF));
        calib.addConstraint(6, EXPECTED_OFFSET*(1-ALLOWED_DIFF),
                EXPECTED_OFFSET*(1+ALLOWED_DIFF));        

    }
    
    @Override
    public void populatePrevCalib() {
        prevCalRead = true;
    }    
    
//    @Override
//    public void populatePrevCalib() {
    public void populatePrevCalib2() {
        
        System.out.println("!!!!!IN populatePrevCalib2 FOR TDC CONV!!!!!");
        
        

        if (calDBSource==CAL_FILE) {

            // read in the values from the text file            
            String line = null;
            try { 

                // Open the file
                FileReader fileReader = 
                        new FileReader(prevCalFilename);

                // Always wrap FileReader in BufferedReader
                BufferedReader bufferedReader = 
                        new BufferedReader(fileReader);            

                line = bufferedReader.readLine();

                while (line != null) {

                    String[] lineValues;
                    lineValues = line.split(" ");

                    int sector = Integer.parseInt(lineValues[0]);
                    int layer = Integer.parseInt(lineValues[1]);
                    int component = Integer.parseInt(lineValues[2]);
                    double slopeLeft = Double.parseDouble(lineValues[3]);
                    double offsetLeft = Double.parseDouble(lineValues[4]);
                    double slopeRight = Double.parseDouble(lineValues[5]);
                    double offsetRight = Double.parseDouble(lineValues[6]);

                    convValues.addEntry(sector, layer, component);
                    convValues.setDoubleValue(slopeLeft,
                            "slope_L", sector, layer, component);                    
                    convValues.setDoubleValue(offsetLeft,
                            "offset_L", sector, layer, component);                    
                    convValues.setDoubleValue(slopeRight,
                            "slope_R", sector, layer, component);                    
                    convValues.setDoubleValue(offsetRight,
                            "offset_R", sector, layer, component);
                    
                    line = bufferedReader.readLine();
                }

                bufferedReader.close();            
            }
            catch(FileNotFoundException ex) {
                ex.printStackTrace();
                System.out.println(
                        "Unable to open file '" + 
                                prevCalFilename + "'");                
            }
            catch(IOException ex) {
                System.out.println(
                        "Error reading file '" 
                                + prevCalFilename + "'");                   
                ex.printStackTrace();
            }            
        }
        else if (calDBSource==CAL_DEFAULT) {
            
            for (int sector = 1; sector <= 24; sector++) {
                for (int layer = 1; layer <= 3; layer++) {
                    int component = 1;
                
                    convValues.addEntry(sector, layer, component);                          
                    convValues.setDoubleValue(EXPECTED_SLOPE,
                            "slope_L", sector, layer, component);                    
                    convValues.setDoubleValue(EXPECTED_OFFSET,
                            "offset_L", sector, layer, component);                    
                    convValues.setDoubleValue(EXPECTED_SLOPE,
                            "slope_R", sector, layer, component);                    
                    convValues.setDoubleValue(EXPECTED_OFFSET,
                            "offset_R", sector, layer, component);

                }
            }            
        }
        else if (calDBSource==CAL_DB) {
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            convValues = dcp.readConstants("/calibration/cnd/TDC_conv");   
            dcp.disconnect();
        }
    }

    @Override
    public void resetEventListener() {

        // GM perform init processing
        for (int sector = 1; sector <= 24; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int component = 1;

                // create all the histograms
//                H2F histL = new H2F("tdcConvLeft", "tdcConvLeft", 50, TDC_MIN[layer], TDC_MAX[layer],
//                        50, -1.0, 1.0);
//
//                histL.setName("tdcConvLeft");
//                histL.setTitle("RF offset vs TDC left : " + LAYER_NAME[layer_index]
//                        + " Sector " + sector + " Paddle " + component);
//                histL.setTitleX("TDC Left");
//                histL.setTitleY("RF offset (ns)");
//
//                H2F histR = new H2F("tdcConvRight", "tdcConvRight", 50, TDC_MIN[layer], TDC_MAX[layer],
//                        50, -1.0, 1.0);
//
//                histR.setName("tdcConvRight");
//                histR.setTitle("RF offset vs TDC right : " + LAYER_NAME[layer_index]
//                        + " Sector " + sector + " Paddle " + component);
//                histR.setTitleX("TDC Right");
//                histR.setTitleY("RF offset (ns)");

                // create all the functions and graphs
//                F1D convFuncLeft = new F1D("convFuncLeft", "[a]+[b]*x", FIT_MIN[layer], FIT_MAX[layer]);
//                GraphErrors convGraphLeft = new GraphErrors("convGraphLeft");
//                convGraphLeft.setName("convGraphLeft");
//                convFuncLeft.setLineColor(FUNC_COLOUR);
//                convFuncLeft.setLineWidth(FUNC_LINE_WIDTH);
//                convGraphLeft.setMarkerSize(MARKER_SIZE);
//                convGraphLeft.setLineThickness(MARKER_LINE_WIDTH);
//
//                F1D convFuncRight = new F1D("convFuncRight", "[a]+[b]*x", FIT_MIN[layer], FIT_MAX[layer]);
//                GraphErrors convGraphRight = new GraphErrors("convGraphRight");
//                convGraphRight.setName("convGraphRight");
//                convFuncRight.setLineColor(FUNC_COLOUR);
//                convFuncRight.setLineWidth(FUNC_LINE_WIDTH);
//                convGraphRight.setMarkerSize(MARKER_SIZE);
//                convGraphRight.setLineThickness(MARKER_LINE_WIDTH);

                // create all the histograms
                TOFH1F timeLHist = new TOFH1F("timeL",
                        "TimeL Sector " + sector + " Layer " + layer + " Component" + component,
                        HIST_BINS, HIST_X_MIN, HIST_X_MAX);
                TOFH1F timeRHist = new TOFH1F("timeR",
                        "TimeR Sector " + sector + " Layer " + layer + " Component" + component,
                        HIST_BINS, HIST_X_MIN, HIST_X_MAX);

                DataGroup dg = new DataGroup(2, 1);
                dg.addDataSet(timeLHist, TIMEL);
                dg.addDataSet(timeRHist, TIMER);

                dataGroups.add(dg, sector, layer, component);

                setPlotTitle(sector, layer, component);

                // initialize the constants array
                Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};                
                // override values
                
                constants.add(consts, sector, layer, component);


            }
        }
    }

    @Override
    public void processEvent(DataEvent event) {

        //DataProvider dp = new DataProvider();
        List<CNDPaddlePair> paddlePairList = DataProvider.getPaddlePairList(event);
        processPaddlePairList(paddlePairList);
    }

    @Override
    public void processPaddlePairList(List<CNDPaddlePair> paddlePairList) {

        for (CNDPaddlePair paddlePair : paddlePairList) {
            
            if (paddlePair.includeInCalib()){

                int sector = paddlePair.getDescriptor().getSector();
                int layer = paddlePair.getDescriptor().getLayer();
                int component = paddlePair.getDescriptor().getComponent();

                dataGroups.getItem(sector,layer,component).getH1F("timeL").fill(paddlePair.tdcToTime(paddlePair.TDCL));
                dataGroups.getItem(sector,layer,component).getH1F("timeR").fill(paddlePair.tdcToTime(paddlePair.TDCR));

                //Don't fill anything for CND right now...
    //            if (paddlePair.goodTrackFound() && paddlePair.TDCL!=0 && paddlePair.TDCR!=0) {
    //                dataGroups.getItem(sector,layer,component).getH2F("tdcConvLeft").fill(
    //                         paddlePair.TDCL, 
    //                         (paddlePair.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
    //                dataGroups.getItem(sector,layer,component).getH2F("tdcConvRight").fill(
    //                        paddlePair.TDCR, 
    //                        (paddlePair.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
    //
    //            }
            }
        }
    }
    
	@Override
	public void timerUpdate() {
		if (fitMethod!=FIT_METHOD_SF) {
			// only analyze at end of file for slice fitter - takes too long
			analyze();
		}
		save();
		calib.fireTableDataChanged();
	}

    @Override
    public void fit(int sector, int layer, int paddle,
            double minRange, double maxRange) {
        
//            JOptionPane.showMessageDialog(new JPanel(),
//                "Nothing to fit for this tab...");        
        
//        System.out.println("Nothing to fit for TDC conv");
        
//
//        H2F convHistL = dataGroups.getItem(sector,layer,paddle).getH2F("tdcConvLeft");
//        H2F convHistR = dataGroups.getItem(sector,layer,paddle).getH2F("tdcConvRight");
//        GraphErrors convGraphLeft = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("convGraphLeft");
//        GraphErrors convGraphRight = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("convGraphRight");
//
//        // fit function to the graph of means
//        if (fitMethod==FIT_METHOD_SF && sector==2) {
//			ParallelSliceFitter psfL = new ParallelSliceFitter(convHistL);
//			psfL.setFitMode(fitMode);
//			psfL.setMinEvents(fitMinEvents);
//			psfL.setBackgroundOrder(backgroundSF);
//			psfL.setNthreads(1);
//			//psfL.setShowProgress(false);
//			setOutput(false);
//			psfL.fitSlicesX();
//			setOutput(true);
//			if (showSlices) {
//				psfL.inspectFits();
//			}
//			convGraphLeft.copy(fixGraph(psfL.getMeanSlices(),"convGraphLeft"));
//
//			ParallelSliceFitter psfR = new ParallelSliceFitter(convHistR);
//			psfR.setFitMode(fitMode);
//			psfR.setMinEvents(fitMinEvents);
//			psfR.setBackgroundOrder(backgroundSF);
//			psfR.setNthreads(1);
//			//psfR.setShowProgress(false);
//			setOutput(false);
//			psfR.fitSlicesX();
//			setOutput(true);
//			if (showSlices) {
//				psfR.inspectFits();
//				showSlices = false;
//			}
//			convGraphRight.copy(fixGraph(psfR.getMeanSlices(),"convGraphRight"));
//		}
//		else if (fitMethod==FIT_METHOD_MAX) {
//			convGraphLeft.copy(maxGraph(convHistL, "convGraphLeft"));
//			convGraphRight.copy(maxGraph(convHistR, "convGraphRight"));
//		}
//		else {
//			convGraphLeft.copy(convHistL.getProfileX());
//			convGraphRight.copy(convHistR.getProfileX());
//		}
//
//        // find the range for the fit
//        double lowLimit;
//        double highLimit;
//
//        if (minRange != UNDEFINED_OVERRIDE) {
//            // use custom values for fit
//            lowLimit = minRange;
//        }
//        else {
//            lowLimit = FIT_MIN[layer];
//        }
//
//        if (maxRange != UNDEFINED_OVERRIDE) {
//            // use custom values for fit
//            highLimit = maxRange;
//        }
//        else {
//            highLimit = FIT_MAX[layer];
//        }
//
//        F1D convFuncLeft = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncLeft");
//        convFuncLeft.setRange(lowLimit, highLimit);
//
//        convFuncLeft.setParameter(0, 0.0);
//        convFuncLeft.setParameter(1, 0.0);
//        try {
//            DataFitter.fit(convFuncLeft, convGraphLeft, fitOption);
//        } catch (Exception e) {
//            System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
//            e.printStackTrace();
//        }
//        
//        F1D convFuncRight = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight");
//        convFuncRight.setRange(lowLimit, highLimit);
//
//        convFuncRight.setParameter(0, 0.0);
//        convFuncRight.setParameter(1, 0.0);
//        try {
//            DataFitter.fit(convFuncRight, convGraphRight, fitOption);
//        } catch (Exception e) {
//            System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
//            e.printStackTrace();
//        }
//        
    }

    public void customFit(int sector, int layer, int paddle){

        JOptionPane.showMessageDialog(new JPanel(),
                "Nothing to fit for this tab...");     
        
//        System.out.println("Nothing to fit for TDC conv");
    
    }

    public Double getConvLeft(int sector, int layer, int paddle) {

        double conv = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[OVERRIDE_LEFT];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            conv = overrideVal;
        }
        else {
            double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncLeft") 
                    .getParameter(1);
            conv = EXPECTED_SLOPE - gradient;
        }
        return conv;
    }

    public Double getConvRight(int sector, int layer, int paddle) {

        double conv = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[OVERRIDE_RIGHT];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            conv = overrideVal;
        }
        else {
            double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight") 
                    .getParameter(1);
            conv = EXPECTED_SLOPE - gradient;
        }
        return conv;
    }  
    

    public double LeadingEdge(H1F hist, double fitThreshold) {

        //*************************************
        //****Now find leading edge point
        int leading_bin = 0;
        double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
        double meanOfDistribution = hist.getMean();
        int binOfMeanOfDistribution = hist.getAxis().getBin(meanOfDistribution);

        for (int i = 0; i < binOfMeanOfDistribution; i++) {
            //****Find leading edge bin with the threshold height
            if (hist.getBinContent(i) < (heightThreshold) && hist.getBinContent(i + 1) >= (heightThreshold)) {
                leading_bin = i;
                break;
            }
        }

        return hist.getAxis().getBinCenter(leading_bin);

    }    
    
    public Double getLeadingEdgeLeft(int sector, int layer, int component) {

        double leadingEdgeLeft = LeadingEdge(dataGroups.getItem(sector, layer, component).getH1F("timeL"), FIT_HEIGHT_THRESHOLD);

        return leadingEdgeLeft;
    }
    
    public Double getLeadingEdgeRight(int sector, int layer, int component) {

        double leadingEdgeRight = LeadingEdge(dataGroups.getItem(sector, layer, component).getH1F("timeR"), FIT_HEIGHT_THRESHOLD);

        return leadingEdgeRight;
    }

    @Override
    public void saveRow(int sector, int layer, int component) {
//        calib.setDoubleValue(getConvLeft(sector,layer,paddle),
//                "tdc_conv_left", sector, layer, paddle);
//        calib.setDoubleValue(getConvRight(sector,layer,paddle),
//                "tdc_conv_right", sector, layer, paddle);
        
        calib.setDoubleValue(EXPECTED_SLOPE,                        
                        "slope_L", sector, layer, component);
        calib.setDoubleValue(EXPECTED_OFFSET,
                        "offset_L", sector, layer, component);        
        calib.setDoubleValue(EXPECTED_SLOPE,                        
                        "slope_R", sector, layer, component);
        calib.setDoubleValue(EXPECTED_OFFSET,
                        "offset_R", sector, layer, component);        

    }

    @Override
    public boolean isGoodPaddle(int sector, int layer, int paddle) {
        
        return (true);

//        return (getConvLeft(sector,layer,paddle) >= EXPECTED_SLOPE*(1-ALLOWED_DIFF)
//                &&
//                getConvLeft(sector,layer,paddle) <= EXPECTED_SLOPE*(1+ALLOWED_DIFF)
//                &&
//                getConvRight(sector,layer,paddle) >= EXPECTED_SLOPE*(1-ALLOWED_DIFF)
//                &&
//                getConvRight(sector,layer,paddle) <= EXPECTED_SLOPE*(1+ALLOWED_DIFF)
//                );

    }

    @Override
    public boolean isGoodComponent(int sector, int layer, int component) {
        
        return (true);

    }    
    
    @Override
    public void setPlotTitle(int sector, int layer, int component) {
		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector,layer,component).getH1F("timeL").setTitle("TimeL - S"+sector+" L"+layer+" C"+component);
		dataGroups.getItem(sector,layer,component).getH1F("timeR").setTitle("TimeR - S"+sector+" L"+layer+" C"+component);          
		dataGroups.getItem(sector,layer,component).getH1F("timeL").setTitleX("Time (ns)");
		dataGroups.getItem(sector,layer,component).getH1F("timeR").setTitleX("Time (ns)");
		dataGroups.getItem(sector,layer,component).getH1F("timeL").setTitleY("Counts");
		dataGroups.getItem(sector,layer,component).getH1F("timeR").setTitleY("Counts");

    }

    @Override
    public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		if (showPlotType == "TIMEL") {

			H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("timeL");
//			fitHist.setTitleX("");
			canvas.draw(fitHist);                    
                    
//			GraphErrors graph = dataGroups.getItem(sector,layer,component).getGraph("convGraphLeft");
//			if (graph.getDataSize(0) != 0) {
//				graph.setTitleX("");
//				graph.setTitleY("");
//				canvas.draw(graph);
//				canvas.draw(dataGroups.getItem(sector,layer,component).getF1D("convFuncLeft"), "same");
//			}
		}
		else if (showPlotType == "TIMER") {
//			GraphErrors graph = dataGroups.getItem(sector,layer,paddle).getGraph("convGraphRight");
//			if (graph.getDataSize(0) != 0) {
//				graph.setTitleX("");
//				graph.setTitleY("");
//				canvas.draw(graph);
//				canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight"), "same");
//			}
                        
                        H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("timeR");
//			fitHist.setTitleX("");
			canvas.draw(fitHist);    
                        
		}
    }
    
	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "TIMEL";
		stepName = "TDC conv - Left";
		super.showPlots(sector, layer);
		showPlotType = "TIMER";
		stepName = "TDC conv - Right";
		super.showPlots(sector, layer);

	}    

            @Override
    public DataGroup getSummary() {

        double[] sectorNumber = new double[24];
        double[] zeroUncs = new double[24];        
        double[] leadingEdgeL3L = new double[24];
        double[] leadingEdgeL3R = new double[24];
        double[] leadingEdgeL2L = new double[24];
        double[] leadingEdgeL2R = new double[24];
        double[] leadingEdgeL1L = new double[24];
        double[] leadingEdgeL1R = new double[24];


        for (int sector = 1; sector <= 24; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int component = 1;

                sectorNumber[sector - 1] = (double) sector;
                zeroUncs[sector - 1] = 0.0;

                if (layer == 3){
                    leadingEdgeL3L[sector - 1] = getLeadingEdgeLeft(sector, layer, component);
                    leadingEdgeL3R[sector - 1] = getLeadingEdgeRight(sector, layer, component);
                } else if (layer == 2){
                    leadingEdgeL2L[sector - 1] = getLeadingEdgeLeft(sector, layer, component);
                    leadingEdgeL2R[sector - 1] = getLeadingEdgeRight(sector, layer, component);
                } else if (layer == 1){
                    leadingEdgeL1L[sector - 1] = getLeadingEdgeLeft(sector, layer, component);
                    leadingEdgeL1R[sector - 1] = getLeadingEdgeRight(sector, layer, component);
                }
            }
        }

        GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
                leadingEdgeL3L, zeroUncs, zeroUncs);

        allL3L.setTitleX("Sector Number");
        allL3L.setTitleY("Leading edge (ns)");
        allL3L.setMarkerSize(MARKER_SIZE);
        allL3L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
                leadingEdgeL3R, zeroUncs, zeroUncs);

        allL3R.setTitleX("Sector Number");
        allL3R.setTitleY("Leading edge (ns)");
        allL3R.setMarkerSize(MARKER_SIZE);
        allL3R.setLineThickness(MARKER_LINE_WIDTH);
        
        GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
                leadingEdgeL2L, zeroUncs, zeroUncs);

        allL2L.setTitleX("Sector Number");
        allL2L.setTitleY("Leading edge (ns)");
        allL2L.setMarkerSize(MARKER_SIZE);
        allL2L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
                leadingEdgeL2R, zeroUncs, zeroUncs);

        allL2R.setTitleX("Sector Number");
        allL2R.setTitleY("Leading edge (ns)");
        allL2R.setMarkerSize(MARKER_SIZE);
        allL2R.setLineThickness(MARKER_LINE_WIDTH);
        
        GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
                leadingEdgeL1L, zeroUncs, zeroUncs);

        allL1L.setTitleX("Sector Number");
        allL1L.setTitleY("Leading edge (ns)");
        allL1L.setMarkerSize(MARKER_SIZE);
        allL1L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
                leadingEdgeL1R, zeroUncs, zeroUncs);

        allL1R.setTitleX("Sector Number");
        allL1R.setTitleY("Leading edge (ns)");
        allL1R.setMarkerSize(MARKER_SIZE);
        allL1R.setLineThickness(MARKER_LINE_WIDTH);           

        DataGroup dg = new DataGroup(2, 3);
        dg.addDataSet(allL3L, 0);
        dg.addDataSet(allL3R, 1);
        dg.addDataSet(allL2L, 2);
        dg.addDataSet(allL2R, 3);
        dg.addDataSet(allL1L, 4);
        dg.addDataSet(allL1R, 5);
        return dg;

    }

        
//    @Override
//    public DataGroup getSummary(int sector, int layer) {
//
//        int layer_index = layer-1;
//        double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
//        double[] convLefts = new double[NUM_PADDLES[layer_index]];
//        double[] convRights = new double[NUM_PADDLES[layer_index]];
//        double[] zeroUncs = new double[NUM_PADDLES[layer_index]];
//
//        for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {
//
//            paddleNumbers[p - 1] = (double) p;
//            convLefts[p - 1] = getConvLeft(sector, layer, p);
//            zeroUncs[p - 1] = 0.0;
//            convRights[p - 1] = getConvRight(sector, layer, p);
//        }
//
//        GraphErrors summL = new GraphErrors("convLeftSumm", paddleNumbers,
//                convLefts, zeroUncs, zeroUncs);
//
//        summL.setTitleX("Paddle Number");
//        summL.setTitleY("TDC Conv Left (ns)");
//        summL.setMarkerSize(MARKER_SIZE);
//        summL.setLineThickness(MARKER_LINE_WIDTH);
//
//        GraphErrors summR = new GraphErrors("convRightSumm", paddleNumbers,
//                convRights, zeroUncs, zeroUncs);
//
//        summR.setTitleX("Paddle Number");
//        summR.setTitleY("TDC Conv Right (ns)");
//        summR.setMarkerSize(MARKER_SIZE);
//        summR.setLineThickness(MARKER_LINE_WIDTH);
//
//        DataGroup dg = new DataGroup(2,1);
//        dg.addDataSet(summL, 0);
//        dg.addDataSet(summR, 1);
//        return dg;
//
//    }
//    
    @Override
	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
		
//    	canvas.getPad(2).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);
//    	canvas.getPad(3).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);
    	
	}

}
