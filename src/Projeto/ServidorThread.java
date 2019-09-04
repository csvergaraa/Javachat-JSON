/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Projeto;

import com.sun.xml.internal.fastinfoset.util.CharArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.json.JSONArray;

/**
 *
 * @author Cesar
 * Classe responsavel por tratar as requisicoes do cliente, receber os pacotes JSON e responder para os clientes
 */
public class ServidorThread implements Runnable {
    private Socket socket;
    TelaServidor main;
    JSONArray jsonArrayItems = new JSONArray();
    BufferedReader recebe;
    String client;
    boolean verificaUsuario;
    PrintStream envia;
    String enviaTodos = "";
    Usuario usuario;
    
    // criar a socket do servidor->cliente
    public ServidorThread(Socket socket, TelaServidor main) {

        this.main = main;
        this.socket = socket;

        try {
            recebe = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            envia = new PrintStream(this.socket.getOutputStream());

            //dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            main.acrescentarMensagem("[SocketThreadIOException]: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {

            String aux;
            // coloquei c aqui para nao iniciar nulo (pois estava dando alguns problemas)
            aux = "c";
                        
            while (true) { // enquanto a thread existe
                
                aux = recebe.readLine().trim();
                JSONObject objetoPacote = new JSONObject(aux);
                String arg = objetoPacote.getString("action");

                // organiza pelo parametro action do JSON
                switch (arg) {
                    case "connect": // receber uma nova conexao
                        String nome = objetoPacote.getString("nome");
                        String tipo = objetoPacote.getString("tipo");
                        String material = objetoPacote.getString("material");
                        String porta = (String) Integer.toString(socket.getPort());
                        String descricao;
                        String nomeUsuario = nome;
                        System.out.println("Protocolo Recebido:" + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + nome + ": " + aux);

                        client = nomeUsuario;
                        verificaUsuario = main.verificaUserName(nomeUsuario);
                        
                        if (tipo.equals("D")) { // se for doador cria um usuario com descricao
                            descricao = objetoPacote.getString("descricao");
                            usuario = new Usuario(nome, tipo, material, porta, descricao);
                        } else { // se for coletor inicia sem descricao
                            usuario = new Usuario(nome, tipo, material, porta);
                        }
                        
                        // verifica se ja existe um usuario com aquele nome, se existir o cara nao consegue conectar (toma um client_disconnect)
                        if (verificaUsuario == true) {
                            System.out.println("Cliente: " + nomeUsuario + " ja existe no sistema!");
                            main.acrescentarMensagem("[SERVIDOR] " + nomeUsuario + " ja existe no sistema!");
                            JSONObject objetoJSON = new JSONObject();
                            objetoJSON.put("action", "client_disconnect");

                            System.out.println("Cliente: " + nomeUsuario + " ja EXISTE!");
                            String palavraJSON = objetoJSON.toString();
                            envia.println(palavraJSON);
                            System.out.println("Protocolo Enviado:" + palavraJSON);
                        // verifica se lotou o servidor, utilizei 50 para maximo de usuarios online
                        } else if (main.clientList.size() == 50) {
                            System.out.println("Servidor atingiu o limite de usuários [" + main.clientList.size() + "].");
                            JSONObject objetoJSON = new JSONObject();
                            objetoJSON.put("action", "client_disconnect");
                            String palavraJSON = objetoJSON.toString();
                            envia.println(palavraJSON);
                            System.out.println("Protocolo Enviado:" + palavraJSON);
                        } else { // se nao tem nome igual e nao ta lotado, faz tudo que tem que fazer
                            String tipoCliente;
                            if (tipo.equals("D")){
                                tipoCliente = "doador";
                                descricao = objetoPacote.getString("descricao");
                                usuario.setDescricao(descricao);
                            } else {
                                tipoCliente = "coletor";
                            }
                            usuario.setNome(nomeUsuario);
                            usuario.setTipo(tipo);
                            usuario.setMaterial(material);
                            usuario.setPorta(porta);
                            main.setClientList(usuario);
                            main.setSocketList(getSocket());

                            main.acrescentarMensagem("[SERVIDOR]: " + nomeUsuario + "(" + tipoCliente + ") entrou na SALA!");
                            main.atualizarTabelaOnline(main.socketList, main.clientList);
                            main.acrescentarMensagem("[SERVIDOR]: Enviando protocolo de confirmacao para  " + nomeUsuario);
                            
                            // manda a lista de usuarios online para todos os usuarios
                            mandaLista();
                        }
                        break;
                    // quando recebe um disconnect de um cliente
                    case "disconnect": // receber uma desconexão
                        String nomeCliente = main.getClientList(this.getSocket()).getNome();
                        System.out.println("Protocolo Recebido pelo " + nomeCliente + ": " + aux);
                        System.out.println("Cliente: " + nomeCliente + " saiu da SALA!");
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + nomeCliente + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] " + nomeCliente + " DESCONECTADO");
                        main.removeFromTheList(getSocket());
                        main.atualizarTabelaOnline(main.socketList, main.clientList);

                        mandaLista();
                        break;
                        
                    case "chat_request_server": // receber um pedido de unicast e encaminhar para o outro client
                        int x;
                        Socket tsoc;
                        int erro = 0;
                        
                        JSONObject objetoJSON = new JSONObject();
                        Usuario clienteMensagem = main.getClientList(this.getSocket());
                        String porta_remetente = clienteMensagem.getPorta();
                        String destinatario = objetoPacote.getString("destinatario");
                        
                        System.out.println("Protocolo recebido do cliente " + clienteMensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clienteMensagem.getNome() + ": " + aux);
                        
                        
                        // verificar se o destinatario é um numero valido
                        try {
                            int d = Integer.parseInt(destinatario);
                        } catch (NumberFormatException | NullPointerException nfe) {
                            PrintStream enviaTodos = new PrintStream(this.getSocket().getOutputStream());
                            objetoJSON.put("remetente", porta_remetente);
                            objetoJSON.put("action", "request_error");
                            erro++;
                            String palavraJSON = objetoJSON.toString();
                            enviaTodos.println(palavraJSON);
                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + clienteMensagem.getNome());
                            main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + clienteMensagem.getNome() + ": " + palavraJSON);
                        }
                        
                        // verificar se a pessoa nao ta pedindo unicast pra ela mesma
                        if (porta_remetente.equals(destinatario)) {
                            PrintStream enviaTodos = new PrintStream(this.getSocket().getOutputStream());
                            objetoJSON.put("remetente", porta_remetente);
                            objetoJSON.put("action", "request_error");
                            erro++;
                            String palavraJSON = objetoJSON.toString();
                            enviaTodos.println(palavraJSON);
                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + clienteMensagem.getNome());
                            main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + clienteMensagem.getNome() + ": " + palavraJSON);
                            break;
                        }
                        
                        System.out.println(main.lista);
                        // verifica se a pessoa ja se encontra em um unicast ( ve se a porta ta na lista da TelaServidor.java [lista])
                        for (String curVal : main.lista){
                            if (curVal.contains(destinatario)){
                                PrintStream enviaTodos = new PrintStream(this.getSocket().getOutputStream());
                                objetoJSON.put("action", "client_busy");
                                erro++;
                                System.out.println("O cliente esta ocupado");
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);
                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + clienteMensagem.getNome());
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + clienteMensagem.getNome() + ": " + palavraJSON);
                                break;
                            }
                          }
                        
                        if (erro > 0)
                            break;
                        
                        System.out.println("O cliente nao esta ocupado.");
                        
                        int enviado = 0;
                        // um for com todos os sockets conectados
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            String portaCliente = main.getClientList(tsoc).getPorta();
                            nomeCliente = main.getClientList(tsoc).getNome();
                            
                            // envia o request de unicast se for a porta destino
                            if (portaCliente.equals(destinatario)) {
                                PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());
                                objetoJSON.put("remetente", porta_remetente);
                                objetoJSON.put("action", "chat_request_client");
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);

                                main.lista.add(porta_remetente);
                                main.lista.add(destinatario);
                                enviado++;
                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeCliente);
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeCliente + ": " + palavraJSON);
                            }
                            
                        }
                        
                        // usei um contador para ver se enviou para alguem, se nao devolve o erro (de que o cara nao ta online)
                        if (enviado == 0) {
                            PrintStream enviaTodos = new PrintStream(this.getSocket().getOutputStream());
                            objetoJSON.put("action", "chat_request_error");
                            String palavraJSON = objetoJSON.toString();
                            enviaTodos.println(palavraJSON);
                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + clienteMensagem.getNome());
                            main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + clienteMensagem.getNome() + ": " + palavraJSON);
                        }
                        break;
                        
                    case "chat_unicast_close_server": // receber um pedido de fechamento de unicast e encaminhar para o outro cliente
                        objetoJSON = new JSONObject();
                        clienteMensagem = main.getClientList(this.getSocket());
                        porta_remetente = clienteMensagem.getPorta();
                        destinatario = objetoPacote.getString("destinatario");
                        
                        System.out.println("Protocolo recebido do cliente" + clienteMensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clienteMensagem.getNome() + ": " + aux);
                       
                        
                        // for para todos os sockets online
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            String portaCliente = main.getClientList(tsoc).getPorta();
                            nomeCliente = main.getClientList(tsoc).getNome();

                            // só mandar o pacote para o cara que tem a porta destino
                            if (portaCliente.equals(destinatario)) {
                                PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());
                                objetoJSON.put("remetente", porta_remetente);
                                objetoJSON.put("action", "chat_unicast_close_client");
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);

                                main.lista.remove(porta_remetente);
                                main.lista.remove(portaCliente);
                                System.out.println(main.lista);
                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeCliente);
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeCliente + ": " + palavraJSON);
                            }
                            
                        }
                        break;
                        
                    case "chat_response_server": // receber uma resposta de solicitacao de unicast e encaminhar para o remetente
                        objetoJSON = new JSONObject();
                        Usuario clientemensagem = main.getClientList(this.getSocket());
                        String portaRemetente = clientemensagem.getPorta();
                        destinatario = objetoPacote.getString("destinatario");
                        String resposta = objetoPacote.getString("resposta");
                        System.out.println("Protocolo recebido do cliente " + clientemensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clientemensagem.getNome() + ": " + aux);
                        
                        
                        // for para os sockets online
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            String portaCliente = main.getClientList(tsoc).getPorta();
                            nomeCliente = main.getClientList(tsoc).getNome();
                            
                            // só mandar a requisição para a porta do destinatário
                            if (portaCliente.equals(destinatario)) {
                                PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());
                                objetoJSON.put("remetente", portaRemetente);
                                objetoJSON.put("resposta", resposta);
                                objetoJSON.put("action", "chat_response_client");
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);

                                // se a resposta for falsa, remove as duas portas da lista (de gente da unicast)
                                if (resposta.equals("false")) {
                                    main.lista.remove(portaRemetente);
                                    main.lista.remove(portaCliente);
                                    System.out.println(main.lista);
                                }
                                /* nesse caso ele nao vai adicionar se for true, pois ele já adiciona os dois clientes
                                * no momento em que um manda o pedido para o outro, para nao ter conflito
                                * de um pedido estar em espera e outra pessoa pedir solicitacao na mesma hora
                                */
                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeCliente);
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeCliente + ": " + palavraJSON);
                            }
                            
                        }
                        
                        break;
                        
                    case "chat_unicast_message_server": // receber mensagem de unicast e encaminhar para o destinatario
                        objetoJSON = new JSONObject();
                        clientemensagem = main.getClientList(this.getSocket());
                        portaRemetente = clientemensagem.getPorta();
                        destinatario = objetoPacote.getString("destinatario");
                        String mensagem = objetoPacote.getString("mensagem");
                        System.out.println("Protocolo recebido do cliente " + clientemensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clientemensagem.getNome() + ": " + aux);
                        
                        
                        // for dos sockets online
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            String portaCliente = main.getClientList(tsoc).getPorta();
                            nomeCliente = main.getClientList(tsoc).getNome();

                            // só manda a mensagem para a porta do destinatario
                            if (portaCliente.equals(destinatario)) {
                                PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());
                                objetoJSON.put("remetente", portaRemetente);
                                objetoJSON.put("action", "chat_unicast_message_client");
                                objetoJSON.put("mensagem", clientemensagem.getNome() + " disse: " + mensagem);
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);

                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeCliente);
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeCliente + ": " + palavraJSON);
                            }
                            
                        }
                        break;
                        
                    case "chat_general_server": // receber mensagem de broadcast e encaminhar para todos
                        objetoJSON = new JSONObject();
                        clientemensagem = main.getClientList(this.getSocket());
                        System.out.println("Protocolo recebido do cliente " + clientemensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clientemensagem.getNome() + ": " + aux);
                        
                        /* for de sockets onlines
                        * nesse caso manda a mensagem para todos os sockets onlines
                        */
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            mensagem = objetoPacote.getString("mensagem");
                            nomeCliente = main.getClientList(tsoc).getNome();
                            PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());

                            objetoJSON.put("mensagem", clientemensagem.getNome() + " disse a todos (BROADCAST): " + mensagem);
                            objetoJSON.put("action", "chat_general_client");

                            String palavraJSON = objetoJSON.toString();
                            enviaTodos.println(palavraJSON);

                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeCliente);
                            main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeCliente + ": " + palavraJSON);
                        }
                        break;
                        
                    case "chat_room_server": // receber mensagem de broadcast e encaminhar a todos por tipo
                        objetoJSON = new JSONObject();
                        clientemensagem = main.getClientList(this.getSocket());
                        String tipoRemetente = clientemensagem.getMaterial();
                        System.out.println("Protocolo recebido do cliente " + clientemensagem.getNome() + ": " + aux);
                        main.acrescentarMensagem("[SERVIDOR] Protocolo Recebido pelo " + clientemensagem.getNome() + ": " + aux);
                        
                        // for de sockets onlines
                        for (x = 0; x < main.socketList.size(); x++) {
                            tsoc = (Socket) main.socketList.elementAt(x);
                            mensagem = objetoPacote.getString("mensagem");
                            String tipoDestinatario = main.getClientList(tsoc).getMaterial();
                            String nomeDestinatario = main.getClientList(tsoc).getNome();

                            // só manda para os clientes do mesmo tipo do remetente
                            if (tipoDestinatario.equals(tipoRemetente)) {
                                PrintStream enviaTodos = new PrintStream(tsoc.getOutputStream());
                                objetoJSON.put("mensagem", clientemensagem.getNome() + " disse a todos do tipo [" + tipoRemetente + "] (MULTICAST): " + mensagem);
                                objetoJSON.put("action", "chat_room_client");
                                String palavraJSON = objetoJSON.toString();
                                enviaTodos.println(palavraJSON);

                                System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nomeDestinatario);
                                main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nomeDestinatario + ": " + palavraJSON);
                            }
                            
                        }
                        break;
                        
                    // quando nao acha nada no argumento action (erro)
                    default:
                        main.acrescentarMensagem("[JSONException]: PROTOCOLO DESCONHECIDO" + aux);
                        System.out.println("Protocolo do cliente " + main.getClientList(this.socket).getNome() + " nao foi reconhecido: " + aux);
                        break;
                    }
                
            }
        } catch (Exception e) {
//            System.out.println("Null pointer exception.");
        }
    }

    public Socket getSocket() {
        return socket;
    }

    /* funcao de atualizar a lista de pessoas onlines
    * é synchronized para nao ter problema de duas pessoas pedirem na mesma hora
    * ele adiciona na fila e é executado uma instrução de cada vez
    */
    synchronized public void mandaLista() throws IOException {
        // invokelater é uma funcao da interface grafica, para nao dar erro na 
        // hora de atualizar
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject objetoJSON;
                    JSONObject objetoAuxiliar = new JSONObject();
                    JSONArray listaJSON = new JSONArray();
                    // cria um objeto JSON
                    objetoAuxiliar.put("action", "client_list");

                    // for dos sockets onlines
                    for (int x = 0; x < main.socketList.size(); x++) {
                        String nome = main.clientList.elementAt(x).getNome();
                        String tipo = main.clientList.elementAt(x).getTipo();
                        String material = main.clientList.elementAt(x).getMaterial();
                        String porta = main.clientList.elementAt(x).getPorta();
                        String descricao;
                        objetoJSON = new JSONObject();
                        if (tipo.equals("D")){
                            descricao = main.clientList.elementAt(x).getDescricao();
                            objetoJSON.put("descricao", descricao);
                        }
                        objetoJSON.put("porta", porta);
                        objetoJSON.put("material", material);
                        objetoJSON.put("tipo", tipo);
                        objetoJSON.put("nome", nome);
                        listaJSON.put(objetoJSON);
                        // adicionar todos os objetos de clientes onlines no atributo lista do json
                        objetoAuxiliar.put("lista", listaJSON);
                    }

                    // for de sockets onlines
                    for (int x = 0; x < main.socketList.size(); x++) {
                        
                        Socket tsoc = (Socket) main.socketList.elementAt(x);
                        // manda o pacote JSON para todos os clientes onlines
                        String nomeCliente = main.getClientList(tsoc).getNome();
                        PrintStream enviaLista = null;
                        try {
                            enviaLista = new PrintStream(tsoc.getOutputStream());
                        } catch (IOException ex) {
                            Logger.getLogger(ServidorThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        String palavraJSON = objetoAuxiliar.toString();
                        enviaLista.println(palavraJSON);
                        System.out.println("Protocolo Enviado: " + palavraJSON + " para o cliente " + nomeCliente);

                    }
                } catch (Exception e) {
                    
                }
            }
            });
    }

    /**
     * @param socket the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}
