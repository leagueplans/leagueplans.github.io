package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item

final case class ItemStack(item: Item, noted: Boolean, quantity: Int)
