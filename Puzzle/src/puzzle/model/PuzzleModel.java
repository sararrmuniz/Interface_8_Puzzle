package puzzle.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PuzzleModel {
    private int[][] tabuleiro;
    private static final int[][] SOLUCAO = {
        {0, 1, 2},
        {3, 4, 5},
        {6, 7, 8}
    };

    public PuzzleModel() {
        this.tabuleiro = new int[3][3];
        reset();
    }

    public void reset() {
        int valor = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tabuleiro[i][j] = valor++;
            }
        }
    }

    public PuzzleModel copia() {
        PuzzleModel copia = new PuzzleModel();
        for (int i = 0; i < 3; i++) {
            System.arraycopy(this.tabuleiro[i], 0, copia.tabuleiro[i], 0, 3);
        }
        return copia;
    }

    public void embaralhar() {
        Random rand = new Random();
        int iteracoes = rand.nextInt(91) + 10;
        for (int i = 0; i < iteracoes; i++) {
            switch (rand.nextInt(4)) {
                case 0:
                    moverParaCima();
                    break;
                case 1:
                    moverParaBaixo();
                    break;
                case 2:
                    moverParaEsquerda();
                    break;
                case 3:
                    moverParaDireita();
                    break;
            }
        }
    }

    public boolean moverParaCima() {
        int[] posVazia = encontrarPosicao(0);
        if (posVazia[0] == 0) {
            return false;
        }
        trocar(posVazia, new int[]{posVazia[0] - 1, posVazia[1]});
        return true;
    }

    public boolean moverParaBaixo() {
        int[] posVazia = encontrarPosicao(0);
        if (posVazia[0] == 2) {
            return false;
        }
        trocar(posVazia, new int[]{posVazia[0] + 1, posVazia[1]});
        return true;
    }

    public boolean moverParaEsquerda() {
        int[] posVazia = encontrarPosicao(0);
        if (posVazia[1] == 0) {
            return false;
        }
        trocar(posVazia, new int[]{posVazia[0], posVazia[1] - 1});
        return true;
    }

    public boolean moverParaDireita() {
        int[] posVazia = encontrarPosicao(0);
        if (posVazia[1] == 2) {
            return false;
        }
        trocar(posVazia, new int[]{posVazia[0], posVazia[1] + 1});
        return true;
    }

    private void trocar(int[] a, int[] b) {
        int temp = tabuleiro[a[0]][a[1]];
        tabuleiro[a[0]][a[1]] = tabuleiro[b[0]][b[1]];
        tabuleiro[b[0]][b[1]] = temp;
    }

    public int[] encontrarPosicao(int valor) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tabuleiro[i][j] == valor) {
                    return new int[]{i, j};
                }
            }
        }
        throw new IllegalArgumentException("Valor não encontrado: " + valor);
    }

    public int calcularDistanciaManhattan() {
        int distancia = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int valor = tabuleiro[i][j];
                if (valor != 0) {
                    int linhaDesejada = valor / 3;
                    int colunaDesejada = valor % 3;
                    distancia += Math.abs(i - linhaDesejada) + Math.abs(j - colunaDesejada);
                }
            }
        }
        return distancia;
    }

    public boolean isSolucionado() {
        return Arrays.deepEquals(tabuleiro, SOLUCAO);
    }

    public void aplicarMovimentos(List<String> movimentos) {
        for (String movimento : movimentos) {
            switch (movimento) {
                case "cima":
                    moverParaCima();
                    break;
                case "baixo":
                    moverParaBaixo();
                    break;
                case "esquerda":
                    moverParaEsquerda();
                    break;
                case "direita":
                    moverParaDireita();
                    break;
            }
        }
    }

    public int[] toArray1D() {
        int[] array = new int[9];
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                array[index++] = tabuleiro[i][j];
            }
        }
        return array;
    }

    public void fromArray1D(int[] array) {
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tabuleiro[i][j] = array[index++];
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(Arrays.toString(tabuleiro[i])).append("\n");
        }
        return sb.toString();
    }
}