package puzzle.controller;

import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import puzzle.model.*;

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
    @FXML
    private VBox infoContainer;
    @FXML
    private Label lblMelhorFitness;
    @FXML
    private Label lblTotalMovimentos;
    @FXML
    private Label lblGeracaoEncontrada;

    private PuzzleModel puzzleModel = new PuzzleModel();
    private volatile boolean pararBusca = false;
    private AlgoritmoGenetico algoritmoGenetico;
    private ScheduledExecutorService executorService;
    private Timeline timelineAnimacao;
    private Pane celulaAnimada;
    private int movimentoAtual = 0;
    private int geracaoAtual = 0;
    private List<List<int[]>> historicoPorGeracao = new ArrayList<>();
    private int geracaoAtualExibicao = 0;
    private Thread buscaThread;
    private int velocidadeAnimacao = 600;
    private static final int DURACAO_PADRAO_ANIMACAO = 300;
    private volatile boolean animacaoEmAndamento = false;

    @FXML
    public void initialize() {
        puzzleModel.embaralhar();
        atualizarTabuleiroNaTela();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel);

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
        historicoPorGeracao.clear();
        infoContainer.setVisible(false);

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

            algoritmoGenetico = new AlgoritmoGenetico(puzzleModel);
            algoritmoGenetico.setTaxaMutacao(taxaMutacao);
            algoritmoGenetico.setTaxaCrossover(taxaCrossover);
            algoritmoGenetico.setTaxaElitismo(taxaElitismo / 100.0);

            buscaThread = new Thread(() -> {
                try {
                    algoritmoGenetico.setAtualizacaoUI((geracao, historicoGeracao) -> {
                        Platform.runLater(() -> {
                            if (!historicoPorGeracao.isEmpty() && historicoPorGeracao.size() >= geracao) {
                                historicoPorGeracao.set(geracao - 1, historicoGeracao);
                            } else {
                                historicoPorGeracao.add(historicoGeracao);
                            }

                            if (geracao == 1) {
                                mostrarMovimentosDaGeracao(0);
                            }
                        });
                    });

                    int[] resultado = algoritmoGenetico.resolver(numeroGeracoes, tamanhoPopulacao, () -> pararBusca);

                    Platform.runLater(() -> {
                        if (!pararBusca) {
                            if (!Arrays.equals(resultado, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8})) {
                                puzzleModel.fromArray1D(resultado);
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
            pararButton.setDisable(true);
        });
    }

    @FXML
    public void reiniciarTabuleiro() {
        limparAnimacoes();
        puzzleModel.embaralhar();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel);
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

        animacaoEmAndamento = true;
        Platform.runLater(() -> pararButton.setDisable(false));

        Platform.runLater(() -> {
            geracaoAtualExibicao = indiceGeracao;
            atualizarCorGeracao(indiceGeracao);
            puzzleModel.reset();
            atualizarTabuleiroNaTela();
            lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1)
                    + " | Movimento: 0/" + (historicoPorGeracao.get(indiceGeracao).size() - 1));
        });

        List<int[]> movimentos = historicoPorGeracao.get(indiceGeracao);
        movimentoAtual = 1;

        limparExecutorService();
        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (movimentoAtual >= movimentos.size()) {
                    limparExecutorService();
                    puzzleModel.fromArray1D(movimentos.get(movimentos.size() - 1));
                    atualizarTabuleiroNaTela();

                    lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1)
                            + " | Movimento: " + (movimentos.size() - 1)
                            + "/" + (movimentos.size() - 1));

                    animacaoEmAndamento = false;

                    if (indiceGeracao < 4) {
                        mostrarMovimentosDaGeracao(indiceGeracao + 1);
                    } else if (indiceGeracao == 4) {
                        int melhorGeracao = algoritmoGenetico.getGeracaoEncontrada();
                        if (melhorGeracao > 4 && melhorGeracao < historicoPorGeracao.size()) {
                            mostrarMovimentosDaGeracao(melhorGeracao);
                        } else {
                            finalizarBusca();
                        }
                    } else {
                        finalizarBusca();
                    }
                    return;
                }

                int[] estadoAtual = puzzleModel.toArray1D();
                int[] proximoEstadoValido = null;
                int movimentosPulados = 0;

                while (movimentoAtual < movimentos.size() && proximoEstadoValido == null) {
                    int[] tentativaEstado = movimentos.get(movimentoAtual);
                    if (!Arrays.equals(estadoAtual, tentativaEstado)) {
                        proximoEstadoValido = tentativaEstado;
                    } else {
                        movimentosPulados++;
                        movimentoAtual++;
                    }
                }

                if (proximoEstadoValido != null) {
                    animarMovimento(estadoAtual, proximoEstadoValido);
                    puzzleModel.fromArray1D(proximoEstadoValido);

                    lblGeracaoAtual.setText("Geração: " + (indiceGeracao + 1)
                            + " | Movimento: " + movimentoAtual
                            + "/" + (movimentos.size() - 1)
                            + (movimentosPulados > 0 ? " | Pulados: " + movimentosPulados : ""));
                    movimentoAtual++;
                }
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

        int valorMovido = novoEstado[posVaziaAtual];
        int posPecaMovida = encontrarIndice(estadoAtual, valorMovido);

        if (!saoAdjacentes(posPecaMovida, posVaziaAtual)) {
            atualizarPosicoesFinais(novoEstado);
            return;
        }

        limparAnimacaoAtual();
        Pane celulaOrigem = (Pane) gridPane.getChildren().get(posPecaMovida);
        Pane celulaDestino = (Pane) gridPane.getChildren().get(posVaziaAtual);

        celulaAnimada = criarCopiaCelula(celulaOrigem);
        GridPane.setRowIndex(celulaAnimada, posPecaMovida / 3);
        GridPane.setColumnIndex(celulaAnimada, posPecaMovida % 3);
        gridPane.getChildren().add(celulaAnimada);

        celulaOrigem.setVisible(false);

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
        atualizarPosicoesFinais(puzzleModel.toArray1D());
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
            Cromossomo melhor = algoritmoGenetico.getMelhorGlobal();
            boolean solucaoOtima = melhor.getFitness() == 0.0; // Verifica se fitness é zero (solução ótima)

            Platform.runLater(() -> {
                infoContainer.setVisible(true);

                if (geracaoAtualExibicao > 4) {
                    lblGeracaoAtual.getStyleClass().removeAll(
                            "generation-0", "generation-1", "generation-2",
                            "generation-3", "generation-4");
                    lblGeracaoAtual.getStyleClass().add("generation-solution");
                    lblGeracaoAtual.setText("★ Melhor Geração: " + (algoritmoGenetico.getGeracaoEncontrada() + 1) + " ★");
                }

                lblMelhorFitness.setText(String.format("%.4f", melhor.getFitness()));
                lblTotalMovimentos.setText(String.valueOf(melhor.getMovimentos().size()));
                lblGeracaoEncontrada.setText(String.valueOf(algoritmoGenetico.getGeracaoEncontrada() + 1));

                if (solucaoOtima) {
                    messageLabel.setText("Solução ótima encontrada!");
                    messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    infoContainer.getStyleClass().add("solucao-otima");
                    lblGeracaoAtual.getStyleClass().add("generation-solution");
                } else {
                    messageLabel.setText("Melhor solução encontrada (não ótima)");
                    messageLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    infoContainer.getStyleClass().remove("solucao-otima");
                }

                if (!animacaoEmAndamento) {
                    pararButton.setDisable(true);
                }
                goButton.setDisable(false);
                reiniciaButton.setDisable(false);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                messageLabel.setText("Erro ao verificar solução: " + e.getMessage());
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            });
        }
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

    private void reabilitarControles() {
        Platform.runLater(() -> {
            goButton.setDisable(false);
            reiniciaButton.setDisable(false);
        });
    }

    private void limparAnimacoes() {
        limparAnimacaoAtual();
        limparExecutorService();
        animacaoEmAndamento = false;
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

            cell.getStyleClass().remove("movimento-invalido");
            label.getStyleClass().remove("movimento-invalido");

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

    private boolean saoAdjacentes(int pos1, int pos2) {
        int linha1 = pos1 / 3, coluna1 = pos1 % 3;
        int linha2 = pos2 / 3, coluna2 = pos2 % 3;

        return (Math.abs(linha1 - linha2) == 1 && coluna1 == coluna2)
                || (Math.abs(coluna1 - coluna2) == 1 && linha1 == linha2);
    }
}
