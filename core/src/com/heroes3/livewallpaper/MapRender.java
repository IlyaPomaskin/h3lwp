package com.heroes3.livewallpaper;

import MapReader.Map;
import MapReader.MapObject;
import MapReader.Tile;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

public class MapRender {
    public AssetManager assets = new AssetManager();
    private final static int TILE_SIZE = 32;
    private SpriteCache terrainCache;
    private TextureAtlas terrains;
    private TextureAtlas mapObjectsAtlas;
    private Rectangle rect;
    private Map map;
    private List<MapSprite> visibleObjects = new ArrayList<MapSprite>();
    private int cacheId = 0;
    private OrthographicCamera camera;

    MapRender(OrthographicCamera camera) {
        this.camera = camera;

        loadAsset("terrains", true);
        loadAsset("mapObjects", true);
        terrains = assets.get(getAssetName("terrains"));
        mapObjectsAtlas = assets.get(getAssetName("mapObjects"));

        Texture.setAssetManager(assets);
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setRandomRect(int width, int height) {
        int offset = 2;
        int screenWidthTiles = MathUtils.ceil(width / MapRender.TILE_SIZE) + offset;
        int screenHeightTiles = MathUtils.ceil(height / MapRender.TILE_SIZE) + offset;
        int rectX = MathUtils.random(0, map.size - screenWidthTiles);
        int rectY = MathUtils.random(0, map.size - screenHeightTiles);
        Rectangle rect = new Rectangle(
                Math.max(rectX, 0), Math.max(rectY, 0),
                screenWidthTiles, screenHeightTiles
        );
        
        setRect(rect);
    }

    private void setRect(Rectangle nextRect) {
        this.rect = nextRect;
        refreshTerrainCache();
        removeQueuedAssets();
        refreshVisibleObjects();
    }

    private Boolean isFitInRect(int x, int y) {
        Boolean fitByHor = x >= rect.x && x <= rect.x + rect.width;
        Boolean fitByVert = y >= rect.y && y <= rect.y + rect.height;

        return fitByHor && fitByVert;
    }

    private void loadAsset(String defName, Boolean rightNow) {
        assets.load(
                getAssetName(defName),
                TextureAtlas.class,
                new TextureAtlasLoader.TextureAtlasParameter(true)
        );
        if (rightNow) {
            assets.finishLoadingAsset(getAssetName(defName));
        }
    }

    private void removeQueuedAssets() {
        Array<String> assetsNames = assets.getAssetNames();

        for (String assetName : assetsNames) {
            if (!assets.isLoaded(assetName)) {
                assets.unload(assetName);
            }
        }
    }

    private String getAssetName(String defName) {
        return String.format("sprites/%s.atlas", defName);
    }

    private void refreshVisibleObjects() {
        visibleObjects.clear();

        for (MapObject object : map.objects) {
            if (isFitInRect(object.x, object.y) && object.z == 0) {
                Array<AtlasRegion> atlasRegions = mapObjectsAtlas.findRegions(object.def.toString());
                if (atlasRegions.size == 0) {
                    System.err.printf("Sprite not found: %s\r\n", object.def.toString());
                    continue;
                }
                visibleObjects.add(new MapSprite(atlasRegions, object.x, object.y, object.z));
            }
        }
    }

    private void refreshTerrainCache() {
        terrainCache = new SpriteCache((int) (rect.height * rect.width * 2), true);

        terrainCache.setTransformMatrix(camera.view);
        terrainCache.setProjectionMatrix(camera.projection);

        terrainCache.beginCache();

        int size = (map.size * map.size) * (map.hasUnderground ? 2 : 1);
        for (int i = 0; i < size; i++) {
            int x = i % map.size;
            int y = MathUtils.ceil(i / map.size);

            if (isFitInRect(x, y)) {
                drawTerrainTileToCache(
                        terrainCache,
                        map.tiles.get(i),
                        (x - ((int) rect.x)) * TILE_SIZE,
                        (y - ((int) rect.y)) * TILE_SIZE
                );
            }
        }

        cacheId = terrainCache.endCache();
    }

    private void drawTerrainTileToCache(SpriteCache sc, Tile tile, int x, int y) {
        TextureRegion terrain = new TextureRegion(
                terrains.findRegion(
                        tile.toFilename(
                                Tile.TilePart.Terrain
                        ),
                        tile.terrainImageIndex
                )
        );
        terrain.flip(tile.flipConf.get(0), tile.flipConf.get(1));
        sc.add(terrain, x, y);

        if (tile.river != Tile.RiverType.No) {
            TextureRegion river = new TextureRegion(terrains.findRegion(tile.toFilename(Tile.TilePart.River), tile.riverImageIndex));
            river.flip(tile.flipConf.get(2), tile.flipConf.get(3));
            sc.add(river, x, y);
        }

        if (tile.road != Tile.RoadType.No) {
            TextureRegion road = new TextureRegion(terrains.findRegion(tile.toFilename(Tile.TilePart.Road), tile.roadImageIndex));
            road.flip(tile.flipConf.get(4), tile.flipConf.get(5));
            sc.add(road, x, y);
        }
    }

    public void renderSprites(SpriteBatch batch) {
        for (MapSprite sprite : visibleObjects) {
            int x = sprite.x + 1;
            int y = sprite.y + 1;

            Boolean isUnderground = map.hasUnderground && sprite.z == 1;

            if (!isUnderground) {
                sprite.render(
                        batch,
                        (x - ((int) rect.x)) * TILE_SIZE,
                        (y - ((int) rect.y)) * TILE_SIZE
                );
            }
        }
    }

    public void drawCache() {
        terrainCache.begin();
        terrainCache.draw(cacheId);
        terrainCache.end();
    }
}
