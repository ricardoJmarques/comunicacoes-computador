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
package clienteudp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ricardo Marques - a55723@alunos.uminho.pt
 */
public class ClienteUDP{

    private static Integer acumulador = 1;
    private static int porta;
    private static int freqAlive;
    private static String endereco;

    /**
     * Método main()
     * @param args argumentos da linha de comandos
     */
    public static void main(String[] args) {
        if (args.length != 2){
            System.out.println("Numero de Argumentos incorrecto\nPara usar:");
            System.out.println("java -jar clienteUDP -f ficheiro.conf");
        }
        else{
            if(args[0].equals("-f")){
                try{
                    System.out.println(args[1]);
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
                
            ScheduledExecutorService SendAlive = Executors.newScheduledThreadPool(1);
        
            SendAlive.scheduleAtFixedRate(()->{
                sendAlive();
            }, 0, freqAlive, TimeUnit.MILLISECONDS);

            try {
                DatagramSocket udpSocketReceive = new DatagramSocket(porta);
                byte[] buffer = new byte[2048];
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    udpSocketReceive.receive(pacote);
                    String mensagem = new String(buffer, 0, pacote.getLength());
                    //System.out.println("Pacote de " + pacote.getAddress().getHostName() + ". Payload: " +  mensagem);
                    pacote.setLength(buffer.length);
                    String[] campos = mensagem.split("\\s+");
                    if (campos[0].equals("I")){
                        String resposta = "R " + campos[1] + " " + campos[2] + " " + netStatus();
                        send(pacote.getAddress().getHostName(), porta, resposta.getBytes());
                    }
                }
            }
            catch (IOException e) {}
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
        porta = Integer.parseInt(p.getProperty("porta"));
        freqAlive = Integer.parseInt(p.getProperty("frequencia"));
        endereco = p.getProperty("endereco");
        if (input != null) {
            input.close();
        }
    }
    
    /**
     * Método send - envia uma mensagem por um socket udp
     * @param host endereço para onde enviar
     * @param port porta para onde enviar
     * @param message array de bytes com a mensagem
     */
    private static void send(String host, int port, byte[] message){
        try {
            InetAddress addr = InetAddress.getByName(host);
            DatagramPacket pacote = new DatagramPacket(message, message.length, addr, port);
            DatagramSocket dataSocket;
            dataSocket = new DatagramSocket();
            dataSocket.send(pacote);
            dataSocket.close();
        }
        catch (IOException e) {}
    }

    /**
     * Método sendAlive - envia uma mensagem de status ao servidor proxy
     */
    private static void sendAlive(){
        String mensagem;
        mensagem = "S " + acumulador.toString();
        send(endereco, porta, mensagem.getBytes());
        acumulador++;
    }
    
    /**
     * Método netStatus - retorna o numero de ligações TCP ativas na maquina
     * @return numero de ligações TCP ativas na maquina
     */
    public static Integer netStatus(){
        String linha;
        Process p;
        String padrao = "(\\s*)(\\d+)( connections established)";
        Pattern regex = Pattern.compile(padrao);
        Matcher m;
        Integer total = 0;
        try {
            p = Runtime.getRuntime().exec("netstat -s");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((linha = br.readLine()) != null){
                m = regex.matcher(linha);
                if (m.matches()){
                    //System.out.println("conecções: " + m1.group(2) );
                    total = Integer.parseInt(m.group(2));
                }
            }
            p.waitFor();
            p.destroy();
        } catch (IOException | InterruptedException e) {}
        return total;
    }
}
