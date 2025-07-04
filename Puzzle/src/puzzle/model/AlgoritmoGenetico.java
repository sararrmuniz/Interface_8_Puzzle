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
    private int elitismo;
    private BiConsumer<Integer, List<int[]>> atualizacaoUI;
    private final Random random = new Random();

    public AlgoritmoGenetico(int[] tabuleiroInicial) {
        this.tabuleiroInicial = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        this.taxaMutacao = 0.3; // Padrão igual ao TCC
        this.taxaCrossover = 0.8; // Padrão igual ao TCC
        this.elitismo = 1; // Padrão igual ao TCC (1% seria populacao.size() * 0.01)
    }

    public void setTaxaMutacao(double taxa) {
        this.taxaMutacao = Math.max(0, Math.min(1, taxa));
    }

    public void setTaxaCrossover(double taxa) {
        this.taxaCrossover = Math.max(0, Math.min(1, taxa));
    }

    public void setElitismo(int elitismo) {
        this.elitismo = Math.max(1, elitismo);
    }

    public void setAtualizacaoUI(BiConsumer<Integer, List<int[]>> callback) {
        this.atualizacaoUI = callback;
    }

    public int[] resolver(int maxGeracoes, int tamanhoPopulacao, Supplier<Boolean> shouldStop) {
        // Logs de inicialização (iguais ao TCC)
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║       ALGORITMO GENÉTICO 8-PUZZLE    ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("\n▬▬▬ CONFIGURAÇÃO INICIAL ▬▬▬");
        System.out.println("▸ Gerações máximas: " + maxGeracoes);
        System.out.println("▸ Tamanho população: " + tamanhoPopulacao);
        System.out.println("▸ Taxa de mutação: " + (taxaMutacao * 100) + "%");
        System.out.println("▸ Taxa de crossover: " + (taxaCrossover * 100) + "%");
        System.out.println("▸ Elitismo: " + elitismo + " indivíduo(s)");
        System.out.println("\n▬▬▬ TABULEIRO INICIAL ▬▬▬");
        imprimirTabuleiro(tabuleiroInicial);

        List<Cromossomo> populacao = gerarPopulacaoInicial(tamanhoPopulacao);
        Cromossomo melhorGlobal = null;
        double optimalError = 0.0;

        for (int geracao = 0; geracao < maxGeracoes; geracao++) {
            if (shouldStop.get()) {
                System.out.println("\n⏹ Busca interrompida pelo usuário");
                break;
            }

            // Log da geração atual (igual ao TCC)
            System.out.println("\n══════ GERAÇÃO " + (geracao + 1) + " ══════");

            // Avaliação
            populacao.forEach(Cromossomo::calcularFitness);
            populacao.sort(Comparator.comparingDouble(Cromossomo::getFitness));
            Cromossomo melhor = populacao.get(0);

            // Log do melhor fitness (igual ao TCC)
            System.out.printf("🏆 Melhor fitness: %.4f (Distância: %.4f + Movimentos: %.4f)\n",
                melhor.getFitness(), melhor.getDistancia(), melhor.getCustoMovimentos());

            if (melhor.getFitness() <= optimalError) {
                System.out.println("\n🎉 SOLUÇÃO ÓTIMA ENCONTRADA! 🎉");
                melhorGlobal = melhor;
                break;
            }

            // Histórico para UI (opcional)
            List<int[]> historicoGeracao = new ArrayList<>();
            int[] estadoAtual = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
            historicoGeracao.add(estadoAtual);

            // Log dos movimentos (igual ao TCC)
            System.out.println("\n🔍 Sequência de movimentos:");
            int contador = 1;
            for (String movimento : melhor.getMovimentos()) {
                estadoAtual = aplicarMovimento(estadoAtual, movimento);
                historicoGeracao.add(Arrays.copyOf(estadoAtual, estadoAtual.length));

                int posVazia = encontrarPosicaoVazia(estadoAtual);
                int pecaMovida = estadoAtual[posVazia];
                System.out.printf("%2d. %-7s: Peça %d\n", contador++, movimento.toUpperCase(), pecaMovida);
            }

            // Atualização da UI
            if (atualizacaoUI != null) {
                atualizacaoUI.accept(geracao + 1, historicoGeracao);
            }

            // Nova população (elitismo + crossover + mutação)
            List<Cromossomo> novaPopulacao = new ArrayList<>();

            // Elitismo (1% da população, igual ao TCC)
            int eliteSize = (int) Math.ceil(populacao.size() * 0.01);
            for (int i = 0; i < eliteSize; i++) {
                novaPopulacao.add(new Cromossomo(populacao.get(i).getTabuleiroInicial()));
            }

            // Crossover e mutação
            while (novaPopulacao.size() < tamanhoPopulacao) {
                Cromossomo pai1 = selecaoPorRoleta(populacao);
                Cromossomo pai2 = selecaoPorRoleta(populacao);

                Cromossomo[] filhos = Cromossomo.crossover(pai1, pai2);
                if (random.nextDouble() < taxaMutacao) filhos[0].mutar();
                if (random.nextDouble() < taxaMutacao) filhos[1].mutar();

                novaPopulacao.add(filhos[0]);
                if (novaPopulacao.size() < tamanhoPopulacao) {
                    novaPopulacao.add(filhos[1]);
                }
            }

            populacao = novaPopulacao;
        }

        // Log do resultado final (igual ao TCC)
        System.out.println("\n▬▬▬ RESULTADO FINAL ▬▬▬");
        if (melhorGlobal != null) {
            System.out.printf("🎖️ Melhor fitness alcançado: %.4f\n", melhorGlobal.getFitness());
            System.out.printf("🔢 Total de movimentos: %d\n", melhorGlobal.getMovimentos().size());
            System.out.println("🏁 Estado final do tabuleiro:");
            imprimirTabuleiro(melhorGlobal.aplicarMovimentos());
        } else {
            System.out.println("Nenhuma solução encontrada.");
        }

        return melhorGlobal != null ? melhorGlobal.aplicarMovimentos() : tabuleiroInicial;
    }

    // Métodos auxiliares (mantidos do TCC)
    private List<Cromossomo> gerarPopulacaoInicial(int tamanhoPopulacao) {
        return IntStream.range(0, tamanhoPopulacao)
            .mapToObj(i -> new Cromossomo(tabuleiroInicial))
            .collect(Collectors.toList());
    }

    private Cromossomo selecaoPorRoleta(List<Cromossomo> populacao) {
        double fitnessTotal = populacao.stream()
            .mapToDouble(c -> 1.0 / (c.getFitness() + 1e-6)) // Igual ao TCC
            .sum();

        double valorRoleta = random.nextDouble() * fitnessTotal;
        double acumulado = 0;

        for (Cromossomo c : populacao) {
            acumulado += 1.0 / (c.getFitness() + 1e-6);
            if (acumulado >= valorRoleta) return c;
        }
        return populacao.get(populacao.size() - 1);
    }

    private int[] aplicarMovimento(int[] tabuleiro, String movimento) {
        int posVazia = encontrarPosicaoVazia(tabuleiro);
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
        }

        return tabuleiro;
    }

    private int encontrarPosicaoVazia(int[] tabuleiro) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == 0) return i;
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
            if (i < 2) System.out.println("\n╠═══╬═══╬═══╣");
        }
        System.out.println("\n╚═══╩═══╩═══╝");
    }
}