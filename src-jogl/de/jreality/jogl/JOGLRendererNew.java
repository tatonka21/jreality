/*
 * Created on Nov 25, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.jreality.jogl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.games.jogl.DebugGL;
import net.java.games.jogl.GL;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLU;
import net.java.games.jogl.util.BufferUtils;
import de.jreality.geometry.SphereHelper;
import de.jreality.jogl.pick.JOGLPickRequestor;
import de.jreality.jogl.pick.PickAction;
import de.jreality.jogl.pick.PickPoint;
import de.jreality.jogl.shader.AbstractJOGLShader;
import de.jreality.jogl.shader.DefaultGeometryShader;
import de.jreality.jogl.shader.RenderingHintsShader;
import de.jreality.scene.Camera;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.Geometry;
import de.jreality.scene.Graphics3D;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.Sphere;
import de.jreality.scene.Transformation;
import de.jreality.scene.event.AppearanceEvent;
import de.jreality.scene.event.AppearanceListener;
import de.jreality.scene.event.GeometryEvent;
import de.jreality.scene.event.GeometryListener;
import de.jreality.scene.event.SceneAncestorListener;
import de.jreality.scene.event.SceneContainerEvent;
import de.jreality.scene.event.SceneContainerListener;
import de.jreality.scene.event.SceneHierarchyEvent;
import de.jreality.scene.event.SceneTreeListener;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.soft.LineShader;
import de.jreality.soft.PointShader;
import de.jreality.soft.PolygonShader;
import de.jreality.soft.ShaderLookup;
import de.jreality.util.CameraUtility;
import de.jreality.util.EffectiveAppearance;
import de.jreality.util.NameSpace;
import de.jreality.util.Rectangle3D;
/**
 * TODO implement  isVisible   bit in SceneGraphNode
 * TODO implement collectAncestorVisitor (see method geometryChanged() at end of file )
 * @author gunn
 *
 */
public class JOGLRendererNew extends SceneGraphVisitor implements JOGLRendererInterface {

	final static Logger theLog;
	static boolean debugGL = false;
	static {
		theLog	= Logger.getLogger("de.jreality.jogl");
		theLog.setLevel(Level.FINEST);
		String foo = System.getProperty("jreality.jogl.debugGL");
		if (foo != null) { if (foo.equals("false")) debugGL = false; else debugGL =true;}
		theLog.setLevel(Level.FINEST);
	}
	
	public final static int MAX_STACK_DEPTH = 30;
	protected int stackDepth;
	JOGLRendererNew globalHandle = null;
	SceneGraphPath currentPath = new SceneGraphPath();
	protected int whichEye;
	
	
	de.jreality.jogl.Viewer theViewer;
	SceneGraphComponent theRoot, auxiliaryRoot;
	JOGLPeerComponent thePeerRoot = null;
	JOGLPeerComponent thePeerAuxilliaryRoot = null;
	ConstructPeerGraphVisitor constructPeer;

	GLCanvas theCanvas;
	Graphics3D gc;
	GL globalGL;
	GLU globalGLU;
	public  boolean texResident;
	int numberTries = 0;		// how many times we have tried to make textures resident
	boolean  useDisplayLists;
	boolean globalIsReflection = false;

	// pick-related stuff
	boolean pickMode = false;
	private final double pickScale = 10000.0;
	Transformation pickT = new Transformation();
	PickPoint[] hits;

	double framerate;
	int lightCount = 0;
	int nodeCount = 0;
	Hashtable geometries = new Hashtable();
	boolean geometryRemoved = false;

	// register for geometry change events
	private class ConstructPeerGraphVisitor extends SceneGraphVisitor	{
		SceneGraphComponent myRoot;
		JOGLPeerComponent thePeerRoot, myParent;
		SceneGraphPath sgp;
		boolean topLevel = true;
		public ConstructPeerGraphVisitor(SceneGraphComponent r, JOGLPeerComponent p)	{
			super();
			myRoot = r;
			sgp = new SceneGraphPath();
			myParent = p;
			//thePeerRoot = p;
		}
		
		private ConstructPeerGraphVisitor(ConstructPeerGraphVisitor pv, JOGLPeerComponent p)	{
			super();
			sgp = (SceneGraphPath) pv.sgp.clone();
			myParent = p;
			topLevel = false;
		}
		
		public void visit(SceneGraphComponent c) {
			sgp.push(c);
			JOGLPeerComponent peer = new JOGLPeerComponent(sgp, myParent);
			if (topLevel) thePeerRoot = peer;
			if (myParent != null) myParent.children.add(peer);
		  	c.childrenAccept(new ConstructPeerGraphVisitor(this, peer));
		  	sgp.pop();
		}

		public Object visit()	{
			visit(myRoot);
			return thePeerRoot;
		}
		
	}
	/**
	 * @param viewer
	 */
	public JOGLRendererNew(de.jreality.jogl.Viewer viewer) {
		super();
		theViewer = viewer;
		gc  = new Graphics3D(viewer);
		theCanvas = ((GLCanvas) viewer.getViewingComponent());
		theRoot = viewer.getSceneRoot();
		useDisplayLists = true;
		theLog.log(Level.FINER, "Looked up logger successfully");
		
		globalHandle = this;
		
		javax.swing.Timer followTimer = new javax.swing.Timer(1000, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {updateGeometryHashtable(); } } );
		followTimer.start();
		
	}



	public SceneGraphComponent getAuxiliaryRoot() {
		return auxiliaryRoot;
	}
	public void setAuxiliaryRoot(SceneGraphComponent auxiliaryRoot) {
		this.auxiliaryRoot = auxiliaryRoot;
		if (auxiliaryRoot != null) {
			ConstructPeerGraphVisitor aux = new ConstructPeerGraphVisitor(this.auxiliaryRoot, null);
			thePeerAuxilliaryRoot = (JOGLPeerComponent) aux.visit();
		}

	}

	/* (non-Javadoc)
	 * @see de.jreality..SceneGraphVisitor#init()
	 */
	public Object visit() {
		//System.err.println("Initializing Visiting");
		// check to see that the root hasn't changed; all other changes handled by hierarchy events
		if (thePeerRoot == null || theViewer.getSceneRoot() != thePeerRoot.getOriginalComponent())	{
			theRoot = theViewer.getSceneRoot();
			constructPeer = new ConstructPeerGraphVisitor( theRoot, null);
			thePeerRoot = (JOGLPeerComponent) constructPeer.visit();	
		}
		
		JOGLRendererHelperNew.initializeGLState(theCanvas);
		
		globalGL.glMatrixMode(GL.GL_PROJECTION);
		globalGL.glLoadIdentity();

		if (!pickMode)
			JOGLRendererHelperNew.handleBackground(theCanvas, theRoot.getAppearance());
		if (pickMode)	{
			globalGL.glMultTransposeMatrixd(pickT.getMatrix());
		}
		theCanvas.setAutoSwapBufferMode(!pickMode);
		
		// We "inline" the visit to the camera since it cannot be visited in the traversal order
		// load the projection transformation
		globalGL.glMultTransposeMatrixd(CameraUtility.getCamera(theViewer).getCameraToNDC(whichEye));

		// prepare for rendering the geometry
		globalGL.glMatrixMode(GL.GL_MODELVIEW);
		globalGL.glLoadIdentity();
		double[] w2c = gc.getWorldToCamera();
		globalGL.glLoadTransposeMatrixd(w2c);
		//isReflection = (Rn.determinant(w2c) > 0) ? false : true;

		if (!pickMode) JOGLRendererHelperNew.processLights(theRoot, globalGL);
		
		JOGLRendererHelperNew.processClippingPlanes(theRoot, globalGL);
		
		nodeCount = 0;			// for profiling info
		texResident = true;		// assume the best ...
		thePeerRoot.render();		
		//System.out.println("Nodes visited in render traversal: "+nodeCount);
		if (!pickMode && thePeerAuxilliaryRoot != null) thePeerAuxilliaryRoot.render();
		globalGL.glLoadIdentity();
		forceResidentTextures();
		return null;
	}
	
	/**
	 * 
	 */
	private void forceResidentTextures() {
		// Try to force textures to be resident if they're not already
		if (!texResident && numberTries < 3)	{
			final Viewer theV = theViewer;
			TimerTask rerenderTask = new TimerTask()	{
				public void run()	{
					theV.render();
				}
			};
			Timer doIt = new Timer();
			doIt.schedule(rerenderTask, 10);
			forceNewDisplayLists();
			//System.out.println("Attempting to force textures resident");
			numberTries++;		// don't keep trying indefinitely
		} else numberTries = 0;
	}


	/**
	 * 
	 */
	private void forceNewDisplayLists() {
		thePeerRoot.setDisplayListDirty(true);
		if (thePeerAuxilliaryRoot != null) thePeerAuxilliaryRoot.setDisplayListDirty(true);
	}


	long frameCount = 0, otime;
	
	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#init(net.java.games.jogl.GLDrawable)
	 */
	public void init(GLDrawable drawable) {
		if (debugGL) {
			drawable.setGL(new DebugGL(drawable.getGL()));
		}
		globalGL = theCanvas.getGL();
		globalGLU = theCanvas.getGLU();

		String vv = globalGL.glGetString(GL.GL_VERSION);
		theLog.log(Level.INFO,"version: "+vv);
		
		otime = System.currentTimeMillis();

		CameraUtility.getCamera(theViewer).setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
		globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());

}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#display(net.java.games.jogl.GLDrawable)
	 */
	public void display(GLDrawable drawable) {
		//if (pickMode) return;
		theCanvas = (GLCanvas) drawable;
		globalGL = theCanvas.getGL();
		Camera theCamera = CameraUtility.getCamera(theViewer);
		if (theCamera != CameraUtility.getCamera(theViewer))	{
			theCamera = CameraUtility.getCamera(theViewer);
			theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
			globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
		}

		//theCamera.update();
		// TODO for split screen stereo, may want to have a real rectangle here, not always at (0,0)
		// for now just do cross-eyed stereo
		if (theCamera.isStereo())		{
			int which = theViewer.getStereoType();
			if (which == Viewer.CROSS_EYED_STEREO)		{
				int w = theCanvas.getWidth()/2;
				int h = theCanvas.getHeight();
				theCamera.setAspectRatio(((double) w)/h);
				theCamera.update();
				whichEye = Camera.RIGHT_EYE;
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				globalGL.glViewport(0,0, w,h);
				visit();
				whichEye = Camera.LEFT_EYE;
				globalGL.glViewport(w, 0, w,h);
				visit();
			} 
			else if (which >= Viewer.RED_BLUE_STEREO &&  which <= Viewer.RED_CYAN_STEREO) {
				theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
				globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				whichEye = Camera.RIGHT_EYE;
		        if (which == Viewer.RED_GREEN_STEREO) globalGL.glColorMask(false, true, false, true);
		        else if (which == Viewer.RED_BLUE_STEREO) globalGL.glColorMask(false, false, true, true);
		        else if (which == Viewer.RED_CYAN_STEREO) globalGL.glColorMask(false, true, true, true);
				visit();
				whichEye = Camera.LEFT_EYE;
		        globalGL.glColorMask(true, false, false, true);
				globalGL.glClear (GL.GL_DEPTH_BUFFER_BIT);
				visit();
		        globalGL.glColorMask(true, true, true, true);
			} 
			else	{
				theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
				globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
				whichEye = Camera.RIGHT_EYE;
				globalGL.glDrawBuffer(GL.GL_BACK_RIGHT);
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				visit();
				whichEye = Camera.LEFT_EYE;
				globalGL.glDrawBuffer(GL.GL_BACK_LEFT);
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				visit();
			}
		} 
		else {
			globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
			globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
			if (!pickMode)	visit();
			else		{
				// set up the "pick transformation"
				IntBuffer selectBuffer = BufferUtils.newIntBuffer(bufsize);
				
				double[] pp3 = new double[3];
				pp3[0] = -pickScale * pickPoint[0]; pp3[1] = -pickScale * pickPoint[1]; pp3[2] = 0.0;
				
				pickT.setTranslation(pp3);
				double[] stretch = {pickScale, pickScale, 1.0};
				pickT.setStretch(stretch);
				boolean store = isUseDisplayLists();
				useDisplayLists = false;
				globalGL.glSelectBuffer(bufsize, selectBuffer);		
				globalGL.glRenderMode(GL.GL_SELECT);
				globalGL.glInitNames();
				globalGL.glPushName(0);
				visit();
				pickMode = false;
				int numberHits = globalGL.glRenderMode(GL.GL_RENDER);
				//System.out.println(numberHits+" hits");
				hits = PickAction.processOpenGLSelectionBuffer(numberHits, selectBuffer, pickPoint,theViewer);
				useDisplayLists = store;
				display(drawable);
			}
		}
		if (++frameCount % 100 == 0) {
			long time = System.currentTimeMillis();
			//theLog.log(Level.FINER,"Frame rate:\t"+(1000000.0/(time-otime)));
			//System.err.println("Frame rate:\t"+(1000000.0/(time-otime)));
			framerate = (100000.0/(time-otime));
			otime = time;
		} 
	}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#displayChanged(net.java.games.jogl.GLDrawable, boolean, boolean)
	 */
	public void displayChanged(GLDrawable arg0, boolean arg1, boolean arg2) {
	}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#reshape(net.java.games.jogl.GLDrawable, int, int, int, int)
	 */
	public void reshape(GLDrawable arg0,int arg1,int arg2,int arg3,int arg4) {
		CameraUtility.getCamera(theViewer).setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
		globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
	}

	/**
	 * @return
	 */
	public boolean isUseDisplayLists() {
		return useDisplayLists;
	}

	/**
	 * @param b
	 */
	public void setUseDisplayLists(boolean b) {
		useDisplayLists = b;
		if (useDisplayLists)	forceNewDisplayLists();
	}

//	public void geometryChanged(GeometryEvent e) {
//		}
		/*
		CollectAncestorsVisitor cav = new CollectAncestorsVisitor(theRoot, sg);
		List anc = (List) cav.visit();
		for (int i = 0; i<anc.size(); ++i)		{
			Object ancx = anc.get(i);
			//System.err.println("ancestor: "+ancx);
			foo = dlTable.get(ancx);
			if (foo!= null) dlTable.remove(ancx);
		}
		*/
//	}
	private final static int POINTDL = 0;
	private final static int LINEDL = 1;
	private final static int FLAT_POLYGONDL = 2;
	private final static int SMOOTH_POLYGONDL = 3;

	private class DisplayListInfo	{
		private boolean validDisplayList, 
					useDisplayList, 	// can decide based on dynamic evaluation whether it makes sense 
					insideDisplayList,
					displayListDirty, realDLDirty[] = new boolean[4];
		private int dl[];
		private int changeCount;
		private long frameCountAtLastChange;
		DisplayListInfo()	{
			super();
			dl = new int[4];
			for (int i = 0; i<4; ++i) { realDLDirty[i] = true; dl[i] = -1;}
			validDisplayList = false;
			insideDisplayList = false;
			frameCountAtLastChange = frameCount;
			changeCount = 0;
		}
		boolean useDisplayList()	{
			if (changeCount <= 3) return true;
			long df = frameCount - frameCountAtLastChange;
			if (useDisplayList && df <= 5) useDisplayList = false;
			else if (!useDisplayList && df >= 5) useDisplayList = true;
			return useDisplayList;
		}

		public void setFlatPolygonDisplayListID(int id)	{
			dl[FLAT_POLYGONDL] = id;
		}
		
		public int getFlatPolygonDisplayListID() {
			return dl[FLAT_POLYGONDL];
		}
		
		public void setSmoothPolygonDisplayListID(int id)	{
			dl[SMOOTH_POLYGONDL] = id;
		}
		
		public int getSmoothPolygonDisplayListID() {
			return dl[SMOOTH_POLYGONDL];
		}
		
		public void setLineDisplayListID(int id)	{
			dl[LINEDL] = id;
		}
		
		public int getLineDisplayListID() {
			return dl[LINEDL];
		}
		
		public void setPointDisplayListID(int id)	{
			dl[POINTDL] = id;
		}
		
		public int getPointDisplayListID() {
			return dl[POINTDL];
		}

		public void setDisplayListID(int type, int id)	{
			dl[type] = id;
		}
		
		public int getDisplayListID(int type) {
			return dl[type];
		}

		public boolean isDisplayListDirty(int type) {
			return realDLDirty[type];
		}
		
		public void setDisplayListDirty(int type, boolean b) {
			realDLDirty[type] = b;
		}
		
		public void setDisplayListsDirty() {
			validDisplayList = false;
			for (int i = 0; i<4; ++i) realDLDirty[i] = true;
		}
		
		public boolean isInsideDisplayList() {
			return insideDisplayList;
		}
		
		public void setInsideDisplayList(boolean b) {
			insideDisplayList = b;
		}
		
		public boolean isValidDisplayList(int type) {
			return false;
		}
		/**
		 * 
		 */
		public void setChange() {
			frameCountAtLastChange = frameCount;
			changeCount++;
			setDisplayListsDirty();
		}
	}
	
	
	public double getFramerate()	{
		return framerate;
	}

	private static  int bufsize = 4096;
	double[] pickPoint = new double[2];
	JOGLPickRequestor  pickRequestor = null;
	public PickPoint[] performPick(double[] p)	{
		if (CameraUtility.getCamera(theViewer).isStereo())		{
			System.out.println("Can't pick in stereo mode");
			return null;
		}
		pickPoint[0] = p[0];  pickPoint[1] = p[1];
		pickMode = true;
		theCanvas.display();	// this calls out display() method  directly
		return hits;
	}
	
	public class JOGLPeerNode	{
		String name;
		
		public String getName()	{
			return name;
		}
		
		public void setName(String n)	{
			name = n;
		}
	}

	/**
	 * 
	 */
	protected void updateGeometryHashtable() {
		Logger.getLogger("de.jreality.jogl").log(Level.FINEST, "Memory usage: "+getMemoryUsage());
		if (geometries == null) return;
		if (!geometryRemoved) return;
		final Hashtable newG = new Hashtable();
		SceneGraphVisitor cleanup = new SceneGraphVisitor()	{
			public void visit(SceneGraphComponent c) {
				if (c.getGeometry() != null) {
					Object peer = geometries.get(c.getGeometry());
					newG.put(c.getGeometry(), peer);
				}
				c.childrenAccept(this);
			}
		};
		cleanup.visit(theRoot);
		//TODO dispose of the peer geomtry nodes which are no longer in the graph
		System.out.println("Old, new hash size: "+geometries.size()+" "+newG.size());
		return;
//		ArrayList removedGeoms = new ArrayList();
//		java.util.Enumeration foo = geometries.keys();
//		while (foo.hasMoreElements())	{
//			Object key = foo.nextElement();
//			if (!newG.containsKey(key)) removedGeoms.add(geometries.get(key));
//		}
		// switch over the hash table
//		synchronized(geometries)	{
//			geometries = newG;
//			//System.out.println("Removing "+removedGeoms.size()+" geometry peers");
//			Iterator iter = removedGeoms.iterator();
//			while (iter.hasNext())	{
//				Object el = iter.next();
//				if (el instanceof JOGLPeerGeometry)	{
//					((JOGLPeerGeometry)el).dispose();
//				}
//			}
//			geometryRemoved = false;
//		}
	}

	public static String getMemoryUsage() {
        Runtime r = Runtime.getRuntime();
        int block = 1024;
        return
                "(memory usage: " + ((r.totalMemory() / block) - (r.freeMemory() / block)) + " kB)";
    }
	
	public class JOGLPeerGeometry extends JOGLPeerNode implements GeometryListener	{
		Geometry originalGeometry;
		Vector proxyGeometry;
		DisplayListInfo dlInfo;
		IndexedFaceSet ifs;
		IndexedLineSet ils;
		PointSet ps;
		int refCount = 0;
		
		protected JOGLPeerGeometry(Geometry g)	{
			super();
			originalGeometry = g;
			name = "JOGLPeer:"+g.getName();
			dlInfo = new DisplayListInfo();
			ifs = null; ils = null; ps = null;
			if (g instanceof IndexedFaceSet) ifs = (IndexedFaceSet) g;
			if (g instanceof IndexedLineSet) ils = (IndexedLineSet) g;
			if (g instanceof PointSet) ps = (PointSet) g;
			originalGeometry.addGeometryListener(this);
		}
		
		public void dispose()		{
			refCount--;
			if (refCount < 0)	{
				System.out.println("Negative reference count!");
			}
			if (refCount == 0)	{
				//System.out.println("Geometry is no longer referenced");
				originalGeometry.removeGeometryListener(this);	
				geometries.remove(originalGeometry);
			}
		}
		
		public void geometryChanged(GeometryEvent ev) {
			System.err.println("JOGLPeerGeometry: geometryChanged");
			//TODO make more differentiated response based on event ev
			final SceneGraphNode sg = (SceneGraphNode) ev.getSource();
			dlInfo.setChange();
		}
		
		
		/**
		 * 
		 */
		public void render(JOGLPeerComponent jpc) {
			//System.out.println("In JOGLPeerGeometry render() for "+originalGeometry.getName());
			RenderingHintsShader renderingHints = jpc.renderingHints;
			DefaultGeometryShader geometryShader = jpc.geometryShader;
			renderingHints.render(globalHandle);
			if (originalGeometry instanceof Sphere)	{
//				IndexedFaceSet foo = getProxyFor((Sphere) originalGeometry);
//				ifs = foo;
//				ils = foo;
//				ps = foo;
				geometryShader.polygonShader.render(globalHandle);
				boolean ss = geometryShader.polygonShader.isSmoothShading();
				// TODO figure out which sphere proxy to use based on distance, LOD, etc
				
				int dlist = JOGLRendererHelperNew.getSphereDLists(2, globalHandle);
				globalGL.glCallList(dlist);
				return;
			}
			if (geometryShader.isFaceDraw() && ifs != null)	{
				geometryShader.polygonShader.render(globalHandle);
				boolean ss = geometryShader.polygonShader.isSmoothShading();
				int type = ss ? SMOOTH_POLYGONDL : FLAT_POLYGONDL;
				if (!processDisplayListState(type))		 // false return implies no display lists used
					JOGLRendererHelperNew.drawFaces(ifs, theCanvas,pickMode, ss);
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						JOGLRendererHelperNew.drawFaces(ifs, theCanvas,pickMode, ss);
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
			if (geometryShader.isEdgeDraw() && ils != null)	{
				geometryShader.lineShader.render(globalHandle);
				int type = LINEDL;
				if (!processDisplayListState(type))		 // false return implies no display lists used
					JOGLRendererHelperNew.drawLines(ils, theCanvas, jpc, pickMode);			
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						JOGLRendererHelperNew.drawLines(ils, theCanvas, jpc, pickMode);			
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
			if (geometryShader.isVertexDraw() && ps != null)	{
				geometryShader.pointShader.render(globalHandle);
				int type = POINTDL;
				boolean spheres = geometryShader.pointShader.isSphereDraw();
				if (spheres || !processDisplayListState(type))		 // false return implies no display lists used
					JOGLRendererHelperNew.drawVertices(ps, globalHandle, geometryShader.pointShader.isSphereDraw(), geometryShader.pointShader.getPointRadius());			
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						JOGLRendererHelperNew.drawVertices(ps, globalHandle, geometryShader.pointShader.isSphereDraw(), geometryShader.pointShader.getPointRadius());			
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
		}

		private boolean processDisplayListState(int type)	{
			DisplayListInfo dl = dlInfo;
			
			//System.out.println("Valid display list for "+pc.getOriginalComponent().getName()+"is "+dl.isValidDisplayList());
			dl.setInsideDisplayList(false);
			if (!useDisplayLists || !dl.useDisplayList()) {
				return false;
			}
			if (!dl.isDisplayListDirty(type))	{
				//System.out.println("Using display list");
				globalGL.glCallList(dlInfo.getDisplayListID(type));
				return true;
			}
			
			if (dl.getDisplayListID(type) != -1) {
				globalGL.glDeleteLists(dl.getDisplayListID(type), 1);
				dl.setDisplayListID(type, -1);
			}
			int nextDL = globalGL.glGenLists(1);

			dl.setDisplayListID(type, nextDL);
			globalGL.glNewList(dl.getDisplayListID(type), GL.GL_COMPILE_AND_EXECUTE);
			//System.out.println("Beginning display list for "+originalGeometry.getName());
			dl.setInsideDisplayList(true);
			return true;
		}

		/**
		 * @param sphere
		 * @return
		 */
		private IndexedFaceSet getProxyFor(Sphere sphere) {
			return SphereHelper.spheres[2];
		}
	}
	
//	public void visit(Sphere sg) {
//		//Primitives.sharedIcosahedron.accept(this);
//		//double lod = renderingHints.getLevelOfDetail();
//		SceneGraphComponent helper = null;
//		System.out.println("Rendering sphere");
//		//if (lod == 0.0) 
//			
//		else	{
//			double area = GeometryUtility.getNDCArea(sg, gc.getObjectToNDC());
//			if (area < .01)	helper = SphereHelper.SPHERE_COARSE;
//			else if (area < .1 ) helper = SphereHelper.SPHERE_FINE;
//			else if (area < .5) helper = SphereHelper.SPHERE_FINER;
//			else helper = SphereHelper.SPHERE_FINEST;
//		}
//		SphereHelper.SPHERE_FINE.accept(this);
//		//Rectangle3D uc = Rectangle3D.unitCube;
//		//SphereHelper.SPHERE_BOUND.accept(this);
//	}
	
	public  JOGLPeerGeometry getJOGLPeerGeometryFor(Geometry g)	{
		JOGLPeerGeometry pg;
		synchronized(geometries)	{
			pg = (JOGLPeerGeometry) geometries.get(g);
			if (pg != null) return pg;
			pg = new JOGLPeerGeometry(g);
			geometries.put(g, pg);			
		}
		return pg;
	}
	
	public class JOGLPeerComponent extends JOGLPeerNode implements TransformationListener, AppearanceListener,
		SceneAncestorListener, SceneContainerListener, SceneTreeListener {
		
		public int[] bindings = new int[2];
		SceneGraphComponent originalComponent;
		EffectiveAppearance eAp;
		Vector children;
		DisplayListInfo dlInfo;
		JOGLPeerComponent parent;
		JOGLPeerGeometry jpg;
		
		Rectangle3D childrenBound;
		Rectangle2D ndcExtent;
		SceneGraphPath	pathToHere;
		boolean isReflection = false;
		
		// for now, keep the primitive shading support
		RenderingHintsShader renderingHints;
		DefaultGeometryShader geometryShader;
		
		
		boolean appearanceIsDirty,
			geometryIsDirty,
			boundIsDirty,
			object2WorldDirty;

		public JOGLPeerComponent(SceneGraphPath sgp, JOGLPeerComponent p)		{
			super();
			if (sgp == null || !(sgp.getLastElement() instanceof SceneGraphComponent))  {
				System.out.println("Invalid parameters to constructor JOGLPeerComponent");
			} else {
				pathToHere = sgp;
				originalComponent = sgp.getLastComponent();
				name = "JOGLPeer:"+originalComponent.getName();
				appearanceIsDirty = true;
				geometryIsDirty = true;
				boundIsDirty = true;
				object2WorldDirty = true;
				children = new Vector();		// always have a child list, even if it's empty
				dlInfo = new DisplayListInfo();
				parent = p;
				if (originalComponent.getGeometry() != null)  {
					jpg = getJOGLPeerGeometryFor(originalComponent.getGeometry());
					jpg.refCount++;
				} else jpg = null;
				originalComponent.addSceneAncestorListener(this);
				originalComponent.addSceneContainerListener(this);
				originalComponent.addSceneTreeListener(this);
				if (originalComponent.getAppearance() != null) originalComponent.getAppearance().addAppearanceListener(this);				
			}
		}
		
		public void dispose()	{
			originalComponent.removeSceneAncestorListener(this);
			originalComponent.removeSceneContainerListener(this);
			originalComponent.removeSceneTreeListener(this);
			if (originalComponent.getAppearance() != null) originalComponent.getAppearance().removeAppearanceListener(this);
			//if (jpg != null)		jpg.dispose();
	
			int n = children.size();
			for (int i = n-1; i>=0; --i)	{
				JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
				child.dispose();
			}

		}
		public void render()		{
			int n = children.size();
			//System.out.println("Rendering "+originalComponent.getName());
			if (!originalComponent.isVisible()) return;
						
			// the following looks very dangerous
			//originalComponent.preRender(gc);
			
			nodeCount++;
			currentPath.push(originalComponent);
			gc.setCurrentPath(currentPath);
			Transformation thisT = originalComponent.getTransformation();
			
			//System.out.println("In JOGLPeerComponent render() for "+originalComponent.getName());
			boolean isR = false;
			if (thisT != null)	{
				//TODO should this reflection management be done in the TransformationChanged() method?
				isR = thisT.getIsReflection();
				if (stackDepth <= MAX_STACK_DEPTH) {
					globalGL.glPushMatrix();
					globalGL.glMultTransposeMatrixd(thisT.getMatrix());
				}
				else globalGL.glLoadTransposeMatrixd(gc.getObjectToCamera());				
			}  
			if (parent != null) isReflection = (isR != parent.isReflection);
			else isReflection = isR;		// should depend on camera transformation ...
			globalGL.glFrontFace(isReflection? GL.GL_CW : GL.GL_CCW);

			if (appearanceIsDirty)	updateAppearance();
			gc.setEffectiveAppearance(eAp);
			
			// render the geometry
			if (jpg != null)	{
				jpg.render(this);
			}
			
			// render the children
			for (int i = 0; i<n; ++i)	{		
				JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
				
				if (pickMode)	globalGL.glPushName(getIndexofPeerChild(i));
				child.render();
				if (pickMode)	globalGL.glPopName();
			}
			
			if (thisT != null)	{
				if (stackDepth <= MAX_STACK_DEPTH) globalGL.glPopMatrix();
				stackDepth--;
			}			
			currentPath.pop();
		}
		

		/**
		 * @return
		 */
		private int getIndexofPeerChild(int which) {
			JOGLPeerComponent child = (JOGLPeerComponent) children.get(which);
			for (int i = 0; i<children.size(); ++i)	{
				if (originalComponent.getChildComponent(i) == child.originalComponent)	{
					return i;
				}
			} 
			System.out.println("Invalid peer child "+which);
			return -1;
		}

		private void setDisplayListDirty(boolean b)	{
			dlInfo.setDisplayListsDirty();
			int n = children.size();
			for (int i = 0; i<n; ++i)	{		
				JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
				child.setDisplayListDirty(b);
			}
		}
		
		private void updateAppearance()	{
			if (parent == null)	{
				if (eAp == null) eAp = EffectiveAppearance.create();
				if (originalComponent.getAppearance() != null )	
					eAp = eAp.create(originalComponent.getAppearance());
				// TODO figure out why I put this here in the first place
				//else eAp = eAp.create(new Appearance());				
			} else {
				if ( parent.eAp == null)	{
					System.out.println("No effective appearance in parent "+parent.getName());
					return;
				}
				if (originalComponent.getAppearance() != null )	
					eAp = parent.eAp.create(originalComponent.getAppearance());
				else eAp = parent.eAp;				
			}
			geometryShader = DefaultGeometryShader.createFromEffectiveAppearance(eAp, "");
			renderingHints = RenderingHintsShader.createFromEffectiveAppearance(eAp, "");
			appearanceIsDirty = false;
		}
		
		public void ancestorAttached(SceneHierarchyEvent ev) {
		}
		
		public void ancestorDetached(SceneHierarchyEvent ev) {
		}
		
		/**
		 * @param sgc
		 * @return
		 */
		private JOGLPeerComponent getPeerForChildComponent(SceneGraphComponent sgc) {
			
			int n = children.size();
			for (int i = 0; i<n; ++i)	{
				JOGLPeerComponent jpc = (JOGLPeerComponent) children.get(i);
				if ( jpc.originalComponent == sgc) { // found!
					return jpc;
				}
			}
			return null;
		}

		public void childAdded(SceneContainerEvent ev) {
			//System.out.println("Container Child added to: "+originalComponent.getName());
			//System.out.println("Event is: "+ev.toString());
			switch (ev.getChildType() )	{
				case SceneContainerEvent.CHILD_TYPE_COMPONENT:
					SceneGraphComponent sgc = (SceneGraphComponent) ev.getNewChildElement();
					ConstructPeerGraphVisitor pv = new ConstructPeerGraphVisitor(sgc, this);
					JOGLPeerComponent pc = (JOGLPeerComponent) pv.visit();
					children.add(pc);
					//pc.updateAppearance();
					break;
				case SceneContainerEvent.CHILD_TYPE_GEOMETRY:
					if (jpg != null)	{
						jpg.refCount--;
						System.out.println("Warning: Adding geometry while old one still valid");
						jpg=null;
					}
					if (originalComponent.getGeometry() != null)  {
						jpg = getJOGLPeerGeometryFor(originalComponent.getGeometry());
						jpg.refCount++;
					} 
					break;
				
			}
		}
		
		public void childRemoved(SceneContainerEvent ev) {
			//System.out.println("Container Child removed from: "+originalComponent.getName());
			switch (ev.getChildType() )	{
				case SceneContainerEvent.CHILD_TYPE_COMPONENT:
					SceneGraphComponent sgc = (SceneGraphComponent) ev.getOldChildElement();
				    JOGLPeerComponent jpc = getPeerForChildComponent(sgc);
				    //System.out.println("removing peer "+jpc.getName());
				    children.remove(jpc);
				    jpc.dispose();		// there are no other references to this child
					break;
				case SceneContainerEvent.CHILD_TYPE_GEOMETRY:
					if (jpg != null) {
						jpg.dispose();		// really decreases reference count
						jpg = null;
					}
					geometryRemoved = true;
					break;
			
		}
		}
		
		public void childReplaced(SceneContainerEvent ev) {
			//System.out.println("Container Child replaced at: "+originalComponent.getName());
			switch(ev.getChildType())	{
				case SceneContainerEvent.CHILD_TYPE_APPEARANCE:
					setAppearanceDirty();
					break;
				case SceneContainerEvent.CHILD_TYPE_GEOMETRY:
					if (jpg != null && jpg.originalGeometry == originalComponent.getGeometry()) break;		// no change, really
					if (jpg != null) {
						jpg.dispose();
						geometryRemoved=true;
						jpg = null;
					}
					if (originalComponent.getGeometry() != null)  {
						jpg = getJOGLPeerGeometryFor(originalComponent.getGeometry());
						jpg.refCount++;
					} 
					break;
				default:
					System.out.println("Taking no action for child type "+ev.getChildType());
					break;
			}
		}
		
		public void childAdded(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child added");
		}
		
		public void childRemoved(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child removed");
		}
		
		public void childReplaced(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child replaced");
		}
		
		public void transformationMatrixChanged(TransformationEvent ev) {
			// TODO notify ancestors that their bounds are no longer valid
		}
		
		public void appearanceChanged(AppearanceEvent ev) {
			//System.out.println("Appearance change "+originalComponent.getName());
			setAppearanceDirty();
		}
		
		private void setAppearanceDirty()	{
			appearanceIsDirty = true;
			dlInfo.setDisplayListsDirty();
			int n = children.size();
			for (int i = 0; i<n; ++i)	{		
				JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
				child.setAppearanceDirty();
			}
		}
		
		public SceneGraphComponent getOriginalComponent() {
			return originalComponent;
		}
		public void setOriginalComponent(SceneGraphComponent originalComponent) {
			this.originalComponent = originalComponent;
		}
	}
	/**
	 * @return
	 */
	public GLCanvas getCanvas()	{
		return theCanvas;
	}

}
