/*
 * Created on Mar 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.jreality.worlds;
import de.jreality.geometry.SphereHelper;
import de.jreality.jogl.shader.ReflectionMap;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Texture2D;
import de.jreality.util.SceneGraphUtilities;

/**
 * @author Charles Gunn
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReflectionMapDemo extends AbstractLoadableScene {

	static String resourceDir = "/homes/geometer/gunn/Pictures/textures/";
	static {
		String foo = System.getProperty("resourceDir");
		if (foo != null)	resourceDir  = foo;
	}
	//static String resourceDir = "/Users/gunn/Library/Textures/";

	Texture2D[] faceTex;
	/**
	 * 
	 */
	public ReflectionMapDemo() {
		super();
	}

	static double[][] square = {{-1,-1,0},{1,-1,0},{1,1,0},{-1,1,0}};
	static double[][] texc = {{0,0},{1,0},{1,1},{0,1}};

	public boolean isEncompass()	{ return true; }
	public boolean addBackPlane()	{ return false; }
	String configResourceDir = "/homes/geometer/gunn/Software/eclipse/workspace/jReality/";
	
	public SceneGraphComponent makeWorld() {

		SceneGraphComponent world = SceneGraphUtilities.createFullSceneGraphComponent("reflectionMap");
		SceneGraphComponent child = SceneGraphUtilities.createFullSceneGraphComponent("child");
		child.setGeometry(SphereHelper.spheres[4]);
		child.getTransformation().setStretch(1.0, 0.6, 1.3);
		world.addChild(child);
		String[] texNameSuffixes = {"rt","lf","up", "dn","bk","ft"};
		ReflectionMap refm = ReflectionMap.reflectionMapFactory("/homes/geometer/gunn/Pictures/textures/desertstorm/desertstorm_", texNameSuffixes, "JPG");
		world.getAppearance().setAttribute(CommonAttributes.POLYGON_SHADER+"."+"reflectionMap", refm);
		world.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW,false);
		return world;
	}
}

