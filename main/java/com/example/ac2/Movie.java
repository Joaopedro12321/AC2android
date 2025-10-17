package com.example.ac2;

public class Movie {
    public String id;
    public String titulo;
    public String diretor;
    public int ano;       // 0 se vazio
    public int nota;      // 0..5
    public String genero; // Ação/Drama/etc.
    public boolean viuNoCinema;

    public Movie() {}

    public Movie(String id, String titulo, String diretor, int ano, int nota, String genero, boolean viuNoCinema) {
        this.id = id;
        this.titulo = titulo;
        this.diretor = diretor;
        this.ano = ano;
        this.nota = nota;
        this.genero = genero;
        this.viuNoCinema = viuNoCinema;
    }
}
