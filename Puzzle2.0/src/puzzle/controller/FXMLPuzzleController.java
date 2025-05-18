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
import java.util.Collections;

public class FXMLPuzzleController {
    @FXML private Label messageLabel;
    @FXML private Label lblGeracaoAtual;
    @FXML private GridPane gridPane;
    @FXML private Button goButton;
    @FXML private Button pararButton;
    @FXML private Button reiniciaButton;
    @FXML private TextField txtNumeroGeracoes;
    @FXML private TextField txtTamanhoPopulacao;
    
    private PuzzleModel puzzleModel = new PuzzleModel();
    private boolean pararBusca = false;
    private AlgoritmoGenetico algoritmoGenetico;
    private ScheduledExecutorService executorService;
    private List<int[]> historicoMovimentos;
    private int movimentoAtual = 0;
    private int geracaoAtual = 0;
    private Timeline timelineAnimacao;
    private int[] tabuleiroAnterior;
    private Pane celulaAnimada;

    @FXML
    public void initialize() {
        puzzleModel.embaralharTabuleiro();
        atualizarTabuleiroNaTela();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel.getTabuleiroAtual());
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
        tabuleiroAnterior = null;
        
        messageLabel.setText("Buscando solução...");
        messageLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        lblGeracaoAtual.setText("Geração: 0");

        try {
            int numeroGeracoes = Integer.parseInt(txtNumeroGeracoes.getText());
            int tamanhoPopulacao = Integer.parseInt(txtTamanhoPopulacao.getText());

            algoritmoGenetico = new AlgoritmoGenetico(puzzleModel.getTabuleiroAtual());
            algoritmoGenetico.setTaxaMutacao(0.25);
            algoritmoGenetico.setTaxaCrossover(0.3);
            algoritmoGenetico.setElitismo(2);

            Thread buscaThread = new Thread(() -> {
                try {
                    algoritmoGenetico.setAtualizacaoUI((geracao, historicoCompleto) -> {
                        Platform.runLater(() -> {
                            geracaoAtual = geracao;
                            lblGeracaoAtual.setText("Geração: " + geracaoAtual);
                            historicoMovimentos = Collections.synchronizedList(historicoCompleto);
                            iniciarAnimacaoMovimentos();
                        });
                    });
                    
                    int[] resultado = algoritmoGenetico.resolver(numeroGeracoes, tamanhoPopulacao);
                    Platform.runLater(() -> {
                        puzzleModel.setTabuleiroAtual(resultado);
                        finalizarBusca();
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

    private void iniciarAnimacaoMovimentos() {
        limparExecutorService();
        
        List<int[]> movimentosValidos = new ArrayList<>();
        int[] estadoAtual = puzzleModel.getTabuleiroAtual();
        movimentosValidos.add(estadoAtual);
        
        for (int[] proximoEstado : historicoMovimentos) {
            if (ehMovimentoValido(estadoAtual, proximoEstado)) {
                movimentosValidos.add(proximoEstado);
                estadoAtual = proximoEstado;
            }
        }
        
        executorService = Executors.newSingleThreadScheduledExecutor();
        movimentoAtual = 0;
        
        executorService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (pararBusca || movimentoAtual >= movimentosValidos.size()) {
                    limparExecutorService();
                    if (movimentoAtual >= movimentosValidos.size()) {
                        finalizarBusca();
                    }
                    return;
                }
                
                puzzleModel.setTabuleiroAtual(movimentosValidos.get(movimentoAtual));
                atualizarTabuleiroNaTela();
                movimentoAtual++;
            });
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private boolean ehMovimentoValido(int[] estadoAtual, int[] proximoEstado) {
        int posVaziaAtual = -1;
        int posVaziaProximo = -1;
        int pecaMovida = -1;
        int diferencas = 0;

        for (int i = 0; i < estadoAtual.length; i++) {
            if (estadoAtual[i] != proximoEstado[i]) {
                if (estadoAtual[i] == 0) {
                    posVaziaAtual = i;
                    pecaMovida = proximoEstado[i];
                } else if (proximoEstado[i] == 0) {
                    posVaziaProximo = i;
                }
                diferencas++;
            }
        }

        if (diferencas != 2 || pecaMovida == 0) {
            return false;
        }

        int diffLinha = Math.abs((posVaziaAtual / 3) - (posVaziaProximo / 3));
        int diffColuna = Math.abs((posVaziaAtual % 3) - (posVaziaProximo % 3));
        
        return (diffLinha == 1 && diffColuna == 0) || 
               (diffLinha == 0 && diffColuna == 1);
    }

    private void atualizarTabuleiroNaTela() {
        int[] tabuleiroAtual = puzzleModel.getTabuleiroAtual();
        
        if (tabuleiroAnterior != null && Arrays.equals(tabuleiroAnterior, tabuleiroAtual)) {
            atualizarPosicoesFinais(tabuleiroAtual);
            return;
        }
        
        if (tabuleiroAnterior == null) {
            tabuleiroAnterior = Arrays.copyOf(tabuleiroAtual, tabuleiroAtual.length);
            atualizarPosicoesFinais(tabuleiroAtual);
            return;
        }
        
        int indiceVazioAnterior = encontrarIndice(tabuleiroAnterior, 0);
        int indiceVazioAtual = encontrarIndice(tabuleiroAtual, 0);
        
        if (indiceVazioAnterior == indiceVazioAtual) {
            atualizarPosicoesFinais(tabuleiroAtual);
            tabuleiroAnterior = Arrays.copyOf(tabuleiroAtual, tabuleiroAtual.length);
            return;
        }
        
        limparAnimacaoAtual();
        atualizarLayoutCelulas();
        
        Pane celulaOrigem = (Pane) gridPane.getChildren().get(indiceVazioAnterior);
        Pane celulaDestino = (Pane) gridPane.getChildren().get(indiceVazioAtual);
        
        int pecaMovendo = tabuleiroAnterior[indiceVazioAtual];
        Label labelMovendo = (Label) celulaOrigem.getChildren().get(0);
        Label labelVazio = (Label) celulaDestino.getChildren().get(0);
        
        labelMovendo.setText(pecaMovendo == 0 ? "" : String.valueOf(pecaMovendo));
        labelVazio.setText("");
        
        int linhaOrigem = indiceVazioAnterior / 3;
        int colunaOrigem = indiceVazioAnterior % 3;
        int linhaDestino = indiceVazioAtual / 3;
        int colunaDestino = indiceVazioAtual % 3;
        
        celulaAnimada = criarCopiaCelula(celulaOrigem);
        GridPane.setRowIndex(celulaAnimada, linhaOrigem);
        GridPane.setColumnIndex(celulaAnimada, colunaOrigem);
        gridPane.getChildren().add(celulaAnimada);
        
        celulaOrigem.setVisible(false);
        
        double width = celulaOrigem.getBoundsInParent().getWidth();
        double height = celulaOrigem.getBoundsInParent().getHeight();
        
        timelineAnimacao = new Timeline();
        
        KeyValue kvX = new KeyValue(celulaAnimada.translateXProperty(), 
                                 (colunaDestino - colunaOrigem) * width);
        KeyValue kvY = new KeyValue(celulaAnimada.translateYProperty(), 
                                 (linhaDestino - linhaOrigem) * height);
        
        KeyFrame kf = new KeyFrame(Duration.millis(300), kvX, kvY);
        timelineAnimacao.getKeyFrames().add(kf);
        
        timelineAnimacao.setOnFinished(e -> {
            gridPane.getChildren().remove(celulaAnimada);
            celulaOrigem.setVisible(true);
            atualizarPosicoesFinais(tabuleiroAtual);
            tabuleiroAnterior = Arrays.copyOf(tabuleiroAtual, tabuleiroAtual.length);
            celulaAnimada = null;
        });
        
        timelineAnimacao.play();
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
                case 1: corFundo = "#ff9999"; break;
                case 2: corFundo = "#ffcc99"; break;
                case 3: corFundo = "#ffff99"; break;
                case 4: corFundo = "#99ff99"; break;
                case 5: corFundo = "#99ccff"; break;
                case 6: corFundo = "#66ccff"; break;
                case 7: corFundo = "#99ffcc"; break;
                case 8: corFundo = "#ffccff"; break;
                default: corFundo = "#f0f0f0"; break;
            }
            
            cell.setStyle("-fx-background-color: " + corFundo + ";");
            cell.setVisible(true);
            
            label.setOpacity(0);
            Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(label.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(label.opacityProperty(), 1))
            );
            fadeIn.play();
        }
    }
    
    private void finalizarBusca() {
    try {
        int[] tabuleiroFinal = puzzleModel.getTabuleiroAtual();
        int[] tabuleiroResolvido = {1, 2, 3, 4, 5, 6, 7, 8, 0};
        
        if (Arrays.equals(tabuleiroFinal, tabuleiroResolvido)) {
            messageLabel.setText("Solução ótima encontrada!");
            messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            messageLabel.setText("Melhor solução encontrada (não ótima)");
            messageLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        }
    } catch (Exception e) {
        messageLabel.setText("Erro ao verificar solução: " + e.getMessage());
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }
    reabilitarControles();
}
    
    private void reabilitarControles() {
        goButton.setDisable(false);
        reiniciaButton.setDisable(false);
        pararButton.setDisable(true);
    }

    @FXML
    public void reiniciarTabuleiro() {
        limparAnimacoes();
        puzzleModel.embaralharTabuleiro();
        algoritmoGenetico = new AlgoritmoGenetico(puzzleModel.getTabuleiroAtual());
        tabuleiroAnterior = null;
        atualizarTabuleiroNaTela();
        messageLabel.setText("");
        lblGeracaoAtual.setText("Geração: 0");
        reabilitarControles();
        pararBusca = false;
        movimentoAtual = 0;
        geracaoAtual = 0;
    }

    @FXML
    public void pararBusca() {
        pararBusca = true;
        limparAnimacoes();
        messageLabel.setText("Busca interrompida.");
        messageLabel.setStyle("-fx-text-fill: red;");
        reabilitarControles();
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