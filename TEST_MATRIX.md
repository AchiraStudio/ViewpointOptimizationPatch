# GPU Test Matrix & Telemetry Verification Guide

This document defines the validation matrix for testing **ViewpointOptimizationPatch v2.0** across GPU hardware configurations on Project Zomboid Build 42.

---

## 1. Hardware Test Matrix

| Environment | GPU Architecture | Target Tier | Verified Sync Mode | Expected Default LOD (`farBlocks`) | Target Budgets (Bake / Upload) |
|---|---|---|---|---|---|
| **Intel Integrated** | Intel HD / UHD Graphics (620, 630, 770) | `LOW_POWER` | GL32 Fence / Fallback Guard | 8 | 1.0 ms / 1.0 MB |
| **Intel Mobile Performance** | Intel Iris Xe / Core Ultra Arc iGPU | `INTEGRATED` | GL32 Fence | 12 | 1.5 ms / 1.5 MB |
| **Intel Discrete** | Intel Arc A580, A750, A770, B580 | `MAINSTREAM` | GL32 Fence | 24 | 2.5 ms / 2.0 MB |
| **AMD APU** | Radeon Vega, 680M, 780M, 890M | `INTEGRATED` | GL32 Fence | 12 | 1.5 ms / 1.5 MB |
| **AMD Mainstream dGPU** | Radeon RX 580, RX 5500, RX 6600, RX 7600 | `MAINSTREAM` | GL32 Fence | 24 | 2.5 ms / 2.0 MB |
| **AMD Enthusiast dGPU** | Radeon RX 6800+, 7800+, 7900+, 9060 XT, 9070 | `HIGH_END` | GL32 Fence | 32 | 4.0 ms / 4.0 MB |
| **NVIDIA Mainstream** | GTX 1060/1650/1660, RTX 2060/3050/3060/4060 | `MAINSTREAM` | GL32 Fence | 24 | 2.5 ms / 2.0 MB |
| **NVIDIA Enthusiast** | RTX 3070+, 4070+, 4080, 4090, 50-series | `HIGH_END` | GL32 Fence | 32 | 4.0 ms / 4.0 MB |
| **Virtual / Unknown / Mesa** | llvmpipe, Microsoft Basic Render, Unrecognized | `SAFE` | Safe Fallback / Non-destructive | 12 | 1.5 ms / 1.0 MB |

---

## 2. In-Game Telemetry Verification

During gameplay, the patch logs telemetry to the console and game log (`console.txt`) every 30 seconds:

```text
[ViewpointPatch] ======================= TELEMETRY REPORT =======================
[ViewpointPatch]   Frame Pacing:   ~59.8 FPS (Avg: 16.71 ms | Peak: 22.4 ms | Total Frames: 1800)
[ViewpointPatch]   GPU Retirement: Queue: 4 (Peak: 49, Avg: 5.2) | Cleared: 1782 | Soft-Waits: 14 | WaitTime: 720 us
[ViewpointPatch]   Adaptive State: Level: 0 | Far LOD: 24 blocks | Bake: 2.50 ms | Upload: 2.00 MB
[ViewpointPatch] =================================================================
```

### Metrics Checklist:
1. **Queue Depth**: Should stay between 1 and 16 under normal rendering. Under chunk streaming spikes, may approach soft cap (48).
2. **Soft-Waits & Wait Time**: Soft cap waits use 50–200 µs budgets. Total wait time should remain a tiny fraction of total render time.
3. **Adaptive State**:
   - `Level 0` (Nominal): Stable 60+ FPS.
   - `Level 1` (Light): Activated during minor frame time spikes (>22.5 ms) or queue buildup (>28).
   - `Level 2-3` (Moderate/Heavy): Activated under severe load to eliminate stutters and protect frame delivery.
   - Recovers back to `Level 0` after 120 consecutive smooth frames with rolling average below 15.0 ms.

---

## 3. Stress Testing Scenarios

1. **First-Person Transition (O Key)**:
   - Rapidly toggle in and out of Viewpoint mode.
   - Verify zero fence creation failures, zero pipeline stalls, and no 100% iGPU freeze.
2. **High-Speed Vehicle Driving**:
   - Drive through dense forest or downtown West Point.
   - Verify floor baking uses Linear/Trilinear filtering and cell upload budgets dynamically throttle without hitching.
3. **Mega-Horde Density**:
   - Spawn or engage 200+ zombies.
   - Observe adaptive level automatically step to Level 1 or 2 to preserve frame pacing.

