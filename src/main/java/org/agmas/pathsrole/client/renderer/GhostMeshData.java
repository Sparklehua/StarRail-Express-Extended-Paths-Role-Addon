package org.agmas.pathsrole.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GhostMeshData {

    private static final String STRUCTURE_PATH = "assets/pathsrole/structures/shrine_ghost.json";
    private static final float GHOST_R = 1.0f;
    private static final float GHOST_G = 1.0f;
    private static final float GHOST_B = 1.0f;
    private static final float GHOST_A = 0.6f;

    private static List<GhostBlock> ghostBlocks = null;
    private static int sizeX, sizeY, sizeZ;

    private static volatile boolean structureLoaded = false;
    private static volatile boolean bakingDone = false;
    private static volatile boolean bakingFailed = false;
    private static final Object bakeLock = new Object();

    /** Pre-baked translucent VertexBuffer */
    static VertexBuffer translucentBuffer = null;

    private GhostMeshData() {}

    static void resetBakingState() {
        synchronized (bakeLock) {
            structureLoaded = false;
            bakingDone = false;
            bakingFailed = false;
            ghostBlocks = null;
            if (translucentBuffer != null) {
                translucentBuffer.close();
                translucentBuffer = null;
            }
        }
    }

    /** Called during renderer construction to load structure JSON, but defers GPU baking. */
    static void loadStructure() {
        if (structureLoaded && ghostBlocks != null) {
            return;
        }

        try {
            InputStream raw = GhostMeshData.class.getClassLoader()
                    .getResourceAsStream(STRUCTURE_PATH);
            if (raw == null) {
                return;
            }

            ghostBlocks = new ArrayList<>();

            JsonObject root = JsonParser.parseReader(new InputStreamReader(raw)).getAsJsonObject();
            JsonObject sizeObj = root.getAsJsonObject("size");
            sizeX = sizeObj.get("x").getAsInt();
            sizeY = sizeObj.get("y").getAsInt();
            sizeZ = sizeObj.get("z").getAsInt();

            JsonArray blocks = root.getAsJsonArray("blocks");
            boolean[][][] occupied = new boolean[sizeX][sizeY][sizeZ];
            List<RawBlock> rawBlocks = new ArrayList<>();

            for (JsonElement elem : blocks) {
                JsonObject entry = elem.getAsJsonObject();
                int x = entry.get("x").getAsInt();
                int y = entry.get("y").getAsInt();
                int z = entry.get("z").getAsInt();
                String stateString = entry.get("state").getAsString();
                if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ) {
                    occupied[x][y][z] = true;
                    rawBlocks.add(new RawBlock(x, y, z, stateString));
                }
            }

            for (RawBlock rb : rawBlocks) {
                if (isSurface(occupied, rb.x, rb.y, rb.z)) {
                    BlockState state = parseBlockState(rb.stateString);
                    if (state != null) {
                        ghostBlocks.add(new GhostBlock(
                                rb.x - sizeX / 2.0f,
                                rb.y - sizeY / 2.0f,
                                rb.z - sizeZ / 2.0f,
                                state));
                    }
                }
            }

            structureLoaded = true;
        } catch (Exception e) {
            ghostBlocks = null;
        }
    }

    private static boolean isSurface(boolean[][][] occupied, int x, int y, int z) {
        return (x == 0 || !occupied[x - 1][y][z])
            || (x == sizeX - 1 || !occupied[x + 1][y][z])
            || (y == 0 || !occupied[x][y - 1][z])
            || (y == sizeY - 1 || !occupied[x][y + 1][z])
            || (z == 0 || !occupied[x][y][z - 1])
            || (z == sizeZ - 1 || !occupied[x][y][z + 1]);
    }

    private static BlockState parseBlockState(String stateString) {
        try {
            String name;
            Map<String, String> props = new LinkedHashMap<>();

            int bracket = stateString.indexOf('[');
            if (bracket >= 0) {
                name = stateString.substring(0, bracket);
                String propPart = stateString.substring(bracket + 1, stateString.length() - 1);
                for (String pair : propPart.split(",")) {
                    if (pair.isEmpty()) continue;
                    int eq = pair.indexOf('=');
                    if (eq >= 0) {
                        props.put(pair.substring(0, eq), pair.substring(eq + 1));
                    }
                }
            } else {
                name = stateString;
            }

            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
            BlockState state = block.defaultBlockState();

            for (Property<?> prop : state.getProperties()) {
                String val = props.get(prop.getName());
                if (val != null) {
                    state = applyProperty(state, prop, val);
                }
            }
            return state;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(
            BlockState state, Property<T> prop, String value) {
        return state.setValue(prop, prop.getValue(value).orElse(state.getValue(prop)));
    }

    /**
     * GPU 烘焙：将所有方块的模型 quad 收集到一个 BufferBuilder 中，
     * 上传到单个 VertexBuffer。只在渲染线程上执行一次。
     */
    private static void bakeMesh() {
        synchronized (bakeLock) {
            if (bakingDone || bakingFailed) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getBlockRenderer() == null) {
                bakingFailed = true;
                return;
            }

            if (ghostBlocks == null || ghostBlocks.isEmpty()) {
                bakingFailed = true;
                return;
            }

            try {
                BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
                RandomSource random = RandomSource.create(42L);

                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder builder = tesselator.begin(
                        VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

                int quadCount = 0;

                for (GhostBlock gb : ghostBlocks) {
                    BakedModel model = dispatcher.getBlockModel(gb.state);

                    for (Direction direction : Direction.values()) {
                        List<net.minecraft.client.renderer.block.model.BakedQuad> quads =
                                model.getQuads(gb.state, direction, random);
                        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
                            writeGhostQuad(builder, quad, gb.x, gb.y, gb.z);
                            quadCount++;
                        }
                    }

                    List<net.minecraft.client.renderer.block.model.BakedQuad> quads =
                            model.getQuads(gb.state, null, random);
                    for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
                        writeGhostQuad(builder, quad, gb.x, gb.y, gb.z);
                        quadCount++;
                    }
                }

                MeshData meshData = builder.build();
                if (meshData != null) {
                    VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    vb.bind();
                    vb.upload(meshData);
                    VertexBuffer.unbind();

                    VertexBuffer old = translucentBuffer;
                    translucentBuffer = vb;
                    if (old != null) {
                        old.close();
                    }
                }

                bakingDone = true;
            } catch (Exception e) {
                bakingFailed = true;
            }
        }
    }

    /** 将单个 quad 写入 buffer builder，应用虚影半透明颜色 */
    private static void writeGhostQuad(BufferBuilder builder,
                                       net.minecraft.client.renderer.block.model.BakedQuad quad,
                                       float bx, float by, float bz) {
        int[] vertices = quad.getVertices();
        int vertexSize = DefaultVertexFormat.BLOCK.getVertexSize();

        for (int v = 0; v < 4; v++) {
            int offset = v * vertexSize / 4;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            int color = vertices[offset + 3];
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float vv = Float.intBitsToFloat(vertices[offset + 5]);
            int packedOverlay = OverlayTexture.NO_OVERLAY;
            int packedLight = 0x00F000F0;
            float nx = (vertices[offset + 7] & 0xFF) / 127.0f - 1.0f;
            float ny = ((vertices[offset + 7] >> 8) & 0xFF) / 127.0f - 1.0f;
            float nz = ((vertices[offset + 7] >> 16) & 0xFF) / 127.0f - 1.0f;

            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            float tr = r * GHOST_R;
            float tg = g * GHOST_G;
            float tb = b * GHOST_B;
            float ta = a * GHOST_A;

            int tintedColor = ((int)(ta * 255) << 24)
                    | ((int)(tr * 255) << 16)
                    | ((int)(tg * 255) << 8)
                    | (int)(tb * 255);

            builder.addVertex(bx + x, by + y, bz + z)
                    .setColor(tintedColor)
                    .setUv(u, vv)
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(nx, ny, nz);
        }
    }

    static void render(Matrix4f modelView, Matrix4f projection) {
        if (!structureLoaded) {
            loadStructure();
            if (!structureLoaded) {
                return;
            }
        }

        if (!bakingDone && !bakingFailed) {
            bakeMesh();
        }

        if (translucentBuffer == null) {
            return;
        }
        if (bakingFailed) {
            return;
        }

        RenderType renderType = RenderType.translucent();
        renderType.setupRenderState();
        translucentBuffer.bind();
        translucentBuffer.drawWithShader(modelView, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    static int getStructureSizeY() {
        if (!structureLoaded) {
            loadStructure();
        }
        return sizeY;
    }

    static double getStructureHalfHeight() {
        if (!structureLoaded) {
            loadStructure();
        }
        return sizeY > 0 ? sizeY / 2.0 : 0;
    }

    static void cleanup() {
        synchronized (bakeLock) {
            if (translucentBuffer != null) {
                translucentBuffer.close();
                translucentBuffer = null;
            }
            bakingDone = false;
            bakingFailed = false;
        }
    }

    private record RawBlock(int x, int y, int z, String stateString) {}
    private record GhostBlock(float x, float y, float z, BlockState state) {}
}