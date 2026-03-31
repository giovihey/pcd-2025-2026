package pcd.sketch01.controller;

import pcd.sketch01.model.Board;
import pcd.sketch01.model.GameManager;
import pcd.sketch01.view.View;
import pcd.sketch01.view.ViewModel;

public class GameController {

    private final Board board;
    private final ViewModel viewModel;
    private final View view;

    private final BotController botController;
    private final GameManager gameManager;

    public GameController(Board board, ViewModel viewModel, View view) {
        this.board = board;
        this.viewModel = viewModel;
        this.view = view;

        long t0 = System.currentTimeMillis();
        this.botController = new BotController(board.getBotBall(), t0);
        this.gameManager = new GameManager(board);

        InputController inputController = new InputController(board.getPlayerBall());
        inputController.registerKeyListener(view.getFrame());
    }

    public void start() {
        viewModel.update(board, 0);
        waitABit();

        long t0 = System.currentTimeMillis();
        long lastUpdateTime = t0;
        long frameCount = 0;

        while (!gameManager.isGameEnded()) {
            long frameStart = System.currentTimeMillis();

            // --- Controller ---
            botController.update();

            // --- Model ---
            long elapsed = frameStart - lastUpdateTime;
            lastUpdateTime = frameStart;
            board.updateState(elapsed);
            gameManager.updateGameState();

            // --- View: snapshot + render (blocks until Swing finishes painting) ---
            frameCount++;
            long dt = frameStart - t0;
            int fps = dt > 0 ? (int)(frameCount * 1000 / dt) : 0;
            viewModel.update(board, fps);
            view.render(); // RenderSynch naturally paces the loop here
        }

        gameManager.printGameResult();
    }

    private void waitABit() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}