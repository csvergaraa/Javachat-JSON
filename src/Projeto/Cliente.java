/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Projeto;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Cesar
 */

/*
Parte lógica do Cliente, responsável por enviar os pacotes JSON para o servidor
*/
public class Cliente implements Runnable {

    private Socket socket;
    TelaCliente main;
    BufferedReader recebe;
    PrintStream envia;
    Vector listaPorta = new Vector();
    Vector online = new Vector();
    Vector listaClientes = new Vector();
    boolean conectado = true;
    Usuario cliente;

    // cria o socket do cliente
    public Cliente(Socket socket, TelaCliente main) {
        this.main = main;
        this.socket = socket;
        try {
            recebe = new BufferedReader(new InputStreamReader(this.getSocket().getInputStream()));
            envia = new PrintStream(this.getSocket().getOutputStream());
        } catch (IOException e) {

        }
    }

    //public Socket get
    @Override
    public void run() {
        try {
            String aux = "";

            while (true) {
                aux = recebe.readLine().trim();

                JSONObject objetoPacote = new JSONObject(aux);

                String arg = objetoPacote.getString("action");
                
                switch (arg) {
                    // pacotes recebidos do servidor
                    case "client_disconnect":
                        System.out.println("Protocolo Recebido: " + aux);
                        System.exit(0);
                        break;
                        
                    case "client_list":
                        main.setVisible(true);
                        listaClientes.removeAllElements();
                        JSONArray listaDados = objetoPacote.getJSONArray("lista");
                        
                        System.out.println("Lista");
                        
                        for (int i = 0; i < listaDados.length(); i++) {
                            JSONObject j = listaDados.getJSONObject(i);
                            String onome = j.getString("nome");
                            String otipo = j.getString("tipo");
                            String omaterial = j.getString("material");
                            String porta = j.getString("porta");
                            String adescricao;
                            
                            if (otipo.equals("D")) {
                                adescricao = j.getString("descricao");
                                cliente = new Usuario(onome, otipo, omaterial, porta, adescricao);
                                System.out.println("Nome: " + cliente.getNome() + " | Tipo: " + cliente.getTipo() + " | Material: " + cliente.getMaterial() + " | Porta: " + cliente.getPorta() + " | Descricao " + cliente.getDescricao());
                            } else {
                                cliente = new Usuario(onome, otipo, omaterial, porta);
                                System.out.println("Nome: " + cliente.getNome() + " | Tipo: " + cliente.getTipo() + " | Material: " + cliente.getMaterial() + " | Porta: " + cliente.getPorta());
                            }
                            listaClientes.add(cliente);
                        }

                        System.out.println("Protocolo Recebido: " + aux);
                        main.atualizarTabelaOnline(listaClientes);
                        break;
                        
                    case "chat_general_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        String mensagem = objetoPacote.getString("mensagem");
                        main.acrescentarMensagem(mensagem);
                        break;
                        
                    case "chat_unicast_message_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        mensagem = objetoPacote.getString("mensagem");
                        main.acrescentarUnicastMensagem(mensagem);
                        break;
                        
                    case "chat_request_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        String portaRemetente = objetoPacote.getString("remetente");
                        Usuario remetente = retornaUsuario(portaRemetente);
                        String nome_remetente = remetente.getNome();
                        
                        int dialogButton = JOptionPane.YES_NO_OPTION;
                        int dialogResult = JOptionPane.showConfirmDialog(null, "Deseja aceitar esta solicitacao de Unicast de " + nome_remetente + "?", "Unicast (" + nome_remetente + ")", dialogButton);
                        
                        JSONObject objetoJSON = new JSONObject();
                        objetoJSON.put("destinatario", portaRemetente);
                        objetoJSON.put("action", "chat_response_server");
                        
                        if (dialogResult == 0) { // yes
                            objetoJSON.put("resposta", "true");
                            String palavraJSON = objetoJSON.toString();
                            main.enviarServer(palavraJSON);
                            main.ocupado = portaRemetente;
                            main.ativarBotaoDesconexao();
                            
                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nome_remetente);
                            main.acrescentarMensagem("[SERVIDOR] Protocolo enviado para " + nome_remetente + ": " + palavraJSON);
                            main.acrescentarUnicastMensagem("Voce aceitou a solicitacao de Unicast de " + nome_remetente + ".");
                        } else {
                            objetoJSON.put("resposta", "false");
                            String palavraJSON = objetoJSON.toString();
                            main.ocupado = "nao";
                            main.enviarServer(palavraJSON);

                            System.out.println("Protocolo enviado: " + palavraJSON + " para o cliente " + nome_remetente);
                            main.acrescentarUnicastMensagem("[SERVIDOR] Protocolo enviado para " + nome_remetente + ": " + palavraJSON);
                        } 
                        break;
                        
                    case "chat_request_error":
                        System.out.println("Protocolo Recebido: " + aux);
                        main.ocupado = "nao";
                        main.acrescentarUnicastMensagem("Houve um erro na sua solicitação Unicast. O usuário solicitado não se encontra online.");
                        break;
                        
                    case "request_error":
                        System.out.println("Protocolo Recebido: " + aux);
                        main.ocupado = "nao";
                        main.acrescentarUnicastMensagem("Erro: A solicitação do Unicast não pode ser processada.");
                        break;
                        
                    case "chat_response_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        portaRemetente = objetoPacote.getString("remetente");
                        String resposta = objetoPacote.getString("resposta");
                        
                        // pega informacoes do remetente
                        remetente = retornaUsuario(portaRemetente);
                        nome_remetente = remetente.getNome();
                        
                        if (resposta.equals("true")) {
                            main.ocupado = portaRemetente;
                            main.conectouUnicast(remetente);
                            main.ativarBotaoDesconexao();
                        } else {
                            main.ocupado = "nao";
                            main.naoAceitouUnicast(remetente);
                            JOptionPane.showMessageDialog(null, nome_remetente + " recusou sua solicitacao de Unicast.", "Solicitacao recusada", JOptionPane.ERROR_MESSAGE);
                        }
                                         
                        break;
                    
                    case "chat_unicast_close_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        portaRemetente = objetoPacote.getString("remetente");
                        remetente = retornaUsuario(portaRemetente);
                        
                        main.desativarBotaoDesconexao();
                        main.desconectouUnicast();
                        main.ocupado = "nao";
                        break;
                        
                    case "chat_room_client":
                        System.out.println("Protocolo Recebido: " + aux);
                        mensagem = objetoPacote.getString("mensagem");
                        main.acrescentarMulticastMensagem(mensagem);
                        break;
                        
                    case "client_busy":
                        System.out.println("Protocolo Recebido: " + aux);
                        JOptionPane.showMessageDialog(null, "ERRO: O cliente selecionado já se encontra em negociação.", "Solicitacao negada", JOptionPane.ERROR_MESSAGE);
                        break;
                        
                    default:
                        System.exit(0);
                        break;
                }

            }
        } catch (Exception e) {
//            System.out.println("Excecao ocorreu.");
        }

    }

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

    // funcao de retornar ip mas acabou que nem usei
    public String retornaIp(String nome) {
        
        try {
            for (int i = 2; i <= listaClientes.size(); i++) {
                String n = (String) listaClientes.elementAt(i);
                if (n.equals(nome)) {
                    return (String) listaClientes.elementAt(i - 1);

                }
            }
        } catch (Exception e) {

        }
        return null;
        
    }

    // funcao de retornar porta (acho que tambem nao usamos)
    public String retornaPorta(String nome) {
        
        try {
            for (int i = 0; i <= listaClientes.size(); i++) {
                Usuario usuario = (Usuario) listaClientes.elementAt(i);
                if (usuario.getNome().equals(nome)) {
                    return usuario.getPorta();
                }
            }
        } catch (Exception e) {

        }
        return "";
        
    }
    
    // retornar todos os usuarios online
    public Usuario retornaUsuario(String porta) {

        try {
            for (int i = 0; i <= listaClientes.size(); i++) {
                Usuario usuario = (Usuario) listaClientes.elementAt(i);
                if (usuario.getPorta().equals(porta)) {
                    return usuario;
                }
            }
        } catch (Exception e) {

        }
        return null;
        
    }

   
}
