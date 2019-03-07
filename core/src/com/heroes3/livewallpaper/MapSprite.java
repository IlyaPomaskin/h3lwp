package com.heroes3.livewallpaper;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

class MapSprite {

    private int currentFrameIndex;
    private Boolean isAnimated;
    private Array<TextureAtlas.AtlasRegion> spritesList;
    int x;
    int y;
    int z;

    MapSprite(Array<TextureAtlas.AtlasRegion> spritesList, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.spritesList = spritesList;

        isAnimated = spritesList.size > 1;
        currentFrameIndex = (new Random()).nextInt(spritesList.size);
    }

    void render(SpriteBatch batch, int x, int y) {
        if (isAnimated) {
            if (spritesList.size - 1 > currentFrameIndex) {
                currentFrameIndex++;
            } else {
                currentFrameIndex = 0;
            }
        }

        TextureRegion frame = spritesList.get(currentFrameIndex);

        batch.draw(frame, x - frame.getRegionWidth(), y - frame.getRegionHeight());
    }
}
