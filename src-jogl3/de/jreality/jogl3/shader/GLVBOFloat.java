package de.jreality.jogl3.shader;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

public class GLVBOFloat extends GLVBO{
	
	public GLVBOFloat(GL3 gl, float[] vertdata, String name){
		this.name = name;
		int[] vertindex = new int[1];
		gl.glGenBuffers(1, vertindex, 0);
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertindex[0]);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, 4*vertdata.length, FloatBuffer.wrap(vertdata), gl.GL_DYNAMIC_READ);
		index = vertindex[0];
		length = vertdata.length;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return GL3.GL_FLOAT;
	}
}