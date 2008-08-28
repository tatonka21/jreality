/**
 *
 * This file is part of jReality. jReality is open source software, made
 * available under a BSD license:
 *
 * Copyright (c) 2003-2006, jReality Group: Charles Gunn, Tim Hoffmann, Markus
 * Schmies, Steffen Weissmann.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of jReality nor the names of its contributors nor the
 *   names of their associated organizations may be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */


package de.jreality.softviewer.shader;

import de.jreality.backends.texture.SimpleTexture;
import de.jreality.shader.CubeMap;
import de.jreality.shader.Texture2D;
import de.jreality.softviewer.EnvironmentTexture;
import de.jreality.softviewer.MipMapedTexture;



/**
 * 
 * @version 1.0
 * @author timh
 *
 */
public class InvertPolygonShader extends DefaultPolygonShader {

    /**
     * 
     */
    public InvertPolygonShader() {
        super();
        
    }
    
    public InvertPolygonShader(de.jreality.shader.DefaultPolygonShader ps) {
        super();
        vertexShader = new InvertVertexShader(ps.getDiffuseColor(),ps.getSpecularCoefficient(),ps.getSpecularExponent(),ps.getTransparency());

        Texture2D tex = ps.getTexture2d();
        if (tex != null && tex.getImage() != null) {
            texture = new SimpleTexture(tex);
           //texture = MipMapedTexture.create(tex);
        }
        /*
        CubeMap cm = ps.getReflectionMap();
        if(cm != null) {
            
            texture = new EnvironmentTexture(cm,texture);
            needsNormals = true;
        } else {
            needsNormals = false;
        }
        */
    }


}