package entities;

import entities.Entity.Rect;
import entities.Bomb;
import java.util.concurrent.atomic.AtomicLong;

public class Ship extends Entity {
    private final int worldWidth;
    private final double speed = 4.8; // 每 tick 移动像素（被 caller 的 dt 控制）
    private double cooldown = 0; // ms
    private final double dropInterval = 400; // ms

    // 新增：朝向，-1 为左，+1 为右（默认向右）
    private int facing = 1;

    public Ship(double x, double y, int worldWidth){
        this.x = x; this.y = y; this.w = 100; this.h = 48;
        this.worldWidth = worldWidth;
        this.facing = 1;
    }

    // moveDir: -1 left, +1 right, 0 none
    public void move(int moveDir){
        if (moveDir != 0) {
            // 更新朝向但不要随时间自动切换
            this.facing = moveDir < 0 ? -1 : 1;
        }
        this.x += moveDir * speed;
        if (this.x < 0) this.x = 0;
        if (this.x > worldWidth - w) this.x = worldWidth - w;
    }

    @Override
    public void update(long dt){
        if (cooldown > 0) cooldown = Math.max(0, cooldown - dt);
    }

    public Bomb dropBomb(){
        if (cooldown > 0) return null;
        cooldown = dropInterval;
        double bx = this.x + this.w/2 - 12;
        double by = this.y + this.h;
        return new Bomb(bx, by);
    }

    public double getX(){ return x; }

    // 新增：返回当前朝向（用于渲染选择图片）
    public int getFacing(){ return facing; }
}
