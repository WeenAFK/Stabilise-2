package com.stabilise.world.gen.terrain;

import static com.stabilise.world.Region.REGION_SIZE_IN_TILES;
import static com.stabilise.world.tile.Tiles.air;
import static com.stabilise.world.tile.Tiles.lava;

import com.stabilise.util.maths.OctaveNoise;
import com.stabilise.world.Region;
import com.stabilise.world.WorldProvider;
import com.stabilise.world.gen.InstancedWorldgen;


public class CaveGen extends InstancedWorldgen {
    
    private final OctaveNoise caveNoise;
    private final OctaveNoise maskNoise;
    
    public CaveGen(WorldProvider w, long seed) {
        super(w, seed);
        
        long mix1 = 0xd74a9ad1417d79a0L;
        long mix2 = 0x7227bebc43323e77L;

        caveNoise = OctaveNoise.simplex(4, seed^mix1)
                .addOctave(128, 2)
                .addOctave(64,  8)
                .addOctave(32,  4)
                .addOctave(16,  1);
        maskNoise = OctaveNoise.simplex(1, seed^mix2)
                .addOctave(512, 1);
    }
    
    @Override
    public void generate(Region r) {
        int offsetX = r.x() * REGION_SIZE_IN_TILES;
        int offsetY = r.y() * REGION_SIZE_IN_TILES;
        
        for(int y = 0, ty = offsetY; y < REGION_SIZE_IN_TILES; y++, ty++) {
            for(int x = 0, tx = offsetX; x < REGION_SIZE_IN_TILES; x++, tx++) {
                tmpPos.set(tx, ty);
                
                float cave = caveNoise.noise(tx, ty);
                // This should produce varying cave types across the world as
                // the noise forms characteristically different contours at
                // different points between 0.25-0.75.
                float caveMask = 0.25f + transformCaveMask(maskNoise.noise(tx,ty))/2;
                
                // Multiply caveNoise or caveMask by 0 to 1 based on the depth
                // to try to deter a great multitude of surface cave entrances
                //if(noise >= 0D && noise <= 20D)
                //    caveMask *= Interpolation.QUADRATIC.easeIn(0.5f, 1, (float)noise / 20);
                
                //if((y < -200 && caveNoise > 0.8D) || (y < -180 && caveNoise > (0.8 - 0.2 * (180+y)/20f)))
                if(ty < -200 && cave > 0.8f)
                    set(tmpPos, lava);
                //else if(caveNoise > 0.45D && caveNoise < 0.55D)
                else if(cave > caveMask - 0.05f && cave < caveMask + 0.05f) {
                //else if(caveNoise > 0.8D)
                    w.setTileAt(tmpPos, air);
                }
            }
        }
    }
    
    /**
     * Transforms the noise portion of the cave mask using the function
     * <tt>f(x) = ((2x - 1)<font size=-1><sup>3</sup></font> + 1) / 2</tt>, so
     * that it tends toward a value of 0.5.
     * 
     * @param caveMask The value for the cave mask noise.
     * 
     * @return The noise transformed such that the value tends toward 0.5.
     */
    private float transformCaveMask(float caveMask) {
        //f(x) = ((2x - 1)^3 + 1) / 2
        caveMask = 2*caveMask - 1;
        caveMask *= caveMask*caveMask;
        return (caveMask+1)/2;
    }
    
}
