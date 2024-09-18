package org.example;

import lombok.Getter;

@Getter
public class Projectile extends Coordinates {

    private final Direction direction;

    public Projectile(int x, int y, Direction direction) {
        super(x, y);
        this.direction = direction;
    }

}
