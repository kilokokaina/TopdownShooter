package org.example;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Enemy extends Coordinates {

    private int enemyHealth;
    private EnemyType type;

    public Enemy(int x, int y, EnemyType type) {
        super(x, y);
        this.enemyHealth = type.getHealth();
        this.type = type;
    }

}
