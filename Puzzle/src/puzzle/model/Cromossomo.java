package puzzle.model;

import java.util.*;
import java.util.stream.Collectors;

public final class Cromossomo {
    private final int[] tabuleiroInicial;
    private List<String> movimentos;
    private double fitness;
    private double distancia;
    private double custoMovimentos;
    
    public static final List<String> MOVIMENTOS_VALIDOS = 
        Collections.unmodifiableList(Arrays.asList("cima", "baixo", "esquerda", "direita"));
    private static final double PENALIDADE_TAMANHO = 0.0001; // Igual ao TCC
    private static final Random random = new Random();

    public Cromossomo(int[] tabuleiroInicial) {
        this.tabuleiroInicial = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        this.movimentos = new ArrayList<>();
        this.fitness = Double.MAX_VALUE;
        
        // Inicialização aleatória (15-40 movimentos, igual ao TCC)
        int numMovimentos = random.nextInt(26) + 15;
        for (int i = 0; i < numMovimentos; i++) {
            movimentos.add(MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size())));
        }
        calcularFitness();
    }

    public void calcularFitness() {
        int[] tabuleiroAtual = aplicarMovimentos();
        this.distancia = calcularDistanciaManhattan(tabuleiroAtual);
        this.custoMovimentos = movimentos.size() * PENALIDADE_TAMANHO;
        this.fitness = distancia + custoMovimentos;
        
        // Solução ótima (igual ao TCC)
        if (distancia == 0) this.fitness = 0.0;
    }

    private int calcularDistanciaManhattan(int[] tabuleiro) {
        int distancia = 0;
        for (int i = 0; i < 9; i++) {
            int valor = tabuleiro[i];
            if (valor != 0) {
                int linhaAtual = i / 3;
                int colunaAtual = i % 3;
                int linhaDesejada = (valor - 1) / 3; // Estado objetivo: [1,2,3,4,5,6,7,8,0]
                int colunaDesejada = (valor - 1) % 3;
                distancia += Math.abs(linhaAtual - linhaDesejada) + Math.abs(colunaAtual - colunaDesejada);
            }
        }
        return distancia;
    }

    public int[] aplicarMovimentos() {
        int[] tabuleiro = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        int posVazia = encontrarPosicaoVazia(tabuleiro);
        
        for (String movimento : movimentos) {
            int novaLinha = posVazia / 3;
            int novaColuna = posVazia % 3;
            int novaPos = -1;

            switch (movimento) {
                case "cima":    if (novaLinha > 0) novaPos = posVazia - 3; break;
                case "baixo":   if (novaLinha < 2) novaPos = posVazia + 3; break;
                case "esquerda": if (novaColuna > 0) novaPos = posVazia - 1; break;
                case "direita":  if (novaColuna < 2) novaPos = posVazia + 1; break;
            }

            if (novaPos != -1) {
                int temp = tabuleiro[posVazia];
                tabuleiro[posVazia] = tabuleiro[novaPos];
                tabuleiro[novaPos] = temp;
                posVazia = novaPos;
            }
        }
        return tabuleiro;
    }

    public void mutar() {
        if (random.nextDouble() < 0.5 || movimentos.isEmpty()) {
            // Adiciona movimento (evitando opostos)
            String ultimoMovimento = movimentos.isEmpty() ? "" : movimentos.get(movimentos.size() - 1);
            String novoMovimento;
            do {
                novoMovimento = MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size()));
            } while (isMovimentoOposto(ultimoMovimento, novoMovimento));
            movimentos.add(novoMovimento);
        } else {
            // Modifica movimento existente (evitando opostos)
            int index = random.nextInt(movimentos.size());
            String movimentoAnterior = index > 0 ? movimentos.get(index - 1) : "";
            String novoMovimento;
            do {
                novoMovimento = MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size()));
            } while (isMovimentoOposto(movimentoAnterior, novoMovimento));
            movimentos.set(index, novoMovimento);
        }
        calcularFitness();
    }

    private boolean isMovimentoOposto(String m1, String m2) {
        return (m1.equals("cima") && m2.equals("baixo")) ||
               (m1.equals("baixo") && m2.equals("cima")) ||
               (m1.equals("esquerda") && m2.equals("direita")) ||
               (m1.equals("direita") && m2.equals("esquerda"));
    }

    public static Cromossomo[] crossover(Cromossomo pai1, Cromossomo pai2) {
        // Ordena pelo melhor fitness (igual ao TCC)
        if (pai2.getFitness() < pai1.getFitness()) {
            Cromossomo temp = pai1;
            pai1 = pai2;
            pai2 = temp;
        }

        int pontoCorte = Math.min(pai1.movimentos.size(), pai2.movimentos.size()) / 2;
        List<String> genesFilho1 = new ArrayList<>(pai1.movimentos.subList(0, pontoCorte));
        genesFilho1.addAll(pai2.movimentos.subList(pontoCorte, pai2.movimentos.size()));

        List<String> genesFilho2 = new ArrayList<>(pai2.movimentos.subList(0, pontoCorte));
        genesFilho2.addAll(pai1.movimentos.subList(pontoCorte, pai1.movimentos.size()));

        Cromossomo filho1 = new Cromossomo(pai1.tabuleiroInicial);
        filho1.setMovimentos(genesFilho1);

        Cromossomo filho2 = new Cromossomo(pai2.tabuleiroInicial);
        filho2.setMovimentos(genesFilho2);

        return new Cromossomo[]{filho1, filho2};
    }

    // Getters e Setters
    public void setMovimentos(List<String> movimentos) {
        this.movimentos = new ArrayList<>(movimentos);
        calcularFitness();
    }
    public double getFitness() { return fitness; }
    public List<String> getMovimentos() { return Collections.unmodifiableList(movimentos); }
    public int[] getTabuleiroInicial() { return Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length); }
    public double getDistancia() { return distancia; }
    public double getCustoMovimentos() { return custoMovimentos; }

    private int encontrarPosicaoVazia(int[] tabuleiro) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == 0) return i;
        }
        return -1;
    }
}