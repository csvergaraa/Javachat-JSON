package Projeto;




import java.io.*;
import java.net.*;
// Classe de abertura do servidor, inicia e para a thread do servidor.
public class Servidor implements Runnable {
    
    ServerSocket servidor;
    
    TelaServidor main;
    boolean manterConectado = true;
   
    
    public Servidor(int port, TelaServidor main){
         System.out.println("Iniciando SERVIDOR na porta: "+ port);
         main.acrescentarMensagem("[SERVIDOR]: Iniciando SERVIDOR na porta: "+ port);
        try {
            this.main = main;
            servidor = new ServerSocket(port);
            System.out.println("[SERVIDOR]: SERVIDOR Iniciado!");
            main.acrescentarMensagem("[SERVIDOR]: SERVIDOR Iniciado!");
            
        } 
        catch (IOException e) { main.acrescentarMensagem("[IOException]: "+ e.getMessage()); } 
        catch (Exception e ){ main.acrescentarMensagem("[Exception]: "+ e.getMessage()); }
    }

   

    @Override
    public void run() {
        
        try {
            while(manterConectado){
                Socket socket = servidor.accept();                
                /** Socket thread **/
                new Thread(new ServidorThread(socket, main)).start();
                
                
                
            }
        } catch (IOException e) {
            main.acrescentarMensagem("[ServidorThreadIOException]: "+ e.getMessage());
        }
    }
    
    
    public void stop(){
        try {
            servidor.close();
            manterConectado = false;
            System.out.println("Agora o SERVIDOR está FECHADO....!");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
