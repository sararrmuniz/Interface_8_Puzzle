package puzzle.model;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class AlgoritmoGenetico {
    private final PuzzleModel tabuleiroInicial;
    private double taxaMutacao;
    private double taxaCrossover;
    private double taxaElitismo;
    private BiConsumer<Integer, List<int[]>> atualizacaoUI;
    private final Random random = new Random();
    private Cromossomo melhorGlobal;
    private int geracaoEncontrada = 0;

    public AlgoritmoGenetico(PuzzleModel tabuleiroInicial) {
        this.tabuleiroInicial = tabuleiroInicial.copia();
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
        imprimirTabuleiro(tabuleiroInicial.toArray1D());

        List<Cromossomo> populacao = new ArrayList<>();
        for (int i = 0; i < tamanhoPopulacao; i++) {
            populacao.add(new Cromossomo(tabuleiroInicial));
        }

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
                melhorGlobal.setGeracaoEncontrada(geracao);
                geracaoEncontrada = geracao;
            }

            System.out.printf("🏆 Melhor fitness: %.4f (Distância: %.4f + Movimentos: %.4f)\n",
                    melhor.getFitness(), melhor.getDistancia(), melhor.getCustoMovimentos());

            System.out.println("\n🔍 Sequência de movimentos:");
            int contador = 1;
            for (String movimento : melhor.getMovimentos()) {
                System.out.printf("%2d. %s\n", contador++, movimento.toUpperCase());
            }

            if (melhor.getFitness() == 0.0) {
                System.out.println("\n🎉 SOLUÇÃO ÓTIMA ENCONTRADA! 🎉");
                int[] solucao = melhor.aplicarMovimentos();

                System.out.println("\n▬▬▬ RESULTADO FINAL ▬▬▬");
                System.out.printf("🎖️ Melhor fitness: %.4f\n", melhor.getFitness());
                System.out.printf("🔢 Movimentos: %d | Geração: %d\n",
                        melhor.getMovimentos().size(), geracaoEncontrada + 1);
                System.out.println("🏁 Tabuleiro final:");
                imprimirTabuleiro(solucao);

                if (atualizacaoUI != null) {
                    enviarParaUI(geracao + 1, melhor);
                }

                return solucao;
            }

            List<Cromossomo> novaPopulacao = new ArrayList<>();
            int eliteSize = (int) Math.ceil(populacao.size() * taxaElitismo);
            eliteSize = Math.max(1, eliteSize);
            novaPopulacao.addAll(populacao.subList(0, eliteSize));

            while (novaPopulacao.size() < tamanhoPopulacao) {
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
                if (novaPopulacao.size() < tamanhoPopulacao) {
                    novaPopulacao.add(filhos[1]);
                }
            }

            populacao = novaPopulacao;

            if (atualizacaoUI != null) {
                enviarParaUI(geracao + 1, melhor);
            }
        }

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
        return tabuleiroInicial.toArray1D();
    }

    private void enviarParaUI(int geracao, Cromossomo melhor) {
        List<int[]> historico = new ArrayList<>();
        PuzzleModel copia = tabuleiroInicial.copia();
        historico.add(copia.toArray1D());

        for (String movimento : melhor.getMovimentos()) {
            switch (movimento) {
                case "cima":
                    copia.moverParaCima();
                    break;
                case "baixo":
                    copia.moverParaBaixo();
                    break;
                case "esquerda":
                    copia.moverParaEsquerda();
                    break;
                case "direita":
                    copia.moverParaDireita();
                    break;
            }
            historico.add(copia.toArray1D());
        }
        atualizacaoUI.accept(geracao, historico);
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

    public Cromossomo getMelhorGlobal() {
        return melhorGlobal;
    }

    public int getGeracaoEncontrada() {
        return geracaoEncontrada;
    }
}