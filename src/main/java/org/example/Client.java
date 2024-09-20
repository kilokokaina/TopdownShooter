package org.example;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Location;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    private static CanvasRenderingContext2D CTX_PLAYER;
    private static CanvasRenderingContext2D CTX_PROJECTILE;
    private static CanvasRenderingContext2D CTX_ENEMIES;

    private static int PLAYER_POINTS = 0;
    private static int PLAYER_HEALTH = 1000;
    private static final AtomicInteger KILL_COUNTER = new AtomicInteger(0);

    private static final AtomicInteger CURRENT_PLAYER_X = new AtomicInteger();
    private static final AtomicInteger CURRENT_PLAYER_Y = new AtomicInteger();

    private static final List<Enemy> ENEMY_LIST = Collections.synchronizedList(new ArrayList<>());

    private static Direction DIRECTION;

    private static final int PROJECTILE_SPEED = 10;

    private static Thread ENEMY_THREAD;

    @JSBody(script = "return window.devicePixelRatio")
    private static native int getDPR();

    private static void renderEnemies(int count, EnemyType enemyType) {
        int enemySize = enemyType.getSize();
        for (int i = 0; i < count; i++) {
            Enemy enemy = new Enemy(
                    (int) ((Math.random() * (1270 - enemySize)) + enemySize),
                    (int) ((Math.random() * (700 - enemySize)) + enemySize),
                    enemyType
            );
            ENEMY_LIST.add(i, enemy);

            CTX_ENEMIES.fillRect(enemy.getX(), enemy.getY(), enemySize, enemySize);

            System.out.println(count + " " + enemyType.name());
        }

    }

    private static void moveEnemies() {
        ENEMY_THREAD = new Thread(() -> {
            while (true) {
                try {
                    int playerPositionX = CURRENT_PLAYER_X.get();
                    int playerPositionY = CURRENT_PLAYER_Y.get();

                    for (Enemy enemy : ENEMY_LIST) {
                        int x = enemy.getX(), y = enemy.getY(), newX, newY, stepDistance = 0;

                        switch (enemy.getType()) {
                            case RUNNER -> stepDistance = 9;
                            case SHOOTER -> stepDistance = 5;
                            case PUDGE -> stepDistance = 3;
                        }

                        if (x > playerPositionX) newX = x - (int) (Math.random() * stepDistance);
                        else newX = x + (int) (Math.random() * stepDistance);
                        if (y > playerPositionY) newY = y - (int) (Math.random() * stepDistance);
                        else newY = y + (int) (Math.random() * stepDistance);

                        if (enemy.getType() == EnemyType.SHOOTER) {
                            if (newX < CURRENT_PLAYER_X.get() + 6 && newX + 6 > CURRENT_PLAYER_X.get() && newY < CURRENT_PLAYER_Y.get()) {
                               sendProjectile(newX, newY, Direction.DOWN, false);
                            } else if (newX < CURRENT_PLAYER_X.get() + 6 && newX + 6 > CURRENT_PLAYER_X.get() && newY > CURRENT_PLAYER_Y.get()) {
                                sendProjectile(newX, newY, Direction.UP, false);
                            } else if (newX > CURRENT_PLAYER_X.get() && newY < CURRENT_PLAYER_Y.get() + 6 && newY + 6 > CURRENT_PLAYER_Y.get()) {
                                sendProjectile(newX, newY, Direction.LEFT, false);
                            } else if (newX < CURRENT_PLAYER_X.get() && newY < CURRENT_PLAYER_Y.get() + 6 && newY + 6 > CURRENT_PLAYER_Y.get()) {
                                sendProjectile(newX, newY, Direction.RIGHT, false);
                            }
                        }

                        if (newX < CURRENT_PLAYER_X.get() + 12 && newX + 12 > CURRENT_PLAYER_X.get()
                                && newY < CURRENT_PLAYER_Y.get() + 12 && newY + 12 > CURRENT_PLAYER_Y.get()) {
                            if (Objects.requireNonNull(enemy.getType()) == EnemyType.PUDGE)
                                HTMLDocument.current().querySelector("#health").setInnerText(String.valueOf(PLAYER_HEALTH -= 10));
                            else HTMLDocument.current().querySelector("#health").setInnerText(String.valueOf(--PLAYER_HEALTH));

                            if (PLAYER_HEALTH <= 0) Location.current().reload();
                        }

                        if (newX > 0 && newX < 1260 && newY > 0 && newY < 700) {
                            CTX_ENEMIES.clearRect(x, y, enemy.getType().getSize(), enemy.getType().getSize());
                            CTX_ENEMIES.fillRect(newX, newY, enemy.getType().getSize(), enemy.getType().getSize());

                            enemy.setX(newX);
                            enemy.setY(newY);
                        }
                    }

                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ENEMY_THREAD.start();
    }

    private static boolean checkHit(int projectileX, int projectileY, boolean player) {
        if (player) {
            for (Enemy enemy : ENEMY_LIST) {
                if (projectileX > enemy.getX() && projectileX < enemy.getX() + enemy.getType().getSize()) {
                    if (projectileY > enemy.getY() && projectileY < enemy.getY() + enemy.getType().getSize()) {
                        enemy.setEnemyHealth(enemy.getEnemyHealth() - 50);
                        System.out.println(enemy.getEnemyHealth());
                        if (enemy.getEnemyHealth() == 0) {
                            CTX_ENEMIES.clearRect(enemy.getX(), enemy.getY(), enemy.getType().getSize(), enemy.getType().getSize());
                            System.out.println(KILL_COUNTER.incrementAndGet());

                            enemy.setX(-30);
                            enemy.setY(-30);
                        }

                        switch (enemy.getType()) {
                            case SHOOTER -> HTMLDocument.current().querySelector("#points").setInnerText(String.valueOf(PLAYER_POINTS += 10));
                            case RUNNER -> HTMLDocument.current().querySelector("#points").setInnerText(String.valueOf(PLAYER_POINTS += 5));
                            case PUDGE -> HTMLDocument.current().querySelector("#points").setInnerText(String.valueOf(PLAYER_POINTS += 20));
                        }

                        if (KILL_COUNTER.get() % 40 == 0) renderEnemies(40, EnemyType.RUNNER);
                        if (KILL_COUNTER.get() % 120 == 0) renderEnemies(10, EnemyType.SHOOTER);
                        if (KILL_COUNTER.get() % 360 == 0) renderEnemies(3, EnemyType.PUDGE);

                        return true;
                    }
                }
            }
        } else {
            if (projectileX > CURRENT_PLAYER_X.get() && projectileX < CURRENT_PLAYER_X.get() + 12) {
                if (projectileY > CURRENT_PLAYER_Y.get() && projectileY < CURRENT_PLAYER_Y.get() + 12) {
                    HTMLDocument.current().querySelector("#health").setInnerText(String.valueOf(PLAYER_HEALTH -= 5));
                    if (PLAYER_HEALTH <= 0) Location.current().reload();

                    return true;
                }
            }
        }

        return false;
    }

    private static void sendProjectile(int startX, int startY, Direction direction, boolean player) {
        switch (direction) {
            case UP -> {
                Thread drawProjectile = new Thread(() -> {
                    int i = 6;
                    while (true) {
                        try {
                            if (startY - i <= -6) break;

                            CTX_PROJECTILE.fillRect(startX + 6, startY - i, 3, 6);
                            CTX_PROJECTILE.clearRect(startX + 6, startY - (i - 6), 3, 6);

                            if (checkHit(startX + 6, startY - i, player)) {
                                CTX_PROJECTILE.clearRect(startX + 6, startY - i, 3, 6);
                                break;
                            }

                            i += 6;

                            Thread.sleep(PROJECTILE_SPEED);
                        } catch (InterruptedException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                });

                drawProjectile.start();
            }
            case DOWN -> {
                Thread drawProjectile = new Thread(() -> {
                    int i = 6;
                    while (true) {
                        try {
                            if (startY + i >= 726) break;

                            CTX_PROJECTILE.fillRect(startX + 6, (startY + 12) + i, 3, 6);
                            CTX_PROJECTILE.clearRect(startX + 6, (startY + 12) + (i - 6), 3, 6);

                            if (checkHit(startX + 6, (startY + 12) + i, player)) {
                                CTX_PROJECTILE.clearRect(startX + 6, (startY + 12) + i, 3, 6);
                                break;
                            }

                            i += 6;

                            Thread.sleep(PROJECTILE_SPEED);
                        } catch (InterruptedException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                });

                drawProjectile.start();
            }
            case RIGHT -> {
                Thread drawProjectile = new Thread(() -> {
                    int i = 6;
                    while (true) {
                        try {
                            if (startX + i >= 1286) break;

                            CTX_PROJECTILE.fillRect((startX + 12) + i, startY + 6, 6, 3);
                            CTX_PROJECTILE.clearRect((startX + 12) + (i - 6), startY + 6, 6, 3);

                            if (checkHit((startX + 12) + i, startY + 6, player)) {
                                CTX_PROJECTILE.clearRect((startX + 12) + i, startY + 6, 6, 3);
                                break;
                            }

                            i += 6;

                            Thread.sleep(PROJECTILE_SPEED);
                        } catch (InterruptedException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                });

                drawProjectile.start();
            }
            case LEFT -> {
                Thread drawProjectile = new Thread(() -> {
                    int i = 6;
                    while (true) {
                        try {
                            if (startX - i <= -6) break;

                            CTX_PROJECTILE.fillRect(startX - i, startY + 6, 6, 3);
                            CTX_PROJECTILE.clearRect(startX - (i - 6), startY + 6, 6, 3);

                            if (checkHit(startX - i, startY + 6, player)) {
                                CTX_PROJECTILE.clearRect(startX - i, startY + 6, 6, 3);
                                break;
                            }

                            i += 6;

                            Thread.sleep(PROJECTILE_SPEED);
                        } catch (InterruptedException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                });

                drawProjectile.start();
            }
        }
    }

    public static void main(String[] args) {
        var document = HTMLDocument.current();

        int DPR = getDPR();

        var canvasPlayer = (HTMLCanvasElement) document.querySelector("#player-layer");
        var rect = canvasPlayer.getBoundingClientRect();
        canvasPlayer.setWidth(rect.getWidth() * DPR);
        canvasPlayer.setHeight(rect.getHeight() * DPR);

        var canvasProjectile = (HTMLCanvasElement) document.querySelector("#projectile-layer");
        canvasProjectile.setWidth(rect.getWidth() * DPR);
        canvasProjectile.setHeight(rect.getHeight() * DPR);

        var canvasEnemies = (HTMLCanvasElement) document.querySelector("#enemy-layer");
        canvasEnemies.setWidth(rect.getWidth() * DPR);
        canvasEnemies.setHeight(rect.getHeight() * DPR);

        CTX_PLAYER = (CanvasRenderingContext2D) canvasPlayer.getContext("2d");
        CTX_PLAYER.setFillStyle("rgb(0,0,150)");
        CTX_PLAYER.scale(DPR, DPR);

        CTX_PROJECTILE = (CanvasRenderingContext2D) canvasProjectile.getContext("2d");
        CTX_PROJECTILE.setFillStyle("rgb(150,0,0)");
        CTX_PROJECTILE.scale(DPR, DPR);

        CTX_ENEMIES = (CanvasRenderingContext2D) canvasEnemies.getContext("2d");
        CTX_ENEMIES.setFillStyle("rgb(0,0,0)");
        CTX_ENEMIES.scale(DPR, DPR);

        var buttonPlay = document.querySelector("#play");

        buttonPlay.onClick(event -> {
            CTX_PLAYER.clearRect(0, 0, 1280, 720);
            CTX_PROJECTILE.clearRect(0, 0, 1280, 720);
            CTX_ENEMIES.clearRect(0, 0, 1280, 720);

            ENEMY_LIST.clear();
            PLAYER_POINTS = 0;
            document.querySelector("#points").setInnerText("0");
            ENEMY_THREAD = null;

            CURRENT_PLAYER_X.set(canvasPlayer.getWidth() / 4);
            CURRENT_PLAYER_Y.set(canvasPlayer.getHeight() / 4);

            CTX_PLAYER.fillRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), 12, 12);

            renderEnemies(40, EnemyType.RUNNER);
            moveEnemies();
        });

        document.addEventListener("keydown", event -> {
            String key = ((KeyboardEvent) event).getCode();
            switch (key) {
                case "KeyW" -> {
                    CTX_PLAYER.clearRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), 12, 12);
                    CTX_PLAYER.fillRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.addAndGet(-9), 12, 12);

                    DIRECTION = Direction.UP;
                }
                case "KeyS" -> {
                    CTX_PLAYER.clearRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), 12, 12);
                    CTX_PLAYER.fillRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.addAndGet(9), 12, 12);

                    DIRECTION = Direction.DOWN;
                }
                case "KeyD" -> {
                    CTX_PLAYER.clearRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), 12, 12);
                    CTX_PLAYER.fillRect(CURRENT_PLAYER_X.addAndGet(9), CURRENT_PLAYER_Y.get(), 12, 12);

                    DIRECTION = Direction.RIGHT;
                }
                case "KeyA" -> {
                    CTX_PLAYER.clearRect(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), 12, 12);
                    CTX_PLAYER.fillRect(CURRENT_PLAYER_X.addAndGet(-9), CURRENT_PLAYER_Y.get(), 12, 12);

                    DIRECTION = Direction.LEFT;
                }
                case "KeyK" -> sendProjectile(CURRENT_PLAYER_X.get(), CURRENT_PLAYER_Y.get(), DIRECTION, true);
                case "Space" -> buttonPlay.click();
            }
        });
    }

}
