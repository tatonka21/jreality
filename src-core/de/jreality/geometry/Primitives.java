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


package de.jreality.geometry;

import java.awt.Color;

import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.ClippingPlane;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Sphere;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.IntArrayArray;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

/**
 * Static methods for generating a variety of geometric primitives either as 
 * instances of {@link Geometry} or {@link SceneGraphComponent}. The main cateegories of
 * primitives are:
 * <ul>
 * <li>Polyhedra: cube, tetrahedron, icosahedron, pyramids, ...</li>
 * <li>Approximations to smooth shapes: sphere, cylinder, torus, ... </li>
 * <li>Points and rectangles</li>
 * <li>Miscellaneous: clipping planes, ... </li>
 * </ul>
 * <p>
 * Note: many of these methods could perhaps profitably be replaced with factory classes.
 * 
 * @author Charles Gunn
 *
 */
public class Primitives {

		private Primitives()	{
			super();
		}
		static private double[][] cubeVerts3 =  
		{{1,1,1},{1,1,-1},{1,-1,1},{1,-1,-1},{-1,1,1},{-1,1,-1},{-1,-1,1},{-1,-1,-1}};

		static private int[][] cubeIndices = {
				{0,2,3,1},
				{1,5,4,0},
				{3,7,5,1},
				{5,7,6,4},
				{2,6,7,3},
				{0,4,6,2}};
		
		static private double[][] cubeColors = {
			{0d, 1d, 0d},
			{0d, 0d, 1d},
			{1d, 0d, 0d},
			{1d, 0d, 1d},
			{1d, 1d, 0d},
			{0d, 1d, 1d}};
			
		public static IndexedFaceSet cube()	{return cube(false);}
		public static IndexedFaceSet coloredCube()	{return cube(true);}
		
		/**
		 * A cube.  If <i>colored</i> is true, then it has face colors.
		 * @param colored
		 * @return
		 */
		public static IndexedFaceSet cube(boolean colored)	{
			
			IndexedFaceSet cube = new IndexedFaceSet(8, 6);

			cube.setFaceAttributes(Attribute.INDICES, new IntArrayArray.Array(cubeIndices));
			cube.setVertexAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(cubeVerts3));
			if (colored)	{
				cube.setFaceAttributes(Attribute.COLORS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(cubeColors));
			}
			IndexedFaceSetUtility.calculateAndSetEdgesFromFaces(cube);
			GeometryUtility.calculateAndSetFaceNormals(cube);		
			return cube;
	}
		/**
		 * @deprecated	Use {@link #box(double, double, double, boolean)}.
		 */public static IndexedFaceSet cube(double width,double hight,double depth,boolean colored){
			return box(width, hight, depth, colored);
		}
		/**
		 * A dimensioned box.  If <i>colored</i> is true, then it has face colors.
		 * @author Gonska
		 * @param width: size in X-dimension 
		 * @param higth: size in Y-dimension
		 * @param depth: size in Z-dimension
		 * @param colored
		 * @return
		 */		
		public static IndexedFaceSet box(double width,double hight,double depth,boolean colored){
			return box (width, hight, depth, colored, Pn.EUCLIDEAN);
		}
		public static IndexedFaceSet box(double width,double hight,double depth,boolean colored, int signature){
			double w=width/2;	double h=hight/2;	double d=depth/2;
			double[][] points =  
			 {{w,h,d},{w,h,-d},{w,-h,d},{w,-h,-d},
			 {-w,h,d},{-w,h,-d},{-w,-h,d},{-w,-h,-d}};
			IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
			ifsf.setVertexCount(8);
			ifsf.setVertexCoordinates(points);
			ifsf.setFaceCount(cubeIndices.length);
			ifsf.setFaceIndices(cubeIndices);
			if (colored) ifsf.setFaceColors(cubeColors);
			ifsf.setSignature(signature);
			ifsf.setGenerateFaceNormals(true);
			ifsf.setGenerateEdgesFromFaces(true);
			ifsf.update();
			return ifsf.getIndexedFaceSet();
		}
		public static IndexedFaceSet coloredCube(double width,double hight,double depth){
			return box(width,hight,depth,true);
		}
		public static IndexedFaceSet cube(double width,double hight,double depth){
			return box(width,hight,depth,false);
		}
		static private double[][] tetrahedronVerts3 =  
		{{1,1,1},{1,-1,-1},{-1,1,-1},{-1,-1,1}};

		static private int[][] tetrahedronIndices = {
				{0,1,2},
				{2,1,3},
				{1,0,3},
				{0,2,3}};
		
		static private double[][] tetrahedronColors = {
			{0d, 1d, 0d},
			{0d, 0d, 1d},
			{1d, 0d, 0d},
			{1d, 0d, 1d}
			};
			
		public static IndexedFaceSet tetrahedron()	{return tetrahedron(false);}
		public static IndexedFaceSet coloredTetrahedron()	{return tetrahedron(true);}
		
		/**
		 * A tetrahedron.  If <i>colored</i> is true, then it has face colors.
		 * @param colored
		 * @return
		 */public static IndexedFaceSet tetrahedron(boolean colored)	{
			
			IndexedFaceSet tetrahedron = new IndexedFaceSet(4, 4);

			tetrahedron.setFaceAttributes(Attribute.INDICES, new IntArrayArray.Array(tetrahedronIndices));
			tetrahedron.setVertexAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(tetrahedronVerts3));
			if (colored)	{
				tetrahedron.setFaceAttributes(Attribute.COLORS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(tetrahedronColors));
			}
			IndexedFaceSetUtility.calculateAndSetEdgesFromFaces(tetrahedron);
			GeometryUtility.calculateAndSetFaceNormals(tetrahedron);		
			return tetrahedron;
	}

		static public double[][] icoVerts3 =  {
				{0.850651026, 0, 0.525731027}, 
				{0.850651026, 0, -0.525731027}, 
				{0.525731027, 0.850651026, 0}, 
				{0.525731027, -0.850651026, 0.0}, 
				{0.0, -0.525731027, 0.850651026}, 
				{0.0, 0.525731027, 0.850651026}, 
				{-0.850651026, 0, -0.525731027}, 
				{ -0.850651026, 0, 0.525731027}, 
				{-0.525731027, 0.850651026, 0}, 
				{ 0.0, 0.525731027, -0.850651026}, 
				{0.0, -0.525731027, -0.850651026}, 
				{-0.525731027, -0.850651026, 0.0}};

		// Don't remove: good for testing backend capability to deal with 4D vertices
		static private double[][] icoVerts4 =  {
				{0.850651026, 0, 0.525731027, 1.0}, 
				{0.850651026, 0, -0.525731027, 1.0}, 
				{0.525731027, 0.850651026, 0, 1.0}, 
				{0.525731027, -0.850651026, 0.0, 1.0}, 
				{0.0, -0.525731027, 0.850651026, 1.0}, 
				{0.0, 0.525731027, 0.850651026, 1.0}, 
				{-0.850651026, 0, -0.525731027, 1.0}, 
				{ -0.850651026, 0, 0.525731027, 1.0}, 
				{-0.525731027, 0.850651026, 0, 1.0}, 
				{ 0.0, 0.525731027, -0.850651026, 1.0}, 
				{0.0, -0.525731027, -0.850651026, 1.0}, 
				{-0.525731027, -0.850651026, 0.0, 1.0}};

		static private int[][] icoIndices = {
						{0, 1, 2},
						{0, 3, 1},
						{0, 4, 3},
						{0, 5, 4},
						{0, 2, 5},
						{6, 7, 8},
						{6, 8, 9},
						{6, 9, 10},
						{6, 10, 11},
						{6, 11, 7},
						{1, 3, 10},
						{3, 4, 11},
						{4, 5, 7},
						{5, 2, 8},
						{2, 1, 9},
						{7, 11, 4},
						{8, 7, 5},
						{9, 8, 2},
						{10, 9, 1},
						{11, 10, 3}};

		public static IndexedFaceSet sharedIcosahedron = null; 
		static  {
			sharedIcosahedron = icosahedron();
		}
				
		/**
		 * 
		 * @return
		 */
		public static IndexedFaceSet icosahedron() {
					
			IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
			ifsf.setVertexCount(12);
			ifsf.setFaceCount(20);
			ifsf.setVertexCoordinates(icoVerts3);
			//ifsf.setVertexNormals(icoVerts3);
			ifsf.setFaceIndices(icoIndices);
			ifsf.setGenerateEdgesFromFaces(true);
			ifsf.setGenerateFaceNormals(true);
			ifsf.update();
			return ifsf.getIndexedFaceSet();
			}
		
		/**
		 * A single point.
		 * @param center
		 * @return
		 */
		public static PointSet point( double[] center)	{
			PointSet ps = new PointSet(1);
			int n = center.length;
			double[][] pts = new double[1][n];
			System.arraycopy(center,0,pts[0],0,n);
			ps.setVertexCountAndAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY.array(n).createReadOnly(pts));
			return ps;
		}
		
		/**
		 * A euclidean sphere with given radius and center.
		 * @param radius
		 * @param x
		 * @param y
		 * @param z
		 * @return
		 */public static SceneGraphComponent sphere(double radius, double x, double y, double z)	{
			return sphere(radius, new double[] {x,y,z}, Pn.EUCLIDEAN);
		}
		
		/**
		 * A euclidean sphere with given radius and center.
		 * @param radius
		 * @param center
		 * @return
		 */public static SceneGraphComponent sphere(double radius, double[] center) {
			return sphere(radius, center, Pn.EUCLIDEAN);
		}
		
		/**
		 * A sphere with given radius and center, with the given metric signature.
		 * @param radius
		 * @param center
		 * @param signature
		 * @return
		 */
		 public static SceneGraphComponent sphere(double radius, double[] center, int signature) {
			SceneGraphComponent sgc = SceneGraphUtility.createFullSceneGraphComponent("sphere");
			if (center == null)  center = Pn.originP3;
			MatrixBuilder.init(null,signature).translate(center).scale(radius).assignTo(sgc.getTransformation());
			sgc.setGeometry(new Sphere());
			return sgc;
		}
		
		/**
		 *  A {@link SceneGraphComponent} with wire-frame sphere (azimuth/elevation coordinate mesh)
		 * @return
		 */
		 public static SceneGraphComponent wireframeSphere() {
			SceneGraphComponent hypersphere = SceneGraphUtility.createFullSceneGraphComponent("wireframe sphere");
			hypersphere.setGeometry(SphereUtility.sphericalPatch(0.0, 0.0, 360.0, 180.0, 40, 20, 1.0));
			Appearance ap = hypersphere.getAppearance();
			ap.setAttribute(CommonAttributes.FACE_DRAW, false);
			ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
			ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
			ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, false);
			ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(200, 200, 200));
			ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.LINE_WIDTH, 0.5);
			return hypersphere;
		}
		
		/**
		 * A unit euclidean cylinder approximated by a prism with <i>n</i>sides.  The cylinder 
		 * has radius 1, is centered on the z-axis, and goes from z=-1 to z=1.
		 * @param n
		 * @return
		 * @see Cylinder  
		 */
		 public static IndexedFaceSet cylinder(int n )	{
			 return cylinder(n, 1, -1, 1, Math.PI*2);
		 }
		 
		 public static IndexedFaceSet cylinder(int n,   double r, double zmin, double zmax, double thetamax) {
			int rn = n+1;
			double[] verts = new double[2*3*rn];
			double angle = 0, delta = thetamax/(n);
			for (int i = 0 ;i<rn; ++i)	{
				angle = i*delta;
				verts[3*(i+rn)] = verts[3*i] = r*Math.cos(angle);
				verts[3*(i+rn)+1] = verts[3*i+1] = r*Math.sin(angle);
				verts[3*i+2] = zmax;
				verts[3*(i+rn)+2] = zmin;
			}
			QuadMeshFactory qmf = new QuadMeshFactory();//Pn.EUCLIDEAN, n+1, 2, true, false);
			qmf.setULineCount(n+1);
			qmf.setVLineCount(2);
			qmf.setClosedInUDirection(Math.abs(Math.PI*2-thetamax) < 10E-8);
			qmf.setVertexCoordinates(verts);
			qmf.setGenerateEdgesFromFaces(true);
			qmf.setGenerateFaceNormals(true);
			qmf.setGenerateVertexNormals(true);
			qmf.update();
			IndexedFaceSet ifs = qmf.getIndexedFaceSet();
			ifs.setGeometryAttributes(CommonAttributes.RMAN_PROXY_COMMAND, "Cylinder "+r+" "+zmin+" "+zmax+" "+180.0/Math.PI * thetamax);
			return ifs;
		}
		
		/**
		 * Construct a pyramid with vertex <i>tip</i> over the polygon <i>base</i>.
		 * The polygon is assumed to be closed -- so the user need not set the last 
		 * vertex to be the same as the first.
		 * @param base
		 * @param tip
		 * @return
		 */public static IndexedFaceSet pyramid(double[][] base, double[] tip)	{
			int n = base.length;
			int l = base[0].length;
			if (l != tip.length)	{
				throw new IllegalArgumentException("Points must have same dimension");
			}
			double[][] newVerts = new double[n+1][l];
			for (int i = 0; i<n; ++i)		System.arraycopy(base[i], 0, newVerts[i], 0, l);
			System.arraycopy(tip, 0, newVerts[n],0,l);
			int[][] indices = new int[n+1][];
			for (int i = 0; i<n; ++i)	{
				indices[i] = new int[3];
				indices[i][0] = i;
				indices[i][1] = (i+1)%n;
				indices[i][2] = n;
			}
			indices[n] = new int[n];
			for (int i = 0; i<n; ++i)	indices[n][i] = i;
//			IndexedFaceSet ifs = IndexedFaceSetUtility.createIndexedFaceSetFrom(indices, newVerts, null, null,null,null);
			IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
			ifsf.setVertexCount(n+1);
			ifsf.setFaceCount(n+1);
			ifsf.setVertexCoordinates(newVerts);
			ifsf.setFaceIndices(indices);
			ifsf.setGenerateEdgesFromFaces(true);
			ifsf.setGenerateFaceNormals(true);
			ifsf.update();
			return ifsf.getIndexedFaceSet();
		}
		
		/**
		 * Generate a torus knot that winds around the given torus the given number of times.
		 * @param R	major radius
		 * @param r	minor radius
		 * @param n	number of windings around the big circle
		 * @param m	number of windings around the small (meridianal) circle
		 * @param nPts	how many segments in the resulting curve.
		 * @return
		 */
		 public static IndexedLineSet discreteTorusKnot(double R, double r, int n, int m, int nPts)	{
			double[][] vertices = new double[nPts][3];
			for (int i = 0; i<nPts; ++i)	{
				double angle = ( i * 2.0 * Math.PI)/ nPts;
				double a = m * angle, A = n * angle;
				double C = Math.cos(A),			S = Math.sin(A);
				double c = r*Math.cos(a), 			s = r*Math.sin(a);
				
				vertices[i][0] = C * (R + c);
				vertices[i][1] = s;
				vertices[i][2] = S * (R+c);
			}
			return IndexedLineSetUtility.createCurveFromPoints(vertices, true);
		}
		/**
		 * Construct a regular polygon lying in the (x,y) plane, lying on the unit-circle there,
		 * and having <i>order</i> edges.
		 * @param order
		 * @return
		 */
		public static IndexedFaceSet regularPolygon(int order) {
			double[][] verts = new double[order][3];
			for (int  i =0; i<order; ++i)	{
				double angle = 2 * (i+.5) * Math.PI/order;
				verts[i][0] = Math.cos(angle);
				verts[i][1] = Math.sin(angle);
				verts[i][2] = 0.0;
			}
			return IndexedFaceSetUtility.constructPolygon(verts);
		}
		
		/**
		 * 
		 * @return {@link #arrow(double, double, double, double, double, boolean)} with final parameter false.
		 */
		public static IndexedLineSet arrow(double x0, double y0, double x1, double y1, double tipSize)	{
			return arrow(x0, y0, x1, y1, tipSize, false);
		}
		
		/**
		 * Generate an an arrow: a line segment joining (x0,y0) to (x1,y1) with a head at the
		 * second point, each half making a 45 degree angle to the segment.  If <i>halfArrow</i>
		 * is true, then only the left half of the arrow is drawn.
		 * @param x0
		 * @param y0
		 * @param x1
		 * @param y1
		 * @param tipSize
		 * @param halfArrow
		 * @return
		 */
		public static IndexedLineSet arrow(double x0, double y0, double x1, double y1, double tipSize, boolean halfArrow)	{
			IndexedLineSet ifs = new IndexedLineSet(4, 3);
			double[][] verts = new double[4][3];
			verts[0][0] = x0;
			verts[0][1] = y0;
			verts[0][2] = 0.0;
			verts[1][0] = x1; 
			verts[1][1] = y1;
			verts[1][2] = 0.0;
			double dx = (x1 - x0)*tipSize;
			double dy = (y1 - y0)*tipSize;
			verts[2][0] = x1 - dx + dy;
			verts[2][1] = y1 - dy - dx;
			verts[2][2] = 0.0;
			verts[3][0] = x1 - dx - dy;
			verts[3][1] = y1 - dy + dx;
			verts[3][2] = 0.0;
			int[][] indices;
			if (halfArrow) indices = new int[2][2];
			else indices = new int[3][2];
			indices[0][0] = 0;
			indices[0][1] = 1;
			indices[1][0] = 1;
			indices[1][1] = 2;
			if (!halfArrow)	{
				indices[2][0] = 1;
				indices[2][1] = 3;			
			}
			ifs.setVertexCountAndAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(verts));
			ifs.setEdgeCountAndAttributes(Attribute.INDICES, StorageModel.INT_ARRAY.array(2).createReadOnly(indices));
			return ifs;
		}
		
		/**
		 * Create a euclidean clipping plane with the given plane equation. 
		 * @param plane	a 4-vector, the plane coordinates of the plane
		 * @return
		 */
		public static SceneGraphComponent clippingPlane(double[] plane)	{
			return clippingPlane(plane, Pn.EUCLIDEAN);
		}

		/**
		 * Create a  clipping plane with the given plane equation with the given signature. 
		 * The points whose inner product with <i>plane</i> are negative, will be clipped away. 
		 * @param plane
		 * @param sig	
		 * @return
		 */
		public static SceneGraphComponent clippingPlane(double[] plane, int sig)	{
			double[] normal = new double[4];
			System.arraycopy(plane, 0, normal, 0, 3);
			double[] rotation = P3.makeRotationMatrix(null, new double[]{0,0,1}, normal);
			double l = Rn.euclideanNormSquared(normal);
			double[] tform;
			if (l != 0)	{
				double f = -plane[3]/l;
				double[] tlate = new double[4];
				Rn.times(tlate, f, normal);
				tlate[3] = 1.0;
				double[] translation = P3.makeTranslationMatrix(null, 	tlate, sig);
				tform =  Rn.times(null, translation, rotation);
			} else tform = rotation;
			SceneGraphComponent cp = SceneGraphUtility.createFullSceneGraphComponent("clippingPlane");
			cp.getTransformation().setMatrix(tform);
			cp.setGeometry(new ClippingPlane());
			return cp;
		}
   
		/**
		 * Create a torus with the given parameters.
		 * @param bR	Major radius
		 * @param sR	Minor radius
		 * @param bDetail	Number of sample points around major circle
		 * @param sDetail	Number of sample points around minor circle
		 * @return
		 */
		public static IndexedFaceSet torus(final double bR, final double sR, int bDetail, int sDetail) {
		    
		    ParametricSurfaceFactory.Immersion immersion =
		        new ParametricSurfaceFactory.Immersion() {

                    public int getDimensionOfAmbientSpace() {
                        return 3;
                    }

                    public void evaluate(double x, double y, double[] targetArray, int arrayLocation) {
                        // TODO Auto-generated method stub
                        double sRMulSinY=sR*Math.sin(y);
                        targetArray[arrayLocation  ] = Math.cos(-x)*(bR+sRMulSinY);
                        targetArray[arrayLocation+1] = sR*Math.cos(y);
                        targetArray[arrayLocation+2] = Math.sin(-x)*(bR+sRMulSinY);   
                    }

					public boolean isImmutable() {
						return true;
					}
		        
		    };
		    
		    ParametricSurfaceFactory factory = new ParametricSurfaceFactory( immersion);
		    
		    factory.setULineCount(bDetail+1);
		    factory.setVLineCount(sDetail+1);
		    
		    factory.setClosedInUDirection(true);
		    factory.setClosedInVDirection(true);
		    
		    factory.setUMax(2*Math.PI);
		    factory.setVMax(2*Math.PI);
		    
		    factory.setGenerateFaceNormals(true);
		    factory.setGenerateVertexNormals(true);
        
		    factory.update();
		    String rmanproxy = String.format("TransformBegin\n"+
		    		"Rotate 90 1 0 0\n"+
		    		"Torus %f %f 0 360 360\n"+
		    		"TransformEnd\n",new Object[]{new Double(bR), new Double(sR)});
		    factory.getIndexedFaceSet().setGeometryAttributes(CommonAttributes.RMAN_PROXY_COMMAND,rmanproxy);
		    return factory.getIndexedFaceSet();
		}
		
        /**
         * Create a unit sphere centered at the origin
         * using latitude/longitude parametrization.  <i>detail</i> specifies how many samples to use
         * in both directions.
         * @param detail
         * @return
         */public static IndexedFaceSet sphere(final int detail ) {
		    
		    ParametricSurfaceFactory.Immersion immersion =
		        new ParametricSurfaceFactory.Immersion() {

                    public int getDimensionOfAmbientSpace() {
                        return 3;
                    }

                    public void evaluate(double x, double y, double[] targetArray, int arrayLocation) {
                        
                        targetArray[arrayLocation  ] = Math.cos(x)*Math.sin(y);
                        targetArray[arrayLocation+1] = Math.sin(x)*Math.sin(y);
                        targetArray[arrayLocation+2] = Math.cos(y);
                    }

					public boolean isImmutable() {
						return true;
					}
		        
		    };
		    
		    ParametricSurfaceFactory factory = new ParametricSurfaceFactory( immersion);
		    
		    factory.setULineCount(detail+1);
		    factory.setVLineCount(detail+1);
		    
		    factory.setClosedInUDirection(true);
		    factory.setClosedInVDirection(false);
		    
		    factory.setUMax(2*Math.PI);
		    factory.setVMin(1e-5);
		    factory.setVMax(Math.PI-1e-5);
		    
            factory.setGenerateFaceNormals(true);
            factory.setGenerateVertexNormals(true);
            factory.setGenerateTextureCoordinates(true);
		    factory.update();
		    
		    return factory.getIndexedFaceSet();
		}
        
	/**
	 * Generate a textured quadrilateral using the given array <i>points</i>.  This
	 * should have length 12 or 16, depending on whether the point coordinates are dehomogenized 
	 * or not.
	 * @param points
	 * @return
	 */public static IndexedFaceSet texturedQuadrilateral(double[] points) {
		    
		    IndexedFaceSetFactory factory = new IndexedFaceSetFactory();
		    
		    factory.setVertexCount( 4 );
		    factory.setFaceCount(1);
		    factory.setVertexCoordinates(points);
		    factory.setFaceIndices(new int[][] {{ 0,1,2,3}});
		    factory.setVertexTextureCoordinates(new double[] { 0,0,1,0,1,1,0,1});
		    factory.setGenerateVertexNormals(true);
		    factory.setGenerateFaceNormals(true);
		    factory.setGenerateEdgesFromFaces(true);
		    
		    factory.update();
		    
		    return factory.getIndexedFaceSet();
		}
    
    /**
     * Generate a rectangular quad mesh centered at the origin with a mesh of dimension
     * <i>(xDetail,yDetail)</i>.
     * 
     * @param xStep
     * @param yStep
     * @param xDetail
     * @param yDetail
     * @return
     */public static IndexedFaceSet plainQuadMesh(double xStep, double yStep, int xDetail, int yDetail) {
      ParametricSurfaceFactory factory = new ParametricSurfaceFactory(new ParametricSurfaceFactory.DefaultImmersion() {
        public void evaluate(double u, double v) {
          x=u;
          y=v;
          z=0;
        }
      });
      
      factory.setULineCount(xDetail+1);
      factory.setVLineCount(yDetail+1);
      
      factory.setClosedInUDirection(false);
      factory.setClosedInVDirection(false);
      
      factory.setUMin(-xStep*xDetail/2);
      factory.setUMax(xStep*xDetail/2);
      factory.setVMin(-yStep*yDetail/2);
      factory.setVMax(yStep*yDetail/2);
      
      factory.setGenerateFaceNormals(true);
      factory.setGenerateVertexNormals(false); // ??
      
      factory.update();
      
      return factory.getIndexedFaceSet();      
    }
}
