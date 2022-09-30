package com.alamin.facedetection

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class FaceData(val boundTop: Int,
                    val boundBottom: Int,
                    val boundLeft: Int,
                    val boundRight:Int,
                    val leftEarPosition: @RawValue Position,
                    val rightEarPosition: @RawValue Position,
                    val leftEyePosition: @RawValue Position,
                    val rightEyePosition: @RawValue Position,
                    val nosePosition: @RawValue Position): Parcelable
