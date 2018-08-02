package org.jlab.calib.services.cnd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.utils.groups.IndexedList;

public class CNDTimeOffsetsRFEventListener extends CNDCalibrationEngine {

    // indices for constants
    public final int OFFSET_OVERRIDE = 0;

    // hists
    public final int VERTEX_RF_RAW_L = 0;
    public final int VERTEX_RF_RAW_R = 1;
    public final int VERTEX_RF_L = 2;
    public final int VERTEX_RF_R = 3;

    private String showPlotType = "VERTEX_RF_L";

    private String fitOption = "RQ";

    public CNDTimeOffsetsRFEventListener() {

        stepName = "RF Offset";
        fileNamePrefix = "CND_CALIB_TIMEOFFSETS_RF";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "time_offset_RF_L/F:time_offset_RF_R/F");

        calib.setName("/calibration/cnd/TimeOffsets_RF");

        calib.setPrecision(3);

        // assign constraints
//        calib.addConstraint(3, -BEAM_BUCKET / 2.0, BEAM_BUCKET / 2.0);
//        calib.addConstraint(4, -BEAM_BUCKET / 2.0, BEAM_BUCKET / 2.0);

    }

    @Override
    public void populatePrevCalib() {

        System.out.println("Populating " + stepName + " previous calibration values");
        if (calDBSource == CAL_FILE) {

            String line = null;
            try {
                System.out.println("File: " + prevCalFilename);
                // Open the file
                FileReader fileReader
                        = new FileReader(prevCalFilename);

                // Always wrap FileReader in BufferedReader
                BufferedReader bufferedReader
                        = new BufferedReader(fileReader);

                line = bufferedReader.readLine();

                while (line != null) {

                    String[] lineValues;
                    lineValues = line.split(" ");

                    int sector = Integer.parseInt(lineValues[0]);
                    int layer = Integer.parseInt(lineValues[1]);
                    int paddle = Integer.parseInt(lineValues[2]);
                    double rfL = Double.parseDouble(lineValues[3]);
                    double rfR = Double.parseDouble(lineValues[4]);

                    rfOffsetValues.addEntry(sector, layer, paddle);
                    rfOffsetValues.setDoubleValue(rfL,
                            "time_offset_RF_L", sector, layer, paddle);
                    rfOffsetValues.setDoubleValue(rfR,
                            "time_offset_RF_R", sector, layer, paddle);

                    line = bufferedReader.readLine();
                }

                bufferedReader.close();
            } catch (FileNotFoundException ex) {
                System.out.println(
                        "Unable to open file '"
                        + prevCalFilename + "'");
                return;
            } catch (IOException ex) {
                System.out.println(
                        "Error reading file '"
                        + prevCalFilename + "'");
                return;
            }
        } else if (calDBSource == CAL_DEFAULT) {
            System.out.println("Default");
            for (int sector = 1; sector <= 6; sector++) {
                for (int layer = 1; layer <= 3; layer++) {
                    int layer_index = layer - 1;
                    for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
                        rfOffsetValues.addEntry(sector, layer, paddle);
                        rfOffsetValues.setDoubleValue(0.0,
                                "time_offset_RF_L", sector, layer, paddle);
                        rfOffsetValues.setDoubleValue(0.0,
                                "time_offset_RF_R", sector, layer, paddle);
                    }
                }
            }
        } else if (calDBSource == CAL_DB) {
            System.out.println("Database Run No: " + prevCalRunNo);
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            rfOffsetValues = dcp.readConstants("/calibration/cnd/timing_offset_rf");
            dcp.disconnect();
        }
        prevCalRead = true;
        System.out.println(stepName + " previous calibration values populated successfully");
    }

    @Override
    public void resetEventListener() {

        // perform init processing
        // create the histograms for the first iteration
        createHists();
    }

    public void createHists() {

        for (int sector = 1; sector <= 24; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int component = 1;

                DataGroup dg = new DataGroup(2, 2);

                // create all the histograms and functions
                H1F fineHistRaw_L
                        = new H1F("fineHistRaw_L", "Fine offset Sector " + sector + " Layer " + " Component" + component,
                                160, -BEAM_BUCKET/2.,BEAM_BUCKET/2.);
                fineHistRaw_L.setTitleX("RF time - vertex time modulo beam bucket (ns)");
                dg.addDataSet(fineHistRaw_L, 0);

                H1F fineHistRaw_R
                        = new H1F("fineHistRaw_R", "Fine offset Sector " + sector + " Layer " + " Component" + component,
                                320, -BEAM_BUCKET,BEAM_BUCKET);
                fineHistRaw_R.setTitleX("vertex time (ns)");
                dg.addDataSet(fineHistRaw_R, 1);

//                H1F fineHist_L
//                        = new H1F("fineHist_L", "Fine offset Sector " + sector + " Layer " + " Component" + component,
//                                160, -2.0, 2.0);
//                fineHist_L.setTitleX("RF time - vertex time modulo beam bucket (ns)");
//                dg.addDataSet(fineHist_L, 2);
//
//                H1F fineHist_R
//                        = new H1F("fineHist_R", "Fine offset Sector " + sector + " Layer " + " Component" + component,
//                                160, -2.0, 2.0);
//                fineHist_R.setTitleX("RF time - vertex time modulo beam bucket (ns)");
//                dg.addDataSet(fineHist_R, 3);

             
                  F1D fineFunc_L = new F1D("fineFunc_L", "[amp]*gaus(x,[mean],[sigma])+[c]", -4.0, 4.0);
                fineFunc_L.setLineColor(FUNC_COLOUR);
                fineFunc_L.setLineWidth(FUNC_LINE_WIDTH);
                dg.addDataSet(fineFunc_L, VERTEX_RF_L);
//                fineFunc_L.setOptStat(1110);
//
//                // create a dummy function in case there's no data to fit 
//                F1D fineFunc_R = new F1D("fineFunc_R", "[amp]*gaus(x,[mean],[sigma])+[a]*x^2+[b]*x+[c]", -1.0, 1.0);
//                fineFunc_R.setLineColor(FUNC_COLOUR);
//                fineFunc_R.setLineWidth(FUNC_LINE_WIDTH);
//                dg.addDataSet(fineFunc_R, VERTEX_RF_R);
//                fineFunc_R.setOptStat(1110);

                dataGroups.add(dg, sector, layer, component);

                // initialize the constants array
                Double[] consts = {UNDEFINED_OVERRIDE};
                // override values
                constants.add(consts, sector, layer, component);
            }
        }
    }

    @Override
    public void processEvent(DataEvent event) {

        List<CNDPaddlePair> paddlePairList = DataProvider.getPaddlePairList(event);
        processPaddlePairList(paddlePairList);

    }

    @Override
    public void processPaddlePairList(List<CNDPaddlePair> paddlePairList) {

        for (CNDPaddlePair paddlePair : paddlePairList) {

            if (paddlePair.includeInCalib()) {

                int sector = paddlePair.getDescriptor().getSector();
                int layer = paddlePair.getDescriptor().getLayer();
                int component = paddlePair.getDescriptor().getComponent();

                double timeOffset = CNDCalibrationEngine.layerValues.getDoubleValue("time_offset_layer", sector, layer, component);
                //System.out.println("timeOffset " + timeOffset);
                //			System.out.println("SLC "+sector+layer+component);
                //			pad.show();
                // fill the fine hists
                //				System.out.println("Paddle included");
                
//                System.out.println("Fill with = " + ((paddlePair.refTime() + (1000 * BEAM_BUCKET) + (0.5 * BEAM_BUCKET)) % BEAM_BUCKET - 0.5 * BEAM_BUCKET ));
                if(paddlePair.refTime()!=-1000. && paddlePair.CHARGE==-1){
                	//System.out.println(paddlePair.refTime());
                dataGroups.getItem(sector, layer, component).getH1F("fineHistRaw_L").fill(
                        (paddlePair.refTime()) % BEAM_BUCKET - 0.5 * BEAM_BUCKET);
               // 
                dataGroups.getItem(sector, layer, component).getH1F("fineHistRaw_R").fill(
                        (paddlePair.layerOffset()-timeOffset));
               
                }
            }
        }
    }

//    @Override
//    public void fit(int sector, int layer, int paddle, double minRange, double maxRange) {
//
//    }

    @Override
    public void fit(int sector, int layer, int paddle, double minRange, double maxRange) {

    	System.out.println("here");
        H1F rawHist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHistRaw_L");
        H1F fineHist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHistRaw_R");

        // move the histogram content to +/- half beam bucket around the peak
        fineHist.reset();
        int maxBin = rawHist.getMaximumBin();
        double maxPos = rawHist.getXaxis().getBinCenter(maxBin);
        int startBin = rawHist.getXaxis().getBin(BEAM_BUCKET * -0.5);
        int endBin = rawHist.getXaxis().getBin(BEAM_BUCKET * 0.5);

        for (int rawBin = startBin; rawBin < endBin; rawBin++) {

            double rawBinCenter = rawHist.getXaxis().getBinCenter(rawBin);
            int fineHistOldBin = fineHist.getXaxis().getBin(rawBinCenter);
            int fineHistNewBin = fineHist.getXaxis().getBin(rawBinCenter);
            double valueBin = rawHist.getBinContent(rawBin);
            double newBinCenter = 0.0;

            if (rawBinCenter > maxPos + 0.5 * BEAM_BUCKET) {
                newBinCenter = rawBinCenter - BEAM_BUCKET;
                fineHistNewBin = fineHist.getXaxis().getBin(newBinCenter);
            }
            if (rawBinCenter < maxPos - 0.5 * BEAM_BUCKET) {
                newBinCenter = rawBinCenter + BEAM_BUCKET;
                fineHistNewBin = fineHist.getXaxis().getBin(newBinCenter);
            }

            fineHist.setBinContent(fineHistOldBin, 0.0);
            fineHist.setBinContent(fineHistNewBin, valueBin);
            System.out.println(valueBin);
        }

        // fit gaussian +p2
        F1D fineFunc = dataGroups.getItem(sector, layer, paddle).getF1D("fineFunc_L");

        // find the range for the fit
        double lowLimit;
        double highLimit;
        if (minRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            lowLimit = minRange;
        } else {
            lowLimit = maxPos - 1.;
        }
        if (maxRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            highLimit = maxRange;
        } else {
            highLimit = maxPos + 1.;
        }

        fineFunc.setRange(lowLimit, highLimit);
        fineFunc.setParameter(0, fineHist.getBinContent(maxBin));
        //fineFunc.setParLimits(0, 0, fineHist.getBinContent(maxBin) + 1.5);
        fineFunc.setParameter(1, maxPos);
        fineFunc.setParameter(2, 0.3);
        fineFunc.setParameter(3, fineHist.getBinContent(maxBin)/5.);

        try {
            DataFitter.fit(fineFunc, fineHist, fitOption);
            //fineHist.setTitle(fineHist.getTitle() + " Fine offset = " + formatDouble(fineFunc.getParameter(1)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    private Double formatDouble(double val) {
        return Double.parseDouble(new DecimalFormat("0.000").format(val));
    }

    @Override
    public void customFit(int sector, int layer, int paddle) {
//
        String[] fields = {"Min range for fit:", "Max range for fit:", "SPACE"
            //				"Amp:", "Mean:", "Sigma:", "Offset:", "SPACE",
            };
        CNDCustomFitPanel panel = new CNDCustomFitPanel(fields, sector, layer);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "Adjust Fit / Override for paddle " + paddle, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {

            double minRange = toDouble(panel.textFields[0].getText());
            double maxRange = toDouble(panel.textFields[1].getText());
           

            // save the override values
            Double[] consts = constants.getItem(sector, layer, paddle);
         

            fit(sector, layer, paddle, minRange, maxRange);

            // update the table
            saveRow(sector, layer, paddle);
            calib.fireTableDataChanged();

        }
    }

    public Double getReso(int sector, int layer, int paddle) {

      

            F1D fineFunc = dataGroups.getItem(sector, layer, paddle).getF1D("fineFunc_L");

            double reso = 0.0;
            
                reso = fineFunc.getParameter(2);
           
        
        return reso;
    }

    @Override
    public void saveRow(int sector, int layer, int paddle) {

        calib.setDoubleValue(getReso( sector,  layer, paddle),
                "time_offset_RF_L", sector, layer, paddle);
        calib.setDoubleValue(getReso( sector,  layer,  paddle),
                "time_offset_RF_R", sector, layer, paddle);

    }

    @Override
    public void writeFile(String filename) {

        // write sigmas to a file then call the super method to write the rfpad
        try {

            String sigFilename = filename.replace("RFPAD", "RFPAD_SIGMA");
            // Open the output file
            File outputFile = new File(sigFilename);
            FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter outputBw = new BufferedWriter(outputFw);

            for (int sector = 1; sector <= 6; sector++) {
                for (int layer = 1; layer <= 3; layer++) {
                    int layer_index = layer - 1;
                    for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
                        String line = new String();
                        F1D fineFunc = dataGroups.getItem(sector, layer, paddle).getF1D("fineFunc");
                        line = sector + " " + layer + " " + paddle + " " + new DecimalFormat("0.000").format(fineFunc.getParameter(2));
                        outputBw.write(line);
                        outputBw.newLine();
                    }
                }
            }

            outputBw.close();
        } catch (IOException ex) {
            System.out.println(
                    "Error writing file '");
            // Or we could just do this: 
            ex.printStackTrace();
        }

        super.writeFile(filename);

    }

    @Override
    public void showPlots(int sector, int layer) {

        showPlotType = "VERTEX_RF";
        stepName = "RF - Vertex Time";
        super.showPlots(sector, layer);

    }

    @Override
    public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

        H1F hist = new H1F();
//        F1D func = new F1D("fineFunc");
//        if (showPlotType == "VERTEX_RF") {
//            hist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHist");
//            func = dataGroups.getItem(sector, layer, paddle).getF1D("fineFunc");
//            //func.setOptStat(0);
//            hist.setTitle("Paddle " + paddle);
//            hist.setTitleX("");
//            hist.setTitleY("");
//            canvas.draw(hist);
//            canvas.draw(func, "same");
//
//        }

        if (showPlotType == "VERTEX_RF_RAW_L") {

            hist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHistRaw_L");
//			fitHist.setTitleX("");
            canvas.draw(hist);
        } else if (showPlotType == "VERTEX_RF_RAW_R") {
            hist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHistRaw_R");
//			fitHist.setTitleX("");
            canvas.draw(hist);
        } else if (showPlotType == "VERTEX_RF_L") {
            hist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHist_L");
//			fitHist.setTitleX("");
            canvas.draw(hist);
        } else if (showPlotType == "VERTEX_RF_R") {
            hist = dataGroups.getItem(sector, layer, paddle).getH1F("fineHist_R");
//			fitHist.setTitleX("");
            canvas.draw(hist);
        }

    }

    @Override
    public boolean isGoodPaddle(int sector, int layer, int paddle) {

        return true;

    }
//    @Override
//    public boolean isGoodPaddle(int sector, int layer, int paddle) {
//
//        return (getOffset(sector, layer, paddle) >= -BEAM_BUCKET / 2.0
//                && getOffset(sector, layer, paddle) <= BEAM_BUCKET / 2.0);
//
//    }

    @Override
    public DataGroup getSummary(int sector, int layer) {

        int layer_index = layer - 1;
        double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
        double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
        double[] values = new double[NUM_PADDLES[layer_index]];
        double[] valueUncs = new double[NUM_PADDLES[layer_index]];

        for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

            paddleNumbers[p - 1] = (double) p;
            paddleUncs[p - 1] = 0.0;
            values[p - 1] = getReso(sector, layer, p);
            valueUncs[p - 1] = 0.0;
        }

        GraphErrors summ = new GraphErrors("summ", paddleNumbers,
                values, paddleUncs, valueUncs);
        summ.setMarkerSize(MARKER_SIZE);
        summ.setLineThickness(MARKER_LINE_WIDTH);

        DataGroup dg = new DataGroup(1, 1);
        dg.addDataSet(summ, 0);
        return dg;

    }
}
