package puzzle.controller;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import puzzle.model.AlgoritmoGenetico;
import puzzle.model.PuzzleModel;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class FXMLPuzzleController {

    // UI Components
    @FXML
    private Label messageLabel;
    @FXML
    private Label lblGeracaoAtual;
    @FXML
    private GridPane gridPane;
    @FXML
    private Button goButton;
    @FXML
    private Button pararButton;
    @FXML
    private Button reiniciaButton;
    @FXML
    private TextField txtNumeroGeracoes;
    @FXML
    private TextField txtTamanhoPopulacao;
    @FXML
    private TextField txtTaxaMutacao;
    @FXML
    private TextField txtTaxaCrossover;
    @FXML
    private TextField txtElitismo;
    @FXML
    private Slider velocidadeSlider;
    @FXML
    private Label lblVelocidade;
    @FXML
    private VBox infoContainer;
    @FXML
    private Label lblMelhorFitness;
    @FXML
    private Label lblTotalMovimentos;
    @FXML
    private Label lblGeracaoEncontrada;

    // Game state
    private PuzzleModel puzzleModel = new PuzzleModel();
    private volatile boolean pararBusca = false;
    private AlgoritmoGenetico algoritmoGenetico;
    private ScheduledExecutorService executorService;
    private Timeline timelineAnimacao;
    private int[] tabuleiroAnterior;
    private Pane celulaAnimada;
    private int movimentoAtual = 0;
    private int geracaoAtual = 0;
    private List<List<int[]>> historicoPorGeracao = new ArrayList<>();
    private int geracaoAtualExibicao = 0;
    private int[] tabuleiroInicial;
    private Thread buscaThread;
    private int velocidadeAnimacao = 600;
    private static final int DURACAO_PADRAO_ANIMACAO = 300;

    @FXML
    public void initialize() {
        puzzleModel.embaralharTabuleiro();
        atualizarTabuleiroNaTela();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel.getTabuleiroAtual());

        velocidadeSlider.setMin(300);
        velocidadeSlider.setValue(600);

        velocidadeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            velocidadeAnimacao = newVal.intValue();
            lblVelocidade.setText(velocidadeAnimacao + " ms");
        });

        Platform.runLater(() -> {
            lblGeracaoAtual.getStyleClass().add("generation-0");
            lblGeracaoAtual.setText("Geração: 0");
        });
    }

    private void atualizarCorGeracao(int numeroGeracao) {
        int corIndex = numeroGeracao % 5;
        Platform.runLater(() -> {
            lblGeracaoAtual.getStyleClass().removeAll(
                    "generation-0", "generation-1", "generation-2",
                    "generation-3", "generation-4", "generation-solution"
            );
            lblGeracaoAtual.getStyleClass().add("generation-" + corIndex);

            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(lblGeracaoAtual.scaleXProperty(), 1),
                            new KeyValue(lblGeracaoAtual.scaleYProperty(), 1)
                    ),
                    new KeyFrame(Duration.millis(100),
                            new KeyValue(lblGeracaoAtual.scaleXProperty(), 1.1),
                            new KeyValue(lblGeracaoAtual.scaleYProperty(), 1.1)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(lblGeracaoAtual.scaleXProperty(), 1),
                            new KeyValue(lblGeracaoAtual.scaleYProperty(), 1)
                    )
            );
            pulse.play();
        });
    }

    @FXML
    public void executarBusca() {
        limparAnimacoes();
        goButton.setDisable(true);
        reiniciaButton.setDisable(true);
        pararButton.setDisable(false);
        pararBusca = false;
        movimentoAtual = 0;
        geracaoAtual = 0;
        geracaoAtualExibicao = 0;
        tabuleiroAnterior = null;
        historicoPorGeracao.clear();
        infoContainer.setVisible(false);

        tabuleiroInicial = Arrays.copyOf(puzzleModel.getTabuleiroAtual(), 9);

        messageLabel.setText("Buscando solução...");
        messageLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        lblGeracaoAtual.setText("Geração: 0");

        try {
            int numeroGeracoes = Integer.parseInt(txtNumeroGeracoes.getText());
            int tamanhoPopulacao = Integer.parseInt(txtTamanhoPopulacao.getText());
            double taxaMutacao = Double.parseDouble(txtTaxaMutacao.getText());
            double taxaCrossover = Double.parseDouble(txtTaxaCrossover.getText());
            double taxaElitismo = Double.parseDouble(txtElitismo.getText());

            if (taxaElitismo <= 0 || taxaElitismo > 100) {
                throw new IllegalArgumentException("Taxa de elitismo deve ser entre 0.1 e 100%");
            }

            algoritmoGenetico = new AlgoritmoGenetico(tabuleiroInicial);
            algoritmoGenetico.setTaxaMutacao(taxaMutacao);
            algoritmoGenetico.setTaxaCrossover(taxaCrossover);
            algoritmoGenetico.setTaxaElitismo(taxaElitismo / 100.0);

            buscaThread = new Thread(() -> {
                try {
                    algoritmoGenetico.setAtualizacaoUI((geracao, historicoGeracao) -> {
                        Platform.runLater(() -> {
                            pararButton.setDisable(false);

                            if (!historicoPorGeracao.isEmpty()
                                    && historicoPorGeracao.size() >= geracao) {
                                historicoPorGeracao.set(geracao - 1, historicoGeracao);
                            } else {
                                historicoPorGeracao.add(historicoGeracao);
                            }

                            if (geracao == 1
                                    || algoritmoGenetico.isSolucaoOtima(historicoGeracao.get(historicoGeracao.size() - 1))) {
                                mostrarMovimentosDaGeracao(geracao - 1);
                            }
                        });
                    });

                    int[] resultado = algoritmoGenetico.resolver(numeroGeracoes, tamanhoPopulacao, () -> pararBusca);

                    Platform.runLater(() -> {
                        if (!pararBusca) {
                            if (!algoritmoGenetico.isSolucaoOtima(resultado)) {
                                puzzleModel.setTabuleiroAtual(resultado);
                            }
                            finalizarBusca();
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Erro: " + e.getMessage());
                        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        reabilitarControles();
                    });
                }
            });
            buscaThread.setDaemon(true);
            buscaThread.start();

        } catch (NumberFormatException e) {
            messageLabel.setText("Valores inválidos!");
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            reabilitarControles();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            reabilitarControles();
        }
    }

    @FXML
    public void pararBusca() {
        pararBusca = true;
        limparAnimacoes();

        if (buscaThread != null && buscaThread.isAlive()) {
            buscaThread.interrupt();
        }

        Platform.runLater(() -> {
            messageLabel.setText("Busca interrompida.");
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            lblGeracaoAtual.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.5), 5, 0, 0, 0);");
            pararButton.setDisable(true);
        });

        reabilitarControles();
    }

    @FXML
    public void reiniciarTabuleiro() {
        limparAnimacoes();
        puzzleModel.embaralharTabuleiro();
        tabuleiroInicial = Arrays.copyOf(puzzleModel.getTabuleiroAtual(), 9);
        algoritmoGenetico = new AlgoritmoGenetico(tabuleiroInicial);
        tabuleiroAnterior = null;
        atualizarTabuleiroNaTela();
        messageLabel.setText("");
        infoContainer.setVisible(false);

        Platform.runLater(() -> {
            lblGeracaoAtual.getStyleClass().removeAll(
                    "generation-0", "generation-1", "generation-2",
                    "generation-3", "generation-4", "generation-solution"
            );
            lblGeracaoAtual.getStyleClass().add("generation-0");
            lblGeracaoAtual.setText("Geração: 0");
        });

        reabilitarControles();
        pararBusca = false;
        movimentoAtual = 0;
        geracaoAtual = 0;
        geracaoAtualExibicao = 0;
        historicoPorGeracao.clear();
    }

    private void mostrarMovimentosDaGeracao(int indiceGeracao) {
        if (pararBusca || indiceGeracao >= historicoPorGeracao.size()) {
            return;
        }

        // Verificação simplificada e robusta
        boolean mostrarEstaGeracao;
        List<int[]> movimentosAtuais = historicoPorGeracao.get(indiceGeracao);
        int[] ultimoEstado = movimentosAtuais.get(movimentosAtuais.size() - 1);

        mostrarEstaGeracao = (indiceGeracao < 5)
                || algoritmoGenetico.isSolucaoOtima(ultimoEstado)
                || (indiceGeracao == algoritmoGenetico.getGeracaoEncontrada());

        if (!mostrarEstaGeracao) {
            if (indiceGeracao + 1 < historicoPorGeracao.size()) {
                Platform.runLater(() -> mostrarMovimentosDaGeracao(indiceGeracao + 1));
            }
            return;
        }

        Platform.runLater(() -> {
            geracaoAtualExibicao = indiceGeracao;
            atualizarCorGeracao(indiceGeracao);
            puzzleModel.setTabuleiroAtual(Arrays.copyOf(tabuleiroInicial, 9));
            lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1)
                    + " | Movimento: 0/" + (movimentosAtuais.size() - 1));
        });

        // Novo sistema de animação com fila
        SequentialTransition sequencia = new SequentialTransition();

        for (int i = 0; i < movimentosAtuais.size() - 1; i++) {
            final int movimentoAtual = i + 1;
            int[] estadoAtual = movimentosAtuais.get(i);
            int[] proximoEstado = movimentosAtuais.get(movimentoAtual);

            KeyFrame kf = new KeyFrame(
                    Duration.millis(velocidadeAnimacao),
                    e -> {
                        if (isMovimentoValido(puzzleModel.getTabuleiroAtual(), proximoEstado)) {
                            animarMovimento(puzzleModel.getTabuleiroAtual(), proximoEstado);
                        }
                        puzzleModel.setTabuleiroAtual(proximoEstado);
                        Platform.runLater(()
                                -> lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1)
                                + " | Movimento: " + movimentoAtual
                                + "/" + (movimentosAtuais.size() - 1))
                        );
                    }
            );

            Timeline frame = new Timeline(kf);
            sequencia.getChildren().add(frame);
        }

        sequencia.setOnFinished(e -> {
            if (algoritmoGenetico.isSolucaoOtima(movimentosAtuais.get(movimentosAtuais.size() - 1))) {
                finalizarBusca();
            } else if (indiceGeracao + 1 < historicoPorGeracao.size()) {
                Platform.runLater(() -> mostrarMovimentosDaGeracao(indiceGeracao + 1));
            }
        });

        sequencia.play();
    }

    private void animarMovimento(int[] estadoAtual, int[] novoEstado) {
        // 1. Encontra a posição do espaço vazio (0) em ambos os estados
        int posVaziaAtual = encontrarIndice(estadoAtual, 0);
        int posVaziaNovo = encontrarIndice(novoEstado, 0);

        // 2. Verifica se o movimento é válido (apenas uma peça adjacente ao vazio se move)
        if (posVaziaAtual == -1 || posVaziaNovo == -1 || !saoAdjacentes(posVaziaAtual, posVaziaNovo)) {
            // Movimento inválido: atualiza o tabuleiro sem animação
            atualizarPosicoesFinais(novoEstado);
            return;
        }

        // 3. Encontra a peça que está se movendo (valor no espaço vazio do novo estado)
        int valorMovido = novoEstado[posVaziaAtual];
        int posPecaMovida = encontrarIndice(estadoAtual, valorMovido);

        // 4. Valida se a peça está adjacente ao espaço vazio
        if (!saoAdjacentes(posPecaMovida, posVaziaAtual)) {
            atualizarPosicoesFinais(novoEstado);
            return;
        }

        // 5. Animação da peça movendo-se para o espaço vazio
        limparAnimacaoAtual();
        Pane celulaOrigem = (Pane) gridPane.getChildren().get(posPecaMovida);
        Pane celulaDestino = (Pane) gridPane.getChildren().get(posVaziaAtual);

        celulaAnimada = criarCopiaCelula(celulaOrigem);
        GridPane.setRowIndex(celulaAnimada, posPecaMovida / 3);
        GridPane.setColumnIndex(celulaAnimada, posPecaMovida % 3);
        gridPane.getChildren().add(celulaAnimada);

        celulaOrigem.setVisible(false);

        // Calcula o deslocamento (da peça para o vazio)
        double deslocX = (posVaziaAtual % 3 - posPecaMovida % 3) * celulaOrigem.getWidth();
        double deslocY = (posVaziaAtual / 3 - posPecaMovida / 3) * celulaOrigem.getHeight();

        timelineAnimacao = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(celulaAnimada.translateXProperty(), 0),
                        new KeyValue(celulaAnimada.translateYProperty(), 0)
                ),
                new KeyFrame(Duration.millis(DURACAO_PADRAO_ANIMACAO),
                        new KeyValue(celulaAnimada.translateXProperty(), deslocX),
                        new KeyValue(celulaAnimada.translateYProperty(), deslocY)
                )
        );

        timelineAnimacao.setOnFinished(e -> {
            gridPane.getChildren().remove(celulaAnimada);
            celulaOrigem.setVisible(true);
            atualizarPosicoesFinais(novoEstado);
            celulaAnimada = null;
        });

        timelineAnimacao.play();
    }

    private void atualizarTabuleiroNaTela() {
        atualizarPosicoesFinais(puzzleModel.getTabuleiroAtual());
    }

    private boolean isMovimentoValido(int[] estadoAtual, int[] novoEstado) {
        // Encontra a posição do espaço vazio (0) em ambos os estados
        int posVaziaAtual = -1;
        int posVaziaNovo = -1;
        int diferencas = 0;

        for (int i = 0; i < estadoAtual.length; i++) {
            if (estadoAtual[i] != novoEstado[i]) {
                diferencas++;
                if (estadoAtual[i] == 0) {
                    posVaziaAtual = i;
                }
                if (novoEstado[i] == 0) {
                    posVaziaNovo = i;
                }
            }
        }

        // Verifica se:
        // 1. Apenas duas peças mudaram de posição (o vazio e uma peça)
        // 2. O movimento foi entre posições adjacentes
        // 3. O movimento foi de uma peça para o espaço vazio
        return diferencas == 2
                && saoAdjacentes(posVaziaAtual, posVaziaNovo)
                && estadoAtual[posVaziaNovo] == novoEstado[posVaziaAtual];
    }

    private boolean saoAdjacentes(int pos1, int pos2) {
        int linha1 = pos1 / 3, coluna1 = pos1 % 3;
        int linha2 = pos2 / 3, coluna2 = pos2 % 3;

        return (Math.abs(linha1 - linha2) == 1 && coluna1 == coluna2)
                || (Math.abs(coluna1 - coluna2) == 1 && linha1 == linha2);
    }

    private Pane criarCopiaCelula(Pane original) {
        Pane copia = new Pane();
        copia.setPrefSize(original.getWidth(), original.getHeight());
        copia.setMinSize(original.getMinWidth(), original.getMinHeight());
        copia.setMaxSize(original.getMaxWidth(), original.getMaxHeight());

        copia.getStyleClass().clear();
        copia.getStyleClass().addAll(original.getStyleClass());
        copia.setStyle(original.getStyle());
        copia.setBorder(original.getBorder());
        copia.setBackground(original.getBackground());
        copia.setEffect(original.getEffect());

        Label labelOriginal = (Label) original.getChildren().get(0);
        Label labelCopia = new Label(labelOriginal.getText());

        labelCopia.setPrefSize(labelOriginal.getWidth(), labelOriginal.getHeight());
        labelCopia.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        labelCopia.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        labelCopia.setFont(labelOriginal.getFont());
        labelCopia.setTextFill(labelOriginal.getTextFill());
        labelCopia.setAlignment(labelOriginal.getAlignment());

        labelCopia.getStyleClass().clear();
        labelCopia.getStyleClass().addAll(labelOriginal.getStyleClass());
        labelCopia.setStyle(labelOriginal.getStyle());

        copia.getChildren().add(labelCopia);
        return copia;
    }

    private int encontrarIndice(int[] tabuleiro, int valor) {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == valor) {
                return i;
            }
        }
        return -1;
    }

    private void atualizarPosicoesFinais(int[] tabuleiro) {
        for (int i = 0; i < 9; i++) {
            Pane cell = (Pane) gridPane.getChildren().get(i);
            Label label = (Label) cell.getChildren().get(0);
            int valor = tabuleiro[i];
            label.setText(valor == 0 ? "" : String.valueOf(valor));

            String corFundo;
            switch (valor) {
                case 1:
                    corFundo = "#ff9999";
                    break;
                case 2:
                    corFundo = "#ffcc99";
                    break;
                case 3:
                    corFundo = "#ffff99";
                    break;
                case 4:
                    corFundo = "#99ff99";
                    break;
                case 5:
                    corFundo = "#99ccff";
                    break;
                case 6:
                    corFundo = "#66ccff";
                    break;
                case 7:
                    corFundo = "#99ffcc";
                    break;
                case 8:
                    corFundo = "#ffccff";
                    break;
                default:
                    corFundo = "#e0e0e0";
                    break;
            }

            cell.setStyle("-fx-background-color: " + corFundo + "; -fx-border-color: #ddd;");
            cell.setVisible(true);

            label.setOpacity(0);
            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(label.opacityProperty(), 0)),
                    new KeyFrame(Duration.millis(300), new KeyValue(label.opacityProperty(), 1))
            );
            fadeIn.play();
        }
    }

    private void finalizarBusca() {
        try {
            int[] tabuleiroFinal = puzzleModel.getTabuleiroAtual();
            int[] tabuleiroResolvido = {1, 2, 3, 4, 5, 6, 7, 8, 0};
            boolean solucaoOtima = Arrays.equals(tabuleiroFinal, tabuleiroResolvido);

            Platform.runLater(() -> {
                // Configura o container de informações
                infoContainer.setVisible(true);
                if (solucaoOtima) {
                    infoContainer.getStyleClass().add("solucao-otima");
                } else {
                    infoContainer.getStyleClass().remove("solucao-otima");
                }

                // Atualiza os valores
                lblMelhorFitness.setText(String.format("%.4f", algoritmoGenetico.getMelhorFitness()));
                lblTotalMovimentos.setText(String.valueOf(algoritmoGenetico.getTotalMovimentosUltimaGeracao()));
                lblGeracaoEncontrada.setText(String.valueOf(algoritmoGenetico.getGeracaoEncontrada() + 1)); // +1 porque começa em 0

                lblGeracaoAtual.getStyleClass().removeAll(
                        "generation-0", "generation-1", "generation-2",
                        "generation-3", "generation-4"
                );
                lblGeracaoAtual.getStyleClass().add("generation-solution");

                if (solucaoOtima) {
                    messageLabel.setText("Solução ótima encontrada!");
                    messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    messageLabel.setText("Melhor solução encontrada (não ótima)");
                    messageLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 14px;");
                }

                pararButton.setDisable(true);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                messageLabel.setText("Erro ao verificar solução: " + e.getMessage());
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                pararButton.setDisable(true);
            });
        }
        reabilitarControles();
    }

    private void reabilitarControles() {
        Platform.runLater(() -> {
            goButton.setDisable(false);
            reiniciaButton.setDisable(false);
        });
    }

    private void limparAnimacoes() {
        limparAnimacaoAtual();
        limparExecutorService();
    }

    private void limparAnimacaoAtual() {
        // Para e limpa a animação em andamento
        if (timelineAnimacao != null) {
            timelineAnimacao.stop();
            timelineAnimacao = null;
        }

        // Remove a célula animada se existir
        if (celulaAnimada != null) {
            gridPane.getChildren().remove(celulaAnimada);
            celulaAnimada = null;
        }

        // Reseta todas as células para o estado padrão
        for (int i = 0; i < 9; i++) {
            Pane cell = (Pane) gridPane.getChildren().get(i);
            cell.setVisible(true);
            Label label = (Label) cell.getChildren().get(0);

            // Remove estilos de erro caso existam
            cell.getStyleClass().remove("movimento-invalido");
            label.getStyleClass().remove("movimento-invalido");

            // Reseta transformações
            label.setTranslateX(0);
            label.setTranslateY(0);
            label.setOpacity(1);
        }
    }

    private void limparExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
    }
}
