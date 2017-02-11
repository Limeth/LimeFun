package cz.creeper.limefun.util;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.Utils;
import com.flowpowered.noise.module.source.Perlin;

public class BalancedPerlin extends Perlin {

    @Override
    public double getValue(double x, double y, double z) {
        double x1 = x;
        double y1 = y;
        double z1 = z;
        double value = 0.0;
        double signal;
        double curPersistence = 1.0;
        double nx, ny, nz;
        int seed;

        x1 *= getFrequency();
        y1 *= getFrequency();
        z1 *= getFrequency();

        for (int curOctave = 0; curOctave < getOctaveCount(); curOctave++) {

            // Make sure that these floating-point values have the same range as a 32-
            // bit integer so that we can pass them to the coherent-noise functions.
            nx = Utils.makeInt32Range(x1);
            ny = Utils.makeInt32Range(y1);
            nz = Utils.makeInt32Range(z1);

            // Get the coherent-noise value from the input value and add it to the
            // final result.
            seed = (getSeed() + curOctave);

            // We want a value in range <-1; 1> instead
            signal = Noise
                    .gradientCoherentNoise3D(nx, ny, nz, seed, getNoiseQuality()) * 2 - 1;
            value += signal * curPersistence;

            // Prepare the next octave.
            x1 *= getLacunarity();
            y1 *= getLacunarity();
            z1 *= getLacunarity();
            curPersistence *= getPersistence();
        }

        return value;
    }
}

