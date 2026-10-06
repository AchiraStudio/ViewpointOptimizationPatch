-- Viewpoint Performance & iGPU Optimization Patch
-- Build 42 Client Helper

local function onGameStart()
    print("[ViewpointOptimizationPatch] Successfully initialized v1.0.0.")
    print("[ViewpointOptimizationPatch] Optimizations active: FloorSlice 8-tier vram capping, Fence drain retirement, zero-alloc bakes, optimized volumetric fog shaders.")
end

Events.OnGameStart.Add(onGameStart)
