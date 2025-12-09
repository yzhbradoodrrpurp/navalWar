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
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.AffineTransform; // 新增：用于保存/恢复绘制变换

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
    private BufferedImage imgMissile; // 新增导弹图片

    // 本地维护的导弹列表与冷却/武器状态
    private final List<Missile> missiles = new ArrayList<>();
    private int selectedWeapon = 1; // 1=bomb, 2=missile
    private long lastMissileTime = 0;
    private final long missileCooldown = 5000; // ms

    // 辅助：矩形碰撞检查（替代缺失的 Entity.Rect.intersect）
    private static boolean rectsIntersect(Entity.Rect a, Entity.Rect b){
        if (a == null || b == null) return false;
        int ax1 = a.x, ay1 = a.y, ax2 = a.x + a.w, ay2 = a.y + a.h;
        int bx1 = b.x, by1 = b.y, bx2 = b.x + b.w, by2 = b.y + b.h;
        // 仅当两个矩形有交叉区域时返回 true
        return !(ax2 <= bx1 || bx2 <= ax1 || ay2 <= by1 || by2 <= ay1);
    }

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

        // missile
        try { imgMissile = ImageIO.read(new File("resources/missile.png")); } catch (IOException e){ imgMissile = null; }

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

                // 武器选择：1 或 2
                if (kc == KeyEvent.VK_1) selectedWeapon = 1;
                if (kc == KeyEvent.VK_2) selectedWeapon = 2;
            }
            @Override
            public void keyReleased(KeyEvent e){
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_A) leftPressed = false;
                if (kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_D) rightPressed = false;
                if (kc == KeyEvent.VK_SPACE) {
                    // dropRequested 由主循环消费一次
                }
            }
        });

        // 定时器：大约 60 FPS
        int delay = 16;
        timer = new Timer(delay, e -> {
            // 1) 处理持续按键
            if (leftPressed) engine.playerMoveLeft();
            if (rightPressed) engine.playerMoveRight();

            // 2) 处理单次投弹/发射请求
            if (dropRequested){
                if (selectedWeapon == 1){
                    // 普通炸弹（仍由 engine 管理）
                    engine.playerDropBomb();
                } else {
                    // 导弹：检查冷却并在 UI 侧创建导弹实体
                    long now = System.currentTimeMillis();
                    if (now - lastMissileTime >= missileCooldown){
                        Ship ship = engine.getShip();
                        if (ship != null){
                            Entity.Rect sbox = ship.getBox();
                            // 导弹从船的上方中心发射
                            double sx = sbox.x + sbox.w/2.0;
                            double sy = sbox.y + sbox.h/2.0;
                            Missile m = new Missile(sx, sy, engine);
                            synchronized (missiles){
                                missiles.add(m);
                            }
                            lastMissileTime = now;
                        }
                    } else {
                        // 冷却中：可以考虑播放声音或提示（此处仅忽略）
                    }
                }
                dropRequested = false;
            }

            // 2.5) 更新 UI 侧导弹（追踪并检测命中）
            synchronized (missiles){
                Iterator<Missile> it = missiles.iterator();
                while (it.hasNext()){
                    Missile m = it.next();
                    m.update(delay);
                    if (!m.isActive()){
                        it.remove();
                        continue;
                    }
                    // 检查是否与任一潜艇相撞（命中触发 Explosion）
                    List<Submarine> subs = engine.getSubs();
                    boolean hit = false;
                    for (Submarine s : subs){
                        if (!s.isActive()) continue;
                        if (rectsIntersect(m.getBox(), s.getBox())){
                            // 在命中处创建爆炸，交由 engine 处理得分与渲染
                            double cx = m.getBox().x + m.getBox().w/2.0;
                            double cy = m.getBox().y + m.getBox().h/2.0;
                            int radius = 50;
                            new Explosion(cx, cy, radius, engine.getSubs(), engine);
                            m.setActive(false);
                            hit = true;
                            break;
                        }
                    }
                    if (hit) it.remove();
                }
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

            // 绘制炸弹（图片或回退） —— 在潜艇与爆炸前绘制，这样能看到炸弹
            List<Bomb> bombs = engine.getBombs();
            for (Bomb b : bombs){
                if (!b.isActive()) continue;
                Entity.Rect box = b.getBox();
                if (imgBomb != null){
                    g2.drawImage(imgBomb, box.x, box.y, box.w, box.h, null);
                } else {
                    g2.setColor(new Color(180, 30, 30));
                    g2.fillOval(box.x, box.y, box.w, box.h);
                }
            }

            // 绘制 UI 侧导弹（在炸弹之后，这样显眼）
            synchronized (missiles){
                for (Missile m : missiles){
                    if (!m.isActive()) continue;
                    Entity.Rect mb = m.getBox();
                    if (imgMissile != null){
                        // 旋转绘制：根据导弹当前朝向绘制图片
                        AffineTransform old = g2.getTransform();
                        double cx = mb.x + mb.w/2.0;
                        double cy = mb.y + mb.h/2.0;
                        double angle = m.getAngle(); // 弧度
                        g2.rotate(angle, cx, cy);
                        g2.drawImage(imgMissile, mb.x, mb.y, mb.w, mb.h, null);
                        // 恢复原始变换，避免影响后续绘制
                        g2.setTransform(old);
                    } else {
                        g2.setColor(new Color(220, 200, 40));
                        g2.fillRect(mb.x, mb.y, mb.w, mb.h);
                    }
                }
            }

            // 绘制潜艇（根据类型选择图片或回退）
            List<Submarine> subs = engine.getSubs();
            for (Submarine s : subs){
                if (!s.isActive()) continue;
                Entity.Rect box = s.getBox();
                BufferedImage subImg = null;
                if (imgSub != null && imgSub.length >= 4){
                    // 变体选择：用 identityHashCode 保持稳定但多样
                    int variant = Math.abs(System.identityHashCode(s)) % 2; // 0 or 1
                    if (s.getType() == Submarine.Type.RED){
                        // RED 使用 q2 (idx1) 或 r1 (idx2)
                        subImg = imgSub[1 + variant]; // 1 or 2
                    } else {
                        // BLACK 使用 h2 (idx3) 或 q1 (idx0)
                        subImg = (variant == 0) ? imgSub[3] : imgSub[0];
                    }
                }
                if (subImg != null){
                    g2.drawImage(subImg, box.x, box.y, box.w, box.h, null);
                } else {
                    // 回退绘制：红色或黑色椭圆以示区分
                    if (s.getType() == Submarine.Type.RED) g2.setColor(new Color(200, 40, 40));
                    else g2.setColor(new Color(30, 30, 30));
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
                BufferedImage eimg = (imgExplosion != null && imgExplosion.length > 0)
                        ? imgExplosion[(int)((System.currentTimeMillis()/120) % imgExplosion.length)]
                        : null;
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

            // 显示当前武器与导弹冷却（右上角）
            String weaponName = (selectedWeapon == 1) ? "BOMB" : "MISSILE";
            long now = System.currentTimeMillis();
            long cdRem = Math.max(0, missileCooldown - (now - lastMissileTime));
            String weaponStatus = String.format("Weapon: %s  CD: %dms", weaponName, cdRem);
            g2.drawString(weaponStatus, 10, 40);
        }
    }
}
