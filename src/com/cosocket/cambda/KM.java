package com.cosocket.cambda;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

/*
Copyright (c) 2014, Cosocket LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

* Neither the name of Cosocket LLC nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class KM {
    protected int n;
    protected int m;
    protected int r;
    protected int nc;
    protected Random rng;
    protected Vector<byte[]> c;
    protected Vector<int[]> a;

    public static final void setbit(int[] d, int j) {d[(j / 32)] |= (1 << (j % 32));}
    public static final void clearbit(int[] d, int j) {d[(j / 32)] &= ~(1 << (j % 32));}
    public static final boolean getbit(int[] d, int j) {return (((d[(j / 32)] >>> (j % 32)) & 1) != 0);}

    public KM(int width, int rows) throws Exception { this(width, width, rows); }
    
    public KM(int width, int cwidth, int rows) throws Exception {
        if (rows < 1) throw new Exception("rows must be greater than 0");
        if ((width % 32) != 0) throw new Exception("width must be a multiple of 32");
        if ((cwidth % 32) != 0) throw new Exception("cwidth must be a multiple of 32");
        
        n = width;
        nc = cwidth;
        m = rows;
        r = n / 2;

        rng = new Random(9081726354L);

        c = new Vector<byte[]>(m);
        for (int i = 0; i < m; i++) c.add(new byte[nc]);
        for (byte[] t : c) Arrays.fill(t, (byte)0);
        
        int nw = n / 32;
        a = new Vector<int[]>(m);
        for (int i = 0; i < m; i++) a.add(new int[nw]);
        for (int[] t : a) {
            for (int i = 0; i < nw; i++) t[i] = rng.nextInt();
        }
    }
        
    protected static final boolean isActive(int r, int[] p, int[] q) {
        int tot = 0;
        int v, w;
        // assert p.length == q.length;
        for (int i = 0; i < p.length; i++) {
            v = p[i];
            w = q[i];
            v = v ^ w;
            v = v - ((v >>> 1) & 0x55555555);
            v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
            tot += (((v + (v >>> 4) & 0xF0F0F0F) * 0x1010101) >>> 24);
        }
        return (tot < r);
    }

    public void store(int[] dk) { this.store(dk,dk); }
    
    public void store(int[] dk, int[] dv) {
        for (int i = 0; i < m; i++) {
            if (isActive(r, a.get(i), dk)) {    
                byte[] l = c.get(i);
                for (int j = 0; j < nc; j++) {
                    if (getbit(dv,j)) { if (l[j] < (byte)+127) l[j]++; }
                    else              { if (l[j] > (byte)-127) l[j]--; }
                }
            }
        }
    }
    
    public int[] retrieve(int[] dk) {
        int cnt = 0;
        int[] sum = new int[nc];
        Arrays.fill(sum, 0);
        
        for (int i = 0; i < m; i++) {
            if (isActive(r, a.get(i), dk)) {
                cnt++;
                byte[] l = c.get(i);
                for (int j = 0; j < nc; j++) if (l[j] > 0) sum[j]++; 
            }
        }
        
        cnt /= 2;
        int[] t = new int[nc / 32];
        Arrays.fill(t, (int)0);
        for (int j = 0; j < nc; j++) if (sum[j] > cnt) setbit(t,j);
        return t;
    }
    
    public static void main(String[] args) throws Exception {
        int abits = 1120;
        int cbits = 2240;
        int rows  = 64000;
        KM  pksdm = null;
        
        if (args.length == 3) {
            abits = Integer.parseInt(args[0]);
            cbits = Integer.parseInt(args[1]);
            rows  = Integer.parseInt(args[2]);
        }
    
        long t0, t1;    
        t0 = System.currentTimeMillis();
        pksdm = new KM(abits,cbits,rows);
        //pksdm = new KM(abits,rows);
        
        t1 = System.currentTimeMillis();
        System.out.println("Ini: " + (t1 - t0) + " ms <" + abits + "," + cbits + "> bits x " + rows + " rows ");
        
        int[] ou, ink, inv;
        ink = new int[abits/32];
        Arrays.fill(ink, (int)0);
        inv = new int[cbits/32];
        Arrays.fill(inv, (int)0);
    
        for (int i : new int[]{11,33,55,77}) {
            KM.setbit(ink, i);
            KM.setbit(inv, i);
        }
    
        t0 = System.currentTimeMillis();
        pksdm.store(ink,inv);
        //pksdm.store(ink);
        t1 = System.currentTimeMillis();
        System.out.println("Put: " + (t1 - t0) + " ms " + Arrays.toString(ink));
        
        KM.clearbit(ink,77);
        
        t0 = System.currentTimeMillis();
        ou = pksdm.retrieve(ink);
        t1 = System.currentTimeMillis();    
        System.out.println("Get: " + (t1 - t0) + " ms " + Arrays.toString(ink) + " => " + Arrays.toString(ou));
    }
}
