package puzzle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlgoritmoGenetico {
    private int[] tabuleiroInicial;
    private double taxaMutacao;
    private double taxaCrossover;
    private int elitismo;
    private BiConsumer<Integer, List<int[]>> atualizacaoUI;
    private Random random = new Random();
    
    public AlgoritmoGenetico(int[] tabuleiroInicial) {
        this.tabuleiroInicial = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
        this.taxaMutacao = 0.25;
        this.taxaCrossover = 0.3;
        this.elitismo = 2;
    }
    
    public void setTaxaMutacao(double taxa) {
        this.taxaMutacao = Math.max(0, Math.min(1, taxa));
    }
    
    public void setTaxaCrossover(double taxa) {
        this.taxaCrossover = Math.max(0, Math.min(1, taxa));
    }
    
    public void setElitismo(int elitismo) {
        this.elitismo = Math.max(0, elitismo);
    }
    
    public void setAtualizacaoUI(BiConsumer<Integer, List<int[]>> callback) {
        this.atualizacaoUI = callback;
    }
    
    public int[] resolver(int maxGeracoes, int tamanhoPopulacao) {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║       ALGORITMO GENÉTICO 8-PUZZLE    ║");
        System.out.println("╚════════════════════════════════════╝");
        
        System.out.println("\n▬▬▬ CONFIGURAÇÃO INICIAL ▬▬▬");
        System.out.println("▸ Gerações máximas: " + maxGeracoes);
        System.out.println("▸ Tamanho população: " + tamanhoPopulacao);
        System.out.println("▸ Taxa de mutação: " + (taxaMutacao*100) + "%");
        System.out.println("▸ Taxa de crossover: " + (taxaCrossover*100) + "%");
        System.out.println("▸ Elitismo: " + elitismo + " indivíduos");
        
        System.out.println("\n▬▬▬ TABULEIRO INICIAL ▬▬▬");
        imprimirTabuleiro(tabuleiroInicial);
        
        List<int[]> historicoCompleto = new ArrayList<>();
        historicoCompleto.add(Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length));
        
        List<Cromossomo> populacao = gerarPopulacaoInicial(tamanhoPopulacao);
        Cromossomo melhorGlobal = null;
        double optimalError = 0.0; // Alterado para exigir fitness = 0
        
        for (int geracao = 0; geracao < maxGeracoes; geracao++) {
            System.out.println("\n══════ GERAÇÃO " + (geracao+1) + " ══════");
            
            for (Cromossomo c : populacao) {
                c.calcularFitness();
            }
            
            populacao.sort(Comparator.comparingDouble(Cromossomo::getFitness));
            Cromossomo melhor = populacao.get(0);
            
            System.out.printf("🏆 Melhor fitness: %.2f (Distância: %.2f + Movimentos: %.2f)\n",
                melhor.getFitness(), melhor.getDistancia(), melhor.getCustoMovimentos());
            
            if (melhor.getFitness() <= optimalError) {
                System.out.println("\n🎉 SOLUÇÃO ÓTIMA ENCONTRADA! 🎉");
                melhorGlobal = melhor;
                break;
            }
            
            List<int[]> historicoGeracao = new ArrayList<>();
            int[] estadoAtual = Arrays.copyOf(tabuleiroInicial, tabuleiroInicial.length);
            historicoGeracao.add(Arrays.copyOf(estadoAtual, estadoAtual.length));
            
            System.out.println("\n🔍 Sequência de movimentos:");
            int contadorMovimentos = 1;
            for (int i = 0; i < melhor.getMovimentos().size(); i++) {
                String movimento = melhor.getMovimentos().get(i);
                int[] estadoAnterior = Arrays.copyOf(estadoAtual, estadoAtual.length);
                
                if (aplicarMovimento(estadoAtual, movimento)) {
                    int posVaziaAntes = encontrarPosicaoVazia(estadoAnterior);
                    int posVaziaDepois = encontrarPosicaoVazia(estadoAtual);
                    int pecaMovida = estadoAnterior[posVaziaDepois];
                    
                    System.out.printf("%2d. %-7s: Peça %d da posição %d → %d\n",
                        contadorMovimentos++,
                        movimento.toUpperCase(), 
                        pecaMovida,
                        posVaziaDepois, 
                        posVaziaAntes);
                    
                    System.out.println("   Estado resultante:");
                    imprimirTabuleiro(estadoAtual);
                    historicoGeracao.add(Arrays.copyOf(estadoAtual, estadoAtual.length));
                }
            }
            
            if (melhorGlobal == null || melhor.getFitness() < melhorGlobal.getFitness()) {
                melhorGlobal = new Cromossomo(melhor.getTabuleiroInicial());
                melhorGlobal.setMovimentos(new ArrayList<>(melhor.getMovimentos()));
                
                System.out.println("\n✨ NOVO MELHOR GLOBAL ✨");
                System.out.printf("🏅 Fitness: %.2f | Movimentos: %d\n", 
                    melhorGlobal.getFitness(), melhorGlobal.getMovimentos().size());
                System.out.println("📊 Estado final:");
                imprimirTabuleiro(melhorGlobal.aplicarMovimentos());
            }
            
            if (atualizacaoUI != null) {
                atualizacaoUI.accept(geracao + 1, historicoGeracao);
            }
            
            List<Cromossomo> novaPopulacao = new ArrayList<>();
            
            for (int i = 0; i < elitismo && i < populacao.size(); i++) {
                Cromossomo elite = new Cromossomo(populacao.get(i).getTabuleiroInicial());
                elite.setMovimentos(new ArrayList<>(populacao.get(i).getMovimentos()));
                novaPopulacao.add(elite);
            }
            
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
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("\n▬▬▬ RESULTADO FINAL ▬▬▬");
        if (melhorGlobal != null) {
            System.out.printf("🎖️ Melhor fitness alcançado: %.2f\n", melhorGlobal.getFitness());
            System.out.printf("🔢 Total de movimentos: %d\n", melhorGlobal.getMovimentos().size());
            System.out.println("🏁 Estado final do tabuleiro:");
            imprimirTabuleiro(melhorGlobal.aplicarMovimentos());
        } else {
            System.out.println("Nenhuma solução encontrada.");
        }
        
        return melhorGlobal != null ? melhorGlobal.aplicarMovimentos() : tabuleiroInicial;
    }
    
    private List<Cromossomo> gerarPopulacaoInicial(int tamanhoPopulacao) {
        return IntStream.range(0, tamanhoPopulacao)
            .mapToObj(i -> new Cromossomo(tabuleiroInicial))
            .collect(Collectors.toList());
    }
    
    private boolean aplicarMovimento(int[] tabuleiro, String movimento) {
        int posVazia = encontrarPosicaoVazia(tabuleiro);
        if (posVazia == -1) return false;

        int linha = posVazia / 3;
        int coluna = posVazia % 3;
        int novaPos = -1;

        switch (movimento.toLowerCase()) {
            case "cima":
                if (linha > 0) novaPos = posVazia - 3;
                break;
            case "baixo":
                if (linha < 2) novaPos = posVazia + 3;
                break;
            case "esquerda":
                if (coluna > 0) novaPos = posVazia - 1;
                break;
            case "direita":
                if (coluna < 2) novaPos = posVazia + 1;
                break;
        }

        if (novaPos != -1 && tabuleiro[novaPos] != 0) {
            swap(tabuleiro, posVazia, novaPos);
            return true;
        }
        return false;
    }
    
    private void swap(int[] tabuleiro, int pos1, int pos2) {
        int temp = tabuleiro[pos1];
        tabuleiro[pos1] = tabuleiro[pos2];
        tabuleiro[pos2] = temp;
    }
    
    private int encontrarPosicaoVazia(int[] tabuleiro) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == 0) return i;
        }
        return -1;
    }
    
    private Cromossomo selecaoPorRoleta(List<Cromossomo> populacao) {
        double fitnessTotal = populacao.stream()
            .mapToDouble(c -> 1.0 / (c.getFitness() + 1))
            .sum();
        
        double valorRoleta = random.nextDouble() * fitnessTotal;
        double acumulado = 0;
        
        for (Cromossomo c : populacao) {
            acumulado += 1.0 / (c.getFitness() + 1);
            if (acumulado >= valorRoleta) {
                return c;
            }
        }
        
        return populacao.get(populacao.size() - 1);
    }
    
    private void imprimirTabuleiro(int[] tabuleiro) {
        System.out.println("╔═══╦═══╦═══╗");
        for (int i = 0; i < 3; i++) {
            System.out.print("║");
            for (int j = 0; j < 3; j++) {
                int valor = tabuleiro[i*3 + j];
                System.out.print(" " + (valor == 0 ? " " : valor) + " ║");
            }
            if (i < 2) System.out.println("\n╠═══╬═══╬═══╣");
        }
        System.out.println("\n╚═══╩═══╩═══╝");
    }
    
    public static int calcularDistanciaManhattan(int[] tabuleiro) {
        return PuzzleModel.calcularDistanciaManhattan(tabuleiro);
    }
}