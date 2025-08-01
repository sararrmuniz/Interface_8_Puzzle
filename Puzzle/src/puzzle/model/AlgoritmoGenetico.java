package puzzle.model;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class AlgoritmoGenetico {

    private final int[] tabuleiroInicial;
    private double taxaMutacao;
    private double taxaCrossover;
    private double taxaElitismo;
    private BiConsumer<Integer, List<int[]>> atualizacaoUI;
    private final Random random = new Random();
    private static final double optimalError = 0.0001;
    private Cromossomo melhorGlobal;
    private int geracaoEncontrada = 0;

    public AlgoritmoGenetico(int[] tabuleiroInicial) {
        this.tabuleiroInicial = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        this.taxaMutacao = 0.3;
        this.taxaCrossover = 0.8;
        this.taxaElitismo = 0.01;
    }

    public void setTaxaMutacao(double taxa) {
        this.taxaMutacao = Math.max(0, Math.min(1, taxa));
    }

    public void setTaxaCrossover(double taxa) {
        this.taxaCrossover = Math.max(0, Math.min(1, taxa));
    }

    public void setTaxaElitismo(double taxa) {
        this.taxaElitismo = Math.max(0.01, Math.min(1.0, taxa));
    }

    public void setAtualizacaoUI(BiConsumer<Integer, List<int[]>> callback) {
        this.atualizacaoUI = callback;
    }
    
     @FunctionalInterface
    public interface AtualizacaoUICallback {
        void accept(Integer geracao, List<int[]> historicoGeracao, Cromossomo melhorGlobal);
    }

    public boolean isSolucaoOtima(int[] tabuleiro) {
        int[] solucao = {1, 2, 3, 4, 5, 6, 7, 8, 0};
        return Arrays.equals(tabuleiro, solucao);
    }

    public boolean isSolucaoCompleta(Cromossomo cromossomo) {
        int[] estadoFinal = cromossomo.aplicarMovimentos();
        int[] solucaoOtima = {1, 2, 3, 4, 5, 6, 7, 8, 0};

        return Arrays.equals(estadoFinal, solucaoOtima)
                && cromossomo.getFitness() <= optimalError
                && cromossomo.getDistancia() == 0;
    }

    public double getMelhorFitness() {
        return melhorGlobal != null ? melhorGlobal.getFitness() : Double.MAX_VALUE;
    }

    public int getTotalMovimentosUltimaGeracao() {
        return melhorGlobal != null ? melhorGlobal.getMovimentos().size() : 0;
    }

    public int getGeracaoEncontrada() {
        return geracaoEncontrada;
    }

    public Cromossomo getMelhorGlobal() {
        return this.melhorGlobal;
    }

    public int[] resolver(int maxGeracoes, int tamanhoPopulacao, Supplier<Boolean> shouldStop) {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║       ALGORITMO GENÉTICO 8-PUZZLE    ║");
        System.out.println("╚════════════════════════════════════╝");

        System.out.println("\n▬▬▬ CONFIGURAÇÃO INICIAL ▬▬▬");
        System.out.println("▸ Gerações máximas: " + maxGeracoes);
        System.out.println("▸ Tamanho população: " + tamanhoPopulacao);
        System.out.println("▸ Taxa de mutação: " + (taxaMutacao * 100) + "%");
        System.out.println("▸ Taxa de crossover: " + (taxaCrossover * 100) + "%");
        System.out.println("▸ Taxa de elitismo: " + (taxaElitismo * 100) + "%");

        System.out.println("\n▬▬▬ TABULEIRO INICIAL ▬▬▬");
        imprimirTabuleiro(tabuleiroInicial);

        List<Cromossomo> populacao = gerarPopulacaoInicial(tamanhoPopulacao);
        melhorGlobal = null;
        geracaoEncontrada = 0;

        for (int geracao = 0; geracao < maxGeracoes; geracao++) {
            if (shouldStop.get()) {
                System.out.println("\n⏹ Busca interrompida pelo usuário");
                break;
            }

            System.out.println("\n══════ GERAÇÃO " + (geracao + 1) + " ══════");

            populacao.forEach(Cromossomo::calcularFitness);
            populacao.sort(Comparator.comparingDouble(Cromossomo::getFitness));
            Cromossomo melhor = populacao.get(0);

            if (melhorGlobal == null || melhor.getFitness() < melhorGlobal.getFitness()) {
                melhorGlobal = melhor;
                geracaoEncontrada = geracao;
            }

            System.out.printf("🏆 Melhor fitness: %.4f (Distância: %.4f + Movimentos: %.4f)\n",
                    melhor.getFitness(), melhor.getDistancia(), melhor.getCustoMovimentos());

            System.out.println("\n🔍 Sequência de movimentos:");
            int contador = 1;
            for (String movimento : melhor.getMovimentos()) {
                System.out.printf("%2d. %s\n", contador++, movimento.toUpperCase());
            }

            if (isSolucaoCompleta(melhor)) {
                System.out.println("\n🎉 SOLUÇÃO ÓTIMA ENCONTRADA! 🎉");
                int[] solucao = melhor.aplicarMovimentos();

                System.out.println("\n▬▬▬ RESULTADO FINAL ▬▬▬");
                System.out.printf("🎖️ Melhor fitness: %.4f\n", melhor.getFitness());
                System.out.printf("🔢 Movimentos: %d | Geração: %d\n",
                        melhor.getMovimentos().size(), geracaoEncontrada + 1);
                System.out.println("🏁 Tabuleiro final:");
                imprimirTabuleiro(solucao);

                if (atualizacaoUI != null) {
                    List<int[]> historico = new ArrayList<>();
                    int[] estadoAtual = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
                    historico.add(estadoAtual);

                    for (String movimento : melhor.getMovimentos()) {
                        estadoAtual = aplicarMovimento(estadoAtual, movimento);
                        historico.add(Arrays.copyOf(estadoAtual, estadoAtual.length));
                    }
                    atualizacaoUI.accept(geracao + 1, historico);
                }

                return solucao;
            }

            if (atualizacaoUI != null) {
                List<int[]> historico = new ArrayList<>();
                int[] estadoAtual = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
                historico.add(estadoAtual);

                for (String movimento : melhor.getMovimentos()) {
                    estadoAtual = aplicarMovimento(estadoAtual, movimento);
                    historico.add(Arrays.copyOf(estadoAtual, estadoAtual.length));
                }
                atualizacaoUI.accept(geracao + 1, historico);
            }

            // Nova geração
            List<Cromossomo> novaPopulacao = new ArrayList<>();
            int eliteSize = (int) Math.ceil(populacao.size() * taxaElitismo);
            eliteSize = Math.max(1, eliteSize);

            for (int i = 0; i < eliteSize; i++) {
                novaPopulacao.add(new Cromossomo(populacao.get(i).getTabuleiroInicial()));
            }

            while (novaPopulacao.size() < populacao.size()) {
                Cromossomo pai1 = selecaoPorRoleta(populacao);
                Cromossomo pai2 = selecaoPorRoleta(populacao);

                Cromossomo[] filhos = Cromossomo.crossover(pai1, pai2);
                if (random.nextDouble() < taxaMutacao) {
                    filhos[0].mutar();
                }
                if (random.nextDouble() < taxaMutacao) {
                    filhos[1].mutar();
                }

                novaPopulacao.add(filhos[0]);
                if (novaPopulacao.size() < populacao.size()) {
                    novaPopulacao.add(filhos[1]);
                }
            }
            populacao = novaPopulacao;
        }

        // Caso não encontre solução ótima
        System.out.println("\n▬▬▬ RESULTADO FINAL ▬▬▬");
        if (melhorGlobal != null) {
            int[] melhorTabuleiro = melhorGlobal.aplicarMovimentos();
            System.out.printf("🎖️ Melhor fitness: %.4f\n", melhorGlobal.getFitness());
            System.out.printf("🔢 Movimentos: %d | Geração: %d\n",
                    melhorGlobal.getMovimentos().size(), geracaoEncontrada + 1);
            System.out.println("🏁 Melhor estado encontrado:");
            imprimirTabuleiro(melhorTabuleiro);
            return melhorTabuleiro;
        }

        System.out.println("Nenhuma solução encontrada.");
        return tabuleiroInicial;
    }

    private List<Cromossomo> gerarPopulacaoInicial(int tamanhoPopulacao) {
        return IntStream.range(0, tamanhoPopulacao)
                .mapToObj(i -> new Cromossomo(tabuleiroInicial))
                .collect(Collectors.toList());
    }

    private Cromossomo selecaoPorRoleta(List<Cromossomo> populacao) {
        double fitnessTotal = populacao.stream()
                .mapToDouble(c -> 1.0 / (c.getFitness() + 1e-6))
                .sum();

        double valorRoleta = random.nextDouble() * fitnessTotal;
        double acumulado = 0;

        for (Cromossomo c : populacao) {
            acumulado += 1.0 / (c.getFitness() + 1e-6);
            if (acumulado >= valorRoleta) {
                return c;
            }
        }
        return populacao.get(populacao.size() - 1);
    }

    private int[] aplicarMovimento(int[] tabuleiro, String movimento) {
        int posVazia = encontrarPosicaoVazia(tabuleiro);
        int novaLinha = posVazia / 3;
        int novaColuna = posVazia % 3;
        int novaPos = -1;

        switch (movimento) {
            case "cima":
                if (novaLinha > 0) {
                    novaPos = posVazia - 3;
                }
                break;
            case "baixo":
                if (novaLinha < 2) {
                    novaPos = posVazia + 3;
                }
                break;
            case "esquerda":
                if (novaColuna > 0) {
                    novaPos = posVazia - 1;
                }
                break;
            case "direita":
                if (novaColuna < 2) {
                    novaPos = posVazia + 1;
                }
                break;
        }

        if (novaPos != -1) {
            int temp = tabuleiro[posVazia];
            tabuleiro[posVazia] = tabuleiro[novaPos];
            tabuleiro[novaPos] = temp;
        }

        return tabuleiro;
    }

    private int encontrarPosicaoVazia(int[] tabuleiro) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private void imprimirTabuleiro(int[] tabuleiro) {
        System.out.println("╔═══╦═══╦═══╗");
        for (int i = 0; i < 3; i++) {
            System.out.print("║");
            for (int j = 0; j < 3; j++) {
                int valor = tabuleiro[i * 3 + j];
                System.out.print(" " + (valor == 0 ? " " : valor) + " ║");
            }
            if (i < 2) {
                System.out.println("\n╠═══╬═══╬═══╣");
            }
        }
        System.out.println("\n╚═══╩═══╩═══╝");
    }
}
