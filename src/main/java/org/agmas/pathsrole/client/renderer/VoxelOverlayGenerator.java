package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;

public class VoxelOverlayGenerator {

    private static final float VOXEL_SCALE = 0.55f;
    private static final float BASE_EXTRUDE = 0.15f;
    private static final float MAX_EXTRUDE_BONUS = 0.55f;

    public static Map<String, List<VoxelData>> generate(NativeImage skin) {
        Map<String, List<VoxelData>> result = new LinkedHashMap<>();
        result.put("Head", generateHeadVoxels(skin));
        result.put("Body", generateBodyVoxels(skin));
        result.put("RightArm", generateRightArmVoxels(skin));
        result.put("LeftArm", generateLeftArmVoxels(skin));
        result.put("RightLeg", generateRightLegVoxels(skin));
        result.put("LeftLeg", generateLeftLegVoxels(skin));
        return result;
    }

    // ==================== Head ====================
    // Cube: origin (-3.5, 6, -2), size [8, 8, 8]
    // UV overlay: hat layer at x=32..63, y=0..15

    private static List<VoxelData> generateHeadVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = -3.5f, oy = 6f, oz = -2f;
        float sx = 8f, sy = 8f, sz = 8f;

        // North (front): UV (40,8)-(47,15), normal (0,0,-1)
        // U→+X, V→-Y, origin at top-left of face
        generateFaceVoxels(skin, voxels, 40, 8, 8, 8,
                ox,       oy + sy, oz,
                1,  0,  0,
                0, -1,  0,
                0,  0, -1);

        // South (back): UV (56,8)-(63,15), normal (0,0,1)
        generateFaceVoxels(skin, voxels, 56, 8, 8, 8,
                ox + sx,  oy + sy, oz + sz,
                -1, 0,  0,
                0, -1,  0,
                0,  0,  1);

        // Up (top): UV (40,0)-(47,7), normal (0,1,0)
        generateFaceVoxels(skin, voxels, 40, 0, 8, 8,
                ox,       oy + sy, oz,
                1,  0,  0,
                0,  0,  1,
                0,  1,  0);

        // Down (bottom): UV (48,0)-(55,7), normal (0,-1,0)
        generateFaceVoxels(skin, voxels, 48, 0, 8, 8,
                ox,       oy,     oz + sz,
                1,  0,  0,
                0,  0, -1,
                0, -1,  0);

        // East (right): UV (32,8)-(39,15), normal (1,0,0)
        generateFaceVoxels(skin, voxels, 32, 8, 8, 8,
                ox + sx,  oy + sy, oz,
                0,  0, -1,
                0, -1,  0,
                1,  0,  0);

        // West (left): UV (48,8)-(55,15), normal (-1,0,0)
        generateFaceVoxels(skin, voxels, 48, 8, 8, 8,
                ox,       oy + sy, oz + sz,
                0,  0,  1,
                0, -1,  0,
                -1, 0,  0);

        return voxels;
    }

    // ==================== Body ====================
    // Cube: origin (-2.5, 0, 0.15742), size [6, 7, 3.5]
    // UV overlay: jacket at x=20..35, y=36..47 (front), etc.

    private static List<VoxelData> generateBodyVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = -2.5f, oy = 0f, oz = 0.15742f;
        float sx = 6f, sy = 7f, sz = 3.5f;
        float pux = sx / 8f;   // 0.75 per pixel U on X faces
        float puy = sy / 12f;  // ~0.583 per pixel V
        float ptx = sx / 8f;   // 0.75 per pixel U on top
        float ptz = sz / 4f;   // 0.875 per pixel V on top

        // North (front): UV (20,36)-(27,47) 8x12
        generateFaceVoxels(skin, voxels, 20, 36, 8, 12,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,   -puy, 0,
                0,    0,  -1);

        // South (back): UV (32,36)-(39,47) 8x12
        generateFaceVoxels(skin, voxels, 32, 36, 8, 12,
                ox + sx,   oy + sy, oz + sz,
                -pux, 0,   0,
                0,   -puy, 0,
                0,    0,   1);

        // Up (top): UV (20,32)-(27,35) 8x4
        generateFaceVoxels(skin, voxels, 20, 32, 8, 4,
                ox,        oy + sy, oz,
                ptx,  0,   0,
                0,    0,   ptz,
                0,    1,   0);

        // Down (bottom): UV (28,32)-(35,35) 8x4
        generateFaceVoxels(skin, voxels, 28, 32, 8, 4,
                ox,        oy,      oz + sz,
                ptx,  0,   0,
                0,    0,  -ptz,
                0,   -1,   0);

        // East (right): UV (16,36)-(19,47) 4x12
        float perx = sz / 4f; // Z step per U pixel
        generateFaceVoxels(skin, voxels, 16, 36, 4, 12,
                ox + sx,   oy + sy, oz,
                0,    0,  -perx,
                0,   -puy, 0,
                1,    0,   0);

        // West (left): UV (28,36)-(31,47) 4x12
        generateFaceVoxels(skin, voxels, 28, 36, 4, 12,
                ox,        oy + sy, oz + sz,
                0,    0,   perx,
                0,   -puy, 0,
                -1,   0,   0);

        return voxels;
    }

    // ==================== RightArm ====================
    // Cube: origin (-2.12215, -1.3656, 0.70742), size [2.5, 6, 2.5]
    // Overlay UV: x=40..55, y=36..47

    private static List<VoxelData> generateRightArmVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = -2.12215f, oy = -1.3656f, oz = 0.70742f;
        float sx = 2.5f, sy = 6f, sz = 2.5f;
        float pux = sx / 4f;   // 0.625 per pixel U on front/back
        float puy = sy / 12f;  // 0.5 per pixel V
        float ptz = sz / 4f;   // 0.625 per pixel on top/bottom

        // North (front): UV (44,36)-(47,47) 4x12
        generateFaceVoxels(skin, voxels, 44, 36, 4, 12,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,   -puy, 0,
                0,    0,  -1);

        // South (back): UV (52,36)-(55,47) 4x12
        generateFaceVoxels(skin, voxels, 52, 36, 4, 12,
                ox + sx,   oy + sy, oz + sz,
                -pux, 0,   0,
                0,   -puy, 0,
                0,    0,   1);

        // Up (top): UV (44,32)-(47,35) 4x4
        generateFaceVoxels(skin, voxels, 44, 32, 4, 4,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,    0,   ptz,
                0,    1,   0);

        // Down (bottom): UV (48,32)-(51,35) 4x4
        generateFaceVoxels(skin, voxels, 48, 32, 4, 4,
                ox,        oy,      oz + sz,
                pux,  0,   0,
                0,    0,  -ptz,
                0,   -1,   0);

        // East (right): UV (40,36)-(43,47) 4x12
        float pex = sz / 4f;
        generateFaceVoxels(skin, voxels, 40, 36, 4, 12,
                ox + sx,   oy + sy, oz,
                0,    0,  -pex,
                0,   -puy, 0,
                1,    0,   0);

        // West (left): UV (48,36)-(51,47) 4x12
        generateFaceVoxels(skin, voxels, 48, 36, 4, 12,
                ox,        oy + sy, oz + sz,
                0,    0,   pex,
                0,   -puy, 0,
                -1,   0,   0);

        return voxels;
    }

    // ==================== LeftArm ====================
    // Cube: origin (2.86731, 0.32612, 0.70742), size [2.5, 6, 2.5]
    // Overlay UV: x=32..47, y=52..63

    private static List<VoxelData> generateLeftArmVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = 2.86731f, oy = 0.32612f, oz = 0.70742f;
        float sx = 2.5f, sy = 6f, sz = 2.5f;
        float pux = sx / 4f;
        float puy = sy / 12f;
        float ptz = sz / 4f;

        // North (front): UV (36,52)-(39,63) 4x12
        generateFaceVoxels(skin, voxels, 36, 52, 4, 12,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,   -puy, 0,
                0,    0,  -1);

        // South (back): UV (44,52)-(47,63) 4x12
        generateFaceVoxels(skin, voxels, 44, 52, 4, 12,
                ox + sx,   oy + sy, oz + sz,
                -pux, 0,   0,
                0,   -puy, 0,
                0,    0,   1);

        // Up (top): UV (36,48)-(39,51) 4x4
        generateFaceVoxels(skin, voxels, 36, 48, 4, 4,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,    0,   ptz,
                0,    1,   0);

        // Down (bottom): UV (40,48)-(43,51) 4x4
        generateFaceVoxels(skin, voxels, 40, 48, 4, 4,
                ox,        oy,      oz + sz,
                pux,  0,   0,
                0,    0,  -ptz,
                0,   -1,   0);

        // East (right): UV (32,52)-(35,63) 4x12
        float pex = sz / 4f;
        generateFaceVoxels(skin, voxels, 32, 52, 4, 12,
                ox + sx,   oy + sy, oz,
                0,    0,  -pex,
                0,   -puy, 0,
                1,    0,   0);

        // West (left): UV (40,52)-(43,63) 4x12
        generateFaceVoxels(skin, voxels, 40, 52, 4, 12,
                ox,        oy + sy, oz + sz,
                0,    0,   pex,
                0,   -puy, 0,
                -1,   0,   0);

        return voxels;
    }

    // ==================== RightLeg ====================
    // Cube: origin (-2.85, 0, -5.75), size [2.5, 2.5, 5.5]
    // Overlay UV: x=0..15, y=36..47

    private static List<VoxelData> generateRightLegVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = -2.85f, oy = 0f, oz = -5.75f;
        float sx = 2.5f, sy = 2.5f, sz = 5.5f;
        float pux = sx / 4f;
        float puy = sy / 12f;
        float ptz = sz / 4f;

        // North (front): UV (4,36)-(7,47) 4x12
        generateFaceVoxels(skin, voxels, 4, 36, 4, 12,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,   -puy, 0,
                0,    0,  -1);

        // South (back): UV (12,36)-(15,47) 4x12
        generateFaceVoxels(skin, voxels, 12, 36, 4, 12,
                ox + sx,   oy + sy, oz + sz,
                -pux, 0,   0,
                0,   -puy, 0,
                0,    0,   1);

        // Up (top): UV (4,32)-(7,35) 4x4
        generateFaceVoxels(skin, voxels, 4, 32, 4, 4,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,    0,   ptz,
                0,    1,   0);

        // Down (bottom): UV (8,32)-(11,35) 4x4
        generateFaceVoxels(skin, voxels, 8, 32, 4, 4,
                ox,        oy,      oz + sz,
                pux,  0,   0,
                0,    0,  -ptz,
                0,   -1,   0);

        // East (right): UV (0,36)-(3,47) 4x12
        float pex = sz / 4f;
        generateFaceVoxels(skin, voxels, 0, 36, 4, 12,
                ox + sx,   oy + sy, oz,
                0,    0,  -pex,
                0,   -puy, 0,
                1,    0,   0);

        // West (left): UV (8,36)-(11,47) 4x12
        generateFaceVoxels(skin, voxels, 8, 36, 4, 12,
                ox,        oy + sy, oz + sz,
                0,    0,   pex,
                0,   -puy, 0,
                -1,   0,   0);

        return voxels;
    }

    // ==================== LeftLeg ====================
    // Cube: origin (1.1, 0, -5.25), size [2.5, 2.5, 5.5]
    // Overlay UV: x=16..31, y=52..63

    private static List<VoxelData> generateLeftLegVoxels(NativeImage skin) {
        List<VoxelData> voxels = new ArrayList<>();
        float ox = 1.1f, oy = 0f, oz = -5.25f;
        float sx = 2.5f, sy = 2.5f, sz = 5.5f;
        float pux = sx / 4f;
        float puy = sy / 12f;
        float ptz = sz / 4f;

        // North (front): UV (20,52)-(23,63) 4x12
        generateFaceVoxels(skin, voxels, 20, 52, 4, 12,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,   -puy, 0,
                0,    0,  -1);

        // South (back): UV (28,52)-(31,63) 4x12
        generateFaceVoxels(skin, voxels, 28, 52, 4, 12,
                ox + sx,   oy + sy, oz + sz,
                -pux, 0,   0,
                0,   -puy, 0,
                0,    0,   1);

        // Up (top): UV (20,48)-(23,51) 4x4
        generateFaceVoxels(skin, voxels, 20, 48, 4, 4,
                ox,        oy + sy, oz,
                pux,  0,   0,
                0,    0,   ptz,
                0,    1,   0);

        // Down (bottom): UV (24,48)-(27,51) 4x4
        generateFaceVoxels(skin, voxels, 24, 48, 4, 4,
                ox,        oy,      oz + sz,
                pux,  0,   0,
                0,    0,  -ptz,
                0,   -1,   0);

        // East (right): UV (16,52)-(19,63) 4x12
        float pex = sz / 4f;
        generateFaceVoxels(skin, voxels, 16, 52, 4, 12,
                ox + sx,   oy + sy, oz,
                0,    0,  -pex,
                0,   -puy, 0,
                1,    0,   0);

        // West (left): UV (24,52)-(27,63) 4x12
        generateFaceVoxels(skin, voxels, 24, 52, 4, 12,
                ox,        oy + sy, oz + sz,
                0,    0,   pex,
                0,   -puy, 0,
                -1,   0,   0);

        return voxels;
    }

    // ==================== Core face voxel generation ====================

    /**
     * Generate voxels for one face of a body part.
     *
     * @param skin     the skin texture image
     * @param out      list to append voxels to
     * @param uvU      UV origin X in the texture
     * @param uvV      UV origin Y in the texture
     * @param uvW      UV width in pixels
     * @param uvH      UV height in pixels
     * @param ox       model-space X at UV origin (top-left of the face)
     * @param oy       model-space Y at UV origin
     * @param oz       model-space Z at UV origin
     * @param dux,dduy,duz  model-space step vector per U pixel
     * @param dvx,dvy,dvz  model-space step vector per V pixel
     * @param nx,ny,nz  face outward normal
     */
    private static void generateFaceVoxels(NativeImage skin, List<VoxelData> out,
                                           int uvU, int uvV, int uvW, int uvH,
                                           float ox, float oy, float oz,
                                           float dux, float duy, float duz,
                                           float dvx, float dvy, float dvz,
                                           float nx, float ny, float nz) {

        float halfUx = dux * 0.5f;
        float halfUy = duy * 0.5f;
        float halfUz = duz * 0.5f;
        float halfVx = dvx * 0.5f;
        float halfVy = dvy * 0.5f;
        float halfVz = dvz * 0.5f;

        for (int vi = 0; vi < uvH; vi++) {
            for (int ui = 0; ui < uvW; ui++) {
                int tx = uvU + ui;
                int ty = uvV + vi;

                if (tx < 0 || tx >= skin.getWidth() || ty < 0 || ty >= skin.getHeight())
                    continue;

                int argb = skin.getPixelRGBA(tx, ty);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 8)
                    continue;

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Center of this pixel on the face surface
                float faceX = ox + ui * dux + vi * dvx + halfUx + halfVx;
                float faceY = oy + ui * duy + vi * dvy + halfUy + halfVy;
                float faceZ = oz + ui * duz + vi * dvz + halfUz + halfVz;

                // Extrude along normal based on alpha
                float alphaNorm = alpha / 255f;
                float extrude = BASE_EXTRUDE + alphaNorm * MAX_EXTRUDE_BONUS;
                float voxelX = faceX + nx * extrude;
                float voxelY = faceY + ny * extrude;
                float voxelZ = faceZ + nz * extrude;

                // Voxel size: proportional to pixel coverage
                float vScale = VOXEL_SCALE * (0.7f + 0.3f * alphaNorm);

                float uLen = (float) Math.sqrt(dux * dux + duy * duy + duz * duz);
                float vLen = (float) Math.sqrt(dvx * dvx + dvy * dvy + dvz * dvz);

                float vW = uLen * vScale;
                float vH = vLen * vScale;
                float vD = vScale * 0.5f;

                if (vW < 0.08f) vW = 0.08f;
                if (vH < 0.08f) vH = 0.08f;
                if (vD < 0.08f) vD = 0.08f;

                int color = (r << 16) | (g << 8) | b;

                out.add(new VoxelData(voxelX, voxelY, voxelZ, vW, vH, vD, color, alpha));
            }
        }
    }

    // ==================== Voxel data class ====================

    public static class VoxelData {
        public final float x, y, z;
        public final float w, h, d;
        public final int color;
        public final int alpha;

        VoxelData(float x, float y, float z, float w, float h, float d, int color, int alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.color = color;
            this.alpha = alpha;
        }
    }
}