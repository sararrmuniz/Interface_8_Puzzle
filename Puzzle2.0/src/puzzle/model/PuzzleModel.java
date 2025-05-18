package puzzle.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PuzzleModel {
    private int[] tabuleiroAtual;
    
    public PuzzleModel() {
        tabuleiroAtual = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0}; // Estado inicial resolvido
    }
    
    public void embaralharTabuleiro() {
        Random rand = new Random();
        tabuleiroAtual = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0};
        
        // Faz 100 movimentos aleatórios válidos para embaralhar
        int emptyPos = 8; // Posição inicial do espaço vazio
        for (int i = 0; i < 100; i++) {
            int[] moves = getMovimentosValidos(emptyPos);
            int move = moves[rand.nextInt(moves.length)];
            swap(emptyPos, move);
            emptyPos = move;
        }
    }
    
    private int[] getMovimentosValidos(int emptyPos) {
        // Retorna posições adjacentes válidas para mover
        List<Integer> moves = new ArrayList<>();
        // Pode mover para cima?
        if (emptyPos - 3 >= 0) moves.add(emptyPos - 3);
        // Pode mover para baixo?
        if (emptyPos + 3 < 9) moves.add(emptyPos + 3);
        // Pode mover para esquerda?
        if (emptyPos % 3 != 0) moves.add(emptyPos - 1);
        // Pode mover para direita?
        if (emptyPos % 3 != 2) moves.add(emptyPos + 1);
        
        return moves.stream().mapToInt(i -> i).toArray();
    }
    
    private void swap(int pos1, int pos2) {
        int temp = tabuleiroAtual[pos1];
        tabuleiroAtual[pos1] = tabuleiroAtual[pos2];
        tabuleiroAtual[pos2] = temp;
    }
    
    public int[] getTabuleiroAtual() {
        return Arrays.copyOf(tabuleiroAtual, tabuleiroAtual.length);
    }
    
    public void setTabuleiroAtual(int[] novoEstado) {
        if (novoEstado.length == 9) {
            this.tabuleiroAtual = Arrays.copyOf(novoEstado, novoEstado.length);
        }
    }
    
    public static int calcularDistanciaManhattan(int[] tabuleiro) {
        int distancia = 0;
        for (int i = 0; i < 9; i++) {
            int valor = tabuleiro[i];
            if (valor != 0) {
                int linhaAtual = i / 3;
                int colunaAtual = i % 3;
                int linhaDesejada = (valor - 1) / 3;
                int colunaDesejada = (valor - 1) % 3;
                distancia += Math.abs(linhaAtual - linhaDesejada) + Math.abs(colunaAtual - colunaDesejada);
            }
        }
        return distancia;
    }
    
    public boolean ehSolucionavel(int[] estado) {
        int inversoes = 0;
        for (int i = 0; i < estado.length; i++) {
            for (int j = i + 1; j < estado.length; j++) {
                if (estado[i] != 0 && estado[j] != 0 && estado[i] > estado[j]) {
                    inversoes++;
                }
            }
        }
        return inversoes % 2 == 0;
    }
}