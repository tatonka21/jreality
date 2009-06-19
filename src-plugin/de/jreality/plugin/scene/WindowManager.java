package de.jreality.plugin.scene;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.basic.Scene;
import de.jreality.plugin.basic.View;
import de.jreality.plugin.basic.View.RunningEnvironment;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.swing.JFakeFrameWithGeometry;
import de.varylab.jrworkspace.plugin.Controller;
import de.varylab.jrworkspace.plugin.Plugin;
import de.varylab.jrworkspace.plugin.PluginInfo;

public class WindowManager extends Plugin implements ChangeListener {

	RunningEnvironment env;
	
	SceneGraphComponent parent;
	
	SceneGraphComponent windowRoot=new SceneGraphComponent("window root");
	private int
		resX=1024,
		resY=768;
	
	private double screenWidth=2.48;
	private double screenHeight=2.00;
	private double screenHeightOffset = 0.4;
	private double distance=screenWidth/2.0;
	
	private boolean showDesktopBorder=true;
	SceneGraphComponent desktopBorder=new SceneGraphComponent("desktop bounds");
	
	List<WeakReference<JFakeFrameWithGeometry>> frameRefs = new LinkedList<WeakReference<JFakeFrameWithGeometry>>();
	
	public WindowManager() {
		desktopBorder.setAppearance(new Appearance());
		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(desktopBorder.getAppearance(), false);
		DefaultLineShader dls = (DefaultLineShader) dgs.getLineShader();
		dls.setTubeRadius(1.0);
		desktopBorder.setGeometry(IndexedLineSetUtility.createCurveFromPoints(new double[][]{{0,0,0},{resX,0,0},{resX,resY,0},{0,resY,0}}, true));
	}
	
	private void updateWindowRootTransformation() {
		double yTranslation = 0;
		if (env != RunningEnvironment.DESKTOP) yTranslation = screenHeight/2+screenHeightOffset;
		MatrixBuilder.euclidean().translate(0,yTranslation,-distance).scale(screenWidth/resX).rotateX(Math.PI).translate(-resX/2, -resY/2, 0).assignTo(windowRoot);
	}
	
	@Override
	public PluginInfo getPluginInfo() {
		return new PluginInfo("Window Manager", "jReality Group");
	}
	
	@Override
	public void install(Controller c) throws Exception {
		env = c.getPlugin(View.class).getRunningEnvironment();
		updateWindowRootTransformation();
		updateParent(c.getPlugin(Scene.class));
		c.getPlugin(Scene.class).addChangeListener(this);
		setShowDesktopBorder(getShowDesktopBorder());
	}
	
	@Override
	public void uninstall(Controller c) throws Exception {
		c.getPlugin(Scene.class).removeChangeListener(this);
		frameRefs.clear();
		setParent(null);
	}

	private void updateParent(Scene scene) {
		SceneGraphComponent newParent = null;
		if (env == RunningEnvironment.DESKTOP) {
			newParent = scene.getCameraComponent();
		} else {
			newParent = scene.getAvatarComponent();
		}
		setParent(newParent);
	}

	private void setParent(SceneGraphComponent newParent) {
		if (parent == newParent) return;
		if (parent != null) parent.removeChild(windowRoot);
		if (newParent != null) newParent.addChild(windowRoot);
		parent = newParent;
	}
	
	@SuppressWarnings("serial")
	public JFakeFrameWithGeometry createFrame(String title) {
		JFakeFrameWithGeometry ff = new JFakeFrameWithGeometry(title) {
			@Override
			public void toFront() {
				int curLayer=0;
				for (Iterator<WeakReference<JFakeFrameWithGeometry>> frames = frameRefs.iterator(); frames.hasNext(); ) {
					JFakeFrameWithGeometry f = frames.next().get();
					if (f == null || f == this) frames.remove();
					else f.setLayer(curLayer++);
				}
				frameRefs.add(new WeakReference<JFakeFrameWithGeometry>(this));
				setLayer(curLayer);
			}
		};
		ff.setDesktopComponent(windowRoot);
		ff.setDesktopWidth(resX);
		ff.setDesktopHeight(resY);
		ff.setLayer(frameRefs.size());
		frameRefs.add(new WeakReference<JFakeFrameWithGeometry>(ff));
		return ff;
	}
	
	public void setShowDesktopBorder(boolean showDesktopBorder) {
		this.showDesktopBorder = showDesktopBorder;
		windowRoot.removeChild(desktopBorder);
		if (showDesktopBorder) windowRoot.addChild(desktopBorder);
	}

	public boolean getShowDesktopBorder() {
		return showDesktopBorder;
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof Scene) {
			Scene scene = (Scene) e.getSource();
			updateParent(scene);
		}
	}

}
