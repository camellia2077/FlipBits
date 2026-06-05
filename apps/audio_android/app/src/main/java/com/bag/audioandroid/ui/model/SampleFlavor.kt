package com.bag.audioandroid.ui.model

enum class SampleFlavor {
    SacredMachine,
    AncientDynasty,
    ImmortalRot,
    ScarletCarnage,
    ExquisiteFall,
    LabyrinthOfMutability,
}

fun effectiveSampleFlavor(
    themeStyle: ThemeStyleOption,
    factionTheme: FactionThemeOption,
): SampleFlavor =
    if (themeStyle == ThemeStyleOption.FactionTheme) {
        factionTheme.sampleFlavor
    } else {
        SampleFlavor.SacredMachine
    }
