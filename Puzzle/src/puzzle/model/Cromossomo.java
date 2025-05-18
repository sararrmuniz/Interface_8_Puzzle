package puzzle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Cromossomo {
    private int[] tabuleiroInicial;
    private List<String> movimentos;
    private double fitness;
    private double distancia;
    private double custoMovimentos;
    
    static final String[] MOVIMENTOS_VALIDOS = {"cima", "baixo", "esquerda", "direita"};
    
    public Cromossomo(int[] tabuleiroInicial) {
        this.tabuleiroInicial = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        this.movimentos = new ArrayList<>();
        this.fitness = Double.MAX_VALUE;
        
        Random rand = new Random();
        int numMovimentos = rand.nextInt(10) + 1; // 1-10 movimentos
        for (int i = 0; i < numMovimentos; i++) {
            movimentos.add(MOVIMENTOS_VALIDOS[rand.nextInt(MOVIMENTOS_VALIDOS.length)]);
        }
        
        calcularFitness();
    }
    
    public void setMovimentos(List<String> movimentos) {
        this.movimentos = new ArrayList<>(movimentos);
        calcularFitness();
    }
    
    public void calcularFitness() {
        int[] tabuleiroAtual = aplicarMovimentos();
        this.distancia = calcularDistanciaQuadratica(tabuleiroAtual);
        this.custoMovimentos = movimentos.size() * 0.01;
        this.fitness = distancia + custoMovimentos;
        
        if (distancia == 0) {
            this.fitness = -1000.0; // Solução ótima
        }
    }
    
    private double calcularDistanciaQuadratica(int[] tabuleiro) {
        double distancia = 0.0;
        for (int i = 0; i < 9; i++) {
            int valor = tabuleiro[i];
            if (valor != 0) {
                int linhaAtual = i / 3;
                int colunaAtual = i % 3;
                int linhaDesejada = (valor - 1) / 3;
                int colunaDesejada = (valor - 1) % 3;
                distancia += Math.pow(linhaAtual - linhaDesejada, 2) + 
                            Math.pow(colunaAtual - colunaDesejada, 2);
            }
        }
        return distancia; // Removida a normalização (/9.0)
    }
    
    public int[] aplicarMovimentos() {
        int[] tabuleiro = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        int emptyPos = encontrarPosicaoVazia(tabuleiro);
        
        for (String movimento : movimentos) {
            int linha = emptyPos / 3;
            int coluna = emptyPos % 3;
            int novaPos = -1;
            
            switch (movimento.toLowerCase()) {
                case "cima":
                    if (linha > 0) novaPos = emptyPos - 3;
                    break;
                case "baixo":
                    if (linha < 2) novaPos = emptyPos + 3;
                    break;
                case "esquerda":
                    if (coluna > 0) novaPos = emptyPos - 1;
                    break;
                case "direita":
                    if (coluna < 2) novaPos = emptyPos + 1;
                    break;
            }
            
            if (novaPos != -1 && tabuleiro[novaPos] != 0) {
                swap(tabuleiro, emptyPos, novaPos);
                emptyPos = novaPos;
            }
        }
        return tabuleiro;
    }
    
    private int encontrarPosicaoVazia(int[] tabuleiro) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == 0) return i;
        }
        return -1;
    }
    
    private void swap(int[] tabuleiro, int pos1, int pos2) {
        int temp = tabuleiro[pos1];
        tabuleiro[pos1] = tabuleiro[pos2];
        tabuleiro[pos2] = temp;
    }
    
    public void mutar() {
        Random rand = new Random();
        double chance = rand.nextDouble();
        
        if (chance < 0.3 || movimentos.isEmpty()) {
            String novoMovimento;
            String ultimoMovimento = movimentos.isEmpty() ? "" : movimentos.get(movimentos.size()-1);
            
            do {
                novoMovimento = MOVIMENTOS_VALIDOS[rand.nextInt(MOVIMENTOS_VALIDOS.length)];
            } while (isMovimentoOposto(ultimoMovimento, novoMovimento));
            
            movimentos.add(novoMovimento);
        } else if (chance < 0.6) {
            if (!movimentos.isEmpty()) {
                movimentos.remove(rand.nextInt(movimentos.size()));
            }
        } else {
            if (!movimentos.isEmpty()) {
                int index = rand.nextInt(movimentos.size());
                String novoMovimento;
                String movimentoAnterior = index > 0 ? movimentos.get(index-1) : "";
                String movimentoPosterior = index < movimentos.size()-1 ? movimentos.get(index+1) : "";
                
                do {
                    novoMovimento = MOVIMENTOS_VALIDOS[rand.nextInt(MOVIMENTOS_VALIDOS.length)];
                } while (isMovimentoOposto(movimentoAnterior, novoMovimento) || 
                        isMovimentoOposto(novoMovimento, movimentoPosterior));
                
                movimentos.set(index, novoMovimento);
            }
        }
        calcularFitness();
    }
    
    private boolean isMovimentoOposto(String movimento1, String movimento2) {
        if (movimento1 == null || movimento2 == null || movimento1.isEmpty() || movimento2.isEmpty()) {
            return false;
        }
        return (movimento1.equals("cima") && movimento2.equals("baixo")) ||
               (movimento1.equals("baixo") && movimento2.equals("cima")) ||
               (movimento1.equals("esquerda") && movimento2.equals("direita")) ||
               (movimento1.equals("direita") && movimento2.equals("esquerda"));
    }
    
    public static Cromossomo[] crossover(Cromossomo pai1, Cromossomo pai2) {
        Random rand = new Random();
        
        if (pai2.getFitness() < pai1.getFitness()) {
            Cromossomo temp = pai1;
            pai1 = pai2;
            pai2 = temp;
        }
        
        List<String> genesFilho1 = new ArrayList<>();
        List<String> genesFilho2 = new ArrayList<>();
        
        int pontoCorte = Math.min(pai1.movimentos.size(), pai2.movimentos.size()) / 2;
        
        genesFilho1.addAll(pai1.movimentos.subList(0, pontoCorte));
        if (pontoCorte < pai2.movimentos.size()) {
            genesFilho1.addAll(pai2.movimentos.subList(pontoCorte, pai2.movimentos.size()));
        }
        
        genesFilho2.addAll(pai2.movimentos.subList(0, pontoCorte));
        if (pontoCorte < pai1.movimentos.size()) {
            genesFilho2.addAll(pai1.movimentos.subList(pontoCorte, pai1.movimentos.size()));
        }
        
        Cromossomo filho1 = new Cromossomo(pai1.tabuleiroInicial);
        filho1.setMovimentos(genesFilho1);
        
        Cromossomo filho2 = new Cromossomo(pai2.tabuleiroInicial);
        filho2.setMovimentos(genesFilho2);
        
        return new Cromossomo[]{filho1, filho2};
    }
    
    public double getFitness() {
        return fitness;
    }
    
    public List<String> getMovimentos() {
        return new ArrayList<>(movimentos);
    }
    
    public int[] getTabuleiroInicial() {
        return Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
    }
    
    public double getDistancia() {
        return distancia;
    }
    
    public double getCustoMovimentos() {
        return custoMovimentos;
    }
}