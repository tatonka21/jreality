package de.jreality.jogl3.optimization;

import de.jreality.jogl3.JOGLRenderState;
import de.jreality.jogl3.geom.JOGLFaceSetEntity;
import de.jreality.jogl3.geom.JOGLFaceSetInstance;

public class Instance {
	
	public Instance(InstanceCollection ic, JOGLFaceSetInstance ins, JOGLRenderState state, int posInVBOs){
		this.collection = ic;
		this.fsi = ins;
		this.state = state;
		this.posInVBOs = posInVBOs;
		dead = false;
		upToDate = true;
		JOGLFaceSetEntity fse = (JOGLFaceSetEntity)fsi.getEntity();
		length = fse.getVBO("vertex_coordinates").getLength();
	}
	
	public void kill(){
		dead = true;
		fsi = null;
		state = null;
	}
	
	public boolean isDead(){
		return dead;
	}
	
	public boolean isAlive(){
		return !dead;
	}
	
	public InstanceCollection collection;
	public JOGLFaceSetInstance fsi;
	public JOGLRenderState state;
	/**
	 * number of floats or integers
	 */
	public int length;
	/**
	 * position in VBO in floats
	 */
	public int posInVBOs;
	/**
	 * dead actually means, that it needs to be nulled in GPU, if it's not removed by defragmentation or merging
	 */
	private boolean dead = false;
	public boolean upToDate = true;
	public int id = 0;
}