package uz.ilhomvich.seamlessportals;

import net.minecraft.util.math.Vec3d;

/** Portal geometry. Facing index: 0=S(+z) 1=W(-x) 2=N(-z) 3=E(+x) (== EnumFacing horizontal index == yaw/90). */
public final class PortalMath {
    public static final double W = 2.0D;
    public static final double H = 3.0D;
    private static final int[] COS = {1, 0, -1, 0};
    private static final int[] SIN = {0, 1, 0, -1};

    private PortalMath() {}

    public static Vec3d normal(int idx) {
        switch (idx & 3) {
            case 0: return new Vec3d(0, 0, 1);
            case 1: return new Vec3d(-1, 0, 0);
            case 2: return new Vec3d(0, 0, -1);
            default: return new Vec3d(1, 0, 0);
        }
    }

    public static Vec3d tangent(int idx) {
        Vec3d n = normal(idx);
        return new Vec3d(n.z, 0, -n.x);
    }

    /** Quarter turns (yaw += q*90) that map direction -nA onto +nB. */
    public static int quarter(int idxA, int idxB) {
        return (idxB - idxA - 2) & 3;
    }

    public static double[] rot(double x, double z, int q) {
        q &= 3;
        return new double[]{x * COS[q] - z * SIN[q], x * SIN[q] + z * COS[q]};
    }

    /** Maps a point seen at portal A (base-bottom-centre a) to portal B (base-bottom-centre b). */
    public static Vec3d transform(Vec3d p, Vec3d a, Vec3d b, int q) {
        double[] r = rot(p.x - a.x, p.z - a.z, q);
        return new Vec3d(b.x + r[0], b.y + (p.y - a.y), b.z + r[1]);
    }

    /** Does segment a->b cross the portal rectangle (either direction)? */
    public static boolean crosses(Vec3d base, int idx, Vec3d a, Vec3d b) {
        Vec3d n = normal(idx);
        double s0 = n.dotProduct(a.subtract(base));
        double s1 = n.dotProduct(b.subtract(base));
        if (!((s0 > 0 && s1 <= 0) || (s0 < 0 && s1 >= 0))) return false;
        double t = s0 / (s0 - s1);
        Vec3d i = new Vec3d(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        Vec3d r = i.subtract(base);
        return Math.abs(r.dotProduct(tangent(idx))) <= W / 2 && r.y >= 0 && r.y <= H;
    }
}
