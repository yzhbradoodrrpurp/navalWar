import engine.GameEngine;
import ui.GameWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameEngine engine = new GameEngine(800, 600);
            // 创建并显示可视化窗口（窗口内部启动定时器并驱动 engine）
            GameWindow gw = new GameWindow(engine, 800, 600);
            gw.setVisible(true);
            engine.start();
        });
    }
}