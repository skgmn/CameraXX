package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FocusMeteringState(
    parameters: FocusMeteringParameters = FocusMeteringParameters(),
    /**
     * @see [FocusMeteringState.meteringPoints]
     */
    meteringPoints: MeteringPoints = TapMeteringPoints()
) {
    internal val progressState = mutableStateOf(FocusMeteringProgress.Idle)

    var parameters by mutableStateOf(parameters)

    /**
     * Can be either of [MeteringPoints.Empty], [TapMeteringPoints], [ManualMeteringPoints].
     * - [MeteringPoints.Empty]: Focus/Metering is disabled.
     * - [TapMeteringPoints]: CameraPreview detect user's tap and automatically focus on there.
     *   Read [TapMeteringPoints.tapOffset] to get coordinates of tapped area.
     * - [ManualMeteringPoints]: CameraPreview will focus on [ManualMeteringPoints.offsets].
     */
    var meteringPoints by mutableStateOf(meteringPoints)

    val progress by progressState
}