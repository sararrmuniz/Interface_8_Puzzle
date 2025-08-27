package puzzle.model;

import java.util.*;

public final class Cromossomo {
    private final PuzzleModel tabuleiroInicial;
    private List<String> movimentos;
    private double fitness;
    private double distancia;
    private double custoMovimentos;
    private int geracaoEncontrada;
    
    public static final List<String> MOVIMENTOS_VALIDOS = 
        Collections.unmodifiableList(Arrays.asList("cima", "baixo", "esquerda", "direita"));
    private static final double PENALIDADE_TAMANHO = 0.0001;
    private static final Random random = new Random();

    public Cromossomo(PuzzleModel tabuleiroInicial) {
        this.tabuleiroInicial = tabuleiroInicial.copia();
        this.movimentos = new ArrayList<>();
        int numMovimentos = random.nextInt(26) + 15;
        for (int i = 0; i < numMovimentos; i++) {
            movimentos.add(MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size())));
        }
        calcularFitness();
    }

    public void calcularFitness() {
        PuzzleModel copia = tabuleiroInicial.copia();
        copia.aplicarMovimentos(movimentos);
        this.distancia = copia.calcularDistanciaManhattan();
        this.custoMovimentos = movimentos.size() * PENALIDADE_TAMANHO;
        this.fitness = distancia + custoMovimentos;
        
        if (copia.isSolucionado()) {
            this.fitness = 0.0;
            this.distancia = 0.0;
            this.custoMovimentos = 0.0;
        }
    }

    public void mutar() {
        if (random.nextDouble() < 0.5 || movimentos.isEmpty()) {
            String ultimo = movimentos.isEmpty() ? "" : movimentos.get(movimentos.size()-1);
            String novo;
            do {
                novo = MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size()));
            } while (isMovimentoOposto(ultimo, novo));
            movimentos.add(novo);
        } else {
            int index = random.nextInt(movimentos.size());
            String anterior = index > 0 ? movimentos.get(index-1) : "";
            String novo;
            do {
                novo = MOVIMENTOS_VALIDOS.get(random.nextInt(MOVIMENTOS_VALIDOS.size()));
            } while (isMovimentoOposto(anterior, novo));
            movimentos.set(index, novo);
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

    public int[] aplicarMovimentos() {
        PuzzleModel copia = tabuleiroInicial.copia();
        copia.aplicarMovimentos(movimentos);
        return copia.toArray1D();
    }

    // Getters e Setters
    public void setMovimentos(List<String> movimentos) {
        this.movimentos = new ArrayList<>(movimentos);
        calcularFitness();
    }

    public double getFitness() {
        return fitness;
    }

    public List<String> getMovimentos() {
        return Collections.unmodifiableList(movimentos);
    }

    public PuzzleModel getTabuleiroInicial() {
        return tabuleiroInicial;
    }

    public double getDistancia() {
        return distancia;
    }

    public double getCustoMovimentos() {
        return custoMovimentos;
    }

    public int getGeracaoEncontrada() {
        return geracaoEncontrada;
    }

    public void setGeracaoEncontrada(int geracao) {
        this.geracaoEncontrada = geracao;
    }
}