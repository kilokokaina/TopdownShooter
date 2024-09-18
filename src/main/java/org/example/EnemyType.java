package org.example;

import lombok.Getter;

@Getter
public enum EnemyType {

    SHOOTER (100, 12), RUNNER (50, 10), PUDGE (500, 24);

    private final int health;
    private final int size;

    EnemyType(int health, int size) {
        this.health = health;
        this.size = size;
    }

}
