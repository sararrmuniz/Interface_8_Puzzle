package puzzle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PuzzleModel {

    private int[] tabuleiroInicial = {1, 2, 3, 4, 5, 6, 7, 8, 0};  // Representação do tabuleiro resolvido
    private int[] tabuleiroAtual = new int[9];  // Estado atual do tabuleiro durante a busca

    public PuzzleModel() {
        embaralharTabuleiro();
    }

    public void embaralharTabuleiro() {
        // Embaralha as peças do tabuleiro e atualiza o estado interno
        List<Integer> pecas = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            pecas.add(i); // Adiciona números de 1 a 8
        }
        pecas.add(0); // Adiciona o espaço vazio
        Collections.shuffle(pecas); // Embaralha as peças

        for (int i = 0; i < 9; i++) {
            tabuleiroAtual[i] = pecas.get(i);
        }
    }

    public void realizarMovimentoAleatorio() {
        // Realiza um movimento aleatório válido no tabuleiro
        int espacoVazio = encontrarEspacoVazio(); // Encontra a posição do espaço vazio
        int[] movimentos = obterMovimentosValidos(espacoVazio); // Obtém movimentos válidos

        if (movimentos.length > 0) {
            int movimentoEscolhido = movimentos[(int) (Math.random() * movimentos.length)]; // Escolhe um movimento
            trocarPecas(espacoVazio, movimentoEscolhido); // Troca a posição do espaço vazio com a peça escolhida
        }
    }

    public boolean estadoAtualEhSolucao() {
        // Verifica se o estado atual do tabuleiro corresponde ao estado resolvido
        return Arrays.equals(tabuleiroAtual, tabuleiroInicial);
    }

    private int encontrarEspacoVazio() {
        // Encontra a posição do espaço vazio (0) no tabuleiro
        for (int i = 0; i < 9; i++) {
            if (tabuleiroAtual[i] == 0) {
                return i;
            }
        }
        return -1; // Valor de erro, caso o espaço vazio não seja encontrado
    }

    private int[] obterMovimentosValidos(int espacoVazio) {
        // Retorna os índices das posições válidas para mover o espaço vazio
        List<Integer> movimentos = new ArrayList<>();
        if (espacoVazio % 3 != 0) {
            movimentos.add(espacoVazio - 1); // Movimento à esquerda
        }
        if (espacoVazio % 3 != 2) {
            movimentos.add(espacoVazio + 1); // Movimento à direita
        }
        if (espacoVazio - 3 >= 0) {
            movimentos.add(espacoVazio - 3); // Movimento para cima
        }
        if (espacoVazio + 3 < 9) {
            movimentos.add(espacoVazio + 3); // Movimento para baixo
        }
        return movimentos.stream().mapToInt(i -> i).toArray();
    }

    private void trocarPecas(int posicao1, int posicao2) {
        // Troca os valores de duas posições no tabuleiro
        int temp = tabuleiroAtual[posicao1];
        tabuleiroAtual[posicao1] = tabuleiroAtual[posicao2];
        tabuleiroAtual[posicao2] = temp;
    }

    //retorna o estado atual do tabuleiro
    public int[] getTabuleiroAtual() {
        return tabuleiroAtual;
    }
}
