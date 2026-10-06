-- Viewpoint Performance & iGPU Optimization Patch
-- Build 42 Client Helper

local function onGameStart()
    print("[ViewpointOptimizationPatch] v2.0.0 Universal GPU & Adaptive Performance Patch active.")
    print("[ViewpointOptimizationPatch] Strict GPU-completion retirement, hardware capability profiling, adaptive budget controller, safe texture baking.")
end

Events.OnGameStart.Add(onGameStart)
