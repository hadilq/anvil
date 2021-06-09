package com.squareup.anvil.sample.pet

import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.sample.God.HEPHAESTUS
import com.squareup.anvil.sample.God.HERA
import com.squareup.anvil.sample.God.ZEUS
import com.squareup.scopes.AppScope
import javax.inject.Inject

interface Pet {
  fun pet(): String
}

@ContributesMultibinding(AppScope::class)
@PetKey(ZEUS)
class ZeusPet @Inject constructor() : Pet {
  override fun pet(): String = "Doggy"
}

@ContributesMultibinding(AppScope::class)
@PetKey(HERA)
class HeraPet @Inject constructor() : Pet {
  override fun pet(): String = "Catty"
}

@ContributesMultibinding(AppScope::class)
@PetKey(HEPHAESTUS)
class HEPHAESTUSPet @Inject constructor() : Pet {
  override fun pet(): String = "Moussy"
}
