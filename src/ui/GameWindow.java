package ui;

import engine.GameEngine;
import entities.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class GameWindow extends JFrame {
    private final GameEngine engine;
    private final DrawPanel panel;
    private final Timer timer;
    private volatile boolean leftPressed = false;
    private volatile boolean rightPressed = false;
    private volatile boolean dropRequested = false;

    // 新增图片资源字段
    private BufferedImage imgBackground;
    private BufferedImage[] imgShip = new BufferedImage[2];
    private BufferedImage imgBomb;
    private BufferedImage[] imgSub; // will init with available sub images
    private BufferedImage[] imgExplosion;

    public GameWindow(GameEngine engine, int width, int height){
        super("Naval War");
        this.engine = engine;
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(width, height);
        this.setLocationRelativeTo(null);

        panel = new DrawPanel();
        panel.setPreferredSize(new Dimension(width, height));
        this.setContentPane(panel);

        // 加载图片资源（优先从 project-root/resources/*.png 加载）
        try {
            imgBackground = ImageIO.read(new File("resources/background.png"));
        } catch (IOException ex) {
            imgBackground = null;
        }
        // ship frames
        try { imgShip[0] = ImageIO.read(new File("resources/ship0.png")); } catch (IOException e){ imgShip[0] = null; }
        try { imgShip[1] = ImageIO.read(new File("resources/ship1.png")); } catch (IOException e){ imgShip[1] = null; }

        // bomb
        try { imgBomb = ImageIO.read(new File("resources/boom.png")); } catch (IOException e){ imgBomb = null; }

        // submarines: q1,q2,r1,h2
        imgSub = new BufferedImage[4];
        try { imgSub[0] = ImageIO.read(new File("resources/q1.png")); } catch (IOException e){ imgSub[0] = null; }
        try { imgSub[1] = ImageIO.read(new File("resources/q2.png")); } catch (IOException e){ imgSub[1] = null; }
        try { imgSub[2] = ImageIO.read(new File("resources/r1.png")); } catch (IOException e){ imgSub[2] = null; }
        try { imgSub[3] = ImageIO.read(new File("resources/h2.png")); } catch (IOException e){ imgSub[3] = null; }

        // explosions: b,b1,b2
        imgExplosion = new BufferedImage[3];
        try { imgExplosion[0] = ImageIO.read(new File("resources/b.png")); } catch (IOException e){ imgExplosion[0] = null; }
        try { imgExplosion[1] = ImageIO.read(new File("resources/b1.png")); } catch (IOException e){ imgExplosion[1] = null; }
        try { imgExplosion[2] = ImageIO.read(new File("resources/b2.png")); } catch (IOException e){ imgExplosion[2] = null; }

        // 键盘处理
        this.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_A) leftPressed = true;
                if (kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_D) rightPressed = true;
                if (kc == KeyEvent.VK_SPACE) dropRequested = true;
            }
            @Override
            public void keyReleased(KeyEvent e){
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_A) leftPressed = false;
                if (kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_D) rightPressed = false;
                if (kc == KeyEvent.VK_SPACE) {
                    // 空格在释放时也可触发一次（防止粘键）
                    // dropRequested 仍由主循环消费一次
                }
            }
        });

        // 定时器：大约 60 FPS
        int delay = 16;
        timer = new Timer(delay, e -> {
            // 1) 处理持续按键
            if (leftPressed) engine.playerMoveLeft();
            if (rightPressed) engine.playerMoveRight();
            // 2) 处理单次投弹请求
            if (dropRequested){
                engine.playerDropBomb();
                dropRequested = false;
            }
            // 3) 推进引擎（以 ms 为单位）
            engine.tick(delay);
            // 4) 重绘
            panel.repaint();
        });

        // 窗口关闭时停止定时器与引擎
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // 确保焦点用于接收按键
                GameWindow.this.requestFocusInWindow();
                timer.start();
            }
            @Override
            public void windowClosing(WindowEvent e){
                timer.stop();
                engine.stop();
                // 退出应用
                System.exit(0);
            }
        });

        pack();
    }

    // 简单绘制面板
    private class DrawPanel extends JPanel {
        public DrawPanel(){
            setDoubleBuffered(true);
            setFocusable(false); // 窗口本身接收键盘
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            // 抗锯齿
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            // 背景：优先图片，否则绘制天空色
            if (imgBackground != null) {
                g2.drawImage(imgBackground, 0, 0, w, h, null);
            } else {
                g2.setColor(new Color(135, 206, 235));
                g2.fillRect(0, 0, w, h);
            }

            // 海面（保持原有色块覆盖，以确保潜艇只能在海里）
            int seaTop = engine.getSeaTop();
            g2.setColor(new Color(28, 107, 160, 220));
            g2.fillRect(0, seaTop, w, h - seaTop);

            // 绘制船（图片或矢量）
            Ship ship = engine.getShip();
            if (ship != null){
                Entity.Rect sbox = ship.getBox();
                // 选择船帧：基于船的朝向（facing），-1 -> imgShip[0], +1 -> imgShip[1]
                int idx = ship.getFacing() < 0 ? 0 : 1;
                BufferedImage frame = imgShip[idx];
                if (frame != null){
                    g2.drawImage(frame, sbox.x, sbox.y, sbox.w, sbox.h, null);
                } else {
                    g2.setColor(new Color(80, 80, 80));
                    g2.fillRect(sbox.x, sbox.y, sbox.w, sbox.h);
                    g2.setColor(new Color(200, 200, 200));
                    g2.fillRect(sbox.x + sbox.w/4, sbox.y + 4, sbox.w/2, sbox.h/3);
                }
            }

            // 绘制潜艇（使用不同图片或回退）
            List<Submarine> subs = engine.getSubs();
            for (Submarine s : subs){
                if (!s.isActive()) continue;
                Entity.Rect box = s.getBox();
                // 根据对象 hash 选择图片索引，确保稳定但多样
                int idx = Math.abs(System.identityHashCode(s)) % imgSub.length;
                BufferedImage subImg = imgSub[idx];
                if (subImg != null){
                    g2.drawImage(subImg, box.x, box.y, box.w, box.h, null);
                } else {
                    g2.setColor(new Color(70, 130, 90));
                    g2.fillRoundRect(box.x, box.y, box.w, box.h, 20, 20);
                    g2.setColor(Color.YELLOW);
                    g2.fillOval(box.x + 8, box.y + box.h/4, 8, 8);
                }
            }

            // 绘制炸弹
            List<Bomb> bombs = engine.getBombs();
            for (Bomb b : bombs){
                if (!b.isActive()) continue;
                Entity.Rect box = b.getBox();
                if (imgBomb != null){
                    g2.drawImage(imgBomb, box.x, box.y, box.w, box.h, null);
                } else {
                    g2.setColor(new Color(30, 30, 30));
                    g2.fillOval(box.x, box.y, box.w, box.h);
                }
            }

            // 绘制爆炸（如果有多帧图片，按时间选择）
            List<Explosion> exps = engine.getExplosions();
            for (Explosion ex : exps){
                Entity.Rect box = ex.getBox();
                int cx = box.x + box.w/2;
                int cy = box.y + box.h/2;
                int r = box.w/2;
                // 选帧：使用当前时间与半径做简单映射
                int frameIndex = (int)((System.currentTimeMillis()/120) % Math.max(1, imgExplosion.length));
                BufferedImage eimg = imgExplosion.length > 0 ? imgExplosion[ frameIndex % imgExplosion.length ] : null;
                if (eimg != null){
                    g2.drawImage(eimg, cx - r, cy - r, r*2, r*2, null);
                } else {
                    g2.setColor(new Color(255, 140, 0, 120));
                    g2.fillOval(cx - r, cy - r, r*2, r*2);
                    g2.setColor(new Color(255, 215, 0, 160));
                    g2.drawOval(cx - r, cy - r, r*2, r*2);
                }
            }

            // UI：分数与状态
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String status = engine.statusString();
            g2.drawString(status, 10, 20);
        }
    }
}
