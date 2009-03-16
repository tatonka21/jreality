package de.jreality.audio;

import de.jreality.scene.data.SampleReader;
import de.jreality.shader.EffectiveAppearance;


/**
 * First attempt to build a reverberator with a feedback delay network...
 * 
 * @author brinkman
 *
 */
public class FDNReverb implements SampleProcessor {

	private SampleReader reader;
	private FDNParameters parameters;
	private float[][] delayLines;
	private int[] delayIndices;
	private float[] outBuffer, inBuffer;
	
	public FDNReverb() {
		// do nothing
	}
	
	public FDNReverb(SampleReader reader, FDNParameters params) {
		initialize(reader);
		setParameters(params);
	}
	
	public void initialize(SampleReader reader) {
		this.reader = reader;
	}

	public void setProperties(EffectiveAppearance app) {
		FDNParameters params = (FDNParameters) app.getAttribute(AudioAttributes.FDN_PARAMETER_KEY, AudioAttributes.DEFAULT_FDN_PARAMETERS, FDNParameters.class);
		if (params!=parameters) {
			setParameters(params);
		}
		float reverbTime = app.getAttribute(AudioAttributes.REVERB_TIME_KEY, AudioAttributes.DEFAULT_REVERB_TIME);
		if (reverbTime!=params.getReverbTime()) {
			params.setReverbTime(reverbTime);
		}
	}

	public synchronized void setParameters(FDNParameters params) {
		int n = params.numberOfLines();
		int sr = reader.getSampleRate();
		
		parameters = params;
		delayIndices = new int[n];
		delayLines = new float[n][];
		inBuffer = new float[n];
		outBuffer = new float[n];
		
		for(int i=0; i<n; i++) {
			delayLines[i] = new float[(int) (params.delayTime(i)*sr+.5)];
		}
	}

	public void clear() {
		reader.clear();
	}

	public int getSampleRate() {
		return reader.getSampleRate();
	}

	public synchronized int read(float[] buffer, int i0, int samples) {
		int nRead = reader.read(buffer, i0, samples);
		int n = parameters.numberOfLines();
		for(int i=0; i<nRead; i++) {
			float v = 0;
			for(int j=0; j<n; j++) {
				v += (outBuffer[j] = getValue(j));
			}
			parameters.map(inBuffer, outBuffer);
			for(int j=0; j<n; j++) {
				setValue(j, buffer[i0] + inBuffer[j]);
				advanceIndex(j);
			}
			buffer[i0++] = v/n;
		}
		return nRead;
	}

	private float getValue(int i) {
		return delayLines[i][delayIndices[i]];
	}
	
	private float setValue(int i, float v) {
		return delayLines[i][delayIndices[i]] = v;
	}
	
	private void advanceIndex(int i) {
		delayIndices[i] = (delayIndices[i]+1) % delayLines[i].length;
	}
}
