package com.heroes3.livewallpaper;

import MapReader.Map;
import MapReader.MapObject;
import MapReader.Tile;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

class MapRender {
    AssetManager assets = new AssetManager();
    final static int TILE_SIZE = 32;
    private TextureAtlas terrains;
    private TextureAtlas mapObjectsAtlas;
    Map map;
    private List<MapSprite> sprites = new ArrayList<MapSprite>();

    MapRender(Map map) {
        loadAsset("terrains");
        loadAsset("mapObjects");
        this.map = map;
        terrains = assets.get(getAssetName("terrains"));
        mapObjectsAtlas = assets.get(getAssetName("mapObjects"));

        Texture.setAssetManager(assets);
        createSprites();
    }

    private void createSprites() {
        for (MapObject object : map.objects) {
            if (object.z == 0) {
                Array<AtlasRegion> atlasRegions = mapObjectsAtlas.findRegions(object.def.toString());
                if (atlasRegions.size == 0) {
                    System.err.printf("Sprite not found: %s\r\n", object.def.toString());
                    continue;
                }
                sprites.add(new MapSprite(atlasRegions, object.x, object.y, object.z));
            }
        }
    }

    private void loadAsset(String defName) {
        assets.load(
                getAssetName(defName),
                TextureAtlas.class,
                new TextureAtlasLoader.TextureAtlasParameter(true)
        );

        assets.finishLoadingAsset(getAssetName(defName));
    }

    private String getAssetName(String defName) {
        return String.format("sprites/%s.atlas", defName);
    }

    private void renderTerrainTile(SpriteBatch batch, Tile tile, int x, int y) {
        TextureRegion terrain = new TextureRegion(
                terrains.findRegion(
                        tile.toFilename(
                                Tile.TilePart.Terrain
                        ),
                        tile.terrainImageIndex
                )
        );
        terrain.flip(tile.flipConf.get(0), tile.flipConf.get(1));
        batch.draw(terrain, x, y);

        if (tile.river != Tile.RiverType.No) {
            TextureRegion river = new TextureRegion(terrains.findRegion(tile.toFilename(Tile.TilePart.River), tile.riverImageIndex));
            river.flip(tile.flipConf.get(2), tile.flipConf.get(3));
            batch.draw(river, x, y);
        }

        if (tile.road != Tile.RoadType.No) {
            TextureRegion road = new TextureRegion(terrains.findRegion(tile.toFilename(Tile.TilePart.Road), tile.roadImageIndex));
            road.flip(tile.flipConf.get(4), tile.flipConf.get(5));
            batch.draw(road, x, y);
        }
    }

    void renderTerrain(SpriteBatch batch) {
        int size = (map.size * map.size) * (map.hasUnderground ? 2 : 1);
        for (int i = 0; i < size; i++) {
            int x = i % map.size;
            int y = MathUtils.ceil(i / map.size);

            renderTerrainTile(
                    batch,
                    map.tiles.get(i),
                    x * TILE_SIZE,
                    y * TILE_SIZE
            );
        }
    }

    void renderSprites(SpriteBatch batch) {
        for (MapSprite sprite : sprites) {
            int x = sprite.x + 1;
            int y = sprite.y + 1;

            Boolean isUnderground = map.hasUnderground && sprite.z == 1;

            if (!isUnderground) {
                sprite.render(
                        batch,
                        x * TILE_SIZE,
                        y * TILE_SIZE
                );
            }
        }
    }
}
