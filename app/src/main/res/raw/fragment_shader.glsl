#version 100
precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D uTexture;
uniform samplerExternalOES uExternalTexture;

// Color arrays - support up to 8 color pairs
uniform vec3 uTargets[8];
uniform vec3 uReplacements[8];
uniform float uTolerances[8];
uniform int uActiveCount;
uniform bool uUseExternalTexture;
uniform bool uShowMask;
uniform bool uUseHSV;
uniform bool uEdgeSmooth;

// Convert RGB to HSV
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// Convert HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Calculate hue distance (circular)
float hueDistance(float h1, float h2) {
    float diff = abs(h1 - h2);
    return min(diff, 1.0 - diff);
}

// Calculate color distance in RGB space
float rgbDistance(vec3 c1, vec3 c2) {
    return distance(c1, c2) / sqrt(3.0);
}

// Calculate color distance in HSV space
float hsvDistance(vec3 hsv1, vec3 hsv2) {
    float hueDist = hueDistance(hsv1.x, hsv2.x);
    float satDist = abs(hsv1.y - hsv2.y);
    float valDist = abs(hsv1.z - hsv2.z);
    return (hueDist * 2.0 + satDist + valDist) / 4.0;
}

void main() {
    vec4 currentColor;
    
    // Sample from appropriate texture
    if (uUseExternalTexture) {
        currentColor = texture2D(uExternalTexture, vTextureCoord);
    } else {
        currentColor = texture2D(uTexture, vTextureCoord);
    }
    
    // Skip transparent pixels
    if (currentColor.a < 0.01) {
        discard;
    }
    
    vec3 currentRGB = currentColor.rgb;
    vec3 currentHSV = rgb2hsv(currentRGB);
    
    int bestMatch = -1;
    float minDiff = 1.0;
    float bestTolerance = 1.0;
    
    // Iterate through all active color pairs
    for (int i = 0; i < 8; i++) {
        if (i >= uActiveCount) break;
        
        float diff;
        if (uUseHSV) {
            vec3 targetHSV = rgb2hsv(uTargets[i]);
            diff = hsvDistance(currentHSV, targetHSV);
        } else {
            diff = rgbDistance(currentRGB, uTargets[i]);
        }
        
        // Check if within tolerance and better than current best
        if (diff < uTolerances[i] && diff < minDiff) {
            minDiff = diff;
            bestMatch = i;
            bestTolerance = uTolerances[i];
        }
    }
    
    // If no match found, discard (transparent)
    if (bestMatch < 0) {
        discard;
    }
    
    // Calculate blend factor for edge smoothing
    float factor = 1.0;
    if (uEdgeSmooth && bestTolerance > 0.0) {
        factor = 1.0 - (minDiff / bestTolerance);
        factor = smoothstep(0.0, 1.0, factor);
    }
    
    vec3 finalColor;
    if (uShowMask) {
        // Mask mode: show white for replaced areas
        finalColor = vec3(factor);
    } else {
        // Normal mode: blend with replacement color
        finalColor = mix(currentRGB, uReplacements[bestMatch], factor);
    }
    
    gl_FragColor = vec4(finalColor, currentColor.a * factor);
}
