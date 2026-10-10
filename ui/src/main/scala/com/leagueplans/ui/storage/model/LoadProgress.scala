package com.leagueplans.ui.storage.model

/** How far through reading a plan's steps the storage worker is */
final case class LoadProgress(stepsRead: Int, totalSteps: Int)
