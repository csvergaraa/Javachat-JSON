/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Projeto;

/**
 *
 * @author Cesar
 *
 * 
 * 
 Classe do Usuário, a variavel ocupado foi criada apenas para o uso no cliente local, para sabermos se a pessoa esta em um unicast ou nao
 Se a pessoa estiver em um multicast, a string ocupada tem a string da porta do outro usuario, se nao, tem a string "nao"
 */
public class Usuario {
    private String nome;
    private String tipo;
    private String material;
    private String descricao;
    private String porta;
    private String ocupado;
    
    public Usuario(String nome, String tipo, String material, String porta, String descricao) {
        this.nome = nome;
        this.tipo = tipo;
        this.material = material;
        this.descricao = descricao;
        this.porta = porta;
        this.ocupado = "";
    }
    
    public Usuario(String nome, String tipo, String material, String porta) {
        this.nome = nome;
        this.tipo = tipo;
        this.material = material;
        this.porta = porta;
        this.ocupado = "";
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getPorta() {
        return porta;
    }

    public void setPorta(String porta) {
        this.porta = porta;
    }

    public String getOcupado() {
        return ocupado;
    }

    public void setOcupado(String ocupado) {
        this.ocupado = ocupado;
    }

}
