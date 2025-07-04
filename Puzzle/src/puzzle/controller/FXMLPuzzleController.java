package puzzle.controller;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
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
import javafx.scene.control.Slider;

public class FXMLPuzzleController {

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
    private int velocidadeAnimacao = 600; // Valor padrão em ms
    private static final int DURACAO_PADRAO_ANIMACAO = 300; // ms

    @FXML
    public void initialize() {
        puzzleModel.embaralharTabuleiro();
        atualizarTabuleiroNaTela();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel.getTabuleiroAtual());

        // Configura o slider de velocidade
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
                    Arrays.asList("generation-0", "generation-1", "generation-2",
                            "generation-3", "generation-4", "generation-solution")
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

        tabuleiroInicial = Arrays.copyOf(puzzleModel.getTabuleiroAtual(), 9);

        messageLabel.setText("Buscando solução...");
        messageLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        lblGeracaoAtual.setText("Geração: 0");

        try {
            int numeroGeracoes = Integer.parseInt(txtNumeroGeracoes.getText());
            int tamanhoPopulacao = Integer.parseInt(txtTamanhoPopulacao.getText());

            algoritmoGenetico = new AlgoritmoGenetico(tabuleiroInicial);
            algoritmoGenetico.setTaxaMutacao(Double.parseDouble(txtTaxaMutacao.getText()));
            algoritmoGenetico.setTaxaCrossover(Double.parseDouble(txtTaxaCrossover.getText()));
            algoritmoGenetico.setElitismo(Integer.parseInt(txtElitismo.getText()));

            buscaThread = new Thread(() -> {
                try {
                    algoritmoGenetico.setAtualizacaoUI((geracao, historicoGeracao) -> {
                        Platform.runLater(() -> {
                            pararButton.setDisable(false);

                            List<int[]> historicoCompleto = new ArrayList<>();
                            historicoCompleto.add(Arrays.copyOf(tabuleiroInicial, 9));
                            historicoCompleto.addAll(historicoGeracao.subList(1, historicoGeracao.size()));

                            historicoPorGeracao.add(historicoCompleto);

                            if (geracao == 1) {
                                geracaoAtualExibicao = 0;
                                mostrarMovimentosDaGeracao(geracaoAtualExibicao);
                            }
                        });
                    });

                    int[] resultado = algoritmoGenetico.resolver(numeroGeracoes, tamanhoPopulacao, () -> pararBusca);

                    Platform.runLater(() -> {
                        if (!pararBusca) {
                            puzzleModel.setTabuleiroAtual(resultado);
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

        Platform.runLater(() -> {
            lblGeracaoAtual.getStyleClass().removeAll(
                    Arrays.asList("generation-0", "generation-1", "generation-2",
                            "generation-3", "generation-4", "generation-solution")
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

        Platform.runLater(() -> {
            atualizarCorGeracao(indiceGeracao);
            puzzleModel.setTabuleiroAtual(Arrays.copyOf(tabuleiroInicial, 9));
            tabuleiroAnterior = null;
            atualizarTabuleiroNaTela();
            lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1));
            pararButton.setDisable(false);
        });

        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<int[]> movimentos = historicoPorGeracao.get(indiceGeracao);
        movimentoAtual = 1;

        limparExecutorService();
        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (movimentoAtual >= movimentos.size()) {
                    limparExecutorService();
                    
                    // Mostra o estado final antes de passar para próxima geração
                    puzzleModel.setTabuleiroAtual(movimentos.get(movimentos.size()-1));
                    atualizarTabuleiroNaTela();
                    
                    // Verifica se é a última geração
                    if (geracaoAtualExibicao + 1 >= historicoPorGeracao.size()) {
                        return;
                    }
                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                if (geracaoAtualExibicao + 1 < historicoPorGeracao.size()) {
                                    geracaoAtualExibicao++;
                                    mostrarMovimentosDaGeracao(geracaoAtualExibicao);
                                }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    return;
                }

                int[] novoEstado = movimentos.get(movimentoAtual);
                animarMovimento(puzzleModel.getTabuleiroAtual(), novoEstado);
                puzzleModel.setTabuleiroAtual(novoEstado);
                movimentoAtual++;
            });
        }, 0, velocidadeAnimacao, TimeUnit.MILLISECONDS);
    }

    private void animarMovimento(int[] estadoAtual, int[] novoEstado) {
        int posVaziaAtual = encontrarIndice(estadoAtual, 0);
        int posVaziaNovo = encontrarIndice(novoEstado, 0);
        
        if (posVaziaAtual == -1 || posVaziaNovo == -1 || !saoAdjacentes(posVaziaAtual, posVaziaNovo)) {
            atualizarPosicoesFinais(novoEstado);
            return;
        }

        limparAnimacaoAtual();
        
        Pane celulaOrigem = (Pane) gridPane.getChildren().get(posVaziaNovo);
        Pane celulaDestino = (Pane) gridPane.getChildren().get(posVaziaAtual);
        
        celulaAnimada = criarCopiaCelula(celulaOrigem);
        GridPane.setRowIndex(celulaAnimada, posVaziaNovo / 3);
        GridPane.setColumnIndex(celulaAnimada, posVaziaNovo % 3);
        gridPane.getChildren().add(celulaAnimada);
        
        celulaOrigem.setVisible(false);
        
        double deslocX = (posVaziaAtual % 3 - posVaziaNovo % 3) * celulaOrigem.getWidth();
        double deslocY = (posVaziaAtual / 3 - posVaziaNovo / 3) * celulaOrigem.getHeight();
        
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

    private boolean isTransicaoValida(int[] anterior, int[] atual) {
        int diffCount = 0;
        int posVaziaAnterior = -1, posVaziaAtual = -1;

        for (int i = 0; i < anterior.length; i++) {
            if (anterior[i] != atual[i]) {
                diffCount++;
                if (anterior[i] == 0) {
                    posVaziaAnterior = i;
                }
                if (atual[i] == 0) {
                    posVaziaAtual = i;
                }
            }
        }

        return diffCount == 2 && saoAdjacentes(posVaziaAnterior, posVaziaAtual);
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

    private void atualizarLayoutCelulas() {
        for (int i = 0; i < 9; i++) {
            Pane cell = (Pane) gridPane.getChildren().get(i);
            cell.autosize();
            Label label = (Label) cell.getChildren().get(0);
            label.autosize();
        }
        gridPane.requestLayout();
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

            Platform.runLater(() -> {
                lblGeracaoAtual.getStyleClass().removeAll(
                        Arrays.asList("generation-0", "generation-1", "generation-2",
                                "generation-3", "generation-4")
                );
                lblGeracaoAtual.getStyleClass().add("generation-solution");

                Timeline flash = new Timeline(
                        new KeyFrame(Duration.millis(100),
                                new KeyValue(lblGeracaoAtual.opacityProperty(), 0.7)),
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(lblGeracaoAtual.opacityProperty(), 1))
                );
                flash.setCycleCount(4);
                flash.play();

                if (Arrays.equals(tabuleiroFinal, tabuleiroResolvido)) {
                    messageLabel.setText("Solução ótima encontrada!");
                    messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    messageLabel.setText("Melhor solução encontrada (não ótima)");
                    messageLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
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
        if (timelineAnimacao != null) {
            timelineAnimacao.stop();
            timelineAnimacao = null;
        }

        if (celulaAnimada != null) {
            gridPane.getChildren().remove(celulaAnimada);
            celulaAnimada = null;
        }

        for (int i = 0; i < 9; i++) {
            Pane cell = (Pane) gridPane.getChildren().get(i);
            cell.setVisible(true);
            Label label = (Label) cell.getChildren().get(0);
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