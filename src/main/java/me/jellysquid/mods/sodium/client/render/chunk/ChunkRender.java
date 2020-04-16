package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChunkRender<T extends ChunkRenderState> {
    private final ColumnRender<T> column;
    private final int chunkX, chunkY, chunkZ;

    private final T renderState;
    private final Box boundingBox;

    private ChunkMeshInfo meshInfo = ChunkMeshInfo.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private int rebuildFrame = -1;
    private int lastVisibleFrame = -1;

    private byte cullingState;
    private byte direction;

    public ChunkRender(ColumnRender<T> column, int chunkX, int chunkY, int chunkZ, T renderState) {
        this.renderState = renderState;
        this.column = column;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int originX = chunkX << 4;
        int originY = chunkY << 4;
        int originZ = chunkZ << 4;

        this.boundingBox = new Box(originX, originY, originZ, originX + 16.0D, originY + 16.0D, originZ + 16.0D);
        this.needsRebuild = true;
    }

    public void cancelRebuildTask() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public Box getBoundingBox() {
        return this.boundingBox;
    }

    public ChunkMeshInfo getMeshInfo() {
        return this.meshInfo;
    }

    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    public boolean needsImportantRebuild() {
        return this.needsRebuild && this.needsImportantRebuild;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.meshInfo.isVisibleThrough(from, to);
    }

    public T getRenderState() {
        return this.renderState;
    }

    public void delete() {
        this.cancelRebuildTask();

        this.renderState.clearData();
        this.setMeshInfo(ChunkMeshInfo.ABSENT);
    }

    private void setMeshInfo(ChunkMeshInfo info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.column.onChunkRenderUpdated(this.meshInfo, info);
        this.meshInfo = info;
    }

    public void scheduleRebuild(boolean important) {
        this.needsImportantRebuild = important;
        this.needsRebuild = true;
    }

    public void upload(ChunkMeshInfo meshInfo) {
        this.renderState.uploadData(meshInfo.getLayers());
        this.setMeshInfo(meshInfo);
    }

    public void finishRebuild() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;
    }

    public boolean isEmpty() {
        return this.meshInfo.isEmpty();
    }

    public void updateCullingState(byte parent, Direction from) {
        this.cullingState = (byte) (parent | (1 << from.ordinal()));
    }

    public boolean canCull(Direction from) {
        return (this.cullingState & 1 << from.ordinal()) > 0;
    }

    public void resetGraphState() {
        this.direction = -1;
        this.cullingState = 0;
    }

    public void setRebuildFrame(int frame) {
        this.rebuildFrame = frame;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public void setDirection(Direction dir) {
        this.direction = (byte) dir.ordinal();
    }

    public int getRebuildFrame() {
        return this.rebuildFrame;
    }

    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    public ColumnRender<T> getColumn() {
        return this.column;
    }

    public boolean isVisible(Frustum frustum, int frame) {
        return this.column.isVisible(frustum, frame) && frustum.isVisible(this.boundingBox);
    }

    public void tickTextures() {
        List<Sprite> sprites = this.getMeshInfo().getAnimatedSprites();

        if (!sprites.isEmpty()) {
            int size = sprites.size();

            // We would like to avoid allocating an iterator here
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                SpriteUtil.ensureSpriteReady(sprites.get(i));
            }
        }
    }

    public int getOriginX() {
        return this.chunkX << 4;
    }

    public int getOriginY() {
        return this.chunkY << 4;
    }

    public int getOriginZ() {
        return this.chunkZ << 4;
    }

    public boolean isWithinDistance(Vec3d pos, int distance) {
        double x = pos.getX() - this.getOriginX();
        double y = pos.getY() - this.getOriginY();
        double z = pos.getZ() - this.getOriginZ();

        return ((x * x) + (y * y) + (z * z)) <= distance;
    }

    public double getSquaredDistance(BlockPos pos) {
        double x = pos.getX() - this.getCenterX();
        double y = pos.getY() - this.getCenterY();
        double z = pos.getZ() - this.getCenterZ();

        return (x * x) + (y * y) + (z * z);
    }

    private double getCenterX() {
        return this.getOriginX() + 8.0D;
    }

    private double getCenterY() {
        return this.getOriginY() + 8.0D;
    }

    private double getCenterZ() {
        return this.getOriginZ() + 8.0D;
    }

    public byte getCullingState() {
        return this.cullingState;
    }

    public Direction getDirection() {
        if (this.direction < 0) {
            return null;
        }

        return DirectionUtil.ALL_DIRECTIONS[this.direction];
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }
}