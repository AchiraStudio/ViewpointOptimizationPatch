================================================================================
VIEWPOINT UNIFIED PERFORMANCE & ENGINE OPTIMIZATION PATCH (v1.1.0)
For Project Zomboid Build 42 (Build 42.21+)
================================================================================

WHAT THIS BUNDLED PATCH DOES:
-----------------------------
1. Strict GPU-Completion Retirement (Safe Fence Drain):
   - Replaced Viewpoint's full-pipeline glFinish() lockups and eliminated unsafe
     frame-count-based evictions.
   - Resources are only freed after verified GPU completion (GL_ALREADY_SIGNALED
     or GL_CONDITION_SATISFIED).
   - Uses adaptive microsecond backpressure on oldest fences during queue growth,
     avoiding pipeline flushes while guaranteeing zero GPU use-after-free.
   - Self-protecting fallback: if sync fences are unsupported or fail, safely
     reverts to engine synchronization with diagnostic logging.

2. Comprehensive GPU Capability Profiling:
   - Probes active context on the render thread (Vendor, Renderer, GL version, limits, VRAM).
   - Accurately classifies hardware tiers (LOW_POWER, INTEGRATED, MAINSTREAM, HIGH_END)
     without brittle substring heuristics.
   - Defaults unknown hardware to safe, non-destructive behavior.

3. Resampling Overhead Reduction:
   - Replaces heavy 36-tap Lanczos floor baking filters with high-speed Linear filtering
     on performance profiles, preventing stutter during chunk streaming without mid-draw
     memory reallocations.

4. 100% Integrity & ViewpointTurbo Compatibility:
   - Does NOT modify Viewpoint.jar directly.
   - Preserves Viewpoint.jar's original SHA-256 hash so ViewpointTurbo's integrity
     check passes and all uniform caching optimizations remain fully active.
   - Instruments classes at runtime through ZombieBuddy's @Patch / ByteBuddy engine.

HOW TO UPLOAD TO STEAM WORKSHOP:
--------------------------------
1. Launch Project Zomboid.
2. In the Main Menu, click "Workshop".
3. Click "Workshop Items".
4. Select "Viewpoint - Performance & Engine Optimization Patch".
5. Click "Upload to Steam Workshop" (or "Submit").
6. Steam will publish the mod under your account!

HOW TO INSTALL MANUALLY (OFFLINE / FOR FRIENDS):
------------------------------------------------
1. Close Project Zomboid.
2. Run "INSTALL_PATCH.bat".
3. Select your hardware profile (Option 1 for iGPU, Option 2 for Dedicated GPU).
4. Launch the game and enjoy smooth first-person gameplay!

HOW TO UNINSTALL:
-----------------
1. Close Project Zomboid.
2. Run "RESTORE_ORIGINAL.bat".
================================================================================
