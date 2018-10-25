/*
 * The MIT License
 *
 * Copyright 2017 Ricardo Marques - a55723@alunos.uminho.pt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package servidor;

import java.util.Objects;

/**
 *
 * @author Ricardo Marques - a55723@alunos.uminho.pt
 */
public class maquinas{
    private String address;
    private int port;
    private int counter;
    private int nReceived;
    private int nLigacoes;
    private long lastPong;
    private long[] receiveTime;
    private int[] smallLost;
    private int pos;
    
    /**
     * Inicia uma instancia de maquinas
     * @param adr endereço do servidor
     * @param prt porta do servidor
     */
    maquinas(String adr, int prt) {
        address = adr;
        port = prt;
        counter = 1;
        nReceived = 0;
        nLigacoes = 0;
        lastPong = 0;
        receiveTime = new long[20];
        smallLost = new int[20];
        pos = 0;
        for (int i =0; i<20; i++){
            receiveTime[i] = 0;
            smallLost[i] = 0;
        }
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getLastPong() {
        return lastPong;
    }

    public void setLastPong(long lastPong) {
        this.lastPong = lastPong;
    }

    public int getCounter() {
        return counter;
    }

    public void incCounter() {
        this.counter++;
    }

    public void incNReceived(){
        this.nReceived++;
    }

    public void setsmallLostLow(int n){
        smallLost[(n%20)] = 0;
    }
    
    public void setsmallLostHigh(int n){
        smallLost[(n%20)] = 1;
    }

    public float getSmallLost() {
        float t = 0;
        int i;
        for(i=0; i<20; i++){
            if(smallLost[i] == 1)
                t++;
        }
        return ((20 - t) / 20) * 100;
    }
    
    public float getLost() {
        float s,r;
        float f =0;
        s = (float)this.counter-1;
        r = (float)this.nReceived;
        if (r != 0 && s >= r)
            f = (((s - r) / r) * 100);
        return f;
    }

    public int getnLigacoes() {
        return nLigacoes;
    }

    public void setnLigacoes(int nl) {
        this.nLigacoes = nl;
    }

    public long getReceiveTime() {
        long soma = 0;
        int i;
        for(i=0; i<20; i++){
            soma += receiveTime[i];
        }
        return soma / 20;
    }

    public void setReceiveTime(long rt) {
        this.receiveTime[pos] = rt;
        this.pos = (pos+1)%20;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.address);
        hash = 19 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final maquinas other = (maquinas) obj;
        if (this.port != other.port) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
}
