/*
 * Created on Apr 15, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package de.jreality.worlds;
import java.awt.Color;

import javax.swing.JMenuBar;

import de.jreality.geometry.LabelSet;
import de.jreality.geometry.WingedEdge;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.util.ConfigurationAttributes;
import de.jreality.util.Pn;
import de.jreality.util.SceneGraphUtilities;


/**
 * @author gunn
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class LabelSetDemo extends AbstractLoadableScene {
	SceneGraphComponent icokit;
	public SceneGraphComponent makeWorld()	{
		
		
		WingedEdge oloid = new WingedEdge(3.0d);
		int num = 100;
		for (int i = 0; i<=num; ++i)  	{
			double angle = 2.0 * Math.PI * ( i/((double) num));
			double[] plane = {Math.cos(angle), Math.sin(angle), .4 * Math.cos(2.5*angle), -1d};
			oloid.cutWithPlane(plane);
		} 
		oloid.update();
		SceneGraphComponent oloidkit = new SceneGraphComponent();
		//oloidkit.addChild(oloid);
		oloidkit.setGeometry(oloid);
		//SceneGraphComponent tubes = WingedEdge.createTubesOnEdges(oloid, .05, 8, 8);
//		SceneGraphComponent tubes = TubeUtility.sticks(oloid, .05, Pn.EUCLIDEAN);
//		GeometryUtility.calculateFaceNormals(tubes);
//		GeometryUtility.calculateVertexNormals(tubes);
//		oloidkit.addChild(tubes);
		
		//SceneGraphComponent s1 = Parser3DS.readFromFile("/homes/geometer/gunn/tmp/read3DS/models/space011.3ds");
		SceneGraphComponent theWorld = SceneGraphUtilities.createFullSceneGraphComponent("oloidWorld");
		theWorld.addChild(oloidkit);
		
		SceneGraphComponent label = SceneGraphUtilities.createFullSceneGraphComponent("labels");
		label.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.WHITE);
		LabelSet ls = LabelSet.labelSetFactory(oloid, null);
		label.setGeometry(ls);
		theWorld.addChild(label);
		return theWorld;
	}

	
	public int getSignature() {
		// TODO Auto-generated method stub
		return Pn.EUCLIDEAN;
	}
	
	public boolean isEncompass()	{return true; }
	public boolean addBackPlane()	{ return true; }
	
	public void setConfiguration(ConfigurationAttributes config) {
		// TODO Auto-generated method stub

	}

	public void customize(JMenuBar menuBar, Viewer viewer) {
//		try {
//			viewer.getSceneRoot().getAppearance().setAttribute("backgroundTexture", new Texture2D("/homes/geometer/gunn/Pictures/grabs/arch-solids.jpg"));
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
