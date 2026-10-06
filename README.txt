================================================================================
VIEWPOINT UNIFIED PERFORMANCE & ENGINE OPTIMIZATION PATCH (v1.1.0)
For Project Zomboid Build 42 (Build 42.21+)
================================================================================

WHAT THIS BUNDLED PATCH DOES:
-----------------------------
1. Eliminates glFinish() GPU Pipeline Stalls:
   - Root cause of the Intel iGPU "100% GPU / freeze on pressing O" bug: Viewpoint's
     Retirement class calls GL11.glFinish() whenever the fence queue reaches 128
     or when glFenceSync returns 0.
   - This patch bounds the fence queue to 32 frames, reclaims older fences proactively,
     and uses a 4-frame deferred ring buffer fallback if driver sync creation fails.
   - Result: ZERO CPU/GPU stalls, NO freeze on pressing O.

2. Safe Render-Thread Hardware Detection & Auto-Tuning:
   - Probes the active GPU architecture on the Render Thread safely.
   - For Intel iGPUs (HD/UHD/Iris Xe) & AMD APUs: Automatically applies the Potato
     profile (Vanilla mode, 128MB floor VRAM, 8-block far LOD, Trilinear filter)
     so lower-end machines run smoothly at 60+ FPS immediately.
   - For Dedicated GPUs (RX 9060 XT, RTX series): Replaces heavy Lanczos floor
     filtering with high-speed Trilinear to eliminate micro-stutters during travel.

3. Resampling Overhead Reduction:
   - Transparently replaces 36-tap Lanczos floor baking filters with high-speed
     Bilinear filtering on performance profiles, preventing stutter during chunk streaming.

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
