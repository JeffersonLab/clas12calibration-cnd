package org.jlab.calib.services.cnd;

import org.jlab.calib.services.cnd.CNDCalibration;
import org.jlab.calib.services.cnd.CNDCalibrationEngine;
import org.jlab.detector.base.DetectorDescriptor;

/**
* CND Calibration suite
* Based on the work of Louise Clark thanks!
*
* @author  Gavin Murdoch
*/

public class CNDPaddlePair {

    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    public static String cnd = "CND";

    private DetectorDescriptor desc = new DetectorDescriptor();

    public int COMP = 0;
    
    public int ADCL = 0;
    public int ADCR = 0;
    public int TDCL = 0;
    public int TDCR = 0;
    public int iTDCL = -1;
    public int iTDCR = -1;
    public float ADC_TIMEL = 0;
    public float ADC_TIMER = 0;
    public double XPOSt = 0.0; // calculated from CND info
    public double YPOSt = 0.0;
    public double ZPOSt = 0.0;
    public double XPOS = 0.0; // extracted from CVT
    public double YPOS = 0.0;
    public double ZPOS = 0.0;
    public double T_LENGTH = 0.0;
    public double PATH_LENGTH = 0.0;
    public double BETA = 0.0;
    public double P = 0.0;
    public double P_t = 0.0;
    public int TRACK_ID = -1;
    public double VERTEX_Z = 0.0;
    public double TRACK_REDCHI2 = 0.0;
    public int CHARGE = 0;
    public double RF_TIME = 124.25;
    public int TRIGGER_BIT = 0;
    public double TOF_TIME = 0.0;
    public int PARTICLE_ID = -1;
    public double EVENT_START_TIME = 0.0;
    public double TIME_STAMP = 0.0;

    public static final int PID_ELECTRON = 11;
    public static final int PID_PION = 211;
    private final double C = 29.98;
    public static final double NS_PER_CH = 0.0234;  //Changed from Louise's value of 0.02345

    //Unallowed values of ADC and TDC
    public static int unallowedADCValue = 0;
    public static int[] unallowedTDCValues = {0, 500000};
    //public static int[] unallowedTDCValues = {13000, 17000};  //Min and max value from GEMC simulations...
//        public static int[] unallowedTDCValues = {5299, 6068};  //Min and max value from visual estimate...

    //Threshold ADC values
    public static int thresholdIndirectHitADCValue = 0;  //Calib challenge of 100 instead of 230?

    public CNDPaddlePair(int sector, int layer, int component) {
        this.desc.setSectorLayerComponent(sector, layer, component);
    }

    public void setAdcTdc(int adcL, int adcR, int tdcL, int tdcR) {
        this.ADCL = adcL;
        this.ADCR = adcR;
        this.TDCL = tdcL;
        this.TDCR = tdcR;
    }

    public void setPos(double xPos, double yPos, double zPos) {
        this.XPOS = xPos;
        this.YPOS = yPos;
        this.ZPOS = zPos;
    }

    public boolean includeInCalib() {

        // Calibration is currently mainly set up to process hits in which:
        // all TDC values are within min and max of TDC window
        // ADCL and ADCR are above some user hard coded threshold to help remove background
        // Require a matched CVT track
    	
        if (CNDCalibration.CVTmatchI == 1) {
        	 
        	return (
            		goodTrackFound()
            		
//            		ADCL > thresholdIndirectHitADCValue
//                    && ADCR > thresholdIndirectHitADCValue
//                    && TDCL > unallowedTDCValues[0]
//                    && TDCL < unallowedTDCValues[1]
//                    && TDCR > unallowedTDCValues[0]
//                    && TDCR < unallowedTDCValues[1]
//                   // && ZPOS!=0.0	
//                    && TRACK_ID > -1
////                   && P_t > 1.0
//                    && TRACK_REDCHI2<50
////                   && iTDCL >0
////                   && iTDCR >0
                   );
           
        } else if (DataProvider.requireNOMatchedCVTTrack == true) {
                return (
                		ADCL > thresholdIndirectHitADCValue
                        && ADCR > thresholdIndirectHitADCValue
                        && TDCL > unallowedTDCValues[0]
                        && TDCL < unallowedTDCValues[1]
                        && TDCR > unallowedTDCValues[0]
                        && TDCR < unallowedTDCValues[1]
                        && TRACK_ID == -2

                       );
            } else {
            return (true
//            		ADCL > thresholdIndirectHitADCValue
//                    && ADCR > thresholdIndirectHitADCValue
//                    && TDCL > unallowedTDCValues[0]
//                    && TDCL < unallowedTDCValues[1]
//                    && TDCR > unallowedTDCValues[0]
//                    && TDCR < unallowedTDCValues[1]
                    //&& Math.abs(TDCL-TDCR) <200
                    //&& TRACK_ID == -1
//                    && iTDCL >0
//                    && iTDCR >0
                    );
            }
        }
       // for no cuts
    	//return true;

    

    //	public boolean includeInCtofVeff() {
    //		// exclude if position is zero or veff is unrealistic
    //		return (this.ZPOS != 0)
    //				&& (Math.abs(position() - this.zPosCTOF()) < 20.0);
    //	}
//    public void setcomp(int c){
//    	this.COMP=c;
//    }
    
    public int comp(){
    	return this.COMP;
    }
    
    public double uturnTloss() {

        double uturnTloss = 0.0;
        double uturnTloss1 = 0.0;

        if (this.desc.getLayer() == 1) {
            uturnTloss = 0.6;
        }
        if (this.desc.getLayer() == 2) {
            uturnTloss = 1.2;
        }
        if (this.desc.getLayer() == 3) {
            uturnTloss = 1.7;
        }
        
       
        
        if (cnd == "CND" ) {
                    uturnTloss1 = CNDCalibrationEngine.uturnTlossValues.getDoubleValue("uturn_tloss",
                                    desc.getSector(), desc.getLayer(), desc.getComponent());
                    if(uturnTloss1!=0){
                    	uturnTloss=uturnTloss1;
                    }
        }
            
            
        return uturnTloss;
    }

    public double veff(int paddle) {

        double veff = 16.0;
        double veff1 = 16.0;
        
        // Assume left paddle
        String constantColumnName = "veff_L";

        if (paddle == 2) {
            constantColumnName = "veff_R";
        }

        if (cnd == "CND" ) {
                    veff1 = CNDCalibrationEngine.veffValues.getDoubleValue(constantColumnName,
                                    desc.getSector(), desc.getLayer(), desc.getComponent());
                    if(veff1!=0.0){
                    	veff=veff1;
                    }
                    //System.out.println("veff "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
        }
        return veff;
    }

    public double veff() {
        double veff = 16.0;
        System.out.println("in vf ");
        if (cnd == "CND") {
            veff = CNDCalibrationEngine.veffValues.getDoubleValue("veff_left",
                    desc.getSector(), desc.getLayer(), desc.getComponent());
            //System.out.println("veff "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
        }
        System.out.println("out vf ");

        return veff;
    }

    public double LRoffset(){
    	double LRoffset =0.0;
    	double LRoffset1 =0.0;
    	
    	 if (cnd == "CND" ) {
    		 LRoffset1 = CNDCalibrationEngine.leftRightValues.getDoubleValue("time_offset_LR",
                     desc.getSector(), desc.getLayer(), desc.getComponent());
    		 if(LRoffset1!=0.0){
    			 LRoffset=LRoffset1;
    		 }
    		 
             //System.out.println("veff "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
         }
    	
    	return LRoffset;
    }
    
    public double LRoffsetad(){
    	double LRoffset =0.0;
    	double LRoffset1 =0.0;
    	
    	 if (cnd == "CND" ) {
    		 LRoffset1 = CNDCalibrationEngine.uturnTlossValues.getDoubleValue("adjusted_LR_offset",
                     desc.getSector(), desc.getLayer(), desc.getComponent());
    		 if(LRoffset1!=0.0){
    			 LRoffset=LRoffset1;
    		 }
    		 
             //System.out.println("veff "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
         }
    	
    	return LRoffset;
    }
    
    //	public double p2p() {
    //		double p2p = 0.0;
    //		if (cnd == "CND") {
    //			p2p = CNDCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle",
    //					desc.getSector(), desc.getLayer(), desc.getComponent())
    //				+ CNDCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent());
    //			//System.out.println("p2p "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+p2p);
    //		} else {
    //			p2p = 0.0;
    //			//p2p = CTOFCalibrationEngine.p2pValues.getItem(desc.getSector(), desc.getLayer(), desc.getComponent());
    //		}
    //
    //		return p2p;
    //	}
    public double rfpad() {
        double rfpad = 0.0;
        if (cnd == "CND") {
            rfpad = CNDCalibrationEngine.rfOffsetValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent());
        }
        return rfpad;
    }

    public double combinedRes() {
        double tr = 0.0;

        double timeL = tdcToTime(TDCL);
        double timeR = tdcToTime(TDCR);
        double lr = leftRightAdjustment();

        tr = ((timeL - timeR - lr) / 2)
                - (paddleY() / veff());

        return tr;
    }

    // timeResidualsADC
    // rename to timeResiduals to use this version comparing TDC time to ADC time
    public double[] timeResidualsADC(double[] lambda, double[] order, int iter) {
        double[] tr = {0.0, 0.0};

        double timeL = tdcToTime(TDCL);
        double timeR = tdcToTime(TDCR);

        tr[LEFT] = timeL - this.ADC_TIMEL;
        tr[RIGHT] = timeR - this.ADC_TIMER;

        return tr;
    }

    public double startTime() {
        double startTime = 0.0;

        double beta = 1.0;
        if (BETA != 0.0) {
            beta = BETA;
        }

        //startTime = TOF_TIME - (PATH_LENGTH/(beta*29.98));
        startTime = p2pAverageHitTime() - (PATH_LENGTH / (beta * 29.98));
        return startTime;
    }

    public double startTimeRFCorr() {
        return startTime() + rfpad();
    }

    public double p2pAverageHitTime() {

        // Code for run11 right now
//		double lr = leftRightAdjustment();
        double lr = 0.0;

//		double tL = timeLeftAfterTW() - (lr/2) 
//		- ((0.5*paddleLength() + paddleY())/this.veff());
        // Not sure what this should be for the case of the CND...
        // Since it's not symetric...
        // Maybe just comes after LR time offset, therefore can test if hit in L or R...
        int directHitInSide = 0;

        if (tdcToTime(TDCL) < tdcToTime(TDCR)) {
            directHitInSide = 1;
        } else {
            directHitInSide = 2;
        }
        double tL = 0.0;
        double tR = 0.0;

        if (directHitInSide == 1) {
            tL = tdcToTime(TDCL) - lr
                    - zPosCND() / 16.0;
            tR = tdcToTime(TDCR) - lr
                    - ((paddleLength() + zPosCND()) / 16.0);
        } else {
            tL = tdcToTime(TDCL) - lr
                    - ((paddleLength() + zPosCND()) / 16.0);
            tR = tdcToTime(TDCR) - lr
                    - zPosCND() / 16.0;
            
        }

        return (tL + tR) / 2.0;

    }

    public double tofTimeRFCorr() {
        return p2pAverageHitTime() + rfpad();
    }


    public double refTimeCorr() {
        return refTime() - rfpad();
    }

    public double deltaTLeft(double offset) {

        double lr = leftRightAdjustment();
        //		System.out.println("deltaTLeft");
        //		System.out.println("S " + desc.getSector() + " L " + desc.getLayer() + " C "
        //				+ desc.getComponent() + " ADCR " + ADCR + " ADCL " + ADCL
        //				+ " TDCR " + TDCR + " TDCL " + TDCL);

        double beta = 1.0;
        if (BETA != 0.0) {
            beta = BETA;
        }
        //		System.out.println("TDCL is "+TDCL);
        //		System.out.println("tdcToTime is "+tdcToTime(TDCL));
        //		System.out.println("lr is "+lr);
        //		System.out.println("rfpad is "+rfpad());
        //		System.out.println("paddleLength is "+paddleLength());
        //		System.out.println("paddleY is "+paddleY());
        //		System.out.println("veff is "+veff());
        //		System.out.println("PATH_LENGTH is "+PATH_LENGTH);
        //		System.out.println("beta is "+beta);
        //		System.out.println("RF_TIME is "+RF_TIME);

        double dtL = tdcToTime(TDCL) - (lr / 2) + rfpad()
                - ((0.5 * paddleLength() + paddleY()) / this.veff())
                - (PATH_LENGTH / (beta * 29.98))
                - this.RF_TIME;

        //		System.out.println("dtL is "+dtL);
        //		System.out.println("ADCL is "+ADCL);
        // subtract the value of the nominal function
        dtL = dtL - (40.0 / (Math.pow(ADCL, 0.5)));
        //        System.out.println("dtL2 is "+dtL);
        //        System.out.println("offset is "+offset);
        dtL = dtL + offset;
        //        System.out.println("dtL3 is "+dtL);
        double bb = CNDCalibrationEngine.BEAM_BUCKET;
        dtL = (dtL + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;
        //        System.out.println("dtL4 is "+dtL);

        //dtL = ((dtL +120.0)%BEAM_BUCKET);
        return dtL;
    }

    public double deltaTRight(double offset) {

        double lr = leftRightAdjustment();

        double beta = 1.0;
        if (BETA != 0.0) {
            beta = BETA;
        }

        double dtR = tdcToTime(TDCR) + (lr / 2) + rfpad()
                - ((0.5 * paddleLength() - paddleY()) / this.veff())
                - (PATH_LENGTH / (beta * 29.98))
                - this.RF_TIME;

        dtR = dtR - (40.0 / (Math.pow(ADCR, 0.5)));
        dtR = dtR + offset;
        double bb = CNDCalibrationEngine.BEAM_BUCKET;
        dtR = (dtR + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;
        //dtR = ((dtR +120.0)%BEAM_BUCKET);

        return dtR;
    }

    public double paddleLength() {

        double len = 0.0;

        if (cnd == "CND") {
            int layer = this.getDescriptor().getLayer();

            if (layer == 1) {
                len = 66.572;
            } else if (layer == 2) {
                len = 70.000;
            } else if (layer == 3) {
                len = 73.428;
            }
        } else {
            //Shouldn't reach this...
            len = 0.0;
        }

        return len;

    }
    
    public double paddleDownstreamEdge() {

        double len = 0.0;

        if (cnd == "CND") {
            int layer = this.getDescriptor().getLayer();

            if (layer == 1) {
                len = 26.976;
            } else if (layer == 2) {
                len = 31.804;
            } else if (layer == 3) {
                len = 35.232;
            }
        } else {
            //Shouldn't reach this...
            len = 0.0;
        }

        return len;

    }

    public double leftRightTimeDiff() {
        return (tdcToTime(TDCL) - tdcToTime(TDCR));
    }

    public double rightLeftTimeDiff() {
        return (tdcToTime(TDCR) - tdcToTime(TDCL));
    }

    public double leftRightTimeAverage() {
        return (0.5 * (tdcToTime(TDCL) + tdcToTime(TDCR)));
    }

    public double effVSum() {

        return (veff(1) + veff(2));
    }

    public double layerOffset() {

        double layerOffset = 0.0;

       
        double beta = 1.0;
        if (BETA != 0.0) {
            beta = BETA;
        }
        double t_tof = (PATH_LENGTH / (beta * 29.98));


        if (EVENT_START_TIME != 0.0 && t_tof!=0.0 && TRACK_ID!=-1 && TIME_STAMP!=-1) {
        	
        	double phase=4.*((TIME_STAMP+1.)%6.);
        	
            layerOffset = leftRightTimeAverage() -phase - EVENT_START_TIME - t_tof
                    - (paddleLength() / 2) * ((1 / veff(1)) + (1 / veff(2))) - (uturnTloss() / 2) - (LRoffsetad()/2);
        }
        return (layerOffset);
    }
    
    public double refTime() {
     
         double rftime =-1000.;
         double beta = 1.0;
         if (BETA != 0.0) {
             beta = BETA;
         }
         double t_tof = (PATH_LENGTH / (beta * 29.98));


         if (this.RF_TIME != 0.0 && t_tof!=0.0 && TRACK_ID!=-1) {
             rftime = leftRightTimeAverage() - this.RF_TIME - t_tof
                     - (paddleLength() / 2) * ((1 / veff(1)) + (1 / veff(2))) - (uturnTloss() / 2) - (LRoffsetad()/2);
         }
     	

          return rftime;   
     }

//	public double timeLeftAfterTW() {
//		if (cnd=="CND") {
//			return tdcToTime(TDCL) - (lamL() / Math.pow(ADCL, ordL()));
//		}
//		else {
//			return tdcToTime(TDCL);
//		}
//	}
//
//	public double timeRightAfterTW() {
//		if (cnd=="CND") {
//			return tdcToTime(TDCR) - (lamR() / Math.pow(ADCR, ordR()));
//		}
//		else {
//			return tdcToTime(TDCR);
//		}
//	}
    public boolean isValidLeftRight() {
        return (tdcToTime(TDCL) != tdcToTime(TDCR));
    }

    double tdcToTime(double value) {
        return NS_PER_CH * value;
    }

//    public double halfTimeDiff() {
//
//        double timeL = timeLeftAfterTW();
//        double timeR = timeRightAfterTW();
//        return (timeL - timeR - leftRightAdjustment()) / 2;
//    }

    public double leftRightAdjustment() {

        double lr = 0.0;

        if (cnd == "CND") {
            lr = CNDCalibrationEngine.leftRightValues.getDoubleValue("time_offset_LR",
                    desc.getSector(), desc.getLayer(), desc.getComponent());
            //System.out.println("lr "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+lr);

        } else {
            lr = CNDCalibrationEngine.leftRightValues.getDoubleValue("time_offset_LR",
                    desc.getSector(), desc.getLayer(), desc.getComponent());
            //lr = -25.0;
        }

        return lr;
    }

//    public double position() {
//
//        return halfTimeDiff() * veff();
//    }

    public double paddleY() {

        double y = 0.0;
        if (cnd == "CND") {
            int sector = desc.getSector();
            double rotation = Math.toRadians((sector - 1) * 60);
            y = YPOS * Math.cos(rotation) - XPOS * Math.sin(rotation);
        } else {
            y = zPosCTOF();
        }
        return y;
    }

    public boolean trackFound() {
        return TRACK_ID != -1;
    }

    ;

	public boolean goodTrackFound() {

        double maxRcs = 75.0;
        double minV = -10.0;
        double maxV = 10.0;
        double minP = 1.0;
        if (cnd == "CND") {
            maxRcs = CNDCalibration.maxRcs;
            minV = CNDCalibration.minV;
            maxV = CNDCalibration.maxV;
            minP = CNDCalibration.minP;
        }

        return (trackFound()
                && TRACK_REDCHI2 < maxRcs
                && VERTEX_Z > minV
                && VERTEX_Z < maxV
                && P > minP
                && chargeMatch());
    }

    public boolean chargeMatch() {

        int trackCharge = 0;
        if (cnd == "CND") {
            trackCharge = CNDCalibration.trackCharge;
        }

        return (trackCharge == CNDCalibration.TRACK_BOTH
                || trackCharge == CNDCalibration.TRACK_NEG && CHARGE == -1
                || trackCharge == CNDCalibration.TRACK_POS && CHARGE == 1);
    }

    public double zPosCTOF() {
        return ZPOS + 19.3;
    }

    public double zPosCND() {

        double hitPositionAdjustment = 0.0;

        // Hard coded values of upstream endface relative to the solenoid centre (assuming target position is vz = 0 )
        // L1 = 38.999cm
        // L2 & L3 = 38.199cm
        if (desc.getLayer() == 1) {
            hitPositionAdjustment = 39.596;
        } else if (desc.getLayer() == 2 || desc.getLayer() == 3) {
            hitPositionAdjustment = 38.196;
        }

        return ZPOS + hitPositionAdjustment;

    }
    
    public double zPostCND() {

        double hitPositionAdjustment = 0.0;

        // Hard coded values of upstream endface relative to the solenoid centre (assuming target position is vz = 0 )
        // L1 = 38.999cm
        // L2 & L3 = 38.199cm
        if (desc.getLayer() == 1) {
            hitPositionAdjustment = 39.596;
        } else if (desc.getLayer() == 2 || desc.getLayer() == 3) {
            hitPositionAdjustment = 38.196;
        }

        return ZPOSt + hitPositionAdjustment;

    }

    public DetectorDescriptor getDescriptor() {
        return this.desc;
    }

    public String toString() {
        return "S " + desc.getSector() + " L " + desc.getLayer() + " C "
                + desc.getComponent() + " ADCR " + ADCR + " ADCL " + ADCL
                + " TDCR " + TDCR + " TDCL " + TDCL;
    }

    public void show() {
        System.out.println("");
        System.out.println("S " + desc.getSector() + " L " + desc.getLayer() + " C "
                + desc.getComponent() + " ADCL " + ADCL + " ADCR " + ADCR
                + " TDCL " + TDCL + " TDCR " + TDCR + " iTDCL " + iTDCL + " iTDCR " + iTDCR);
        System.out.println("XPOS " + XPOS + " YPOS " + YPOS + " ZPOS " + ZPOS + " PATH_LENGTH " + PATH_LENGTH + " T_LENGTH " + T_LENGTH + " TRACK_ID " + TRACK_ID);
        System.out.println("BETA " + BETA + " P " + P + " RF_TIME " + RF_TIME + " TOF_TIME " + TOF_TIME);
//		System.out.println("VERTEX_Z "+VERTEX_Z+" TRACK_REDCHI2 "+TRACK_REDCHI2+" CHARGE "+CHARGE+" TRIGGER_BIT "+TRIGGER_BIT);
        System.out.println("VERTEX_Z " + VERTEX_Z + " CHARGE " + CHARGE + " TRIGGER_BIT " + TRIGGER_BIT);
        System.out.println("paddleLength " + paddleLength());

//		System.out.println("goodTrackFound "+goodTrackFound()+" chargeMatch "+chargeMatch());
//		System.out.println("refTime "+refTime()+" startTime "+startTime()+" averageHitTime "+p2pAverageHitTime());
//		System.out.println("tofTimeRFCorr "+tofTimeRFCorr()+" startTimeRFCorr "+startTimeRFCorr());
//		System.out.println("rfpad "+rfpad()+" lamL "+lamL()+" ordL "+ordL()+" lamR "+lamR()+" ordR "+ordR()+" LR "+leftRightAdjustment()+" veff "+veff());
//		System.out.println("paddleLength "+paddleLength()+" paddleY "+paddleY());
//		System.out.println("timeLeftAfterTW "+timeLeftAfterTW()+" timeRightAfterTW "+timeRightAfterTW());
//		System.out.println("deltaTLeft "+this.deltaTLeft(0.0)+ " deltaTRight "+this.deltaTRight(0.0));
    }

}
