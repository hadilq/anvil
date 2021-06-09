package com.squareup.anvil.sample.pet

import com.squareup.anvil.sample.God
import dagger.MapKey

@MapKey
annotation class PetKey(val value: God)
