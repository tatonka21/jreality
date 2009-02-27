package de.jreality.audio;

/**
 * 
 * Simple low-pass filter (discretization of an RC low-pass filter); see http://en.wikipedia.org/wiki/Low-pass_filter
 * for background.
 * 
 * @author brinkman
 *
 */
public class LowPassFilter implements DistanceCue {
	
	private static final float threshold = 1e-16f; // dynamic range between hearing threshold and instant perforation of eardrum
	private float sampleRate;
	private float cutOff;
	private float alpha = 1f;
	private float value = 0f;
	
	public LowPassFilter() {
		// do nothing
	}
	
	public LowPassFilter(float sampleRate) {
		setSampleRate(sampleRate);
	}
	
	public LowPassFilter(float sampleRate, float cutOff) {
		setSampleRate(sampleRate);
		setCutOff(cutOff);
	}
	
	public void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	public void setCutOff(float cutOff) {
		this.cutOff = cutOff;
		float tau = (float) (1/(2*Math.PI*cutOff));  // RC time constant
		alpha = 1/(1+tau*sampleRate);
	}
	
	public float getCutOff() {
		return cutOff;
	}

	public float initialize(float v) {
		value = v;
		return v;
	}
	
	public float nextValue(float v) {
		value += alpha*(v-value);
		return value;
	}

	public float nextValue(float v, float r) {
		setCutOff(44000/(1+r));
		return nextValue(v);
	}

	public boolean hasMore() {
		return Math.abs(value)>threshold;
	}
	
	public void reset() {
		initialize(0);
	}
}
