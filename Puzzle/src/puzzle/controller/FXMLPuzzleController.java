package puzzle.controller;

import puzzle.model.PuzzleModel;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.control.TextField;
import puzzle.model.PuzzleModel;

public class FXMLPuzzleController {

    @FXML
    private Label messageLabel;  // Label para exibir mensagens de status

    @FXML
    private GridPane gridPane;  // Grid onde o tabuleiro será exibido

    @FXML
    private Button goButton;  // Botão para iniciar a busca pela solução

    @FXML
    private Button pararButton;  // Botão para parar a busca

    @FXML
    private TextField txtRepeticoes;  // Campo de texto para o número máximo de repetições

    private PuzzleModel puzzleModel = new PuzzleModel(); // Instância do modelo
    private boolean pararBusca = false;  // Flag para interromper o processo de busca

    @FXML
    public void initialize() {
        // Inicializa o jogo: embaralha o tabuleiro e configura eventos dos botões
        puzzleModel.embaralharTabuleiro();
        atualizarTabuleiroNaTela();
        
        goButton.setOnAction(e -> goButtonClick());
        pararButton.setOnAction(e -> pararBusca());
    }

    @FXML
    public void goButtonClick() {
        // Inicia o processo de busca pela solução
        goButton.setDisable(true); // Desabilita o botão "Go" durante a execução
        int repeticoesMax = Integer.parseInt(txtRepeticoes.getText()); // Obtém o número máximo de repetições

        Thread buscaThread = new Thread(() -> {
            boolean encontrouSolucao = buscarSolucao(repeticoesMax);
            Platform.runLater(() -> { // Atualiza a interface após a execução da busca
                if (encontrouSolucao) {
                    messageLabel.setText("Solução encontrada com sucesso!");
                    messageLabel.setStyle("-fx-text-fill: green;");
                } else {
                    messageLabel.setText("Não foi possível encontrar a solução.");
                    messageLabel.setStyle("-fx-text-fill: red;");
                }
                goButton.setDisable(false); // Reabilita o botão "Go" após a execução
            });
        });

        buscaThread.start(); // Inicia a thread de busca
    }

    public boolean buscarSolucao(int repeticoesMax) {
        // Realiza a busca pela solução do quebra-cabeça
        int repeticoes = 0;
        boolean solucaoEncontrada = false;

        while (!solucaoEncontrada && repeticoes < repeticoesMax && !pararBusca) {
            puzzleModel.realizarMovimentoAleatorio(); // Faz um movimento aleatório

            if (puzzleModel.estadoAtualEhSolucao()) {
                solucaoEncontrada = true; // Verifica se o tabuleiro está na solução
            }

            repeticoes++;
            atualizarTabuleiroNaTela(); // Atualiza visualmente o tabuleiro

            try {
                Thread.sleep(150); // Atraso para tornar as ações visíveis
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return solucaoEncontrada;
    }

    private void atualizarTabuleiroNaTela() {
    Platform.runLater(() -> {
        int[] tabuleiroAtual = puzzleModel.getTabuleiroAtual();
        for (int i = 0; i < 9; i++) {
            int linha = i / 3;
            int coluna = i % 3;

            Pane cell = (Pane) gridPane.getChildren().get(i);
            Label label = (Label) cell.getChildren().get(0);
            int valor = tabuleiroAtual[i];

            // Define o texto (vazio para o espaço em branco)
            label.setText(valor == 0 ? "" : String.valueOf(valor));

            // Define a cor de fundo da célula com base no número
            String corFundo;
            switch (valor) {
                case 1: corFundo = "#ff9999"; break; // Vermelho claro
                case 2: corFundo = "#ffcc99"; break; // Laranja claro
                case 3: corFundo = "#ffff99"; break; // Amarelo
                case 4: corFundo = "#99ff99"; break; // Verde claro
                case 5: corFundo = "#99ccff"; break; // Azul claro
                case 6: corFundo = "#cc99ff"; break; // Roxo claro
                case 7: corFundo = "#ff99ff"; break; // Rosa
                case 8: corFundo = "#66cccc"; break; // Ciano
                default: corFundo = "white"; break; // Mantém o espaço vazio
            }

            // Aplica o estilo diretamente na célula
            cell.setStyle("-fx-background-color: " + corFundo + "; -fx-border-color: black; -fx-border-width: 2px; -fx-border-radius: 5px;");
        }
    });
}

    @FXML
    public void reiniciarTabuleiro() {
        // Reinicia o tabuleiro embaralhando-o novamente
        puzzleModel.embaralharTabuleiro();
        atualizarTabuleiroNaTela();
        messageLabel.setText(""); // Limpa mensagens de status
        goButton.setDisable(false); // Habilita o botão "Go"
        pararBusca = false;  // Reseta a flag de interrupção
    }

    @FXML
    public void pararBusca() {
        // Interrompe o processo de busca pela solução
        pararBusca = true;
    }
}
