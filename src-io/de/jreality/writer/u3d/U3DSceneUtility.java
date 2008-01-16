package de.jreality.writer.u3d;

import static de.jreality.geometry.GeometryUtility.calculateBoundingBox;
import static de.jreality.geometry.Primitives.sphere;
import static de.jreality.math.MatrixBuilder.euclidean;
import static de.jreality.scene.data.Attribute.INDICES;
import static de.jreality.scene.data.AttributeEntityUtility.createAttributeEntity;
import static de.jreality.shader.CommonAttributes.AMBIENT_COEFFICIENT;
import static de.jreality.shader.CommonAttributes.AMBIENT_COLOR;
import static de.jreality.shader.CommonAttributes.DIFFUSE_COEFFICIENT;
import static de.jreality.shader.CommonAttributes.LIGHTING_ENABLED;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.REFLECTION_MAP;
import static de.jreality.shader.CommonAttributes.SKY_BOX;
import static de.jreality.shader.CommonAttributes.SPECULAR_COEFFICIENT;
import static de.jreality.shader.CommonAttributes.TEXTURE_2D;
import static de.jreality.shader.TextureUtility.createTexture;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Math.PI;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.io.JrScene;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.Light;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.Sphere;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.scene.data.IntArrayArray;
import de.jreality.shader.CubeMap;
import de.jreality.shader.EffectiveAppearance;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.util.Rectangle3D;
import de.jreality.writer.u3d.texture.SphereMapGenerator;

public class U3DSceneUtility {


	
	public static HashMap<SceneGraphComponent, Collection<SceneGraphComponent>> getParentsMap(Collection<SceneGraphComponent> l) {
		HashMap<SceneGraphComponent, Collection<SceneGraphComponent>> r = new HashMap<SceneGraphComponent, Collection<SceneGraphComponent>>();
		for (SceneGraphComponent c : l)
			r.put(c, new LinkedList<SceneGraphComponent>());
		for (SceneGraphComponent c : l) {
			for (int i = 0; i < c.getChildComponentCount(); i++) {
				SceneGraphComponent child = c.getChildComponent(i);
				Collection<SceneGraphComponent> parents = r.get(child);
				parents.add(c);
			}
		}
		return r;
	}
	
	
	
	private static List<SceneGraphComponent> getFlatScene_R(SceneGraphComponent root) {
		LinkedList<SceneGraphComponent> r = new LinkedList<SceneGraphComponent>();
		r.add(root);
		for (int i = 0; i < root.getChildComponentCount(); i++)
			r.addAll(getFlatScene_R(root.getChildComponent(i)));
		HashSet<SceneGraphComponent> uniqueSet = new HashSet<SceneGraphComponent>(r);
		return new LinkedList<SceneGraphComponent>(uniqueSet);
	}	
	
	
	public static List<SceneGraphComponent> getSceneGraphComponents(JrScene scene) {
		return getFlatScene_R(scene.getSceneRoot());
	}
	
	
	public static <
		T extends SceneGraphNode
	> HashMap<T, String> getUniqueNames(Collection<T> l) 
	{
		HashMap<String, List<T>>
			nameMap = new HashMap<String, List<T>>();
		HashMap<T, String> r = new HashMap<T, String>();
		for (T c : l) {
			if (c == null) continue;
			String name = c.getName();
			List<T> cList = nameMap.get(c.getName());
			if (cList == null) {
				cList = new LinkedList<T>();
				nameMap.put(name, cList);
			}
			cList.add(c);
		}
		for (T c : l) {
			if (c == null) continue;
			String name = c.getName();
			List<T> cList = nameMap.get(c.getName());
			if (cList.size() > 1) {
				DecimalFormat df = new DecimalFormat("000");
				int index = 1;
				for (T notUniqueC : cList) {
					String newName = name + df.format(index);
					r.put(notUniqueC, newName);
					index++;
				}
			} else {
				r.put(c, name);
			}
		}
		return r;
	}
	
	private static List<SceneGraphComponent> getViewNodes_R(SceneGraphComponent root) {
		LinkedList<SceneGraphComponent> r = new LinkedList<SceneGraphComponent>();
		if (root.getCamera() != null)
			r.add(root);
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			List<SceneGraphComponent> subList = getViewNodes_R(root.getChildComponent(i));
			if (subList.size() != 0)
				r.addAll(subList);
		}
		HashSet<SceneGraphComponent> uniqueSet = new HashSet<SceneGraphComponent>(r);
		return new LinkedList<SceneGraphComponent>(uniqueSet);
	}	
	
	
	public static List<SceneGraphComponent> getViewNodes(JrScene scene) {
		return getViewNodes_R(scene.getSceneRoot());
	}
	
	
	private static List<SceneGraphComponent> getLightNodes_R(SceneGraphComponent root) {
		LinkedList<SceneGraphComponent> r = new LinkedList<SceneGraphComponent>();
		if (root.getLight() != null)
			r.add(root);
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			List<SceneGraphComponent> subList = getLightNodes_R(root.getChildComponent(i));
			if (subList.size() != 0)
				r.addAll(subList);
		}
		HashSet<SceneGraphComponent> uniqueSet = new HashSet<SceneGraphComponent>(r);
		return new LinkedList<SceneGraphComponent>(uniqueSet);
	}	
	
	
	public static List<SceneGraphComponent> getLightNodes(JrScene scene) {
		return getLightNodes_R(scene.getSceneRoot());
	}
	
	
	public static void printComponents(Collection<SceneGraphComponent> l) {
		System.out.println("SceneGraphComponents -------------------");
		for (SceneGraphComponent c : l) {
			System.out.println(c.getName());
		}
		System.out.println("----------------------------------------");
	}
	
	
	public static <
		T extends SceneGraphNode
	> void printNameMap(HashMap<T, String> map) {
		System.out.println("Names ----------------------------------");
		for (SceneGraphNode c : map.keySet()) {
			System.out.println(c.getName() + " -> " + map.get(c));
		}
		System.out.println("----------------------------------------");
	}
	
	public static void printNodes(String title, Collection<? extends SceneGraphNode> l) {
		System.out.println(title + " -----------------------------");
		for (SceneGraphNode g : l) {
			System.out.println(g.getName());
		}
		System.out.println("----------------------------------------");
	}
	
	public static void printTextures(Collection<U3DTexture> l) {
		System.out.println("Textures -----------------------------");
		for (U3DTexture g : l) {
			System.out.println(g.getImage());
		}
		System.out.println("----------------------------------------");
	}
	
	public static <
		T extends EffectiveAppearance
	> void printAppearanceNameMap(HashMap<T, String> map) {
		System.out.println("Material Names ----------------------------------");
		for (EffectiveAppearance c : map.keySet()) {
			System.out.println(c + " -> " + map.get(c));
		}
		System.out.println("----------------------------------------");
	}
	
	public static <
		T extends U3DTexture
	> void printTextureNameMap(HashMap<T, String> map) {
		System.out.println("Texture Names ----------------------------------");
		for (U3DTexture c : map.keySet()) {
			System.out.println(c.getImage() + " -> " + map.get(c));
		}
		System.out.println("----------------------------------------");
	}
	

	private static List<Geometry> getGeometries_R(SceneGraphComponent root) {
		LinkedList<Geometry> r = new LinkedList<Geometry>();
		if (root.getGeometry() != null)
			r.add(root.getGeometry());
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			List<Geometry> subList = getGeometries_R(root.getChildComponent(i));
			if (subList.size() != 0)
				r.addAll(subList);
		}
		HashSet<Geometry> uniqueSet = new HashSet<Geometry>(r);
		return new LinkedList<Geometry>(uniqueSet);
	}	
	
	
	public static List<Geometry> getGeometries(JrScene scene) {
		return getGeometries_R(scene.getSceneRoot());
	}
	
	
	private static List<Camera> getCameras_R(SceneGraphComponent root) {
		LinkedList<Camera> r = new LinkedList<Camera>();
		if (root.getCamera() != null)
			r.add(root.getCamera());
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			List<Camera> subList = getCameras_R(root.getChildComponent(i));
			if (subList.size() != 0)
				r.addAll(subList);
		}
		HashSet<Camera> uniqueSet = new HashSet<Camera>(r);
		return new LinkedList<Camera>(uniqueSet);
	}	
	
	
	public static List<Camera> getCameras(JrScene scene) {
		return getCameras_R(scene.getSceneRoot());
	}
	
	
	private static List<Light> getLights_R(SceneGraphComponent root) {
		LinkedList<Light> r = new LinkedList<Light>();
		if (root.getLight() != null)
			r.add(root.getLight());
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			List<Light> subList = getLights_R(root.getChildComponent(i));
			if (subList.size() != 0)
				r.addAll(subList);
		}
		HashSet<Light> uniqueSet = new HashSet<Light>(r);
		return new LinkedList<Light>(uniqueSet);
	}	
	
	
	public static List<Light> getLights(JrScene scene) {
		return getLights_R(scene.getSceneRoot());
	}
	
	
	
	public static IndexedFaceSet prepareFaceSet(IndexedFaceSet ifs) {
		IntArrayArray fData = (IntArrayArray)ifs.getFaceAttributes(INDICES);
		int[][] faces = fData.toIntArrayArray(null);
		boolean needsTreatment = false;
		int numFaces = 0; // count faces
		for (int i = 0; i < faces.length; i++) {
			if (faces[i].length != 3) {
				needsTreatment = true;
				numFaces += faces[i].length - 2;
			} else {
				numFaces++;
			}
		}
		if (!needsTreatment)
			return ifs;
		IndexedFaceSet rifs = new IndexedFaceSet();
		int[][] newFaceData = new int[numFaces][];
		int j = 0;
		for (int[]f : faces) {
			if (f.length != 3){
				int v = f.length;
				for (int k = 0; k < v / 2 - 1; k++){
					newFaceData[j++] = new int[]{ f[k], f[k+1], f[v - 1 - k]};
					newFaceData[j++] = new int[]{ f[k+1], f[v - 2 - k], f[v - 1 - k]};
				}
				if (v % 2 != 0) {
					int k = v / 2 - 1;
					newFaceData[j++] = new int[]{ f[k-1], f[k], f[k+1]};
				}
			} else {
				newFaceData[j++] = f;
			}
		}
		rifs.setVertexCountAndAttributes(ifs.getVertexAttributes());
		rifs.setFaceCountAndAttributes(INDICES, new IntArrayArray.Array(newFaceData));
		return rifs;
	}
	
	
	public static HashMap<Geometry, Geometry> prepareGeometry(Collection<Geometry> geometry) {
		HashMap<Geometry, Geometry> r = new HashMap<Geometry, Geometry>();
		for (Geometry g : geometry) {
			if (g instanceof IndexedFaceSet) {
				IndexedFaceSet p = prepareFaceSet((IndexedFaceSet)g);
				r.put(g, p);
			}
			else if (g instanceof IndexedLineSet)
				r.put(g, null);
			else if (g instanceof PointSet)
				r.put(g, g);
			else if (g instanceof Sphere) {
				r.put(g, prepareFaceSet(sphere(30)));
			}
		}
		return r;
	}
	
	
	private static void fillAppearanceMap_R(SceneGraphComponent root, HashMap<SceneGraphComponent, EffectiveAppearance> map) {
		EffectiveAppearance ea = map.get(root);
		for (int i = 0; i < root.getChildComponentCount(); i++) {
			SceneGraphComponent child = root.getChildComponent(i);
			Appearance app = child.getAppearance();
			if (app != null) {
				EffectiveAppearance childEa = ea.create(child.getAppearance());
				map.put(child, childEa);
			} else {
				map.put(child, ea);
			}
			fillAppearanceMap_R(child, map);
		}
	}
	
	
	public static HashMap<SceneGraphComponent, EffectiveAppearance> getAppearanceMap(JrScene scene) {
		HashMap<SceneGraphComponent, EffectiveAppearance> map = new HashMap<SceneGraphComponent, EffectiveAppearance>();
		SceneGraphComponent root = scene.getSceneRoot();
		EffectiveAppearance ea = EffectiveAppearance.create();
		Appearance app = root.getAppearance();
		if (app != null) {
			EffectiveAppearance rootEa = ea.create(root.getAppearance());
			map.put(root, rootEa);
		} else {
			map.put(root, ea);
		}
		fillAppearanceMap_R(root, map);
		// remove non-geometry materials
		LinkedList<SceneGraphComponent> keys = new LinkedList<SceneGraphComponent>(map.keySet());
		for (SceneGraphComponent c : keys) {
			if (c.getGeometry() == null)
				map.remove(c);
		}
		return map;
	}
	
	
	public static HashMap<EffectiveAppearance, String> getAppearanceNames(Collection<EffectiveAppearance> apps) {
		HashMap<EffectiveAppearance, String> map = new HashMap<EffectiveAppearance, String>();
		int number = 1;
		DecimalFormat df = new DecimalFormat("000");
		for (EffectiveAppearance ae : apps) {
			map.put(ae, "Material " + df.format(number));
			number++;
		}
		return map;
	}
	
	
	
	public static HashMap<EffectiveAppearance, U3DTexture> getSphereMapsMap(Collection<EffectiveAppearance> apps) {
		HashMap<EffectiveAppearance, U3DTexture> r = new HashMap<EffectiveAppearance, U3DTexture>();
		for (EffectiveAppearance a : apps) {
		    if (AttributeEntityUtility.hasAttributeEntity(CubeMap.class, POLYGON_SHADER + "." + REFLECTION_MAP, a)) {
		    	CubeMap tex = (CubeMap) createAttributeEntity(CubeMap.class, POLYGON_SHADER + "." + REFLECTION_MAP, a);
		    	BufferedImage img = SphereMapGenerator.create(tex, 768, 768);
		    	ImageData data = new ImageData(img);
		    	U3DTexture u3dTex = new U3DTexture(data);
		    	u3dTex.setIntesity(tex.getBlendColor().getAlpha() / 255.0f);
		    	r.put(a, u3dTex);
		    }
		}
		return r;
	}
	
	
	public static HashMap<CubeMap, byte[]> prepareSphereMap(Collection<CubeMap> maps) {
		HashMap<CubeMap, byte[]> r = new HashMap<CubeMap, byte[]>();
		for (CubeMap cm : maps) {
			BufferedImage bi = SphereMapGenerator.create(cm, 768, 768);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try {
				ImageIO.write(bi, "PNG", buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			r.put(cm, buffer.toByteArray());
		}
		return r;
	}
	
	
	public static HashMap<U3DTexture, String> getTextureNames(String prefix, Collection<U3DTexture> l) {
		HashMap<U3DTexture, String> map = new HashMap<U3DTexture, String>();
		int number = 1;
		DecimalFormat df = new DecimalFormat("000");
		for (U3DTexture ae : l) {
			map.put(ae, prefix + " " + df.format(number));
			number++;
		}
		return map;
	}
	
	
	public static HashMap<EffectiveAppearance, U3DTexture> getTextureMap(Collection<EffectiveAppearance> apps) {
		HashMap<EffectiveAppearance, U3DTexture> r = new HashMap<EffectiveAppearance, U3DTexture>();
		for (EffectiveAppearance a : apps) {
		    if (AttributeEntityUtility.hasAttributeEntity(Texture2D.class, POLYGON_SHADER + "." + TEXTURE_2D, a)) {
		    	Texture2D tex = (Texture2D) createAttributeEntity(Texture2D.class, POLYGON_SHADER + "." + TEXTURE_2D, a);
		    	if (tex != null && tex.getImage() != null)
		    		r.put(a, new U3DTexture(tex));
		    }
		}
		return r;
	}
	
	
	
	public static BufferedImage getBufferedImage(ImageData img) {
		byte[] byteArray = img.getByteArray();
		int w = img.getWidth();
		int h = img.getHeight();
	    BufferedImage bi = new BufferedImage(w, h, TYPE_INT_ARGB);
        WritableRaster raster = bi.getRaster();
        int[] pix = new int[4];
        for (int y = 0, ptr = 0; y < h; y++) {
        	for (int x = 0; x < w; x++, ptr += 4) {
	            pix[0] = 0xFF & byteArray[ptr + 0];
	            pix[1] = 0xFF & byteArray[ptr + 1];
	            pix[2] = 0xFF & byteArray[ptr + 2];
	            pix[3] = 0xFF & byteArray[ptr + 3];
	            raster.setPixel(x, h - y - 1, pix);
        	}
        }
        return bi;
	}
	
	
	
	public static byte[] preparePNGImageData(ImageData img) {
		BufferedImage bi = getBufferedImage(img);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			ImageIO.write(bi, "PNG", buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toByteArray();
	}
	
	
	public static HashMap<U3DTexture, byte[]> preparePNGTextures(Collection<U3DTexture> textures) {
		HashMap<U3DTexture, byte[]> r = new HashMap<U3DTexture, byte[]>();
		for (U3DTexture tex : textures) {
			ImageData img = tex.getImage();
			r.put(tex, preparePNGImageData(img));
		}
		return r;
	}
	
	
	public static HashMap<Geometry, Rectangle3D> getBoundingBoxes(Collection<Geometry> l) {
		HashMap<Geometry, Rectangle3D> r = new HashMap<Geometry, Rectangle3D>();
		for (Geometry g : l) {
			if (g instanceof PointSet) {
				PointSet ps = (PointSet) g;
				r.put(g, calculateBoundingBox(ps));
			} else {
				r.put(g, new Rectangle3D(MAX_VALUE, MAX_VALUE, MAX_VALUE));
			}
		}
		return r;
	}
	
	
	public static SceneGraphComponent getSkyBox(JrScene scene) {
		Appearance rootApp = scene.getSceneRoot().getAppearance();
		if (rootApp == null) return null;
		CubeMap skyBox = (CubeMap)createAttributeEntity(CubeMap.class, SKY_BOX, rootApp, false);
		if (skyBox == null) return null;
		if (skyBox.getFront() 	== null
		||	skyBox.getBack() 	== null
		|| 	skyBox.getLeft() 	== null
		||	skyBox.getRight() 	== null
		|| 	skyBox.getTop() 	== null
		|| 	skyBox.getBottom() 	== null)
			return null;
		SceneGraphComponent r = new SceneGraphComponent();
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(4);
		ifsf.setFaceCount(1);
		ifsf.setVertexCoordinates(new double[][]{{1,1,0},{1,-1,0},{-1,-1,0},{-1,1,0}});
		ifsf.setFaceIndices(new int[][]{{0,1,2,3}});
		double o = 0.005;
		ifsf.setVertexTextureCoordinates(new double[][]{{1-o,1-o},{1-o,o},{o,o},{o,1-o}});
		ifsf.update();
		
		SceneGraphComponent front = new SceneGraphComponent();
		Appearance frontApp = new Appearance();
		createTexture(frontApp, POLYGON_SHADER, skyBox.getFront());
		front.setAppearance(frontApp);
		front.setGeometry(ifsf.getGeometry());
		front.setName("front");
		euclidean().translate(0, 0, 1.0).rotate(PI, 0, 1, 0).assignTo(front);

		SceneGraphComponent back = new SceneGraphComponent();
		Appearance backApp = new Appearance();
		createTexture(backApp, POLYGON_SHADER, skyBox.getBack());
		back.setAppearance(backApp);
		back.setGeometry(ifsf.getGeometry());
		back.setName("back");
		euclidean().translate(0, 0, -1.0).assignTo(back);

		SceneGraphComponent top = new SceneGraphComponent();
		Appearance topApp = new Appearance();
		createTexture(topApp, POLYGON_SHADER, skyBox.getTop());
		top.setAppearance(topApp);
		top.setGeometry(ifsf.getGeometry());
		top.setName("bottom");
		euclidean().translate(0, 1.0, 0).rotate(PI / 2, 1, 0, 0).rotate(-PI / 2, 0, 0, 1).assignTo(top);

		SceneGraphComponent bottom = new SceneGraphComponent();
		Appearance bottomApp = new Appearance();
		createTexture(bottomApp, POLYGON_SHADER, skyBox.getBottom());
		bottom.setAppearance(bottomApp);
		bottom.setGeometry(ifsf.getGeometry());
		bottom.setName("top");
		euclidean().translate(0, -1.0, 0).rotate(-PI / 2, 1, 0, 0).rotate(PI / 2, 0, 0, 1).assignTo(bottom);

		SceneGraphComponent left = new SceneGraphComponent();
		Appearance leftApp = new Appearance();
		createTexture(leftApp, POLYGON_SHADER, skyBox.getLeft());
		left.setAppearance(leftApp);
		left.setGeometry(ifsf.getGeometry());
		left.setName("left");
		euclidean().translate(-1.0, 0, 0).rotate(PI / 2, 0, 1, 0).assignTo(left);

		SceneGraphComponent right = new SceneGraphComponent();
		Appearance rightApp = new Appearance();
		createTexture(rightApp, POLYGON_SHADER, skyBox.getRight());
		right.setAppearance(rightApp);
		right.setGeometry(ifsf.getGeometry());
		right.setName("right");
		euclidean().translate(1.0, 0, 0).rotate(-PI / 2, 0, 1, 0).assignTo(right);
		  
		r.addChildren(front, back, top, bottom, left, right);
		r.setName("skybox");
		euclidean().rotate(PI / 2, 0, 1, 0).rotate(PI, 1, 0, 0).scale(1000.0).assignTo(r);
		
		Appearance skyBoxApp = new Appearance();
		skyBoxApp.setAttribute(POLYGON_SHADER + "." + LIGHTING_ENABLED, false);
		skyBoxApp.setAttribute(POLYGON_SHADER + "." + AMBIENT_COLOR, WHITE);
		skyBoxApp.setAttribute(POLYGON_SHADER + "." + AMBIENT_COEFFICIENT, 1.0);
		skyBoxApp.setAttribute(POLYGON_SHADER + "." + DIFFUSE_COEFFICIENT, 0.0);
		skyBoxApp.setAttribute(POLYGON_SHADER + "." + SPECULAR_COEFFICIENT, 0.0);
		r.setAppearance(skyBoxApp);
		return r;
	}
	
	
	private static void getVisibility_R(
		SceneGraphComponent root, 
		HashMap<SceneGraphComponent, Boolean> map, 
		boolean subTreeV
	) {
		boolean visible = root.isVisible() & subTreeV;
		map.put(root, visible);
		for (int i = 0; i < root.getChildComponentCount(); i++)
			getVisibility_R(root.getChildComponent(i), map, visible);
	}
	
	public static HashMap<SceneGraphComponent, Boolean> getVisibility(JrScene scene) {
		HashMap<SceneGraphComponent, Boolean> r = new HashMap<SceneGraphComponent, Boolean>();
		getVisibility_R(scene.getSceneRoot(), r, true);
		return r;
	}
	
	
}