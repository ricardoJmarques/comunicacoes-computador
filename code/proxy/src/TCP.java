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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/**
 *
 * @author Ricardo Marques - a55723@alunos.uminho.pt
 */
public class TCP extends Thread{

    private Socket clientSocket = null;
    private Socket serverSocket = null;
    private static final int BUFFSIZE = 4096;
    private final String endereco;
    private final int porta;
    private InputStream inC;
    private OutputStream outC;
    private InputStream inS;
    private OutputStream outS;
    private boolean status = true;
    
    private static final boolean LOG = false;
    
    /**
     * Inicia uma instancia de TCP
     * 
     * @param socket - socket que iniciou a ligação
     * @param p - porta do socket
     * @param s - endereço do servidor HTTP
     */
    public TCP(Socket socket, int p, String s) {
        this.clientSocket = socket;
        this.endereco = s;
        this.porta = p;
    }

    @Override
    public void run() {

        while (status) {
            try {

                final InputStream streamFromClient = clientSocket.getInputStream();
                final OutputStream streamToClient = clientSocket.getOutputStream();

                try {
                    serverSocket = new Socket(endereco, porta);
                }
                catch (IOException e) {
                    clientSocket.close();
                    if(LOG){System.err.println(e);}
                    continue;
                }

                final InputStream streamFromServer = serverSocket.getInputStream();
                final OutputStream streamToServer = serverSocket.getOutputStream();

                Thread t = new Thread() {
                    @Override
                    public void run() {
                        pipeSocket(streamFromClient, streamToServer);
                    }
                };
                t.start();
                
                pipeSocket(streamFromServer, streamToClient);
            }
            catch (IOException e) {
                if(LOG){System.err.println(e);}
                status = false;
            }
            finally {
                try {
                    if (serverSocket != null)
                        serverSocket.close();
                    if (clientSocket != null)
                        clientSocket.close();
                }
                catch (IOException e) {if(LOG){System.err.println(e);}}
            }
        }
    }
    
    /**
     * Método pipeSocket - interliga o ouput de um socket para o imput de outro
     * @param streamFrom - stream do origem
     * @param streamTo - stream do destino
     */
    private void pipeSocket(InputStream streamFrom, OutputStream streamTo){
        int bytesLidos;
        byte[] buffer = new byte[BUFFSIZE];
        try {
            while ((bytesLidos = streamFrom.read(buffer)) != -1) {
                streamTo.write(buffer, 0, bytesLidos);
                streamTo.flush();
            }
        }
        catch (IOException e) {if(LOG){System.err.println(e);}}
        try {
            streamTo.close();
        }
        catch (IOException e) {if(LOG){System.err.println(e);}}
    }
}
