/*
 * Created on May 12, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package de.jreality.worlds;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JMenuBar;

import net.java.games.jogl.GL;

import de.jreality.geometry.CatenoidHelicoid;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.QuadMeshShape;
import de.jreality.geometry.SphereHelper;
import de.jreality.geometry.Torus;
import de.jreality.geometry.TubeUtility;
import de.jreality.jogl.DiscreteSpaceCurve;
import de.jreality.jogl.SkyBox;
import de.jreality.jogl.shader.DefaultVertexShader;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.ReflectionMap;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Texture2D;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.CameraUtility;
import de.jreality.util.ConfigurationAttributes;
import de.jreality.util.P3;
import de.jreality.util.Pn;
import de.jreality.util.Rn;
import de.jreality.util.SceneGraphUtilities;

/**
 * @author Charles Gunn
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JOGLSkyBox extends AbstractLoadableScene {

	static String resourceDir = "./";
	static {
		String foo = System.getProperty("resourceDir");
		if (foo != null)	resourceDir  = foo;
	}
	//static String resourceDir = "/Users/gunn/Library/Textures/";
	/**
	 * 
	 */
	public JOGLSkyBox() {
		super();
	}

	static double[][] square = {{-1,-1,0},{1,-1,0},{1,1,0},{-1,1,0}};
	static double[][] texc = {{0,0},{1,0},{1,1},{0,1}};

	public boolean encompass()	{ return false; }
	ConfigurationAttributes config = null;
	String configResourceDir = "/homes/geometer/gunn/Software/eclipse/workspace/jReality/";
	/* (non-Javadoc)
	 * @see de.jreality.portal.WorldMaker#setConfiguration(de.jreality.portal.util.Configuration)
	 */
	public void setConfiguration(ConfigurationAttributes config) {
		File f = new File(configResourceDir+"test/de/jreality/worlds/JOGLSkyBox.props");
		this.config = new ConfigurationAttributes(f, config);
		String foo = this.config.getProperty("resourceDir", resourceDir);
		resourceDir = foo;
	}
	
	public boolean addBackPlane()	{ return false; }
	public SceneGraphComponent makeWorld() {

		SceneGraphComponent root = new SceneGraphComponent();
		root.setName("theWorld");
		root.setTransformation(new Transformation());
		
		String[] texNameSuffixes = {"rt","lf","up", "dn","bk","ft"};
		ReflectionMap refm = ReflectionMap.reflectionMapFactory("textures/desertstorm/desertstorm_", texNameSuffixes, "JPG");
		refm.setApplyMode(Texture2D.GL_COMBINE);
		refm.setBlendColor(new Color(1.0f, 1.0f, 1.0f, 0.6f));

		
		double[][] pos = new double[6][3];
		for (int i = 0; i<6; ++i)	{
			double angle = i*Math.PI * 2.0/(6.0);
			pos[i][0] = 4 * Math.cos(angle);
			pos[i][1] = 0.0;
			pos[i][2] = 4 * Math.sin(angle);
		}
		IndexedFaceSet globeSet=new CatenoidHelicoid(40);
		globeSet.setFaceAttributes(Attribute.NORMALS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(GeometryUtility.calculateFaceNormals(globeSet)));
		globeSet.setName("CatHel1");
		SceneGraphComponent globeNode1= new SceneGraphComponent();
		globeNode1.setName("Comp1");
		Transformation gt= new Transformation();
		gt.setTranslation(pos[0]);
		gt.setStretch(.3);
		globeNode1.setTransformation(gt);
	   Appearance ap1 = new Appearance();
	   ap1.setAttribute(CommonAttributes.DIFFUSE_COLOR, java.awt.Color.RED);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, java.awt.Color.BLACK);
	   ap1.setAttribute(CommonAttributes.LIGHTING_ENABLED,true);
	   ap1.setAttribute(CommonAttributes.SMOOTH_SHADING,false);
	   ap1.setAttribute(CommonAttributes.EDGE_DRAW,false);
	   ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, java.awt.Color.WHITE);
	   ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+"textureEnabled",true);
	   double[] vec = {1d, 1.5d, 1d};
	   Texture2D tex2d = null;
	   try {
		tex2d = new Texture2D(Readers.resolveDataInput("textures/grid256rgba.png"));
	   } catch (IOException e1) {
		e1.printStackTrace();
	   }
	   ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.TEXTURE_2D, tex2d);
		//ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+"reflectionMap", refm);
	   tex2d.setTextureMatrix( P3.makeStretchMatrix(null, vec));
	   globeNode1.setAppearance(ap1);
	   //rootAp = ap1;
		globeNode1.setGeometry(globeSet);
  
  
		// 2.
	   CatenoidHelicoid catHel=new CatenoidHelicoid(20);
	   catHel.setName("CatHel2");
	   catHel.setFaceAttributes(Attribute.NORMALS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly( GeometryUtility.calculateFaceNormals(catHel)));
	   catHel.buildEdgesFromFaces();
		SceneGraphComponent globeNode2= new SceneGraphComponent();
		globeNode2.setName("Comp1");
		gt= new Transformation();
		gt.setTranslation(pos[1]);
		gt.setStretch(.3);
		globeNode2.setTransformation(gt);
		globeNode2.setGeometry(catHel);
		//globeNode2.setGeometry(globeSet);
	   ap1 = new Appearance();
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,new Color(255,0,255));
	   ap1.setAttribute(CommonAttributes.LINE_WIDTH,2.0);
	   ap1.setAttribute(CommonAttributes.DIFFUSE_COLOR,new java.awt.Color(.2f, .5f, .5f, 1f));
	   ap1.setAttribute(CommonAttributes.FACE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.EDGE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.VERTEX_DRAW,true);
	   ap1.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED,true);
	   ap1.setAttribute(CommonAttributes.SMOOTH_SHADING,true);
	   ap1.setAttribute(CommonAttributes.NORMALS_DRAW,true);
	   ap1.setAttribute(CommonAttributes.NORMAL_SCALE, 1.0);
	   globeNode2.setAppearance(ap1);
  
 
	   catHel=new CatenoidHelicoid(20);
	   catHel.setName("CatHel3");
	   catHel.buildEdgesFromFaces();
		SceneGraphComponent globeNode3= new SceneGraphComponent();
		gt= new Transformation();
		gt.setTranslation(pos[2]);
		gt.setStretch(.3);
		globeNode3.setTransformation(gt);
		globeNode3.setGeometry(catHel);
	   ap1 = new Appearance();
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(200, 150, 0));
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.EDGE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW,true);
//	   ap1.setAttribute(CommonAttributes.LINE_STIPPLE,true);
//	   ap1.setAttribute(CommonAttributes.LINE_STIPPLE_PATTERN,0x1c47);
//	   ap1.setAttribute(CommonAttributes.ANTIALIASING_ENABLED,true);
	   ap1.setAttribute(CommonAttributes.FACE_DRAW,false);
	   ap1.setAttribute(CommonAttributes.VERTEX_DRAW,false);
	   globeNode3.setAppearance(ap1);

	   DiscreteSpaceCurve torus1 = DiscreteSpaceCurve.discreteTorusKnot(1.0, .4,4,5,400);
	   double[][] pts = torus1.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
	   QuadMeshShape tube = TubeUtility.makeTubeAsIFS(pts, .2d, null, TubeUtility.PARALLEL, true, Pn.EUCLIDEAN,0);
	   tube.setFaceAttributes(Attribute.NORMALS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(GeometryUtility.calculateFaceNormals(tube)));
	   tube.setVertexAttributes(Attribute.NORMALS, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(GeometryUtility.calculateVertexNormals(tube)));
	   SceneGraphComponent globeNode4= new SceneGraphComponent();
	   gt= new Transformation();
	   gt.setTranslation(pos[3]);
	   //gt.setRotation( Math.PI/2.0,1.0, 0.0, 0.0);
	   //gt.setStretch(.3);
	   globeNode4.setTransformation(gt);
	   //globeNode4.setGeometry(torus1);
	   globeNode4.setGeometry(tube);
	   ap1 = new Appearance();
	   ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, java.awt.Color.BLUE);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,java.awt.Color.BLACK);
	   ap1.setAttribute(CommonAttributes.LINE_WIDTH,1.0);
	   ap1.setAttribute(CommonAttributes.POINT_RADIUS,3.0);
	   ap1.setAttribute(CommonAttributes.FACE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.EDGE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.LIGHTING_ENABLED,true);
	   //ap1.setAttribute(CommonAttributes.SMOOTH_SHADING,true);
	   ap1.setAttribute(CommonAttributes.NORMALS_DRAW,false);
	   ap1.setAttribute(CommonAttributes.LINE_WIDTH, 1.0);
	   ap1.setAttribute(CommonAttributes.LINE_STIPPLE,false);
	   ap1.setAttribute(CommonAttributes.VERTEX_DRAW,false);
	   //globeNode4.setAppearance(ap1);
 
	   Torus torus= new Torus(2.3, 1.5, 40,60);
	   torus.calculateNormals();
	   SceneGraphComponent globeNode5= new SceneGraphComponent();
	   gt= new Transformation();
	   gt.setTranslation(pos[4]);
	   //gt.setRotation( Math.PI/2.0,1.0, 0.0, 0.0);
	   gt.setStretch(.3);
	   globeNode5.setTransformation(gt);
	   globeNode5.setGeometry(torus); //SphereHelper.spheres[4]); //torus);
	   
	   ap1 = new Appearance();
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+"reflectionMap", refm);
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, java.awt.Color.YELLOW);
		ap1.setAttribute(CommonAttributes.EDGE_DRAW,false);
	    globeNode5.setAppearance(ap1);
		
	   torus= new Torus(2.3, 1.5, 20, 20);
	   torus.calculateNormals();
	   SceneGraphComponent globeNode6= new SceneGraphComponent();
	   gt= new Transformation();
	   gt.setTranslation(pos[5]);
	   gt.setRotation( Math.PI/2.0,1.0, 0.0, 0.0);
	   gt.setStretch(.3);
	   globeNode6.setTransformation(gt);
	   //globeNode6.setGeometry(torus);
	   globeNode6.setGeometry(GeometryUtility.implode(torus, -.35));
	   //SceneGraphComponent s1 = Parser3DS.readFromFile("/homes/geometer/gunn/tmp/read3DS/models/space011.3ds");
	   //globeNode6.addChild(s1);


		sbkit =SceneGraphUtilities.createFullSceneGraphComponent("skybox");
		ap1 = sbkit.getAppearance();
		ap1.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		ap1.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, java.awt.Color.WHITE);
		faceTex = refm.getFaceTextures();
		SkyBox sb = new SkyBox(faceTex);
		sbkit.addChild(sb);
		sb.getTransformation().setStretch(100.0);
	
		root.addChild(sbkit);

		root.addChild(globeNode1);
		root.addChild(globeNode2);
		root.addChild(globeNode3);
	   root.addChild(globeNode4);
	   root.addChild(globeNode5);	  
	   root.addChild(globeNode6);	

		
		return root;
	}
	
	public boolean isEncompass() {return false;}
 
	Texture2D[] faceTex;
	SceneGraphComponent sbkit;
	public void customize(JMenuBar menuBar, Viewer viewer) {
		//sb.getTransformation().setTranslation(0.0d, 0.0d, -4.0d);
		CameraUtility.getCamera(viewer).setFar(500.0);
	}
}

