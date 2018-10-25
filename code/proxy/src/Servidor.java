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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.System.getProperty;
import java.net.ServerSocket;
import java.util.Properties;

/**
 *
 * @author Ricardo Marques - a55723@alunos.uminho.pt
 */
public class Servidor{
    private static int udpPort;
    private static int tcpPort;
    private static long probeTimer; // delay de envio de probes
    private static long timeoutTimer; // delay de teste de conectividade
    private static long timeoutDelay; // timeout da sessao udp com os servidores
    private static float pesoPerdas;
    private static float pesoRTT;
    private static float pesoLigacoes;
    
    /**
     * 
     * @param args - argumentos da linha de comandos
     * @throws InterruptedException
     * @throws IOException 
     */
    public static void main(String[] args) throws InterruptedException, IOException{

        if (args.length != 2){
            System.out.println("Numero de Argumentos incorrecto\nPara usar:");
            System.out.println("java -jar Servidor -f ficheiro.conf");
        }
        else{
            if(args[0].equals("-f")){
                try{
                    readConfig(args[1]);
                }
                catch (IOException e){
                    System.out.println("Erro ao ler ficheiro de configuração.");
                    System.exit(-1);
                }
            }
            else {
                System.out.println("Erro nos argumentos.");
                System.exit(-1);
            }

            // arranca servidor UDP
            UDP srvUDP;
            srvUDP = new UDP(udpPort, probeTimer, timeoutTimer, timeoutDelay, pesoPerdas, pesoRTT, pesoLigacoes);
            srvUDP.start();
        
            // escuta de socket TCP
            ServerSocket tcpSocket = null;
            boolean status = true;

            try {
                tcpSocket = new ServerSocket(tcpPort);
            } catch (IOException e) {
                System.exit(-1);
            }
            // por cada novo pedido no socket tcp, cria nova instancia da classe TCP
            while (status) {
                new TCP(tcpSocket.accept(), tcpPort, srvUDP.getBestServer()).start();
            }
            tcpSocket.close();
        }
    }
    
    /**
     * Método readConfig - Lê e carrega o ficheiro de configuração 
     * 
     * @param ficheiro - string com o nome do ficheiro do configuração 
     * @throws IOException
     */    
    private static void readConfig(String ficheiro) throws IOException{
        Properties p = new Properties();
        InputStream input = null;
        input = new FileInputStream(ficheiro);
        p.load(input);
        udpPort = Integer.parseInt(p.getProperty("portaUDP"));
        tcpPort = Integer.parseInt(p.getProperty("portaTCP"));
        probeTimer = Long.parseLong(p.getProperty("probeTIMER"));
        timeoutTimer = Long.parseLong(p.getProperty("probeTimeout"));
        timeoutDelay = Long.parseLong(p.getProperty("timeout"));
        pesoPerdas = Float.parseFloat(p.getProperty("pesoPerdas"));
        pesoRTT = Float.parseFloat(p.getProperty("pesoRTT"));
        pesoLigacoes = Float.parseFloat(p.getProperty("pesoLigacoes"));
        if (input != null) {
            input.close();
        }
    }
}