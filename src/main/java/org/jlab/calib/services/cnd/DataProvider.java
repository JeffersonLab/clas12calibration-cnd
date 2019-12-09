package org.jlab.calib.services.cnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.calib.temp.BaseHit;
import org.jlab.calib.temp.DetectorLocation;
import org.jlab.calib.temp.IMatchedHit;
import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.physics.RecEvent;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.component.ScintillatorMesh;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ftof.FTOFDetector;
import org.jlab.geom.detector.ftof.FTOFDetectorMesh;
import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Path3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;

/**
 * CND Calibration suite
 * Based on the work of Louise Clark thanks!
 *
 * @author  Gavin Murdoch
 */

public class DataProvider {

	//*********
	//    For piplus sim STTime will be set by default to -100, so have to use RFTIME time instead in global offset calc
	//    For Pythia STTime will be set correctly, and will be good for run11, BUT will be offset for run17 until FTOF is recalibrated
	private static boolean quaterPiPlusSim = true;  //set true if using quater PiPlus sim

	//*********
	//*********
	private static boolean test = false;  //print out contents of banks, etc.
	private static boolean isHipoFile = true;  // set whether looking at HIPO file or EVIO
	private static boolean isCookedOrGEMC = false;  //true for cooked data.  piplus sims, as there is an issue with tLength naming being all lower case

	public static boolean requireMatchedCVTTrack = false;  //Set if strictly required, or to include both matched and unmatched
	public static boolean requireNOMatchedCVTTrack = false;

	public static boolean onlyTDC = false;      // use to debug TDC to be REMOVED

	private static boolean useHitsBankProcessing = true;  //true to include hits bank processing
	private static boolean useNonHitsBankProcessing = false;  //true to include non hits bank processing (adc and tdc banks), but only if non hits bank found!

	public static List<CNDPaddlePair> getPaddlePairList(DataEvent event) {

		List<CNDPaddlePair> paddlePairList = new ArrayList<CNDPaddlePair>();

		//        if (test) {
		//event.show();
		//        }
		// HIPO or EVIO
		if (isHipoFile) {
			paddlePairList = getPaddlePairListHipo(event);
		} else {
			paddlePairList = getPaddlePairListDgtz(event);
		}

		return paddlePairList;

	}

	private static int getIdx(DataBank bank, int hitOrder, int hitSector, int hitLayer, int hitComponent) {

		int idx = -1;

		// Return first bank row matching the hit S L C
		for (int i = 0; i < bank.rows(); i++) {

			int sector = bank.getByte("sector", i);
			int layer = bank.getByte("layer", i);
			int component = bank.getShort("component", i);
			int order = bank.getByte("order", i);

			if (sector == hitSector && layer == hitLayer && component == hitComponent && order == hitOrder) {
				idx = i;
				break;
			}
		}

		return idx;

	}

	//	public static void getPaddlePairListHipo(DataEvent event){
	public static List<CNDPaddlePair> getPaddlePairListHipo(DataEvent event) {

		if (test) {

			event.show();

			if (event.hasBank("CND::adc")) {
				event.getBank("CND::adc").show();
			}
			if (event.hasBank("CND::tdc")) {
				event.getBank("CND::tdc").show();
			}

			if (event.hasBank("CND::hits")) {
				//				if (event.hasBank("CND::adc")) {
				//					event.getBank("CND::adc").show();
				//				}
				//				if (event.hasBank("CND::tdc")) {
				//					event.getBank("CND::tdc").show();
				//				}
				if (event.hasBank("CND::hits")) {
					event.getBank("CND::hits").show();
				}
				if (event.hasBank("REC::Event")) {
					event.getBank("REC::Event").show();
					// Want to use STTime
				}
				//                            if (event.hasBank("CVTRec::Tracks")) {
				//                                    event.getBank("CVTRec::Tracks").show();
				//                            }                            
			}
			//			if (event.hasBank("CVTRec::Tracks")) {
			//				event.getBank("CVTRec::Tracks").show();
			//			}
			if (event.hasBank("RUN::rf")) {
				event.getBank("RUN::rf").show();
			}
			//			if (event.hasBank("RUN::config")) {
			//				event.getBank("RUN::config").show();
			//			}
			//			if (event.hasBank("MC::Particle")) {
			//				event.getBank("MC::Particle").show();
			//			}
		}

		ArrayList<CNDPaddlePair> paddlePairList = new ArrayList<CNDPaddlePair>();

		// Only continue if we have adc and tdc banks
		if (!event.hasBank("CND::adc") || !event.hasBank("CND::tdc")) {
			return paddlePairList;
		}

		//System.out.println("ADC");
		DataBank adcBank = event.getBank("CND::adc");
		//System.out.println("TDC");
		DataBank tdcBank = event.getBank("CND::tdc");
		//DataBank RunBank = event.getBank("RUN::config");

		//adcBank.show();
		//tdcBank.show();
		//RunBank.show();

		// iterate through hits bank getting corresponding adc and tdc

		if(onlyTDC){

			//System.out.println("there");
			for (int i = 0; i < tdcBank.rows(); i++) {
				//System.out.println("here");
				int order = tdcBank.getByte("order", i);
				int tdc = tdcBank.getInt("TDC", i);
				//System.out.println(tdc);
				int sector = tdcBank.getByte("sector", i);
				int layer = tdcBank.getByte("layer", i);

				int component = 1;

				CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);

				for (int j = i+1; j < tdcBank.rows(); j++) {
					//System.out.println("here ! ");
					int orderj = tdcBank.getByte("order", j);
					int tdcj = tdcBank.getInt("TDC", j);
					int sectorj = tdcBank.getByte("sector", j);
					int layerj = tdcBank.getByte("layer", j);
					int TDCL=0;
					int TDCR=0;
					//System.out.println(tdcj);

					if(sector==sectorj && layer==layerj && Math.abs(order-orderj)==1){

						//System.out.println("here !!!!!!!!!!! ");

						if(order==2){
							TDCL=tdc;
							TDCR=tdcj;
						}
						if(order==3){
							TDCL=tdcj;
							TDCR=tdc;
						}

						//System.out.println(TDCL +" "+ TDCR);

						paddlePair.setAdcTdc(
								500,
								500,
								TDCL,
								TDCR);

						paddlePairList.add(paddlePair);

						break;

					}

				}


			}


		}


		if (event.hasBank("CND::hits") && !onlyTDC) {

			DataBank hitsBank = event.getBank("CND::hits");
			//			System.out.println();
			//			hitsBank.show();
			//			event.show();
			//			adcBank.show();
			//			tdcBank.show();

			for (int hitIndex = 0; hitIndex < hitsBank.rows(); hitIndex++) {

				int sector = (int) hitsBank.getByte("sector", hitIndex);
				int layer = (int) hitsBank.getByte("layer", hitIndex);
				//int c = (int) hitsBank.getShort("component", hitIndex);
				// ********************************
				int component = 1;

				CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);


				int adcIdx1 = hitsBank.getShort("indexLadc", hitIndex);
				int adcIdx2 = hitsBank.getShort("indexRadc", hitIndex);
				int tdcIdx1 = hitsBank.getShort("indexLtdc", hitIndex);
				int tdcIdx2 = hitsBank.getShort("indexRtdc", hitIndex);

				paddlePair.iTDCL=tdcIdx1;
				paddlePair.iTDCR=tdcIdx2;
				
				paddlePair.TIMEH=hitsBank.getFloat("time", hitIndex);

				paddlePair.setAdcTdc(
						adcBank.getInt("ADC", adcIdx1),
						adcBank.getInt("ADC", adcIdx2),
						tdcBank.getInt("TDC", tdcIdx1),
						tdcBank.getInt("TDC", tdcIdx2));


				//find the paddle using the veff and LR offset
				int c=0;
				double vL=paddlePair.veff(1);
				double vR=paddlePair.veff(2);
				double LRad=paddlePair.LRoffsetad();
				double LR=paddlePair.LRoffset();
				double offset;
				if(LRad!=0.0){
					offset=LRad;
				}
				else offset=LR;
				//System.out.println(" offset "+offset+ " LRad "+LRad +" LR "+LR+ " vl " +vL);

				double delta= paddlePair.paddleLength()*((1./vL)-(1./vR));
				double deltaR= (paddlePair.TDCL*0.0234) - ((paddlePair.TDCR*0.0234)-offset);

				if (deltaR<delta) {
					c=1;
				}
				else if (deltaR>delta) {
					c=2;
				}
				else continue;


				paddlePair.COMP=c;

				// co-ordinates of track wrt to middle of the counter from CVT
				double tx = hitsBank.getFloat("tx", hitIndex);
				double ty = hitsBank.getFloat("ty", hitIndex);
				double tz = hitsBank.getFloat("tz", hitIndex);

				paddlePair.setPos(tx, ty, tz);

				// co-ordinates of track wrt to middle of the counter from CND
				double x = hitsBank.getFloat("x", hitIndex);
				double y = hitsBank.getFloat("y", hitIndex);
				double z = hitsBank.getFloat("z", hitIndex);
				paddlePair.XPOSt=x;
				paddlePair.YPOSt=y;
				paddlePair.ZPOSt=z;

				paddlePair.ADC_TIMEL = adcBank.getFloat("time", adcIdx1);
				paddlePair.ADC_TIMER = adcBank.getFloat("time", adcIdx2);

				paddlePair.TOF_TIME = hitsBank.getFloat("time", hitIndex);
				
				// this is the way the pathlength should be retrived, in case it is wrongly calculated the line in the CVT section recalculate it
				//paddlePair.PATH_LENGTH = hitsBank.getFloat("pathlength", hitIndex);

				if (isCookedOrGEMC == true) {
					paddlePair.T_LENGTH = hitsBank.getFloat("tLength", hitIndex);
				} else {
					paddlePair.T_LENGTH = hitsBank.getFloat("tlength", hitIndex);
				}

				// Get the absolute event start time if available (from forward detetors reconstructing it)
				if (event.hasBank("REC::Event")) {
					DataBank eventRecBank = event.getBank("REC::Event");

					double vertexTrigger=0.0;
					
					if (event.hasBank("REC::Particle")) {
							DataBank particle = event.getBank("REC::Particle");
							
							vertexTrigger=particle.getFloat("vz", 0);
						
					}

					paddlePair.VERTEX_TRIGGER=vertexTrigger;
					
					
					
					// System.out.println("STTime = " + eventRecBank.getFloat("STTime", 0));
					if(CNDCalibration.hipoV==CNDCalibration.hipo4) {
						if (eventRecBank.getFloat("startTime", 0) != -1000.0) {
							paddlePair.EVENT_START_TIME = eventRecBank.getFloat("startTime", 0);
							//System.out.println("Stt "+eventRecBank.getFloat("startTime", 0));
						}
					}

					if(CNDCalibration.hipoV==CNDCalibration.hipo3) {
						if (eventRecBank.getFloat("STTime", 0) != -1000.0) {
							paddlePair.EVENT_START_TIME = eventRecBank.getFloat("STTime", 0);
						}
					}
					paddlePair.RF_TIME = eventRecBank.getFloat("RFTime", 0);



				}

				if (event.hasBank("RUN::config")) {
					DataBank eventrunBank = event.getBank("RUN::config");

					paddlePair.RUN= eventrunBank.getInt("run", 0);
					//System.out.println(paddlePair.RUN);
					if (eventrunBank.getLong("timestamp", 0) != -1) {

						paddlePair.TIME_STAMP = eventrunBank.getLong("timestamp", 0);
						//System.out.println("phase = " + paddlePair.TRIGGER_PHASE);
					}				
				}
				//
				//				if (event.hasBank("RUN::rf")) {
				//					// get the RF time with id=1
				//					DataBank rfBank = event.getBank("RUN::rf");
				//					double trf = 0.0;
				//					for (int rfIdx = 0; rfIdx < rfBank.rows(); rfIdx++) {
				//						if (rfBank.getShort("id", rfIdx) == 1) {
				//							trf = rfBank.getFloat("time", rfIdx);
				//						}
				//					}
				//
				//
				//					paddlePair.RF_TIME = trf;
				//				}

				if (event.hasBank("CTOF::hits")) {

					boolean noCTOFmatch=true;
					DataBank CTOFBank = event.getBank("CTOF::hits");
					double r=25.11+1.5;
					//CTOFBank.show();
					//hitsBank.show();
					int nctof=CTOFBank.rows();
					for(int i=0;i<nctof;i++){
						int componentCTOF=CTOFBank.getInt("component", i);
						double energyCTOF=CTOFBank.getFloat("energy", i);

						double phi=(7.5/2)+(7.5*componentCTOF);
						double xc=r*Math.cos(Math.toRadians(phi));
						double yc=r*Math.sin(Math.toRadians(phi));

						double dist=Math.sqrt((xc-paddlePair.XPOSt)*(xc-paddlePair.XPOSt)+(yc-paddlePair.YPOSt)*(yc-paddlePair.YPOSt));

						if(dist<25 && energyCTOF>2){
							noCTOFmatch=false;
						}
					}

					if(noCTOFmatch && DataProvider.requireNOMatchedCVTTrack == true){
						if(paddlePair.TRACK_ID == -1){
							paddlePair.TRACK_ID = -2;

							//							System.out.println();
							//							CTOFBank.show();
							//							System.out.println(" component "+paddlePair.getDescriptor().getSector()*2);
							//							hitsBank.show();
							//							event.getBank("CVTRec::Tracks").show();
						}
					}

				}


				if (event.hasBank("CVTRec::Tracks")) {
					DataBank trkBank = event.getBank("CVTRec::Tracks");
					//DataBank rfBank = event.getBank("RUN::rf");

					if (event.hasBank("RUN::config")) {
						DataBank configBank = event.getBank("RUN::config");
						//	paddlePair.TRIGGER_BIT = configBank.getInt("trigger", 0);

					}

					// Get track
					int trkId = hitsBank.getShort("trkID", hitIndex);
					paddlePair.TRACK_ID = trkId;


					// only use hit with associated track
					if (trkId != -1 ){

						double mom = trkBank.getFloat("p", trkId);
						double pt = trkBank.getFloat("pt", trkId);
						double beta = mom / Math.sqrt(mom * mom + 0.139 * 0.139);
						//double beta = mom / Math.sqrt(mom * mom + 0.938 * 0.938);
						paddlePair.BETA = beta;
						paddlePair.P = mom;
						paddlePair.P_t = pt;

						paddlePair.VERTEX_Z = trkBank.getFloat("z0", trkId);
						paddlePair.CHARGE = trkBank.getInt("q", trkId);
						paddlePair.TRACK_REDCHI2 = trkBank.getFloat("chi2", trkId);
						
						double layerh=paddlePair.getDescriptor().getLayer();
						double qh=paddlePair.CHARGE;
						double solenoid=event.getBank("RUN::config").getFloat("solenoid", 0);
						double d0=trkBank.getFloat("d0", trkId)*10.;
						double tandip=trkBank.getFloat("tandip",trkId);
						
						double pathlength=0.0;
						
						double rho = (0.000299792458 * qh * 5. * solenoid) / pt;
						
						double r= 290 + (layerh - 0.5) * 30. + (layerh - 1) * 1.;//250;//
						
						double par = 1. - ((r * r - d0 * d0) * rho * rho) / (2. * (1. + d0 * Math.abs(rho)));
			       		
						double newPathLength = Math.abs(Math.acos(par) / rho);
			        	
						double zh = paddlePair.VERTEX_Z*10. + newPathLength * tandip;
						pathlength=Math.sqrt((zh-paddlePair.VERTEX_Z*10.)*(zh-paddlePair.VERTEX_Z*10.)+(newPathLength*newPathLength));
						
						paddlePair.PATH_LENGTH = pathlength/10.;
						
						double t_tof = (paddlePair.PATH_LENGTH / (beta * 29.98));
						//if(qh==-1)System.out.println(pathlength);
						//System.out.println("SLC" + paddlePair.getDescriptor().getSector()+" "+paddlePair.getDescriptor().getLayer()+" "+paddlePair.getDescriptor().getComponent()+" paddle vt " + (paddlePair.TIMEH-paddlePair.EVENT_START_TIME-t_tof) + "  "+ (paddlePair.layerOffset()));

					}

				}

				//				if (!event.hasBank("CVTRec::Tracks")) {
				//					//System.out.println("trk id "+ paddlePair.TRACK_ID);
				//					paddlePair.TRACK_ID = -2;
				//					//System.out.println("trk id "+ paddlePair.TRACK_ID);
				//				}

				//				if (event.hasBank("REC::Particle")) {
				//
				//					DataBank particle = event.getBank("REC::Particle");
				//					boolean allFD=true;
				//					//					System.out.println();
				//					//					System.out.println("event ");
				//					for(int i=0; i<particle.rows(); i++){
				//						double px = particle.getFloat("px", i);
				//						double py = particle.getFloat("py", i);
				//						double pz = particle.getFloat("pz", i);
				//						double q = particle.getInt("charge", i);
				//						double pid = particle.getInt("pid", i);
				//
				//						double p=Math.sqrt(px * px + py * py + pz * pz);
				//						double theta = Math.toDegrees(Math.acos(pz/p));
				//
				//						//						System.out.println("pid "+particle.getInt("pid", i));
				//						//						System.out.println("theta "+theta);
				//						//						System.out.println("q "+q);
				//
				//						if( theta>38 ){
				//							allFD=false;
				//
				//						}
				//
				//					}
				//
				//
				//					if(allFD && DataProvider.requireNOMatchedCVTTrack == true){
				//						if(paddlePair.TRACK_ID == -1){
				//							paddlePair.TRACK_ID = -2;
				//							//System.out.println("event in");
				//						}
				//					}
				//				}

				//				paddlePair.show();
				//				System.out.println("Adding paddlePair to list");
				//				paddlePairList.add(paddlePair);
				if (useHitsBankProcessing == true && paddlePair.includeInCalib()) {	

					paddlePairList.add(paddlePair);
					//	System.out.println(paddlePair.RF_TIME);
					//System.out.println(paddlePair.ZPOS);
					//					paddlePair.show();
					//					event.getBank("CND::tdc").show();

				}
			}

		} else {
			// no hits bank, so just use adc and tdc
			if (useNonHitsBankProcessing == true) {

				//System.out.println("HERE!");

				// Firstly do checks if any ADC entries are alive, then add these
				// Secondly do checks on TDCs entries to see if both ADCs were dead, and even is both ADCs and 1 TDC is dead!

				// N.B. order values:
				// ADCL = 0
				// ADCR = 1
				// TDCL = 2
				// TDCR = 3

				// Get entry for every PMT in ADC bank
				// Use ADC bank to determine which layer has a nonzero ADC readout for either left or right
				// Then loop through all ADCs to look for the coupled right or left ADC readout (and process even if one dead ADC channel)
				// TDC bank only has actual hits, so can just search the whole bank for matching SLC for first left and right hits                
				// Use ADC bank to determine which events to process (adcL or adcR > 0)


				//for no cut

				//				for (int i = 0; i < adcBank.rows(); i++) {
				//
				//					boolean ignorePaddle = false;  //whether to ignore processing paddle (as already part of a pair found)
				//					int order = adcBank.getByte("order", i);
				//					int adc = adcBank.getInt("ADC", i);
				//					int sector = adcBank.getByte("sector", i);
				//					int layer = adcBank.getByte("layer", i);
				//					int component = adcBank.getShort("component", i);
				//					
				//					CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);
				//					
				//					if(order==0)
				//					{paddlePair.setAdcTdc(adc, 0, 0, 0);}
				//					if(order==1)
				//					{paddlePair.setAdcTdc(0, adc, 0, 0);}
				//					
				//					paddlePairList.add(paddlePair);
				//				}
				//				
				//				for (int i = 0; i < tdcBank.rows(); i++) {
				//
				//					boolean ignorePaddle = false;  //whether to ignore processing paddle (as already part of a pair found)
				//					int order = tdcBank.getByte("order", i);
				//					int tdc = tdcBank.getInt("TDC", i);
				//					int sector = tdcBank.getByte("sector", i);
				//					int layer = tdcBank.getByte("layer", i);
				//					int component = tdcBank.getShort("component", i);
				//					
				//					CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);
				//					
				//					if(order==2)
				//					{paddlePair.setAdcTdc(0, 0, tdc, 0);}
				//					if(order==3)
				//					{paddlePair.setAdcTdc(0, 0, 0, tdc);}
				//					
				//					paddlePairList.add(paddlePair);
				//				}
				//			}

				// a remettre

				for (int i = 0; i < adcBank.rows(); i++) {
					//
					boolean ignorePaddle = false;  //whether to ignore processing paddle (as already part of a pair found)
					int order = adcBank.getByte("order", i);
					int adc = adcBank.getInt("ADC", i);

					// Look for matched hit whether a nonzero adcL or adcR value was read in first from the bank
					if (adc != CNDPaddlePair.unallowedADCValue) {

						int[] adcValues = {0, 0};
						int[] tdcValues = {0, 0};
						float[] timeValues = {0, 0};
						//                        int[] pedValues = {0, 0};

						int sector = adcBank.getByte("sector", i);
						int layer = adcBank.getByte("layer", i);
						int component = adcBank.getShort("component", i);

						for (int p = 0; p < paddlePairList.size(); p++) {

							if (sector == paddlePairList.get(p).getDescriptor().getSector()
									&& layer == paddlePairList.get(p).getDescriptor().getLayer()) {
								ignorePaddle = true;
								break;
								// break as there shouldn't be multiple ADC info in bank for the one component...
								// don't want to erroneously process the same hit twice either...
							}

						}

						//						for (int p = 0; p < adcBank.rows(); p++) {
						//
						//							if (i!=p && sector == adcBank.getByte("sector", p)
						//									&& layer == adcBank.getByte("layer", p)
						//									&& order == adcBank.getByte("order", p)) {
						//								ignorePaddle = true;
						//								break;
						//								// break as there shouldn't be multiple ADC info in bank for the one component...
						//								// don't want to erroneously process the same hit twice either...
						//							}
						//
						//						}




						// Process paddle which was not already part of a hit in a pair already added
						if (ignorePaddle == false) {



							adcValues[1 * order] = adc;
							timeValues[1 * order] = adcBank.getFloat("time", i);

							for (int j = 0; j < adcBank.rows(); j++) {
								int s = adcBank.getByte("sector", j);
								int l = adcBank.getByte("layer", j);
								int c = adcBank.getShort("component", j);
								int o = adcBank.getByte("order", j);
								if (s == sector && l == layer && c == component && o != order) {
									// matching the other adc values
									adcValues[1 * o] = adcBank.getInt("ADC", j);
									timeValues[1 * o] = adcBank.getFloat("time", j);
									break;
								}
							}

							// Now get matching TDCs
							// can search whole bank as it has fewer rows (only hits)
							// break when you find - so always take the first one found
							for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
								int s = adcBank.getByte("sector", tdci);
								int l = adcBank.getByte("layer", tdci);
								int c = tdcBank.getShort("component", tdci);
								int o = tdcBank.getByte("order", tdci);
								if (s == sector && l == layer && c == component && o == 2) {
									// matching tdc L
									tdcValues[o - 2] = tdcBank.getInt("TDC", tdci);
									break;
								}
							}

							for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
								int s = adcBank.getByte("sector", tdci);
								int l = adcBank.getByte("layer", tdci);
								int c = tdcBank.getShort("component", tdci);
								int o = tdcBank.getByte("order", tdci);
								if (s == sector && l == layer && c == component && o == 3) {
									// matching tdc R
									tdcValues[o - 2] = tdcBank.getInt("TDC", tdci);
									break;
								}
							}

							int adcL = adcValues[0];
							int adcR = adcValues[1];
							int tdcL = tdcValues[0];
							int tdcR = tdcValues[1];
							float adcTimeL = timeValues[0];
							float adcTimeR = timeValues[1];

							CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);
							paddlePair.setAdcTdc(adcL, adcR, tdcL, tdcR);
							paddlePair.ADC_TIMEL = adcTimeL;
							paddlePair.ADC_TIMER = adcTimeR;

							//                            if (sector == 10 && layer == 3){
							//                                System.out.println("\nadcL = " + adcL);
							//                                System.out.println("adcR = " + adcR);
							//                                System.out.println("tdcL = " + tdcL);
							//                                System.out.println("tdcR = " + tdcR);
							//                            }                            

							// Currently want Status step to process all events satisying above where even one ADC is alive



							if (paddlePair.includeInCalib()) {
								paddlePairList.add(paddlePair);
								//System.out.println(paddlePair.TRACK_ID);
								//System.out.println("ignore Paddle "+ignorePaddle);



							}


						}
					}

				}  //End of adcBank loop

				// Have added all events with alive ADCs, so now look for when both ADCs are dead:

				// Now loop through TDC banks to look for hits, matched or unmatched
				// Use TDC bank to determine which events to process (tdcL or tdcR > 0)
				//				for (int i = 0; i < tdcBank.rows(); i++) {
				//
				//					boolean ignorePaddle = false;  //whether to ignore processing paddle (as already part of a pair found)
				//					int order = tdcBank.getByte("order", i);
				//					int tdc = tdcBank.getInt("TDC", i);
				//
				//					// Look for matched hit whether a nonzero tdcL or tdcR value was read in first from the bank
				//					if (tdc != CNDPaddlePair.unallowedTDCValues[0]
				//							&& tdc != CNDPaddlePair.unallowedTDCValues[1]) {
				//
				//						int[] tdcValues = {0, 0};
				//
				//						int sector = tdcBank.getByte("sector", i);
				//						int layer = tdcBank.getByte("layer", i);
				//						int component = tdcBank.getShort("component", i);
				//
				//						for (int p = 0; p < paddlePairList.size(); p++) {
				//
				//							if (sector == paddlePairList.get(p).getDescriptor().getSector()
				//									&& layer == paddlePairList.get(p).getDescriptor().getLayer()) {
				//								ignorePaddle = true;
				//								break;
				//								// break as don't want to process events where ADC were alive
				//								// don't want to erroneously process the same TDC hit twice either...
				//							}
				//
				//						}
				//
				//						// Process paddle which was not already part of a hit in a pair already added
				//						if (ignorePaddle == false) {
				//
				//							//Set tdc value already picked up
				//							tdcValues[order-2] = tdc;
				//
				//							// Now get matching TDC
				//
				//							//Check if any more hits left to look for a match:
				//							if (i < (tdcBank.rows() - 1)) {
				//
				//								// Search bank from current row + 1
				//								// break if you find - so always take the first one found                                
				//								for (int tdci = i+1; tdci < tdcBank.rows(); tdci++) {
				//									int s = tdcBank.getByte("sector", tdci);
				//									int l = tdcBank.getByte("layer", tdci);
				//									int c = tdcBank.getShort("component", tdci);
				//									int o = tdcBank.getByte("order", tdci);
				//									if (s == sector && l == layer && c == component && o != order) {
				//										// matching tdc if there is one
				//										tdcValues[o - 2] = tdcBank.getInt("TDC", tdci);
				//										break;
				//									}
				//								}
				//
				//							} //Otherwise initialised value of 0.0 is fine for the dead TDC (as it couldn't be found)
				//
				//							//N.B. Both ADCs are dead in this loop
				//							int adcL = 0;
				//							int adcR = 0;
				//							int tdcL = tdcValues[0];
				//							int tdcR = tdcValues[1];
				//
				//							CNDPaddlePair paddlePair = new CNDPaddlePair(sector, layer, component);
				//							paddlePair.setAdcTdc(adcL, adcR, tdcL, tdcR);
				//
				//							//                            if (sector == 10 && layer == 3){
				//							//                                System.out.println("\ntdcL = " + tdcL);
				//							//                                System.out.println("tdcR = " + tdcR);
				//							//                            }          
				//
				//							// Currently want Status step to process all events satisying above where even one TDC is alive
				//							if (paddlePair.includeInCalib()) {
				//								paddlePairList.add(paddlePair);
				//							}
				//						}
				//					}
				//
				//				}

			}
		}
		//                System.out.println(" paddlePairList.size() = "+paddlePairList.size());

		return paddlePairList;
	}

	// Works for GEMC output July 2017:
	public static List<CNDPaddlePair> getPaddlePairListDgtz(DataEvent event) {

		if (test) {

			event.show();

			if (event.hasBank("CND::dgtz")) {
				event.getBank("CND::dgtz").show();
			}

		}

		ArrayList<CNDPaddlePair> paddlePairList = new ArrayList<CNDPaddlePair>();

		// Only continue if we have dgtz bank
		if (!event.hasBank("CND::dgtz")) {
			return paddlePairList;
		}

		if (event.hasBank("CND::dgtz") == true) {

			EvioDataBank bank = (EvioDataBank) event.getBank("CND::dgtz");

			// Let's store the first TDC and ADC values of all the multihits for this sector.
			// Can do a check on them later that they all fall within 10ns for example.
			for (int i = 0; i < bank.rows(); i++) {

				int sector = bank.getInt("sector", i);
				int layer = bank.getInt("layer", i);
				int component = bank.getInt("component", i);
				int adcL = bank.getInt("ADCL", i);
				int adcR = bank.getInt("ADCR", i);
				int tdcL = bank.getInt("TDCL", i);
				int tdcR = bank.getInt("TDCR", i);

				CNDPaddlePair paddlePair = new CNDPaddlePair(
						bank.getInt("sector", i),
						bank.getInt("layer", i),
						bank.getInt("component", i));

				paddlePair.setAdcTdc(
						adcL,
						adcR,
						tdcL,
						tdcR);

				if (test) {
					System.out.println("Adding paddle " + sector + " " + layer + " " + component);
					System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
				}

				// Want to add all paddles for Status tab
				paddlePairList.add(paddlePair);

			}  // End of for (int i = 0; i < bank.rows(); i++)
		}  //End of if (event.hasBank("CND::dgtz") == true)

		// This will return the pairList
		return paddlePairList;

	}

	public static List<CNDPaddlePair> getPaddleListHipo(DataEvent event) {

		boolean refPaddleFound = false;
		boolean testPaddleFound = false;

		if (test) {

			event.show();
			if (event.hasBank("FTOF::adc")) {
				event.getBank("FTOF::adc").show();
			}
			if (event.hasBank("FTOF::tdc")) {
				event.getBank("FTOF::tdc").show();
			}
			if (event.hasBank("FTOF::hits")) {
				event.getBank("FTOF::hits").show();
			}
			if (event.hasBank("HitBasedTrkg::HBTracks")) {
				event.getBank("HitBasedTrkg::HBTracks").show();
			}
			if (event.hasBank("TimeBasedTrkg::TBTracks")) {
				event.getBank("TimeBasedTrkg::TBTracks").show();
			}
			if (event.hasBank("RUN::rf")) {
				event.getBank("RUN::rf").show();
			}
			if (event.hasBank("RUN::config")) {
				event.getBank("RUN::config").show();
			}
			if (event.hasBank("MC::Particle")) {
				event.getBank("MC::Particle").show();
			}
		}

		ArrayList<CNDPaddlePair> paddleList = new ArrayList<CNDPaddlePair>();

		// Only continue if we have adc and tdc banks
		if (!event.hasBank("FTOF::adc") || !event.hasBank("FTOF::tdc")) {
			return paddleList;
		}

		DataBank adcBank = event.getBank("FTOF::adc");
		DataBank tdcBank = event.getBank("FTOF::tdc");

		//		if (event.hasBank("TimeBasedTrkg::TBTracks")) {
		//			DataBank testBank = event.getBank("TimeBasedTrkg::TBTracks");
		//			for (int tbtIdx=0; tbtIdx<testBank.rows(); tbtIdx++) {
		//				// fill test hist
		//				CNDCalibration.trackRCS.fill(testBank.getFloat("chi2", tbtIdx)/testBank.getShort("ndf", tbtIdx));
		//				CNDCalibration.trackRCS2.fill(testBank.getFloat("chi2", tbtIdx)/testBank.getShort("ndf", tbtIdx));
		//				CNDCalibration.vertexHist.fill(testBank.getFloat("Vtx0_z", tbtIdx));
		//				
		//			}
		//		}
		// iterate through hits bank getting corresponding adc and tdc
		if (event.hasBank("FTOF::hits")) {
			DataBank hitsBank = event.getBank("FTOF::hits");

			for (int hitIndex = 0; hitIndex < hitsBank.rows(); hitIndex++) {

				double tx = hitsBank.getFloat("tx", hitIndex);
				double ty = hitsBank.getFloat("ty", hitIndex);
				double tz = hitsBank.getFloat("tz", hitIndex);
				//System.out.println("tx ty tz"+tx+" "+ty+" "+tz);

				CNDPaddlePair paddle = new CNDPaddlePair(
						(int) hitsBank.getByte("sector", hitIndex),
						(int) hitsBank.getByte("layer", hitIndex),
						(int) hitsBank.getShort("component", hitIndex));
				paddle.setAdcTdc(
						adcBank.getInt("ADC", hitsBank.getShort("adc_idx1", hitIndex)),
						adcBank.getInt("ADC", hitsBank.getShort("adc_idx2", hitIndex)),
						tdcBank.getInt("TDC", hitsBank.getShort("tdc_idx1", hitIndex)),
						tdcBank.getInt("TDC", hitsBank.getShort("tdc_idx2", hitIndex)));
				paddle.setPos(tx, ty, tz);
				paddle.ADC_TIMEL = adcBank.getFloat("time", hitsBank.getShort("adc_idx1", hitIndex));
				paddle.ADC_TIMER = adcBank.getFloat("time", hitsBank.getShort("adc_idx2", hitIndex));
				paddle.TOF_TIME = hitsBank.getFloat("time", hitIndex);

				//System.out.println("Paddle created "+paddle.getDescriptor().getSector()+paddle.getDescriptor().getLayer()+paddle.getDescriptor().getComponent());
				if (event.hasBank("TimeBasedTrkg::TBTracks") && event.hasBank("RUN::rf")) {

					DataBank tbtBank = event.getBank("TimeBasedTrkg::TBTracks");
					DataBank rfBank = event.getBank("RUN::rf");

					if (event.hasBank("RUN::config")) {
						DataBank configBank = event.getBank("RUN::config");
						paddle.TRIGGER_BIT = configBank.getInt("trigger", 0);
					}

					// get the RF time with id=1
					double trf = 0.0;
					for (int rfIdx = 0; rfIdx < rfBank.rows(); rfIdx++) {
						if (rfBank.getShort("id", rfIdx) == 1) {
							trf = rfBank.getFloat("time", rfIdx);
						}
					}

					// Identify electrons and store path length etc for time walk
					int trkId = hitsBank.getShort("trackid", hitIndex);
					double energy = hitsBank.getFloat("energy", hitIndex);

					System.out.println("trkId energy trf " + trkId + " " + energy + " " + trf);

					// only use hit with associated track and a minimum energy
					if (trkId != -1 && energy > 1.5) {

						double c3x = tbtBank.getFloat("c3_x", trkId - 1);
						double c3y = tbtBank.getFloat("c3_y", trkId - 1);
						double c3z = tbtBank.getFloat("c3_z", trkId - 1);
						double path = tbtBank.getFloat("pathlength", trkId - 1) + Math.sqrt((tx - c3x) * (tx - c3x) + (ty - c3y) * (ty - c3y) + (tz - c3z) * (tz - c3z));
						paddle.PATH_LENGTH = path;
						paddle.RF_TIME = trf;

						// Get the momentum and record the beta (assuming every hit is a pion!)
						double px = tbtBank.getFloat("p0_x", trkId - 1);
						double py = tbtBank.getFloat("p0_y", trkId - 1);
						double pz = tbtBank.getFloat("p0_z", trkId - 1);
						double mom = Math.sqrt(px * px + py * py + pz * pz);
						double beta = mom / Math.sqrt(mom * mom + 0.139 * 0.139);
						paddle.BETA = beta;
						paddle.P = mom;
						paddle.TRACK_ID = trkId;
						paddle.VERTEX_Z = tbtBank.getFloat("Vtx0_z", trkId - 1);
						paddle.CHARGE = tbtBank.getInt("q", trkId - 1);

						if (CNDCalibration.maxRcs != 0.0) {
							paddle.TRACK_REDCHI2 = tbtBank.getFloat("chi2", trkId - 1) / tbtBank.getShort("ndf", trkId - 1);
						} else {
							paddle.TRACK_REDCHI2 = -1.0;
						}

						if (paddle.getDescriptor().getComponent() == 13
								&& paddle.getDescriptor().getLayer() == 1 && trkId != -1) {
							//refPaddleFound = true;
						}
						if (paddle.getDescriptor().getComponent() == 35
								&& paddle.getDescriptor().getLayer() == 2 && trkId != -1) {
							//testPaddleFound = true;
						}

						// check if it's an electron by matching to the generated particle
						int q = tbtBank.getByte("q", trkId - 1);
						double p0x = tbtBank.getFloat("p0_x", trkId - 1);
						double p0y = tbtBank.getFloat("p0_y", trkId - 1);
						double p0z = tbtBank.getFloat("p0_z", trkId - 1);
						Particle recParticle = new Particle(11, p0x, p0y, p0z, 0, 0, 0);

						//						System.out.println("q "+q);
						//						System.out.println("recParticle.p() "+recParticle.p());
						//						System.out.println("electronGen.p() "+electronGen.p());
						// select negative tracks matching the generated electron as electron candidates
						//						if(q==-1
						//								&& Math.abs(recParticle.p()-electronGen.p())<0.5
						//								&& Math.abs(Math.toDegrees(recParticle.theta()-electronGen.theta()))<2.0
						//								&& Math.abs(Math.toDegrees(recParticle.phi()-electronGen.phi()))<8) {
						//							paddle.PARTICLE_ID = CNDPaddlePair.PID_ELECTRON;
						//						} 
						//						else {
						//							paddle.PARTICLE_ID = CNDPaddlePair.PID_PION;
						//						}
					}
				}

				if (refPaddleFound && testPaddleFound) {

					event.show();
					if (event.hasBank("FTOF::adc")) {
						event.getBank("FTOF::adc").show();
					}
					if (event.hasBank("FTOF::tdc")) {
						event.getBank("FTOF::tdc").show();
					}
					if (event.hasBank("FTOF::hits")) {
						event.getBank("FTOF::hits").show();
					}
					if (event.hasBank("HitBasedTrkg::HBTracks")) {
						event.getBank("HitBasedTrkg::HBTracks").show();
					}
					if (event.hasBank("TimeBasedTrkg::TBTracks")) {
						event.getBank("TimeBasedTrkg::TBTracks").show();
					}
					if (event.hasBank("RUN::rf")) {
						event.getBank("RUN::rf").show();
					}
					if (event.hasBank("RUN::config")) {
						event.getBank("RUN::config").show();
					}
					if (event.hasBank("MC::Particle")) {
						event.getBank("MC::Particle").show();
					}
					refPaddleFound = false;
					testPaddleFound = false;
				}

				//System.out.println("Adding paddle to list");
				if (paddle.includeInCalib()) {
					paddleList.add(paddle);
					//					System.out.println("Paddle added to list SLC "+paddle.getDescriptor().getSector()+paddle.getDescriptor().getLayer()+paddle.getDescriptor().getComponent());
					//					System.out.println("Particle ID "+paddle.PARTICLE_ID);
					//					System.out.println("position "+paddle.XPOS+" "+paddle.YPOS);
					//					System.out.println("trackFound "+paddle.trackFound());
				}
			}
		} else {
			// no hits bank, so just use adc and tdc

			// based on cosmic data
			// am getting entry for every PMT in ADC bank
			// ADC R two indices after ADC L (will assume right is always after left)
			// TDC bank only has actual hits, so can just search the whole bank for matching SLC
			for (int i = 0; i < adcBank.rows(); i++) {
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order == 0 && adc != 0) {

					int sector = adcBank.getByte("sector", i);
					int layer = adcBank.getByte("layer", i);
					int component = adcBank.getShort("component", i);
					int adcL = adc;
					int adcR = 0;
					float adcTimeL = adcBank.getFloat("time", i);
					float adcTimeR = 0;
					int tdcL = 0;
					int tdcR = 0;

					for (int j = 0; j < adcBank.rows(); j++) {
						int s = adcBank.getByte("sector", j);
						int l = adcBank.getByte("layer", j);
						int c = adcBank.getShort("component", j);
						int o = adcBank.getByte("order", j);
						if (s == sector && l == layer && c == component && o == 1) {
							// matching adc R
							adcR = adcBank.getInt("ADC", j);
							adcTimeR = adcBank.getFloat("time", j);
							break;
						}
					}

					// Now get matching TDCs
					// can search whole bank as it has fewer rows (only hits)
					// break when you find so always take the first one found
					for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
						int s = tdcBank.getByte("sector", tdci);
						int l = tdcBank.getByte("layer", tdci);
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (s == sector && l == layer && c == component && o == 2) {
							// matching tdc L
							tdcL = tdcBank.getInt("TDC", tdci);
							break;
						}
					}
					for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
						int s = tdcBank.getByte("sector", tdci);
						int l = tdcBank.getByte("layer", tdci);
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (s == sector && l == layer && c == component && o == 3) {
							// matching tdc R
							tdcR = tdcBank.getInt("TDC", tdci);
							break;
						}
					}

					if (test) {
						System.out.println("Values found " + sector + layer + component);
						System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
					}

					if (adcL > 100 && adcR > 100) {

						CNDPaddlePair paddle = new CNDPaddlePair(
								sector,
								layer,
								component);
						paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
						paddle.ADC_TIMEL = adcTimeL;
						paddle.ADC_TIMER = adcTimeR;

						if (paddle.includeInCalib()) {

							if (test) {
								System.out.println("Adding paddle " + sector + layer + component);
								System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
							}
							paddleList.add(paddle);
						}
					}
				}
			}

		}

		return paddleList;
	}

	public static void systemOut(String text) {
		boolean test = false;
		if (test) {
			System.out.println(text);
		}
	}

}
