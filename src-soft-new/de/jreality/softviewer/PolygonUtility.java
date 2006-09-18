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

package de.jreality.softviewer;

import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import com.sun.org.apache.bcel.internal.generic.NEWARRAY;

/**
 * This class capsules utility methods for intersecting polygons. It is
 * <em> not</em> speed optimized. It is mainly for file exports at the moment.
 * 
 * @version 1.0
 * @author <a href="mailto:hoffmann@math.tu-berlin.de">Tim Hoffmann</a>
 */
public class PolygonUtility {
    private static final boolean DEBUG = false;

    private static final int CLIPPED_OUT = -1;

    private static final int CLIPPED_PARTIAL = 1;

    private static final int CLIPPED_IN = 0;

    /**
     * 
     */
    public PolygonUtility() {
        super();
    }

    private static int once = 0;

    public static void dehomogenize(AbstractPolygon p) {
        for (int i = 0; i < p.getLength(); i++) {
            double[] vd = p.getPoint(i);
            double w = 1 / vd[AbstractPolygon.SW];
            vd[AbstractPolygon.SX] *= w;
            vd[AbstractPolygon.SY] *= w;
            vd[AbstractPolygon.SZ] *= w;
            vd[AbstractPolygon.SW] = 1;
        }

    }

    private static double[] zNormal = new double[] {0,0,1};
    
    
    /**
     * we assume that the triangles are dehomogenized
     * 
     * @param a
     *            the triangle to test
     * @param b
     *            the triangle to test against
     * @return 1 if a lies behind or is independent of b, 0 if a and b
     *         intersect, and -1 if b lies behind a
     */
    public static int liesBehind(Triangle a, Triangle b) {
        // first test: coarse z separation:
        if (maxZ(b) < minZ(a))
            return 1;
        if (maxZ(a) < minZ(b))
            return -1;

        
        double[] x = b.getPoint(0);
        double[] y = b.getPoint((0 + 1) % 3);
        double[] z = b.getPoint((0 + 2) % 3);

        // find the plane through the three
        double[] d1 = VecMat.difference(y, Triangle.SX, x,
                Triangle.SX);
        double[] d2 = VecMat.difference(z, Triangle.SX, x,
                Triangle.SX);

        double[] normalA = new double[3];
        VecMat.cross(d1, d2, normalA);
        VecMat.normalize(normalA);
        double d = VecMat.dot(normalA,0,
                x,Polygon.SX);
        
        // test if both lie in same plane:
        if(
                Math.abs(VecMat.dot(normalA,0,a.getP0(),Polygon.SX)-d)<EPS &&
                Math.abs(VecMat.dot(normalA,0,a.getP1(),Polygon.SX)-d)<EPS &&
                Math.abs(VecMat.dot(normalA,0,a.getP2(),Polygon.SX)-d)<EPS
                ) 
            return 1;
        
        
        int sign = (int) Math.signum(VecMat.dot(normalA, zNormal));
        int result = testPolygonInHalfSpace(a,Polygon.SX, normalA,-sign,d);
        
        if(result == CLIPPED_IN) {
            //System.out.println("fast track");
            return 1;
        }

        
        // clip along teh sides of b
        // if nothing is left a was behind
        AbstractPolygon p = a;
        Polygon clipPoly = null;
        for (int j = 0; j < 3; j++) {
            double[] lx = b.getPoint(j);
            double[] ly = b.getPoint((j + 1) % 3);
            double[] lz = b.getPoint((j + 2) % 3);

            // find the plane through x, y, and z normal
            double[] ld1 = VecMat.difference(lx, Triangle.SX,ly, Triangle.SX);
            //d2 = VecMat.difference(z, Triangle.SX, zNormal,1);

            double[] lnormalA = new double[3];
            VecMat.cross(ld1, zNormal, lnormalA);
            VecMat.normalize(lnormalA);
            double ldist = VecMat.dot(lnormalA,0, lx,Triangle.SX);
            int lsign = (int) Math.signum( VecMat.dot(lnormalA,0, lz,Triangle.SX)-ldist);
                   
            clipPoly = new Polygon(3);

            int lresult = clipToHalfspace(p,Polygon.SX,lnormalA,-lsign,ldist,clipPoly);
            if(lresult == CLIPPED_OUT) return 1;
            if(lresult == CLIPPED_PARTIAL)
                p = clipPoly;
        }
        
        //find what is potentially in front:
        Polygon poly  = new Polygon(3);
        result = clipToHalfspace(p,Polygon.SX,normalA,-sign,d,poly);
        
        if(result == CLIPPED_PARTIAL)
            return 0;
        if(result == CLIPPED_IN)
            return 1;
        else 
            return -1;
    }

    
    public static Triangle[] cutOut(Triangle a, Triangle b) {

        Vector<Triangle>  v = new Vector<Triangle>();
        
        // clip along the sides of b
        // if nothing is left a was behind
        AbstractPolygon p = a;
        Polygon clipPoly = null;
        for (int j = 0; j < 3; j++) {
            double[] lx = b.getPoint(j);
            double[] ly = b.getPoint((j + 1) % 3);
            double[] lz = b.getPoint((j + 2) % 3);

            // find the plane through x, y, and z normal
            double[] ld1 = VecMat.difference(lx, Triangle.SX,ly, Triangle.SX);
            //d2 = VecMat.difference(z, Triangle.SX, zNormal,1);

            double[] lnormalA = new double[3];
            VecMat.cross(ld1, zNormal, lnormalA);
            VecMat.normalize(lnormalA);
            double ldist = VecMat.dot(lnormalA,0, lx,Triangle.SX);
            int lsign = (int) Math.signum( VecMat.dot(lnormalA,0, lz,Triangle.SX)-ldist);
                   
            clipPoly = new Polygon(3);

            int lresult = clipToHalfspace(p,Polygon.SX,lnormalA,-lsign,ldist,clipPoly);
            if(lresult == CLIPPED_PARTIAL) {
                Polygon poly  = new Polygon(3);
                clipToHalfspace(p,Polygon.SX,lnormalA,lsign,ldist,poly);
                v.addAll(Arrays.asList(poly.triangulate(null,new ArrayStack(0))));
                p = clipPoly;
            }
            if(lresult == CLIPPED_OUT) {
                //if(p.getLength()>2)
                v.addAll(Arrays.asList(p.triangulate(null,new ArrayStack(0))));
                p = clipPoly;
                clipPoly.setLength(0);
                break;
            }
                
        }
        if(p.getLength() != 0) {
            //v.addAll(Arrays.asList(p.triangulate(null,new ArrayStack(0))));
//            double[] x = b.getPoint(0);
//            double[] y = b.getPoint((0 + 1) % 3);
//            double[] z = b.getPoint((0 + 2) % 3);
//
//            // find the plane through the three
//            double[] d1 = VecMat.difference(y, Triangle.SX, x,
//                    Triangle.SX);
//            double[] d2 = VecMat.difference(z, Triangle.SX, x,
//                    Triangle.SX);
//
//            double[] normalA = new double[3];
//            VecMat.cross(d1, d2, normalA);
//            VecMat.normalize(normalA);
//            double d = VecMat.dot(normalA,0,
//                    x,Polygon.SX);
//            int sign = (int) Math.signum(VecMat.dot(normalA, zNormal));
//            int result = testPolygonInHalfSpace(a,Polygon.SX, normalA,sign,d);  
//            if(result == CLIPPED_IN) {
//                v.addAll(Arrays.asList(p.triangulate(null,new ArrayStack(0))));
//            } else if(result == CLIPPED_PARTIAL) {
//                
//            Polygon poly  = new Polygon (3);
//                result = clipToHalfspace(p,Polygon.SX,normalA,-sign,d,poly);
//            }
        }
        
        return v.toArray(new Triangle[0]);
    }
    
    
    
    private static String s(double[] u) {
        return "("+u[0]+", "+u[1]+", "+u[2]+")";
    }

    private static double minZ(Triangle t) {
        return Math.min(t.getP0()[AbstractPolygon.SZ], Math.min(
                t.getP1()[AbstractPolygon.SZ], t.getP2()[AbstractPolygon.SZ]));
    }

    private static double maxZ(Triangle t) {
        return Math.max(t.getP0()[AbstractPolygon.SZ], Math.max(
                t.getP1()[AbstractPolygon.SZ], t.getP2()[AbstractPolygon.SZ]));
    }

    private static final double EPS = 0.00000001;
    //private static final double EPS = 0.01;

    private static int testTriangleInHalfSpace(Triangle t,
            double[] planeNormal, int sign, double k
    // TrianglePipeline pipeline
    ) {
        if (DEBUG) {
            System.out.println(" Normal: (" + planeNormal[0] + ", "
                    + planeNormal[1] + ", " + planeNormal[2] + ")");
        }

        // ****
        int hin = 0;
        int hout = 0;
        double[] vd = t.getP0();
        double test = sign
                * (VecMat.dot(vd, Triangle.SX, planeNormal, 0) - k
                        * vd[Triangle.SW]);
        if (test < -EPS)
            hin++;
        else if (test > EPS)
            hout++;

        vd = t.getP1();
        test = sign
                * (VecMat.dot(vd, Triangle.SX, planeNormal, 0) - k
                        * vd[Triangle.SW]);
        if (test < -EPS)
            hin++;
        else if (test > EPS)
            hout++;

        vd = t.getP2();
        test = sign
                * (VecMat.dot(vd, Triangle.SX, planeNormal, 0) - k
                        * vd[Triangle.SW]);
        if (test < -EPS)
            hin++;
        else if (test > EPS)
            hout++;

        if (hin == 0) {
            return CLIPPED_OUT;
        }
        if (hout == 0) {
            return CLIPPED_IN;
        }
        return CLIPPED_PARTIAL;
    }

    public static int clipTriangleToHalfspace(
    // int polPos,
            Triangle tri, double[] planeNormal, int sign, double k, Triangle a, // for
            // the
            // result
            Triangle b // for the result
    ) {

        int testResult = testTriangleInHalfSpace(tri, planeNormal, sign, k);

        if (testResult != CLIPPED_PARTIAL)
            return testResult;

        int result = 1;

        double[] u = tri.getPoint(2);
        double[] v = tri.getPoint(0);
        double tu = sign
                * (VecMat.dot(u, Triangle.SX, planeNormal, 0) - k
                        * u[Triangle.SW]);
        double tv = 0;
        // HERE!!!!!

        int newTriVertex = 0;
        Triangle newTri = a;
        a.setShadingFrom(tri);
        for (int i = 0; i < 3; i++, u = v, tu = tv) {
            v = tri.getPoint(i);
            tv = sign
                    * (VecMat.dot(v, Triangle.SX, planeNormal, 0) - k
                            * v[Triangle.SW]);
            if (DEBUG)
                System.out.println(" new tv " + tv);
            if (tu <= 0. ^ tv <= 0.) { // edge crosses plane...
                double t = tu / (tu - tv);

                double[] vd = newTri.getPoint(newTriVertex);
                for (int j = 0; j < Triangle.VERTEX_LENGTH; j++) {
                    vd[j] = u[j] + t * (v[j] - u[j]);
                }
                newTriVertex++;
            }
            if (tv <= 0.) { // vertex v is in ...
                newTri.setPointFrom(newTriVertex++, v);

                // newP.vertices[newP.length++] = v;
            }
            if (newTriVertex == 3) {
                b.setPointFrom(0, a.getP0());
                b.setPointFrom(1, a.getP2());
                b.setShadingFrom(tri);
                newTri = b;

                newTriVertex = 2;
                result = 2;
            }
        }
        return result;
    }

    private static int testPolygonInHalfSpace(final AbstractPolygon p,
            final int off, final double[] planeNormal, final int sign,
            final double k) {
        if (DEBUG) {
            System.out.println(" Normal: (" + planeNormal[0] + ", "
                    + planeNormal[1] + ", " + planeNormal[2] + ")");
            System.out.println(" p.length: " + p.getLength());
        }
        int length = p.getLength();
        if (length == 0)
            return CLIPPED_OUT;

        // ****
        int hin = 0;
        int hout = 0;

        for (int i = 0; i < length; i++) {
            double[] v = p.getPoint(i);

            double test = sign
                    * (VecMat.dot(v, off, planeNormal, 0) - k * v[off + 3]);
            if (DEBUG)
                System.out.println(" vertex: (" + v[off] + "," + " "
                        + v[off + 1] + ", " + v[off + 2] + ") ~ " + test);

            //original:
// if (test < -EPS)
// hin++;
// else if (test > EPS)
// hout++;
            
//            if (test < EPS)
//                hin++;
//                else if (test > -EPS)
//                hout++;
            if (test < -EPS)
                hin++;
            else if (test > EPS)
                hout++;
// 
        }
        if (hin == 0) {
            return CLIPPED_OUT;
        }
        if (hout == 0) {
            return CLIPPED_IN;
        }
        return CLIPPED_PARTIAL;
    }

    /**
     * @param p
     *            the polygon to clip
     * @param off
     *            the offset into vertex data (AbstractPolygon.SX or
     *            AbstractPolygon.WX
     * @param planeNormal
     *            the normal of the plane to clip against
     * @param sign
     *            the side of the plane
     * @param k
     *            the distance of the plane from the origin
     * @param dst
     *            the polygon to place the clipped result in.
     * @return
     */
    public static int clipToHalfspace(final AbstractPolygon p, final int off,
            final double[] planeNormal, final int sign, final double k,
            final Polygon dst) {

        int testResult = testPolygonInHalfSpace(p, off, planeNormal, sign, k);

        if (testResult != CLIPPED_PARTIAL)
            return testResult;
        // testResult == 1;
        int length = p.getLength();

        double[] u = p.getPoint(length - 1);
        double[] v = p.getPoint(0);
        double tu = sign
                * (VecMat.dot(u, off, planeNormal, 0) - k * u[off + 3]);
        double tv = 0;
        // HERE!!!!!
        dst.setLength(0);
        int pos = 0;
        for (int i = 0; i < length; i++, u = v, tu = tv) {
            v = p.getPoint(i);
            tv = sign * (VecMat.dot(v, off, planeNormal, 0) - k * v[off + 3]);
            if (DEBUG)
                System.out.println(" new tv " + tv);
            if (tu <= 0. ^ tv <= 0.) { // edge crosses plane...
                double t = tu / (tu - tv);
                double[] vd = dst.getPoint(pos++);
                if (DEBUG)
                    System.out.println(" new vertex " + pos);
                for (int j = 0; j < Triangle.VERTEX_LENGTH; j++) {
                    vd[j] = u[j] + t * (v[j] - u[j]);
                }
            }
            if (tv <= 0.) { // vertex v is in ...
                dst.setPointFrom(pos++, v);
            }
        }
        dst.setShadingFrom(p);
        return testResult;
    }

    /**
     * @param a
     *            triangle to split
     * @param b
     *            triangle to intersect against
     * @param s
     *            signum giving the side of the plane of b to get.
     * @return an array of triangles
     */
    public static Triangle[] intersect(Triangle a, Triangle b, int s,
            Polygon res, ArrayStack stack) {

        double[] x = b.getPoint(0);
        double[] y = b.getPoint(1);
        double[] z = b.getPoint(2);

        // find the plane through the three
        double[] d1 = VecMat.difference(y, Triangle.SX, x, Triangle.SX);
        double[] d2 = VecMat.difference(z, Triangle.SX, x, Triangle.SX);
        double[] normal = new double[3];
        VecMat.cross(d1, d2, normal);
        VecMat.normalize(normal);
        double dist = VecMat.dot(normal, 0, x, Triangle.SX);
        int sign = (int) (s * Math.signum(VecMat.dot(normal, new double[] { 0,
                0, 1 })));
        Triangle u = new Triangle();
        Triangle v = new Triangle();
        clipToHalfspace(a, Triangle.SX, normal, sign, dist, res);
        if (res.getLength() >= 3)
            return res.triangulate(null, stack);
        else {
            System.err.println("WARNING intersect generated polygon of length "
                    + res.getLength());
            return new Triangle[0];
        }
    }

}
