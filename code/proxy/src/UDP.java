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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ricardo Marques - a55723@alunos.uminho.pt
 */
public class UDP extends Thread{
    private static Map<String, maquinas> pollingTable = null;
    private static Map<String, Long> statusTable = null;

    private static ScheduledExecutorService MaquinasExec;
    private static ScheduledFuture<?> testMachines;
    private static ScheduledFuture<?> probeMachines;

    private Thread t;
    private static boolean status;
    private static int porta;
    private static long delayProbe;
    private static long delayTest;
    private static long timeout;

    private long maxRTT;
    private long maxLIG;
    private float maxLOST;
    private float maxSLOST;
    private static String adr;
    private static float pontuacao;
    private static float pesoPerdas;
    private static float pesoRTT;
    private static float pesoLigacoes;

    // variaveis de log
    private static final boolean LOG = false;
    private static final boolean LOG1 = false;
    private static final boolean LOG2 = true;
    private static final boolean LOG3 = false;
    private static final boolean LOG4 = false;
    private static final boolean LOG5 = false;

    /**
     * 
     * @param prt porta do servidor
     * @param dp tempo em ms para efetuar o de pedido de informação aos servidores HTTP
     * @param dt tempo em ms para efetuar a verificação de timeout dos servidores HTTP
     * @param tOut tempo em ms para que um servidor entre em timeout
     * @param pp peso das perdas na formula de calculo do servidor mais disponivel
     * @param pr peso do Round Trip Time na formula de calculo do servidor mais disponivel
     * @param pl peso do numero de Ligações na formula de calculo do servidor mais disponivel
     */
    UDP(int prt, long dp, long dt, long tOut, float pp, float pr, float pl) {
        porta = prt;
        delayProbe = dp;
        delayTest = dt;
        timeout = tOut;
        status = true;
        maxRTT = 0;
        maxLIG = 0;
        maxLOST = 0;
        maxSLOST = 0;
        adr = null;
        pontuacao = 100;
        pesoPerdas = pp;
        pesoRTT = pr;
        pesoLigacoes = pl;
    }
    
    @Override
    public void run() {
        pollingTable = new ConcurrentHashMap<>();
        statusTable = new ConcurrentHashMap<>();
        MaquinasExec = Executors.newScheduledThreadPool(1000);
        
        // agendamento da execussao da verificacao de timeout aos servidores
        testMachines = MaquinasExec.scheduleWithFixedDelay(()->{
            testMaquinas(timeout);
        }, 0, delayTest, TimeUnit.MILLISECONDS);

        // agendamento da execussao de Probing aos servidores
        probeMachines = MaquinasExec.scheduleWithFixedDelay(()->{
            probeMaquinas();
        }, 0, delayProbe, TimeUnit.MILLISECONDS);

        // inicia a escuta em UDP
        startUDP();
    }

    @Override
    public void start () {
        if (t == null) {
            t = new Thread (this, "UDPLISTEN");
            t.start ();
        }
    }

    public void stopUDP () {
        MaquinasExec.shutdown();
        status = false;
        
    }
    /**
     * Método getBestServer - calcula qual o servidor mais disponivel
     * @return uma String com o endereço do servidor mais disponivel
     */
    public String getBestServer() {
        pontuacao = 100;
        maxRTT = 0;
        maxLIG = 0;
        maxLOST = 0;
        maxSLOST = 0;
        adr=null;
        // procura o maximo de cada medida de entre todas as maquinas ligadas
        pollingTable.forEach( (k,m) -> {
            long rt = m.getReceiveTime();
            long nl = m.getnLigacoes();
            float lst = m.getLost();
            float slst = m.getSmallLost();
            if(maxRTT <= rt) maxRTT = rt;
            if(maxLIG <= nl) maxLIG = nl;
            if(maxLOST <= lst) maxLOST = lst;
            if(maxSLOST <= slst) maxSLOST = slst;
        });
        //calcula pontuacao ponderada
        pollingTable.forEach( (k,m) -> {
            float r, l, ll;
            float rt = m.getReceiveTime();
            float nl = m.getnLigacoes();
            float lst = m.getLost();
            float slst = m.getSmallLost();
            if(rt > 0)
                r = (rt / (float)maxRTT )* 100f;
            else
                r = 0;
            if(nl > 0)
                l = (nl/ (float)maxLIG )* 100f;
            else
                l = 0;
            if(lst > 0)
                ll = (((lst+slst)/2f) / maxLOST )* 100f;
            else
                ll = 0;
            float p = ( pesoPerdas * ll ) + (pesoRTT * r) + (pesoLigacoes * l);
            if (p <= pontuacao){
                pontuacao = p;
                adr = k;
            }
            if(LOG4){
                String S = "Servidor " + k + ". Ping#: " + m.getCounter() + ". Perdas: " + lst + "% | " + m.getSmallLost() + "%. RTT: " + rt + "ms. Ligacoes: " + nl;
                S += "\t\tcom pontuacao: " + p + "%\t. Perdas: " + ll + "%. RTT: " + r + "%. Ligacoes: " + l + "%.";
                System.out.println(S);
            }
        });
        return adr;
    }
    
    /**
     * Método startUDP - inicia a escuta da porta UDP 
     */
    private static void startUDP(){
        try {
            DatagramSocket dataSocket = new DatagramSocket(porta);
            byte[] buffer = new byte[2048];
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            while (status) {
                dataSocket.receive(pacote);
                String s = new String(buffer, 0, pacote.getLength()); // pacote udp
                String a = pacote.getAddress().getHostName(); // endereco do servidor
                long t = System.currentTimeMillis(); //timestamp da recepcao do pacote
                new Thread() {
                    @Override
                    public void run() {
                    parseMensagem(s, a, t);
                    }
                }.start();
                pacote.setLength(buffer.length);
            }
        }
        catch (IOException | NumberFormatException e){
            System.err.println(e);
        }
    }

    /**
     * Método parseMensagem - verifica o conteudo de um PDU recebido por UDP
     *                        e atua de acordo com o conteudo do PDU
     * @param mensagem - dados recebidos
     * @param addr - endereço que originou a mensagem
     * @param time - timestamp do tempo de chegada
     */
    private static void parseMensagem(String mensagem, String addr, long time) {
        String[] campos = mensagem.split("\\s+");
        maquinas maquina;
        switch (campos[0]) {
            case "S": //recebeu status do servidor
                if (statusTable.containsKey(addr)) {                    
                    statusTable.put(addr, time);
                    if(LOG3){
                        System.out.println("Maquina " + addr + " actualizou estado para " + Integer.parseInt(campos[1]));
                    }
                }
                else { //primeiro pedido do servidor http, cria nova estrutura de dados
                    maquina = new maquinas(addr, porta);
                    pollingTable.put(addr, maquina);
                    statusTable.put(addr, time);
                    if(LOG2){
                        System.out.println("Maquina " + addr + " estabeleceu ligação");
                    }
                }
            break;
            case "R": //recebeu replay do servidor
                maquina = pollingTable.get(addr);
                if (maquina != null) {
                    long last = Long.parseLong(campos[2]);
                    int n = Integer.parseInt(campos[1]);
                    int l = Integer.parseInt(campos[3]);
                    long r = time-last;
                    maquina.incNReceived();
                    maquina.setReceiveTime(r);
                    maquina.setnLigacoes(l);
                    maquina.setsmallLostHigh(n);
                    if(LOG){
                        System.out.println("Maquina " + addr + " respondeu ao pedido " + n + " com RTT: " + r);
                    }
                }
            break;
        }
    }

    /**
     * Método testMaquinas - verifica se os servidores HTTP não respondem e elimina-os.
     * @param delay - tempo em ms para o timeout
     */
    private static void testMaquinas(long delay){
        statusTable.forEach( (k,m) -> {
            if (System.currentTimeMillis() - m > delay){ //termina ligacao por timeout
                if(LOG1){
                    System.out.println("Servidor " + k + " deixou de responder.\nA fechar sessao.");
                }
                pollingTable.remove(k);
                statusTable.remove(k);
            }
        });
    }

    /**
     * Método probeMáquinas - para cada servidor da lista de servidores, envia um pedido de informação
     */
    private static void probeMaquinas(){
        if(LOG5){System.out.println("\n########################################## Estado das Máquinas ##########################################");}
        pollingTable.forEach( (k,m) -> {
            if(LOG5){System.out.println("Servidor " + k + ". Ping#: " + m.getCounter() + ". Perdas: " + m.getLost() + " | " + m.getSmallLost() + ". RTT: " + m.getReceiveTime() + "ms. Ligacoes: " + m.getnLigacoes());}
            String mensagem = "I " + m.getCounter() + " " + System.currentTimeMillis();
            send(m.getAddress(), m.getPort(), mensagem.getBytes());
            m.setsmallLostLow(m.getCounter());
            m.incCounter();
        });
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
    
}
